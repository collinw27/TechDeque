package oop.practical.techdeque.game;

import net.sourceforge.argparse4j.ArgumentParsers;
import oop.practical.techdeque.combat.CombatManager;
import oop.practical.techdeque.deck.Deck;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

public final class WorldManager
{
    private record Enemy (int x, int y, String enemyType, int health) {}

    private Deck playerDeck;
    private HashMap<String, Deck> enemyDecks;
    private boolean warMode;

    private int playerX, playerY = 0;
    private int playerHP = 20;
    private int playerWins, playerLosses = 0;
    private ArrayList<Enemy> remaningEnemies = new ArrayList<>();

    // Hacky workaround for detecting Input receiving null

    private boolean didExit = false;

    // Currently, only the GameManager has the ability to load decks
    // Since enemies need to access the builtin decks, I see 3 reasonable options:
    // 1) pass in a reference to GameManager to this class
    // 2) pass in a reference to an external object solely responsible for loading decks
    // 3) just pass in the decks to this constructor (seems easiest to me)

    public WorldManager(Deck playerDeck, HashMap<String, Deck> enemyDecks, boolean warMode)
    {
        this.playerDeck = playerDeck.copy();
        this.enemyDecks = new HashMap<>(enemyDecks);
        this.warMode = warMode;

        remaningEnemies.add(new Enemy(2, 0, "Fire", 10));
        remaningEnemies.add(new Enemy(0, 2, "Grass", 15));
        remaningEnemies.add(new Enemy(2, 2, "Water", 20));
    }

    public List<Object> start()
    {
        var parser = ArgumentParsers.newFor("").build();
        var subparsers = parser.addSubparsers().dest("command");
        var move = subparsers.addParser("move");
        move.addArgument("direction").choices(List.of("n", "e", "s", "w"));
        subparsers.addParser("map");
        subparsers.addParser("exit");

        List<Object> results = Input.loop(parser, args -> switch (args.getString("command")) {
            case "move" -> move(args.getString("direction"));
            case "map" -> getMap();
            case "exit" -> exit();
            default -> throw new AssertionError(args.getString("command"));
        });

        if (didExit)
            results.add("%s (%s-%s)".formatted(playerHP, playerWins, playerLosses));

        return results;
    }

    private String move(String direction)
    {
        int oldX = playerX;
        int oldY = playerY;
        switch (direction)
        {
            case "n" -> playerY -= 1;
            case "e" -> playerX += 1;
            case "s" -> playerY += 1;
            case "w" -> playerX -= 1;
            default -> throw new AssertionError(direction);
        }
        if (playerX < 0 || playerX > 2 || playerY < 0 || playerY > 2)
            throw new IllegalArgumentException("Invalid player position");

        // Modify list outside of for loop to avoid weirdness

        Optional<Enemy> encounteredEnemy = Optional.empty();
        for (var enemy : remaningEnemies)
        {
            if (enemy.x == playerX && enemy.y == playerY)
                encounteredEnemy = Optional.of(enemy);
        }
        if (encounteredEnemy.isPresent())
        {
            Enemy enemy = encounteredEnemy.get();
            System.out.printf("You encountered a %s enemy!\n", enemy.enemyType);

            // Load predefined enemy deck

            if (!enemyDecks.containsKey(enemy.enemyType))
                throw new AssertionError("Did not define deck " + enemy.enemyType);
            Deck enemyDeck = enemyDecks.get(enemy.enemyType).copy();

            // Run combat and modify state after
            // Assumed that player must have >0 HP to win (i.e. cannot draw)
            // Otherwise, they would automatically lose the next battle

            CombatManager combatManager = new CombatManager(playerDeck, enemyDeck, warMode
                ? CombatManager.Mode.WAR_HEALTH
                : CombatManager.Mode.NORMAL
            );
            combatManager.setDefaultHP(playerHP, enemy.health());
            var results = combatManager.start();
            if (results.playerWon())
            {
                playerHP = results.playerResult();
                remaningEnemies.remove(enemy);
                playerWins += 1;
                System.out.printf("You advanced to (%s, %s).\n", playerX, playerY);
            }
            else
            {
                playerX = oldX;
                playerY = oldY;
                playerHP = 20;
                playerLosses += 1;
                System.out.printf("You remain at (%s, %s).\n", playerX, playerY);
            }
        }
        else
        {
            System.out.printf("You advanced to (%s, %s).\n", playerX, playerY);
        }
        return "(%s, %s)".formatted(playerX, playerY);
    }

    private String getMap()
    {
        char[] map = new char[9];
        Arrays.fill(map, ' ');
        for (var enemy : remaningEnemies)
        {
            map[enemy.x + enemy.y * 3] = enemy.enemyType.charAt(0);
        }
        map[playerX + playerY * 3] = 'P';

        // There's probably a better way of doing this, but it's fine for a simple board

        String s = String.valueOf(map);
        String mapString = s.substring(0, 3) + '\n' + s.substring(3, 6) + '\n' + s.substring(6, 9);
        System.out.println(mapString);
        return mapString;
    }

    // Workaround for Input.loop()

    private Object exit()
    {
        didExit = true;
        System.out.println("Exiting and displaying results:");
        System.out.printf("HP: %s    |    W-L: %s-%s\n", playerHP, playerWins, playerLosses);
        return null;
    }
}
