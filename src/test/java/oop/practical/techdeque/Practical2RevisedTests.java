package oop.practical.techdeque;

import oop.practical.techdeque.game.GameManager;
import oop.practical.techdeque.game.Input;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public final class Practical2RevisedTests {

    @ParameterizedTest
    @MethodSource
    public void testSpecialtyCard(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testSpecialtyCard() {
        return Stream.of(
            Arguments.of("Shield", "card FireShield", "FireShield"),
            Arguments.of("Spear", "card GrassSpear", "GrassSpear"),
            Arguments.of("Ultimate", "card Tempest", "Tempest")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testSpecialtyCombat(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testSpecialtyCombat() {
        return Stream.of(
            Arguments.of("Shield Attack Negation", """
                playerDeck Fire-V
                enemyDeck FireShield
                combat --war
                """, "0-0"),
            Arguments.of("Shield Attack Advantage", """
                playerDeck Fire-I
                enemyDeck GrassShield
                combat --war
                """, "3-0"),
            Arguments.of("Shield Attack Disadvantage", """
                playerDeck Fire-I
                enemyDeck WaterShield
                combat --war
                """, "0-0"),
            //Modified in V3; no bonus damage for equal types
            //Arguments.of("Spear Shield Attack Break", """
            //    playerDeck FireSpear
            //    enemyDeck FireShield
            //    combat --war
            //    """, "3-0"),
            Arguments.of("Spear Shield Attack Advantage", """
                playerDeck GrassSpear
                enemyDeck WaterShield
                combat --war
                """, "6-0"),
            Arguments.of("Spear Shield Attack Disadvantage", """
                playerDeck WaterSpear
                enemyDeck GrassShield
                combat --war
                """, "1-0"),
            Arguments.of("Ultimate Rank II", """
                playerDeck Inferno
                enemyDeck Fire-II
                combat --war
                """, "3-0"),
            Arguments.of("Ultimate Rank II Advantage", """
                playerDeck Solarbeam
                enemyDeck Water-II
                combat --war
                """, "7-0"),
            Arguments.of("Ultimate Rank II Disadvantage", """
                playerDeck Solarbeam
                enemyDeck Fire-II
                combat --war
                """, "0-0"),
            Arguments.of("Ultimate Shield Attack Break", """
                playerDeck Inferno
                enemyDeck FireShield
                combat --war
                """, "4-0"),
            Arguments.of("Ultimate Shield Attack Advantage", """
                playerDeck Solarbeam
                enemyDeck WaterShield
                combat --war
                """, "8-0"),
            Arguments.of("Ultimate Shield Attack Disadvantage", """
                playerDeck Tempest
                enemyDeck GrassShield
                combat --war
                """, "0-0")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDeckEdit(String name, String command, Object expected) {
        test(command, expected);
    }

    public static Stream<Arguments> testDeckEdit() {
        return Stream.of(
            Arguments.of("Edit Current", """
                deck edit
                add Fire-I 3
                add Grass-II 2
                add Water-III 1
                exit
                deck view
                """, "[Fire-I, Fire-I, Fire-I, Grass-II, Grass-II, Water-III]"),
            Arguments.of("Edit Name", """
                deck edit Name
                add Fire-I 3
                add Grass-II 2
                add Water-III 1
                exit
                deck view Name
                """, "[Fire-I, Fire-I, Fire-I, Grass-II, Grass-II, Water-III]"),
            Arguments.of("Add Valid Copies", """
                deck edit
                add Grass-IV
                add Grass-IV
                exit
                deck view
                """, "[Grass-IV, Grass-IV]"),
            Arguments.of("Add Invalid Copies", """
                deck edit
                add Grass-V
                add Grass-V
                """, new Object[] { IllegalArgumentException.class }),
            Arguments.of("Remove Valid Copies", """
                deck edit
                add Grass-IV 2
                remove Grass-IV 1
                exit
                deck view
                """, "[Grass-IV]"),
            Arguments.of("Remove Invalid Copies", """
                deck edit
                add Grass-IV 2
                remove Grass-IV -1
                """, new Object[] { IllegalArgumentException.class }),
            Arguments.of("Load Builtin", """
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
            // Note: This test *will* write to ./saves by virtue of using Save.
            // In a "real" system, you would generally prefer to mock Save
            // instead of relying on File IO behavior.
            Arguments.of("Save & Load Custom", """
                deck edit Custom
                add Fire-I 3
                add Grass-II 2
                add Water-III 1
                exit
                deck save Custom.txt
                deck edit Custom
                clear
                exit
                deck load Custom.txt
                """, "[Fire-I, Fire-I, Fire-I, Grass-II, Grass-II, Water-III]")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDeckEquip(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testDeckEquip() {
        return Stream.of(
            Arguments.of("Player", """
                deck edit Name
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                add Grass-IV 2
                add Grass-V 1
                add GrassShield 1
                exit
                playerDeck --equip Name
                deck view
                """, List.of(
                "Grass-I", "Grass-I", "Grass-I",
                "Grass-II", "Grass-II", "Grass-II",
                "Grass-III", "Grass-III",
                "Grass-IV", "Grass-IV",
                "Grass-V",
                "GrassShield"
            ).toString()),
            // No deck view for enemy, so leverage combat behavior
            Arguments.of("Enemy", """
                deck edit Name
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                add Grass-IV 2
                add Grass-V 1
                add GrassShield 1
                exit
                enemyDeck --equip Name
                playerDeck Grass-II Grass-II Grass-II Grass-III Grass-III Grass-III Grass-IV Grass-IV Grass-V Grass-V Grass-V Grass-V
                combat --war
                """, "10-0"),
            Arguments.of("Minimum Size", """
                deck edit Name
                add Grass-V 1
                exit
                playerDeck --equip Name
                """, IllegalArgumentException.class),
            Arguments.of("Type Balance", """
                deck edit Name
                add Grass-V 1
                add Fire-I 3
                add Fire-II 2
                add Fire-III 2
                add Fire-IV 2
                add FireShield 3
                exit
                playerDeck --equip Name
                """, IllegalArgumentException.class)
        );
    }

    private static void test(String commands, Object expected) {
        Input.SCANNER = new Scanner(commands.trim()); //trailing newline
        Input.INTERACTIVE = false;
        try {
            var results = new GameManager().loop();
            assertMatch(expected, results);
        } catch (Throwable t) {
            // Hacky, but compare the thrown exception with the unwrapped
            // expected value to support an expected IllegalArgumentException
            // matching either return/throw behavior for AssertionError.
            var unwrapped = expected;
            while (unwrapped instanceof Object[] subcommandExpected) {
                Assertions.assertEquals(1, subcommandExpected.length);
                unwrapped = subcommandExpected[0];
            }
            assertMatch(unwrapped, List.of(t));
        }
    }

    private static void assertMatch(Object expected, List<?> results) {
        assertSetupSuccess(results.subList(0, Math.max(results.size() - 1, 0)));
        var result = results.isEmpty() ? null : results.getLast();
        switch (expected) {
            // Hack to separate subcommand structures that should be unwrapped
            // (e.g. deck edit) from regular List values (e.g. deck view). Tests
            // should wrap the expected value in new Object[] {...}.
            case Object[] subcommandExpected -> {
                Assertions.assertEquals(1, subcommandExpected.length);
                var subcommandResults = Assertions.assertInstanceOf(List.class, result);
                assertMatch(subcommandExpected[0], subcommandResults);
            }
            // If we expect an IllegalArgumentException for validation, also
            // accept AssertionError from Input.prompt utilizing argparse4j.
            case Class<?> clazz when clazz == IllegalArgumentException.class -> {
                Assertions.assertTrue(result instanceof IllegalArgumentException || result instanceof AssertionError, String.valueOf(result));
            }
            case Class<?> clazz -> Assertions.assertInstanceOf(clazz, result);
            case Boolean success when success -> Assertions.assertFalse(result instanceof Exception, String.valueOf(result));
            case String string -> Assertions.assertEquals(string, String.valueOf(result));
            default -> Assertions.assertEquals(expected, result);
        }
    }

    private static void assertSetupSuccess(List<?> results) {
        results.forEach(result -> {
            switch (result) {
                // Input.loop returns a List of subcommand results, but other
                // commands may also return a List (e.g. deck view). Since we're
                // only validating none of the elements are Throwables, this
                // should be "good enough" for our use case.
                case List<?> potentialSubmenuCommands -> assertSetupSuccess(potentialSubmenuCommands);
                case Throwable t -> Assertions.fail("Unexpected Exception in result (failed setup command?)", t);
                default -> {}
            };
        });
    }

}
