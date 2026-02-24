package oop.practical.techdeque.combat;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
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
    private boolean warMode;

    // Called `score` for generality, but represents HP in non-war mode

    private int playerScore = 0;
    private int enemyScore = 0;

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

    public CombatManager(Deck playerDeck, Deck enemyDeck, boolean warMode)
    {
        this.playerDeck = playerDeck.copy();
        this.enemyDeck = enemyDeck.copy();
        this.warMode = warMode;
    }

    public String start()
    {
        return (warMode ? startWar() : startNormal());
    }

    private String startWar()
    {
        while (true)
        {
            // Automatically draw

            Optional<Card> playerCard = playerDeck.drawTop();
            Optional<Card> enemyCard = enemyDeck.drawTop();

            // Terminate if neither drew a card (deck empty)

            if (playerCard.isEmpty() && enemyCard.isEmpty())
                break;

            // Play the cards against each other
            // Abstracted to playCards() due to shared behavior with normal mode

            playCards(playerCard, enemyCard);
        }

        System.out.printf("Final: %s-%s%n", playerScore, enemyScore);
        return String.format("%s-%s", playerScore, enemyScore);
    }

    private String startNormal()
    {
        var parser = ArgumentParsers.newFor("").build();
        parser.addArgument("num").type(Integer.class).choices(Arguments.range(1, 3));

        // Both start at 10 HP

        playerScore = 10;
        enemyScore = 10;

        while (true)
        {
            // Attempt to draw 3 cards, give player a choice

            Optional<Card> playerCard = Optional.empty();
            Optional<Card> enemyCard = Optional.empty();
            ArrayList<Card> playerChoices = playerDeck.drawCards(3);
            ArrayList<Card> enemyChoices = enemyDeck.drawCards(3);

            // Choose card 1-3
            // For edge case where fewer than 3 cards were drawn,
            // draw a null card if input is invalid

            if (!playerChoices.isEmpty())
            {
                System.out.printf("You drew");
                for (int i = 0; i < playerChoices.size(); i++)
                {
                    System.out.printf(String.format(" %s) %s", i + 1, playerChoices.get(i)));
                }
                System.out.println();
                int cardIndex = Input.prompt(parser, "select 1-3: ").get("num");
                cardIndex -= 1;
                if (cardIndex < playerChoices.size())
                    playerCard = Optional.of(playerChoices.get(cardIndex));
            }

            // Enemy chooses highest-ranked card, with the
            // earliest card being used as a tiebreaker

            if (!enemyChoices.isEmpty())
            {
                int highestRank = 1;
                enemyCard = Optional.of(enemyChoices.get(0));
                for (int i = 1; i < enemyChoices.size(); i++)
                {
                    if (enemyChoices.get(i).rank() > highestRank)
                    {
                        enemyCard = Optional.of(enemyChoices.get(i));
                        highestRank = enemyCard.get().rank();
                    }
                }
            }

            // Reshuffle here vs during block

            if (playerCard.isEmpty())
                playerDeck.reshuffle();
            if (enemyCard.isEmpty())
                enemyDeck.reshuffle();

            // Play the cards against each other
            // Abstracted to playCards() due to shared behavior with normal mode

            playCards(playerCard, enemyCard);

            // End game when out of health

            if (enemyScore <= 0 || playerScore <= 0)
            {
                if (enemyScore <= 0 && playerScore <= 0)
                    System.out.println(String.format("It's a draw!", playerScore, enemyScore));
                else if (enemyScore <= 0)
                    System.out.println(String.format("You win %s-%s!", playerScore, enemyScore));
                else
                    System.out.println(String.format("You lost %s-%s!", playerScore, enemyScore));
                return String.format("%s-%s", playerScore, enemyScore);
            }
        }
    }

    private void playCards(Optional<Card> playerChoice, Optional<Card> enemyChoice)
    {
        // Start by working out how much damage was dealt, and to whom
        // Do-While is used to allow `break`, reducing nested if-else

        int damage = 0;
        Entity target = Entity.NONE;

        // Both reshuffled: 1 damage each

        do
        {
            if (enemyChoice.isEmpty() && playerChoice.isEmpty())
            {
                damage = 1;
                target = Entity.BOTH;
                break;
            }

            // One entity reshuffled: 1 + RANK damage

            if (enemyChoice.isEmpty())
            {
                damage = 1 + playerChoice.get().rank();
                target = Entity.ENEMY;
                break;
            }
            else if (playerChoice.isEmpty())
            {
                damage = 1 + enemyChoice.get().rank();
                target = Entity.PLAYER;
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
                damage = (3 + abs(rankDifference));
            }
            else if (rankDifference != 0)
            {
                target = (rankDifference > 0) ? Entity.ENEMY : Entity.PLAYER;
                damage = rankDifference;
            }
            else
            {
                target = Entity.NONE;
                break;
            }

            // Now that targets have been decided, work out if specialty cards
            // cancel out any behavior

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
                    damage = 1;
                    if (typeDifference != 0)
                        damage += 3 + abs(rankDifference);
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
                    target = target.opponent();
                }
            }
            if (actorSpecialty.isPresent() && actorSpecialty.get().equals(Card.Specialty.ULTIMATE))
            {
                // Ultimate attack

                if (typeDifference != 0 && targetCard.rank() <= 2)
                    damage += 1;
            }
            if (targetSpecialty.isPresent() && targetSpecialty.get().equals(Card.Specialty.ULTIMATE))
            {
                // Ultimate negation (priority over offensive ultimate card)

                damage = 0;
            }
        }
        while (false);

        // Modify the score/HP, depending on mode

        if (warMode)
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
                actionString += (warMode) ? "You played nothing " : "You reshuffled ";
            else
                actionString += String.format("You played %s ", playerChoice.get());

            if (enemyChoice.isEmpty())
                actionString += (warMode) ? "vs nothing, " : "while the enemy reshuffled, ";
            else
                actionString += String.format("vs %s, ", enemyChoice.get());

            String pointsStr = String.format("%s point%s", damage, (damage == 1) ? "" : "s");
            actionString += switch (target)
            {
                case Entity.ENEMY -> (warMode)
                    ? String.format("scoring %s!", pointsStr)
                    : String.format("dealing %s damage!", damage);
                case Entity.PLAYER -> (warMode)
                    ? String.format("enemy scores %s!", pointsStr)
                    : String.format("taking %s damage!", damage);
                case Entity.BOTH -> (warMode)
                    ? String.format("everybody scores %s!", pointsStr)
                    : String.format("everybody takes %s damage!", damage);
                default -> throw new UnsupportedOperationException("Invalid target");
            };
        }

        System.out.printf("%s (%s, %s)\n", actionString, playerScore, enemyScore);
    }
}
