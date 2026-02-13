package oop.practical.techdeque.combat;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import oop.practical.techdeque.deck.Card;
import oop.practical.techdeque.deck.Deck;
import oop.practical.techdeque.game.Input;

import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public class CombatManager
{
    Deck playerDeck;
    Deck enemyDeck;

    public CombatManager(Deck playerDeck, Deck enemyDeck)
    {
        this.playerDeck = playerDeck.copy();
        this.enemyDeck = enemyDeck.copy();
    }

    public String startWar()
    {
        int playerScore = 0;
        int enemyScore = 0;

        while (true)
        {
            // Automatically draw

            Card playerCard = playerDeck.drawTop();
            Card enemyCard = enemyDeck.drawTop();

            // Terminate if neither drew a card (deck empty)

            if (playerCard == null && enemyCard == null)
                break;

            // Compare card values, with special cases for empty cards

            if (enemyCard == null)
            {
                int points = calculateDamage(playerCard, enemyCard);
                playerScore += points;
                System.out.println(String.format(
                    "You played %s vs nothing, scoring %s points! (%s, %s)",
                    playerCard, points, playerScore, enemyScore
                ));
            }
            else if (playerCard == null)
            {
                int points = -calculateDamage(playerCard, enemyCard);
                enemyScore += points;
                System.out.println(String.format(
                    "You played nothing vs %s, opponent scores %s points! (%s, %s)",
                    enemyCard, points, playerScore, enemyScore
                ));
            }
            else
            {
                int points = calculateDamage(playerCard, enemyCard);
                if (points == 0)
                {
                    System.out.println(String.format("It's a draw! (%s, %s)",
                        playerScore, enemyScore)
                    );
                }
                else if (points > 0)
                {
                    playerScore += points;
                    System.out.println(String.format(
                        "You played %s vs %s, scoring %s points! (%s, %s)",
                        playerCard, enemyCard, points, playerScore, enemyScore
                    ));
                }
                else
                {
                    enemyScore += -points;
                    System.out.println(String.format(
                        "You played %s vs %s, opponent scores %s points! (%s, %s)",
                        playerCard, enemyCard, -points, playerScore, enemyScore
                    ));
                }
            }
        }

        System.out.println(String.format("Final: %s-%s", playerScore, enemyScore));
        return String.format("%s-%s", playerScore, enemyScore);
    }

    public String startNormal()
    {
        int playerHP = 10;
        int enemyHP = 10;

        var parser = ArgumentParsers.newFor("").build();
        parser.addArgument("num").type(Integer.class).choices(Arguments.range(1, 3));

        while (true)
        {
            // Attempt to draw 3 cards, give player a choice

            Card playerCard = null;
            Card enemyCard = null;
            ArrayList<Card> playerChoices = playerDeck.drawCards(3);
            ArrayList<Card> enemyChoices = enemyDeck.drawCards(3);
            if (!enemyChoices.isEmpty())
                enemyCard = enemyChoices.get(0);

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
                    playerCard = playerChoices.get(cardIndex);
            }

            // Enemy chooses highest-ranked card, with the
            // earliest card being used as a tiebreaker

            if (!enemyChoices.isEmpty())
            {
                int highestRank = 1;
                enemyCard = enemyChoices.get(0);
                for (int i = 1; i < enemyChoices.size(); i++)
                {
                    if (enemyChoices.get(i).rank() > highestRank)
                    {
                        enemyCard = enemyChoices.get(i);
                        highestRank = enemyCard.rank();
                    }
                }
            }

            // Reshuffle here vs during block

            if (playerCard == null)
                playerDeck.reshuffle();
            if (enemyCard == null)
                enemyDeck.reshuffle();

            // Compare card values, with special cases for empty cards

            if (enemyCard == null && playerCard == null)
            {
                playerHP = max(playerHP - 1, 0);
                enemyHP = max(enemyHP - 1, 0);
                System.out.println(String.format(
                    "You and the enemy both reshuffled, taking 1 damage each! (%s, %s)",
                    playerHP, enemyHP
                ));
            }
            else if (enemyCard == null)
            {
                int points = calculateDamage(playerCard, enemyCard);
                enemyHP = max(enemyHP - points, 0);
                System.out.println(String.format(
                    "The enemy reshuffled vs %s, taking %s damage! (%s, %s)",
                    playerCard, points, playerHP, enemyHP
                ));
            }
            else if (playerCard == null)
            {
                int points = -calculateDamage(playerCard, enemyCard);
                playerHP = max(playerHP - points, 0);
                System.out.println(String.format(
                    "You reshuffled vs %s, taking %s damage! (%s, %s)",
                    enemyCard, points, playerHP, enemyHP
                ));
            }
            else
            {
                int points = calculateDamage(playerCard, enemyCard);
                if (points == 0)
                {
                    System.out.println(String.format("It's a draw! (%s, %s)",
                        playerHP, enemyHP)
                    );
                }
                else if (points > 0)
                {
                    enemyHP = max(enemyHP - points, 0);
                    System.out.println(String.format(
                        "You played %s vs %s, dealing %s damage! (%s, %s)",
                        playerCard, enemyCard, points, playerHP, enemyHP
                    ));
                }
                else
                {
                    playerHP = max(playerHP + points, 0); // points are negative her
                    System.out.println(String.format(
                        "You played %s vs %s, taking %s damage! (%s, %s)",
                        playerCard, enemyCard, -points, playerHP, enemyHP
                    ));
                }
            }

            // End game when out of health

            if (enemyHP <= 0 || playerHP <= 0)
            {
                if (enemyHP <= 0 && playerHP <= 0)
                    System.out.println(String.format("It's a tie!", playerHP, enemyHP));
                else if (enemyHP <= 0)
                    System.out.println(String.format("You win %s-%s!", playerHP, enemyHP));
                else
                    System.out.println(String.format("You lost %s-%s!", playerHP, enemyHP));
                return String.format("%s-%s", playerHP, enemyHP);
            }
        }
    }

    // If player wins, damage > 0
    // If enemy wins, damage < 0

    int calculateDamage(Card playerCard, Card enemyCard)
    {
        // If no card played, use 1 + rank

        if (enemyCard == null)
            return 1 + playerCard.rank();
        else if (playerCard == null)
            return -(1 + enemyCard.rank());

        // Otherwise, work out damage based on type/rank differential

        int typeDifference = playerCard.compareType(enemyCard);
        int rankDifference = playerCard.compareRank(enemyCard);
        int damage = 0;

        // Bonus for type difference
        // Keep the sign of typeDifference to match who takes damage

        if (typeDifference != 0)
            damage = (typeDifference > 0 ? 1 : -1) * (3 + abs(rankDifference));
        else
            damage = rankDifference;
        return damage;
    }
}
