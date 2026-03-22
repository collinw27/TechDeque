package oop.practical.techdeque.combat;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import oop.practical.techdeque.deck.Card;
import oop.practical.techdeque.deck.Deck;
import oop.practical.techdeque.game.Input;

import java.util.ArrayList;
import java.util.Optional;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public final class CombatManager
{
    private Deck playerDeck;
    private Deck enemyDeck;

    // As it existed before, war mode implied 2 things:
    // 1) automatic drawing & no reshuffle, 2) points instead of health
    // However, since war mode can now be played using health,
    // these things must be specified separately
    // This technically means you could play normal mode with points,
    // although no such option currently exists in the Mode enum

    public enum Mode { NORMAL, WAR_POINTS, WAR_HEALTH }
    private boolean doAutoDraw;
    private boolean doPoints;
    private ArgumentParser actionParser;

    // Entity state
    // Called `score` for generality, but can also represent HP as well
    // It's fine that we don't have a way of resetting these, since each
    // instance of this class should represent its own game

    private int playerScore = 0;
    private int enemyScore = 0;
    private ArrayList<Card> playerHand = new ArrayList<>();

    // For use later on

    private enum Entity {
        NONE, PLAYER, ENEMY, BOTH;

        public Entity opponent()
        {
            return switch (this)
            {
                case PLAYER -> ENEMY;
                case ENEMY -> PLAYER;
                default -> NONE;
            };
        }
    }

    // Allows the world map to get more detail about the results

    public record CombatResults(
        boolean playerWon,
        int playerResult,
        String resultString
    ) {}

    public CombatManager(Deck playerDeck, Deck enemyDeck, Mode gameMode)
    {
        this.playerDeck = playerDeck.copy();
        this.enemyDeck = enemyDeck.copy();
        this.doAutoDraw = (gameMode != Mode.NORMAL);
        this.doPoints = (gameMode == Mode.WAR_POINTS);

        // HP defaults to 10, and can be modified manually

        if (!doPoints)
        {
            playerScore = 10;
            enemyScore = 10;
        }

        // Easier to set up parser here instead of after starting

        actionParser = ArgumentParsers.newFor("").build();
        var subparsers = actionParser.addSubparsers().dest("command");
        var parserSelect = subparsers.addParser("select");
        parserSelect.addArgument("num").type(Integer.class).choices(Arguments.range(1, 3));
        var parserDiscard = subparsers.addParser("discard");
        parserDiscard.addArgument("num").type(Integer.class).choices(Arguments.range(1, 3));
    }

    // Use when NOT in points mode and need to manually set HP

    public void setDefaultHP(int playerHP, int enemyHP)
    {
        if (!doPoints)
        {
            playerScore = playerHP;
            enemyScore = enemyHP;
        }
    }

    public CombatResults start()
    {
        while (true)
        {
            // Automatically draw (dependent on mode)

            Optional<Card> playerCard = doAutoDraw ? drawPlayerTop() : drawPlayerCard();
            Optional<Card> enemyCard = doAutoDraw ? drawEnemyTop() : drawEnemyCard();

            // AUTODRAW: Terminate if neither drew a card, winner has most score/HP
            // !AUTODRAW: Reshuffle if empty

            if (doAutoDraw && playerCard.isEmpty() && enemyCard.isEmpty())
            {
                System.out.printf("Final: %s-%s%n", playerScore, enemyScore);
                return new CombatResults(playerScore > enemyScore, playerScore, "%s-%s".formatted(playerScore, enemyScore));
            }
            else if (!doAutoDraw)
            {
                if (playerCard.isEmpty())
                    playerDeck.reshuffle();
                if (enemyCard.isEmpty())
                    enemyDeck.reshuffle();
            }

            // Play the cards against each other
            // Abstracted to playCards() due to shared behavior with normal mode

            playCards(playerCard, enemyCard);

            // !POINTS: End game when out of health

            if (!doPoints && (enemyScore <= 0 || playerScore <= 0))
            {
                if (enemyScore <= 0 && playerScore <= 0)
                    System.out.println("It's a draw!");
                else if (enemyScore <= 0)
                    System.out.printf("You win %s-%s!\n", playerScore, enemyScore);
                else
                    System.out.printf("You lost %s-%s!\n", playerScore, enemyScore);
                return new CombatResults(playerScore > 0, playerScore, "%s-%s".formatted(playerScore, enemyScore));
            }
        }
    }

    // Simple methods used in war mode

    private Optional<Card> drawPlayerTop()
    {
        return playerDeck.drawTop();
    }

    private Optional<Card> drawEnemyTop()
    {
        return enemyDeck.drawTop();
    }

    // Used for letting a player choose within their current hand

    private Optional<Card> drawPlayerCard()
    {
        // Fill remaning hand with 3 cards, if possible

        Optional<Card> playerCard = Optional.empty();
        playerHand.addAll(playerDeck.drawCards(3 - playerHand.size()));

        // Choose card 1-3
        // Optionally discard unless there is one remaining

        if (!playerHand.isEmpty())
        {
            while (true)
            {
                // Print message

                System.out.print("You drew");
                for (int i = 0; i < playerHand.size(); i++)
                {
                    System.out.printf(String.format(" %s) %s", i + 1, playerHand.get(i)));
                }
                System.out.println();

                // Use Input to validate user's choice
                // If out of range, error instead of reprompting

                var result = Input.prompt(actionParser, "select/discard (1-%s): ".formatted(playerHand.size()));
                if (result.getString("command").equals("select"))
                {
                    int cardIndex = result.getInt("num") - 1;
                    if (cardIndex < playerHand.size())
                    {
                        playerCard = Optional.of(playerHand.remove(cardIndex));
                        break;
                    }
                    else
                        throw new IllegalArgumentException("Out of range");
                }
                else if (result.getString("command").equals("discard"))
                {
                    int cardIndex = result.getInt("num") - 1;
                    if (playerHand.size() == 1)
                        throw new IllegalArgumentException("Cannot discard only card");
                    else if (cardIndex < playerHand.size())
                        playerHand.remove(cardIndex);
                    else
                        throw new IllegalArgumentException("Out of range");
                }
                else
                    throw new AssertionError("Invalid command");
            }
        }
        return playerCard;
    }

    // For enemy drawing in normal mode, selects one card
    // and discards the remaining ones

    private Optional<Card> drawEnemyCard()
    {
        Optional<Card> enemyCard = Optional.empty();
        ArrayList<Card> enemyHand = enemyDeck.drawCards(3);

        // Enemy chooses highest-ranked card, with the
        // earliest card being used as a tiebreaker

        if (!enemyHand.isEmpty())
        {
            enemyCard = Optional.of(enemyHand.getFirst());
            int highestRank = enemyCard.get().rank();
            for (int i = 1; i < enemyHand.size(); i++)
            {
                if (enemyHand.get(i).rank() > highestRank)
                {
                    enemyCard = Optional.of(enemyHand.get(i));
                    highestRank = enemyCard.get().rank();
                }
            }
        }
        return enemyCard;
    }

    private void playCards(Optional<Card> playerChoice, Optional<Card> enemyChoice)
    {
        // Start by working out how much damage was dealt, and to whom
        // Do-While is used to allow `break`, reducing nested if-else

        int damage = 0;
        Entity target = Entity.NONE;

        do
        {
            // Both reshuffled: 1 damage each

            if (enemyChoice.isEmpty() && playerChoice.isEmpty())
            {
                damage = 1;
                target = Entity.BOTH;
                break;
            }

            // One entity reshuffled: 1 + RANK damage
            // Doesn't apply if other entity played a shield

            if (enemyChoice.isEmpty())
            {
                target = Entity.ENEMY;
                damage = 1 + playerChoice.get().rank();
                Optional<Card.Specialty> specialty = playerChoice.get().specialty();
                if (specialty.isPresent() && specialty.get().equals(Card.Specialty.SHIELD))
                    damage = 0;
                break;
            }
            else if (playerChoice.isEmpty())
            {
                target = Entity.PLAYER;
                damage = 1 + enemyChoice.get().rank();
                Optional<Card.Specialty> specialty = enemyChoice.get().specialty();
                if (specialty.isPresent() && specialty.get().equals(Card.Specialty.SHIELD))
                    damage = 0;
                break;
            }

            // Otherwise, work out damage based on specialty or type/rank differential

            Card playerCard = playerChoice.get();
            Card enemyCard = enemyChoice.get();
            int typeDifference = playerCard.compareType(enemyCard);
            int rankDifference = playerCard.compareRank(enemyCard);

            // Bonus for type difference
            // Keep the sign of typeDifference to match who takes damage
            // (positive = enemy takes damage, negative = player...)

            if (typeDifference != 0)
            {
                target = (typeDifference > 0) ? Entity.ENEMY : Entity.PLAYER;
                damage = 3 + abs(rankDifference);
            }
            else if (rankDifference != 0)
            {
                target = (rankDifference > 0) ? Entity.ENEMY : Entity.PLAYER;
                damage = abs(rankDifference);
            }
            else
            {
                target = Entity.NONE;
                break;
            }

            // Now that targets have been decided, work out if specialty cards
            // cancel out or add any behavior

            Card actorCard = (target == Entity.ENEMY) ? playerCard : enemyCard;
            Card targetCard = (target == Entity.PLAYER) ? playerCard : enemyCard;
            Optional<Card.Specialty> actorSpecialty = actorCard.specialty();
            Optional<Card.Specialty> targetSpecialty = targetCard.specialty();

            // The actor/target were decided as the result of type/rank differences
            // This eliminates the need for certain checks,
            // e.g. the actor will never be at a type disadvantage

            if (targetSpecialty.isPresent() && targetSpecialty.get().equals(Card.Specialty.SHIELD))
            {
                // Shield negation

                if (typeDifference == 0)
                    damage = 0;

                // Spear override (potential type advantage)

                if (actorSpecialty.isPresent() && actorSpecialty.get().equals(Card.Specialty.SPEAR))
                {
                    damage = 1 + abs(rankDifference);
                    if (typeDifference != 0)
                        damage += 3;
                }
            }
            if (actorSpecialty.isPresent() && actorSpecialty.get().equals(Card.Specialty.SHIELD))
            {
                // Shield no-op

                damage = 0;

                // Spear override (no type advantage)

                if (targetSpecialty.isPresent() && targetSpecialty.get().equals(Card.Specialty.SPEAR))
                {
                    damage = 1;
                    if (typeDifference == 0)
                        damage += abs(rankDifference);
                    target = target.opponent();
                }
            }
            if (actorSpecialty.isPresent() && actorSpecialty.get().equals(Card.Specialty.ULTIMATE))
            {
                // Ultimate attack

                if (typeDifference != 0 && targetCard.rank() <= 2)
                    damage += 1;

                // Same-type ultimate attack (also applies against shield)

                else if (typeDifference == 0)
                    damage = abs(rankDifference);
            }
            if (targetSpecialty.isPresent() && targetSpecialty.get().equals(Card.Specialty.ULTIMATE))
            {
                // Ultimate negation if rank <= 2

                if (actorCard.rank() <= 2)
                    damage = 0;
            }
        }
        while (false);

        // Modify the score/HP, depending on mode

        if (doPoints)
        {
            if (target == Entity.PLAYER || target == Entity.BOTH)
                enemyScore += damage;
            if (target == Entity.ENEMY || target == Entity.BOTH)
                playerScore += damage;
        }
        else
        {
            if (target == Entity.PLAYER || target == Entity.BOTH)
                playerScore = max(playerScore - damage, 0);
            if (target == Entity.ENEMY || target == Entity.BOTH)
                enemyScore = max(enemyScore - damage, 0);
        }

        // Now for the fun part, print the appropriate message

        String actionString = "";
        if (target == Entity.NONE)
            actionString = "It's a draw!";
        else
        {
            if (playerChoice.isEmpty())
                actionString += (doAutoDraw) ? "You played nothing " : "You reshuffled ";
            else
                actionString += String.format("You played %s ", playerChoice.get());

            if (enemyChoice.isEmpty())
                actionString += (doAutoDraw) ? "vs nothing, " : "while the enemy reshuffled, ";
            else
                actionString += String.format("vs %s, ", enemyChoice.get());

            String pointsStr = String.format("%s point%s", damage, (damage == 1) ? "" : "s");
            actionString += switch (target)
            {
                case Entity.ENEMY -> (doPoints)
                    ? String.format("scoring %s!", pointsStr)
                    : String.format("dealing %s damage!", damage);
                case Entity.PLAYER -> (doPoints)
                    ? String.format("enemy scores %s!", pointsStr)
                    : String.format("taking %s damage!", damage);
                case Entity.BOTH -> (doPoints)
                    ? String.format("everybody scores %s!", pointsStr)
                    : String.format("everybody takes %s damage!", damage);
                default -> throw new UnsupportedOperationException("Invalid target");
            };
        }

        System.out.printf("%s (%s, %s)\n", actionString, playerScore, enemyScore);
    }
}
