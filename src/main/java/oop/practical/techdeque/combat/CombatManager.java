package oop.practical.techdeque.combat;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
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

    private enum Entity { PLAYER, ENEMY, NONE, BOTH };

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

    private void playCards(Optional<Card> playerCard, Optional<Card> enemyCard)
    {
        // Start by working out how much damage was dealt, and to whom

        int damage = 0;
        Entity target = Entity.NONE;

        // Both reshuffled: 1 damage each

        if (enemyCard.isEmpty() && playerCard.isEmpty())
        {
            damage = 1;
            target = Entity.BOTH;
        }

        // One entity reshuffled: 1 + RANK damage

        else if (enemyCard.isEmpty())
        {
            damage = 1 + playerCard.get().rank();
            target = Entity.ENEMY;
        }
        else if (playerCard.isEmpty())
        {
            damage = 1 + enemyCard.get().rank();
            target = Entity.PLAYER;
        }

        // Otherwise, work out damage based on type/rank differential

        else
        {
            int typeDifference = playerCard.get().compareType(enemyCard.get());
            int rankDifference = playerCard.get().compareRank(enemyCard.get());

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
            }
        }

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
            if (playerCard.isEmpty())
                actionString += (warMode) ? "You played nothing " : "You reshuffled ";
            else
                actionString += String.format("You played %s ", playerCard.get());

            if (enemyCard.isEmpty())
                actionString += (warMode) ? "vs nothing, " : "while the enemy reshuffled, ";
            else
                actionString += String.format("vs %s, ", enemyCard.get());

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
