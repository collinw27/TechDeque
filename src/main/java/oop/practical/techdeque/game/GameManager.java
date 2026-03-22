package oop.practical.techdeque.game;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import oop.practical.techdeque.combat.CombatManager;
import oop.practical.techdeque.deck.Card;
import oop.practical.techdeque.deck.Deck;
import oop.practical.techdeque.deck.EditableDeck;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameManager
{
    HashMap<String, EditableDeck> savedDecks = new HashMap<>();
    EditableDeck playerDeck = new EditableDeck();
    EditableDeck enemyDeck = new EditableDeck();
    boolean allowPrinting = true;

    public List<Object> loop()
    {
        // See https://argparse4j.github.io for documentation
        var parser = ArgumentParsers.newFor("").build();
        var subparsers = parser.addSubparsers().dest("command");

        var card = subparsers.addParser("card");
        card.addArgument("spec").type(String.class);

        var deck = subparsers.addParser("deck");
        var deckSubparsers = deck.addSubparsers().dest("subcommand");
        var deckView = deckSubparsers.addParser("view");
        deckView.addArgument("name").type(String.class).nargs("?");
        var deckEdit = deckSubparsers.addParser("edit");
        deckEdit.addArgument("name").type(String.class).nargs("?");
        var deckLoad = deckSubparsers.addParser("load");
        deckLoad.addArgument("name").type(String.class);
        var deckSave = deckSubparsers.addParser("save");
        deckSave.addArgument("name").type(String.class);

        var playerDeck = subparsers.addParser("playerDeck");
        playerDeck.addArgument("--equip").type(String.class);
        playerDeck.addArgument("cards").type(String.class).nargs("*");

        var enemyDeck = subparsers.addParser("enemyDeck");
        enemyDeck.addArgument("--equip").type(String.class);
        enemyDeck.addArgument("cards").type(String.class).nargs("*");

        var combat = subparsers.addParser("combat");
        combat.addArgument("--war").action(Arguments.storeTrue());
        subparsers.addParser("test");
        subparsers.addParser("exit");

        var play = subparsers.addParser("play");
        play.addArgument("--war").action(Arguments.storeTrue());

        print("Welcome to TechDeque! Enter -h for help.");
        return Input.loop(parser, args -> switch (args.getString("command")) {
            case "card" -> card(args.get("spec"));
            case "deck" -> switch (args.getString("subcommand")) {
                case "view" -> deckView(Optional.ofNullable(args.getString("name")));
                case "edit" -> deckEdit(Optional.ofNullable(args.getString("name")));
                case "load" -> deckLoad(args.getString("name"), true);
                case "save" -> deckSave(args.getString("name"));
                default -> throw new AssertionError(args.getString("subcommand"));
            };
            case "playerDeck" -> playerDeck(Optional.ofNullable(args.getString("equip")), args.get("cards"));
            case "enemyDeck" -> enemyDeck(Optional.ofNullable(args.getString("equip")), args.get("cards"));
            case "combat" -> combat(args.getBoolean("war"));
            case "play" -> play(args.getBoolean("war"));
            case "test" -> testCombat();
            case "exit" -> null;
            default -> throw new AssertionError(args.getString("command"));
        });
    }

    private Card card(String spec)
    {
        String pattern1 = "([A-Z][a-z]*)-([IV]+)";
        String pattern2 = "([A-Za-z]+)([0-9])";
        Matcher matcher = Pattern.compile(pattern1).matcher(spec);
        Matcher bonusMatcher = Pattern.compile(pattern2).matcher(spec);

        // Case 1: (Type)-(Roman Numeral)

        if (spec.matches(pattern1) && matcher.find())
        {
            Optional<Card.Type> cardType = Card.parseType(matcher.group(1));
            if (cardType.isEmpty())
                throw new IllegalArgumentException("Invalid card type: " + matcher.group(1));
            Optional<Integer> cardRank = Card.parseRank(matcher.group(2));
            if (cardRank.isEmpty())
                throw new IllegalArgumentException("Invalid card rank: " + matcher.group(2));
            return new Card(cardType.get(), cardRank.get());
        }

        // Case 2 (bonus): (type)(number)

        else if (spec.matches(pattern2) && bonusMatcher.find())
        {
            Optional<Card.Type> cardType = Card.parseType(bonusMatcher.group(1));
            if (cardType.isEmpty())
                throw new IllegalArgumentException("Invalid card type: " + bonusMatcher.group(1));
            int cardRank;
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
            return new Card(cardType.get(), cardRank);
        }

        // Case 3: (specialty name)

        else
        {
            Optional<Card.SpecialtyCard> specialty = Card.parseSpecialty(spec);
            if (specialty.isEmpty())
                throw new IllegalArgumentException("Invalid card: " + spec);
            return new Card(specialty.get());
        }
    }

    // Prints the result of a command to the terminal
    // Conditional: only applies if printing is enabled
    // This is used to prevent output when this class is being
    // used by the deckLoad() method

    private void print(String str)
    {
        if (allowPrinting)
            System.out.println(str);
    }

    private Object deckView(Optional<String> name)
    {
        EditableDeck selectedDeck;
        if (name.isEmpty())
            selectedDeck = playerDeck;
        else if (savedDecks.containsKey(name.get()))
            selectedDeck = savedDecks.get(name.get());
        else
            throw new IllegalArgumentException("Invalid deck: " + name.get());
        print("Contents of deck (%s cards total):".formatted(selectedDeck.getSize()));
        print(String.join("\n", selectedDeck.getStringList()));
        return selectedDeck;
    }

    private List<Object> deckEdit(Optional<String> name)
    {
        EditableDeck selectedDeck;

        // If no name provided, edit player's current deck

        if (name.isEmpty())
        {
            selectedDeck = playerDeck;
            print("Now editing current deck.");
        }

        // Otherwise, retrieve named deck, creating it if necessary
        // Name must be in PascalCase

        else
        {
            if (!name.get().matches("[A-Z][A-Za-z]*"))
                throw new IllegalArgumentException("Deck name must be capitalized and alphabetic.");
            if (!savedDecks.containsKey(name.get()))
                savedDecks.put(name.get(), new EditableDeck());
            selectedDeck = savedDecks.get(name.get());
            print("Now editing deck \"%s\".".formatted(name.get()));
        }

        var parser = ArgumentParsers.newFor("").build();
        var subparsers = parser.addSubparsers().dest("command");
        var clear = subparsers.addParser("clear");
        var add = subparsers.addParser("add");
        add.addArgument("card").type(String.class);
        add.addArgument("copies").type(Integer.class).nargs("?").setDefault(1);
        var remove = subparsers.addParser("remove");
        remove.addArgument("card").type(String.class);
        remove.addArgument("copies").type(Integer.class).nargs("?").setDefault(1);;
        var exit = subparsers.addParser("exit");

        List<Object> results = Input.loop(parser, args -> switch (args.getString("command")) {
            case "clear" -> deckEditClear(selectedDeck);
            case "add" -> deckEditAdd(selectedDeck, args.getString("card"), args.getInt("copies"));
            case "remove" -> deckEditRemove(selectedDeck, args.getString("card"), args.getInt("copies"));
            case "exit" -> null;
            default -> throw new AssertionError(args.getString("command"));
        });
        print("Exited deck editor.");
        return results;
    }

    private int deckEditClear(EditableDeck deck)
    {
        print("Cleared deck.");
        return deck.clear();
    }

    // 0 and negative indices are also valid, simply calls a different method

    private int deckEditAdd(EditableDeck deck, String cardString, int copies)
    {
        Card card = card(cardString);
        int result = 0;
        if (copies > 0)
            result = deck.addCard(card, copies, true);
        else if (copies < 0)
            result = -deck.removeCard(card, -copies);
        deck.sort();
        print("Added %s cop%s of %s.".formatted(result, result == 1 ? "y":"ies", card));
        return result;
    }

    private int deckEditRemove(EditableDeck deck, String cardString, int copies)
    {
        Card card = card(cardString);
        int result = 0;
        if (copies > 0)
            result = deck.removeCard(card, copies);
        else if (copies < 0)
            result = -deck.addCard(card, -copies, true);
        print("Removed %s cop%s of %s.".formatted(result, result == 1 ? "y":"ies", card));
        return result;
    }

    private Object deckLoad(String fileName, boolean logResult)
    {
        String deckName = fileName;
        if (fileName.matches("[A-Za-z]*\\.txt"))
            deckName = fileName.substring(0, fileName.length() - 4);
        else if (!fileName.matches("Fire|Water|Grass"))
            throw new IllegalArgumentException("Invalid deck name.");

        try
        {
            allowPrinting = false;
            List<Object> result = Save.load(this, fileName);
            allowPrinting = true;
            for (Object o : result)
            {
                if (o instanceof Exception)
                    throw new IllegalArgumentException(((Exception) o).getMessage());
            }
            if (logResult)
                print("Loaded deck \"%s\".".formatted(fileName));
            return savedDecks.get(deckName);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Invalid deck.", e);
        }
    }

    private boolean deckSave(String fileName)
    {
        String deckName = fileName.substring(0, fileName.length() - 4);
        if (!fileName.matches("[A-Za-z]*\\.txt"))
            throw new IllegalArgumentException("Invalid deck name.");

        if (!savedDecks.containsKey(deckName))
            throw new IllegalArgumentException("Invalid deck: " + fileName);
        EditableDeck selectedDeck = savedDecks.get(deckName);

        String fileBody = "deck edit %s\nclear\n".formatted(deckName);
        fileBody += String.join("\n", selectedDeck.getStringList().stream().map(
            s -> "add %s 1".formatted(s)).toList()
        );
        fileBody += "\nexit\n";

        try
        {
            Save.save(fileName, fileBody);
            return true;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Invalid deck.", e);
        }
    }

    private Object playerDeck(Optional<String> equip, List<String> cards)
    {
        playerDeck = equipDeck(equip, cards);
        return playerDeck;
    }

    private Object enemyDeck(Optional<String> equip, List<String> cards)
    {
        enemyDeck = equipDeck(equip, cards);
        return enemyDeck;
    }

    // Used by both player and enemy for deck validation
    // Note that validation is only performed if using --equip

    private EditableDeck equipDeck(Optional<String> equip, List<String> cards)
    {
        EditableDeck deck = new EditableDeck();
        if (equip.isPresent())
        {
            if (!cards.isEmpty())
                throw new IllegalArgumentException("Both card list and --equip provided");
            if (!savedDecks.containsKey(equip.get()))
                throw new IllegalArgumentException("Invalid deck: " + equip.get());
            deck = savedDecks.get(equip.get()).duplicate();
            deck.validate();
        }
        else
        {
            // # of copies is NOT validated here!

            for (String card : cards)
                deck.addCard(card(card), 1, false);
        }

        return deck;
    }

    private Object combat(boolean war)
    {
        CombatManager combatManager = new CombatManager(playerDeck.buildDeck(), enemyDeck.buildDeck(), war
            ? CombatManager.Mode.WAR_POINTS
            : CombatManager.Mode.NORMAL
        );
        return combatManager.start().resultString();
    }

    private Object play(boolean war)
    {
        deckLoad("Fire", false);
        deckLoad("Grass", false);
        deckLoad("Water", false);
        WorldManager worldManager = new WorldManager(playerDeck.buildDeck(), new HashMap<>(Map.of(
            "Fire", savedDecks.get("Fire").buildDeck(),
            "Grass", savedDecks.get("Grass").buildDeck(),
            "Water", savedDecks.get("Water").buildDeck()
        )), war);
        return worldManager.start();
    }

    private Object testCombat()
    {
        deckLoad("Water", true);
        deckLoad("Fire", true);
        playerDeck(Optional.of("Water"), new ArrayList<>());
        enemyDeck(Optional.of("Fire"), new ArrayList<>());
        return combat(false);
    }
}
