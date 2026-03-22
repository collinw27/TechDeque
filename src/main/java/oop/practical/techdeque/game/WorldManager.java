package oop.practical.techdeque.game;

import net.sourceforge.argparse4j.ArgumentParsers;
import oop.practical.techdeque.combat.CombatManager;
import oop.practical.techdeque.deck.Deck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.max;
import static java.lang.Math.min;

public final class WorldManager
{
    private record Enemy (int x, int y, String enemyType, int health) {}

    private Deck playerDeck;
    private Deck enemyDeck;
    private boolean warMode;

    private int playerX, playerY = 0;
    private int playerHP = 20;
    private int playerWins, playerLosses = 0;
    private ArrayList<Enemy> remaningEnemies = new ArrayList<>();

    // Hacky workaround for detecting Input receiving null

    private boolean didExit = false;

    public WorldManager(Deck playerDeck, Deck enemyDeck, boolean warMode)
    {
        this.playerDeck = playerDeck.copy();
        this.enemyDeck = enemyDeck.copy();
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
            case "n" -> playerY = max(playerY - 1, 0);
            case "e" -> playerX = min(playerX + 1, 2);
            case "s" -> playerY = min(playerY + 1, 2);
            case "w" -> playerX = max(playerX - 1, 0);
            default -> throw new AssertionError(direction);
        }

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
            CombatManager combatManager = new CombatManager(playerDeck, enemyDeck, warMode
                    ? CombatManager.Mode.WAR_HEALTH
                    : CombatManager.Mode.NORMAL
            );
            combatManager.setDefaultHP(playerHP, enemy.health());
            var results = combatManager.start();

            // Assumed that player must have >0 HP to win (i.e. cannot draw)
            // Otherwise, they would automatically lose the next battle

            if (results.playerWon())
            {
                playerHP = results.playerResult();
                remaningEnemies.remove(enemy);
                playerWins += 1;
            }
            else
            {
                playerX = oldX;
                playerY = oldY;
                playerHP = 20;
                playerLosses += 1;
            }
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
        String s = String.valueOf(map);
        String mapString = s.substring(0, 3) + '\n' + s.substring(3, 6) + '\n' + s.substring(6, 9);
        System.out.println(mapString);
        return mapString;
    }

    private Object exit()
    {
        didExit = true;
        return null;
    }
}
