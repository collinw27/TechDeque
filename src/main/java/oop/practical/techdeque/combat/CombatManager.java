package oop.practical.techdeque.combat;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import oop.practical.techdeque.deck.Card;
import oop.practical.techdeque.deck.Deck;
import oop.practical.techdeque.game.Input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public final class CombatManager
{
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

    // Player and enemy state are both stored in CombatEntity instance

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

    private class CombatEntity
    {
        public Entity type;
        public Deck deck;
        public ArrayList<Card> hand = new ArrayList<>();
        public int score = 0;
        public int HP = 10;
        // HP defaults to 10, and can be modified manually
        public Optional<Card> shield = Optional.empty();
        public Optional<Card> card = Optional.empty();

        CombatEntity(Entity type, Deck deck)
        {
            this.type = type;
            this.deck = deck;
        }
    }

    CombatEntity player;
    CombatEntity enemy;
    private HashMap<Entity, CombatEntity> combatants = new HashMap<>();

    // Tidy container for passing between combat functions

    private record RoundResult(
        Entity target,
        int damage
    ) {}

    // Allows the world map to get more detail about the results

    public record CombatResult(
        boolean playerWon,
        int playerResult,
        String resultString
    ) {}

    public CombatManager(Deck playerDeck, Deck enemyDeck, Mode gameMode)
    {
        doAutoDraw = (gameMode != Mode.NORMAL);
        doPoints = (gameMode == Mode.WAR_POINTS);

        player = new CombatEntity(Entity.PLAYER, playerDeck.copy());
        enemy = new CombatEntity(Entity.ENEMY, enemyDeck.copy());
        combatants = new HashMap<>(Map.of(Entity.PLAYER, player, Entity.ENEMY, enemy));

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
            player.HP = playerHP;
            enemy.HP = enemyHP;
        }
    }

    public CombatResult start()
    {
        while (true)
        {
            // Automatically draw (dependent on mode)

            player.card = doAutoDraw ? drawPlayerTop() : drawPlayerCard();
            enemy.card = doAutoDraw ? drawEnemyTop() : drawEnemyCard();

            // AUTODRAW: Terminate if neither drew a card, winner has most score/HP
            // !AUTODRAW: Reshuffle if empty

            if (doAutoDraw && player.card.isEmpty() && enemy.card.isEmpty())
            {
                System.out.printf("Final: %s-%s%n", player.score, enemy.score);
                return new CombatResult(player.score > enemy.score, player.HP, "%s-%s".formatted(player.score, enemy.score));
            }
            else if (!doAutoDraw)
            {
                for (CombatEntity entity : combatants.values())
                {
                    if (entity.deck.isEmpty())
                        entity.deck.reshuffle();
                }
            }

            // Equip shield before any cards are played
            // If a shield is equipped this turn, the played card
            // will technically be a null card

            attemptEquip(player);
            attemptEquip(enemy);

            // Play the cards against each other
            // The loser gets a chance to use their shield

            RoundResult result = playCards(player.card, enemy.card);
            applyResult(player.card, enemy.card, result);

            // !POINTS: End game when out of health

            if (enemy.HP <= 0 || player.HP <= 0)
            {
                if (enemy.HP <= 0 && player.HP <= 0)
                    System.out.println("It's a draw!");
                else if (enemy.HP <= 0)
                    System.out.printf("You win %s-%s!\n", player.HP, enemy.HP);
                else
                    System.out.printf("You lost %s-%s!\n", player.HP, enemy.HP);
                return new CombatResult(player.HP > 0, player.HP, "%s-%s".formatted(player.HP, enemy.HP));
            }
        }
    }

    // Simple methods used in war mode

    private Optional<Card> drawPlayerTop()
    {
        return player.deck.drawTop();
    }

    private Optional<Card> drawEnemyTop()
    {
        return enemy.deck.drawTop();
    }

    // Used for letting a player choose within their current hand

    private Optional<Card> drawPlayerCard()
    {
        // Fill remaning hand with 3 cards, if possible

        Optional<Card> playerCard = Optional.empty();
        player.hand.addAll(player.deck.drawCards(3 - player.hand.size()));

        // Choose card 1-3
        // Optionally discard unless there is one remaining

        if (!player.hand.isEmpty())
        {
            while (true)
            {
                // Print message

                System.out.print("You drew");
                for (int i = 0; i < player.hand.size(); i++)
                {
                    System.out.printf(String.format(" %s) %s", i + 1, player.hand.get(i)));
                }
                System.out.println();

                // Use Input to validate user's choice
                // If out of range, error instead of reprompting

                var result = Input.prompt(actionParser, "select/discard (1-%s): ".formatted(player.hand.size()));
                if (result.getString("command").equals("select"))
                {
                    int cardIndex = result.getInt("num") - 1;
                    if (cardIndex < player.hand.size())
                    {
                        playerCard = Optional.of(player.hand.remove(cardIndex));
                        break;
                    }
                    else
                        throw new IllegalArgumentException("Out of range");
                }
                else if (result.getString("command").equals("discard"))
                {
                    int cardIndex = result.getInt("num") - 1;
                    if (player.hand.size() == 1)
                        throw new IllegalArgumentException("Cannot discard only card");
                    else if (cardIndex < player.hand.size())
                        player.hand.remove(cardIndex);
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
    // Thus, the enemy hand isn't utilized

    private Optional<Card> drawEnemyCard()
    {
        Optional<Card> enemyCard = Optional.empty();
        ArrayList<Card> enemyHand = enemy.deck.drawCards(3);

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

    private void attemptEquip(CombatEntity entity)
    {
        if (entity.card.isPresent() && entity.card.get().isSpecialty(Card.Specialty.SHIELD))
        {
            System.out.print(entity.type == Entity.PLAYER ? "You " : "The enemy ");
            System.out.printf("equipped %s", entity.card.get());
            if (entity.shield.isPresent())
                System.out.printf(", replacing %s.\n", entity.shield.get());
            else
                System.out.println(".");
            entity.shield = Optional.of(entity.card.get());
        }
    }

    private RoundResult playCards(Optional<Card> playerChoice, Optional<Card> enemyChoice)
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

        return new RoundResult(target, damage);
    }

    private void applyResult(Optional<Card> playerChoice, Optional<Card> enemyChoice, RoundResult result)
    {
        // Modify the score/HP, depending on mode

        if (doPoints)
        {
            if (result.target == Entity.PLAYER || result.target == Entity.BOTH)
                enemy.score += result.damage;
            if (result.target == Entity.ENEMY || result.target == Entity.BOTH)
                player.score += result.damage;
        }
        else
        {
            if (result.target == Entity.PLAYER || result.target == Entity.BOTH)
                player.HP = max(player.HP - result.damage, 0);
            if (result.target == Entity.ENEMY || result.target == Entity.BOTH)
                enemy.HP = max(enemy.HP - result.damage, 0);
        }

        // Now for the fun part, print the appropriate message

        String actionString = "";
        if (result.target == Entity.NONE)
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

            String pointsStr = String.format("%s point%s", result.damage, (result.damage == 1) ? "" : "s");
            actionString += switch (result.target)
            {
                case Entity.ENEMY -> (doPoints)
                        ? String.format("scoring %s!", pointsStr)
                        : String.format("dealing %s damage!", result.damage);
                case Entity.PLAYER -> (doPoints)
                        ? String.format("enemy scores %s!", pointsStr)
                        : String.format("taking %s damage!", result.damage);
                case Entity.BOTH -> (doPoints)
                        ? String.format("everybody scores %s!", pointsStr)
                        : String.format("everybody takes %s damage!", result.damage);
                default -> throw new UnsupportedOperationException("Invalid target");
            };
        }

        if (doPoints)
            System.out.printf("%s (%s, %s)\n", actionString, player.score, enemy.score);
        else
            System.out.printf("%s (%s, %s)\n", actionString, player.HP, enemy.HP);
    }
}
