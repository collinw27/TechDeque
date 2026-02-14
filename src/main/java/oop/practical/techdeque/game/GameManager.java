package oop.practical.techdeque.game;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import oop.practical.techdeque.combat.CombatManager;
import oop.practical.techdeque.deck.Card;
import oop.practical.techdeque.deck.Deck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameManager {

    Deck playerDeckDefault = new Deck(new ArrayList<>());
    Deck enemyDeckDefault = new Deck(new ArrayList<>());

    public List<Object> loop()
    {
        // See https://argparse4j.github.io for documentation
        var parser = ArgumentParsers.newFor("").build();
        var subparsers = parser.addSubparsers().dest("command");
        var card = subparsers.addParser("card");
        card.addArgument("spec").type(String.class);
        card.addArgument("--war").type(Boolean.class);
        var playerDeck = subparsers.addParser("playerDeck");
        var enemyDeck = subparsers.addParser("enemyDeck");
        playerDeck.addArgument("cards").type(String.class).nargs("*");
        enemyDeck.addArgument("cards").type(String.class).nargs("*");
        var combatParser = subparsers.addParser("combat");
        combatParser.addArgument("--war").action(Arguments.storeTrue());
        var testParser = subparsers.addParser("test");

        System.out.println("Welcome to TechDeque! Enter -h for help.");
        var results = new ArrayList<>();
        while (Input.SCANNER.hasNext()) {
            try {
                var args = Input.prompt(parser, "");
                var result = switch (args.getString("command")) {
                    case "card" -> card(args.get("spec"));
                    case "playerDeck" -> playerDeck(args.get("cards"));
                    case "enemyDeck" -> enemyDeck(args.get("cards"));
                    case "combat" -> startCombat(args.get("war"));
                    case "test" -> startTest();
                    default -> throw new AssertionError(args.getString("command"));
                };
                results.add(result);
            } catch (RuntimeException e) {
                System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                results.add(e);
            }
        }
        return results;
    }

    private Card card(String spec)
    {
        if (spec.isEmpty())
            throw new IllegalArgumentException("Empty card spec");

        Matcher matcher = Pattern.compile("([A-Z][a-z]*)-([IV]+)").matcher(spec);
        Matcher bonusMatcher = Pattern.compile("([a-z]+)([0-9])").matcher(spec);

        // Case 1: (Type)-(Roman Numeral)

        if (matcher.find())
        {
            Card.Type cardType = Card.parseType(matcher.group(1));
            Integer cardRank = Card.parseRank(matcher.group(2));
            return new Card(cardType, cardRank);
        }

        // Case 2 (bonus): (type)(number)

        else if (bonusMatcher.find())
        {
            Card.Type cardType = Card.parseType(bonusMatcher.group(1));
            Integer cardRank;
            try
            {
                cardRank = Integer.parseInt(bonusMatcher.group(2));
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Invalid card rank: " + bonusMatcher.group(2));
            }
            if (!Card.isValidRank(cardRank))
                throw new IllegalArgumentException("Invalid card rank: " + bonusMatcher.group(2));
            return new Card(cardType, cardRank);
        }

        // Otherwise invalid

        else
        {
            throw new IllegalArgumentException("Invalid card: " + spec);
        }
    }

    private Deck generateDeck(List<String> cards)
    {
        ArrayList<Card> cardList = new ArrayList<>();
        for (String card : cards)
        {
            cardList.add(card(card));
        }
        return new Deck(cardList);
    }

    // These methods might be redundant, but I'm keeping them here
    // in case they're useful later

    private Deck playerDeck(List<String> cards)
    {
        playerDeckDefault = generateDeck(cards);
        return playerDeckDefault;
    }

    private Deck enemyDeck(List<String> cards)
    {
        enemyDeckDefault = generateDeck(cards);
        return enemyDeckDefault;
    }

    private String startCombat(Boolean warMode)
    {
        CombatManager combat = new CombatManager(playerDeckDefault, enemyDeckDefault, warMode);
        return combat.start();
    }

    private String startTest()
    {
        playerDeck(new ArrayList<String>(Arrays.asList(
            "grass4", "water2", "fire3"
        )));
        enemyDeck(new ArrayList<String>(Arrays.asList(
            "water1", "fire1", "grass1", "water1", "fire1", "grass1", "water1", "fire1", "grass2"
        )));
        return startCombat(false);
    }
}
