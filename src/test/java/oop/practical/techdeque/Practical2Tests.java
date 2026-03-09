package oop.practical.techdeque;

import oop.practical.techdeque.game.GameManager;
import oop.practical.techdeque.game.Input;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Practical2Tests {

    @ParameterizedTest
    @MethodSource
    public void testSpecialtyCard(String name, String card, Object expected) {
        test("card " + card, expected);
    }

    public static Stream<Arguments> testSpecialtyCard() {
        return Stream.of(
            Arguments.of("Shield", "GrassShield", "GrassShield"),
            Arguments.of("Spear", "WaterSpear", "WaterSpear"),
            Arguments.of("Ultimate", "Inferno", "Inferno")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDeckEdit(String name, String command, Object expected) {
        test(command, expected);
    }

    public static Stream<Arguments> testDeckEdit() {
        return Stream.of(
            Arguments.of("Edit", """
                deck edit
                add Grass-IV
                exit
                deck view
                """, "[Grass-IV]"),
            Arguments.of("Add Copies", """
                deck edit
                add Grass-IV 2
                exit
                deck view
                """, "[Grass-IV, Grass-IV]"),
            Arguments.of("Remove Copies", """
                deck edit
                add Grass-IV 2
                remove Grass-IV 1
                exit
                deck view
                """, "[Grass-IV]"),
            Arguments.of("Load", """
                deck load Grass
                deck view Grass
                """, List.of(
                    "Fire-I",
                    "Fire-II",
                    "FireShield",
                    "FireSpear",
                    "Grass-I", "Grass-I", "Grass-I",
                    "Grass-II", "Grass-II",
                    "Grass-III", "Grass-III",
                    "Grass-IV",
                    "Grass-V",
                    "GrassShield",
                    "GrassSpear",
                    "Solarbeam",
                    "Water-I", "Water-I",
                    "Water-II",
                    "Water-III"
                ).toString()),
            Arguments.of("Add zero", """
            deck edit
            add Grass-I 0
            exit
            deck view
            """, "[]"),
            Arguments.of("Remove zero", """
            deck edit
            remove Grass-I 0
            exit
            deck view
            """, "[]"),
            Arguments.of("Remove nonexistent", """
            deck edit
            remove Grass-I 1
            exit
            deck view
            """, "[]"),
            Arguments.of("Add negative", """
            deck edit
            add Grass-I 3
            add Grass-I -2
            exit
            deck view
            """, "[Grass-I]"),
            Arguments.of("Remove negative", """
            deck edit
            remove Grass-I -2
            exit
            deck view
            """, "[Grass-I, Grass-I]"),
            Arguments.of("Modify current", """
            deck edit Name
            add Grass-I 3
            add Grass-II 3
            add Grass-III 2
            add Grass-IV 2
            add Grass-V 1
            exit
            playerDeck --equip Name
            deck edit Name
            remove Grass-V
            exit
            deck view
            """, "[Grass-I, Grass-I, Grass-I, Grass-II, Grass-II, Grass-II, Grass-III, Grass-III, Grass-IV, Grass-IV, Grass-V]"),
            Arguments.of("Equip combat", """
            deck edit Name
            add Grass-I 3
            add Grass-II 3
            add Grass-III 2
            add Grass-IV 2
            add Grass-V 1
            exit
            playerDeck --equip Name
            enemyDeck Grass-II Grass-II Grass-II Grass-III Grass-III Grass-III Grass-IV Grass-IV Grass-V Grass-V Grass-V
            combat --war
            """, "0-10"),
            Arguments.of("Multiple above max copies", """
            deck edit
            add Grass-V 1
            remove Grass-V 2
            """, false),
            Arguments.of("Multiple above max removal", """
            deck edit
            add Grass-I 3
            remove Grass-I
            remove Grass-I
            remove Grass-I
            remove Grass-I
            """, false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDeckValidation(String name, String command, Object expected) {
        test(command, expected);
    }

    public static Stream<Arguments> testDeckValidation() {
        //TODO: Ensure GameManager uses --equip, not single -equip!
        return Stream.of(
            Arguments.of("Valid", """
                deck load Grass
                playerDeck --equip Grass
                """, true),
            Arguments.of("Minimum Size", """
                deck edit Name
                add Grass-V 1
                exit
                playerDeck --equip name
                """, IllegalArgumentException.class),
            Arguments.of("Type Balance", """
                deck edit Name
                add Grass-V 1
                add Solarbeam 1
                add Fire-I 3
                add Fire-II 3
                add Fire-III 2
                exit
                playerDeck --equip name
                """, IllegalArgumentException.class),
            Arguments.of("Too many rank I", """
                deck edit Name
                add Grass-V 1
                add Solarbeam 1
                add Fire-I 2
                add Grass-I 4
                add Grass-II 3
                add Grass-III 2
                exit
                playerDeck --equip name
                """, IllegalArgumentException.class),
            Arguments.of("Too many rank III", """
                deck edit Name
                add Grass-V 1
                add Solarbeam 1
                add Fire-I 2
                add Grass-I 3
                add Grass-II 3
                add Grass-III 3
                exit
                playerDeck --equip name
                """, IllegalArgumentException.class),
            Arguments.of("Too many rank V", """
                deck edit Name
                add Grass-V 1
                add Solarbeam 2
                add Fire-I 2
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                exit
                playerDeck --equip name
                """, IllegalArgumentException.class),
            Arguments.of("Invalid rank V type", """
                deck edit Name
                add Grass-V 1
                add Tempest 1
                add Fire-I 2
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                exit
                playerDeck --equip name
                """, IllegalArgumentException.class),
            Arguments.of("playerDeck multiple", """
                playerDeck Grass-IV Water-II Fire-III
                """, "[Grass-IV, Water-II, Fire-III]"),
            Arguments.of("enemyDeck", """
                enemyDeck Grass-IV Water-II Fire-III
                """, "[Grass-IV, Water-II, Fire-III]")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testMiscTest(String name, String commands, Object expected)
    {
        test(commands, expected);
    }

    public static Stream<Arguments> testMiscTest()
    {
        return Stream.of(
            Arguments.of("War test", """
                playerDeck Grass-IV Water-II Fire-III
                enemyDeck Water-I Grass-I Fire-I
                combat --war
                """, "8-4"),
            Arguments.of("Regular test", """
                playerDeck Grass-IV Water-II Fire-III
                enemyDeck Water-I Fire-I Grass-I Water-I Fire-I Grass-I Water-I Fire-I Grass-II
                combat
                1
                3
                """, true),
            Arguments.of("Reshuffle (Both)", """
                playerDeck Grass-III Grass-III Grass-III Grass-II Grass-II Grass-II
                enemyDeck Grass-I Grass-I Grass-I Grass-I Grass-I Grass-I
                combat
                1
                1
                1
                1
                1
                """, true),
            Arguments.of("Player selection (3 cards)", """
                playerDeck Grass-V Grass-V Grass-V Water-I Water-I Grass-I
                enemyDeck Water-I Water-I Water-I Water-I Water-I Water-I
                combat
                1
                3
                """, true),
            Arguments.of("Enemy selection (Rank)", """
                playerDeck Grass-V Grass-V Grass-V Grass-II Grass-II Grass-II
                enemyDeck Water-I Water-I Water-I Fire-I Water-II Fire-I
                combat
                1
                1
                """, true),
            Arguments.of("Enemy selection (Order)", """
                playerDeck Grass-V Grass-V Grass-V Grass-II Grass-II Grass-II
                enemyDeck Water-I Water-I Water-I Fire-I Water-II Fire-II
                combat
                1
                1
                """, true),
            Arguments.of("Overkill", """
                playerDeck Grass-V Grass-V Grass-V Grass-V Grass-V Grass-V
                enemyDeck Water-I Water-I Water-I Water-I Water-I Water-I
                combat
                1
                1
                """, true),
            Arguments.of("Custom save", """
                deck edit Name
                add Grass-IV
                exit
                deck save Name.txt
                deck edit Name
                clear
                add Fire-I
                exit
                deck load Name.txt
                deck view Name
                """, true),
            Arguments.of("Exactly half basic cards", """
                deck edit Name
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                add Grass-IV 2
                add Grass-V 1
                add FireShield 3
                add GrassShield 3
                add WaterShield 3
                add GrassSpear 2
                exit
                playerDeck --equip Name
                """, true),
                Arguments.of("Below half basic cards", """
                deck edit Name
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                add Grass-IV 2
                add Grass-V 1
                add FireShield 3
                add GrassShield 3
                add WaterShield 3
                add GrassSpear 2
                add Solarbeam 1
                exit
                playerDeck --equip Name
                """, IllegalArgumentException.class)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testSpecialtyCombat(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testSpecialtyCombat() {
        return Stream.of(
            Arguments.of("Shield Defense", """
                playerDeck Fire-III
                enemyDeck FireShield
                combat --war
                """, "0-0"),
            Arguments.of("Shield Offense", """
                playerDeck FireShield
                enemyDeck Grass-I
                combat --war
                """, "0-0"),
            Arguments.of("Spear Advantage", """
                playerDeck GrassSpear
                enemyDeck WaterShield
                combat --war
                """, "6-0"),
            Arguments.of("Spear Disadvantage", """
                playerDeck GrassSpear
                enemyDeck FireShield
                combat --war
                """, "1-0"),
            Arguments.of("Ultimate Advantage", """
                playerDeck Solarbeam
                enemyDeck Water-I
                combat --war
                """, "8-0"),
            Arguments.of("Ultimate Disadvantage", """
                playerDeck Solarbeam
                enemyDeck Fire-II
                combat --war
                """, "0-0"),
            Arguments.of("Shield reshuffle", """
                playerDeck FireShield
                combat --war
                """, "0-0"),
            Arguments.of("Spear shield defense break", """
                playerDeck FireShield
                enemyDeck FireSpear
                combat --war
                """, "0-3"),
                Arguments.of("Ultimate shield attack break", """
                playerDeck Inferno
                enemyDeck FireShield
                combat --war
                """, "4-0"),
                Arguments.of("Ultimate shield defense break", """
                playerDeck FireShield
                enemyDeck Inferno
                combat --war
                """, "0-4"),
                Arguments.of("Ultimate shield attack disadvantage", """
                playerDeck Tempest
                enemyDeck GrassShield
                combat --war
                """, "0-0"),
                Arguments.of("Ultimate basic rank 1", """
                playerDeck Inferno
                enemyDeck Fire-I
                combat --war
                """, "4-0"),
                Arguments.of("Ultimate basic rank 2", """
                playerDeck Solarbeam
                enemyDeck Grass-II
                combat --war
                """, "3-0"),
                Arguments.of("Ultimate basic rank 3", """
                playerDeck Tempest
                enemyDeck Water-III
                combat --war
                """, "2-0"),
                Arguments.of("Ultimate disadvantage rank 2", """
                playerDeck Solarbeam
                enemyDeck Fire-II
                combat --war
                """, "0-0"),
                Arguments.of("Ultimate disadvantage rank 3", """
                playerDeck Tempest
                enemyDeck Grass-III
                combat --war
                """, "0-5")
        );
    }

    private static void test(String commands, Object expected) {
        Input.SCANNER = new Scanner(commands.trim()); //trailing newline
        Input.INTERACTIVE = false;
        var received = new GameManager().loop();
        Assertions.assertAll(IntStream.range(0, received.size()).mapToObj(i -> () -> {
            if (i < received.size() - 1) {
                // Expect any non-error result from setup commands
                Assertions.assertFalse(received.get(i) instanceof Exception, received.get(i).toString());
            } else if (expected instanceof Class<?> clazz) {
                Assertions.assertInstanceOf(clazz, received.get(i), received.get(i).toString());
            } else if (expected instanceof Boolean) {
                Assertions.assertFalse(received.get(i) instanceof Exception);
            } else if (expected instanceof String) {
                Assertions.assertEquals(expected, received.get(i).toString());
            } else {
                Assertions.assertEquals(expected, received.get(i));
            }
        }));
    }

}
