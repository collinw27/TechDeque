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

public final class Practical3Tests {

    @ParameterizedTest
    @MethodSource
    public void testSpecialtyCombatV3(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testSpecialtyCombatV3() {
        return Stream.of(
            Arguments.of("Spear Shield Attack Break", """
                playerDeck FireSpear
                enemyDeck FireShield
                combat --war
                """, "2-0"),
            Arguments.of("Shield Hanging Activated", """
                playerDeck FireShield Grass-I
                enemyDeck Grass-I Fire-V
                combat --war
                """, "0-0"),
            Arguments.of("Shield Hanging Consumed", """
                playerDeck FireShield Grass-I Grass-I
                enemyDeck Grass-I Fire-V Fire-V
                combat --war
                """, "0-7"),
            Arguments.of("Shield Hanging Pierced", """
                playerDeck Grass-I FireSpear
                enemyDeck FireShield Fire-I
                combat --war
                """, "2-0")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testRegularCombatV3(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testRegularCombatV3() {
        return Stream.of(
            Arguments.of("Select Command", """
                playerDeck Grass-III Grass-III Grass-III Grass-III Grass-III Grass-III
                enemyDeck Water-I Water-I Water-I Water-I Water-I Water-I
                combat
                select 1
                select 3
                """, "10-0"),
            Arguments.of("Select Invalid", """
                playerDeck Grass-III Grass-III
                enemyDeck Water-I Water-I Water-I
                combat
                select 3
                """, IllegalArgumentException.class),
            Arguments.of("Select Hand Retention", """
                playerDeck Grass-III Grass-III Grass-III Fire-I Fire-I Fire-I
                enemyDeck Water-I Water-I Water-I Water-I Water-I Water-I
                combat
                select 1
                select 1
                """, "10-0"),
            Arguments.of("Discard Command", """
                playerDeck Grass-III Fire-I Fire-I Fire-I Grass-III
                enemyDeck Water-I Water-I Water-I Water-I Water-I Water-I
                combat
                discard 3
                select 1
                select 3
                """, "10-0"),
            Arguments.of("Example Scenario", """
                playerDeck Grass-I Water-I Fire-I Fire-II Fire-III
                enemyDeck Water-I Water-I Water-I Grass-I Grass-I Grass-I
                combat
                discard 2
                select 1
                select 2
                select 2
                """, "10-0")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDeckEquipV3(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testDeckEquipV3() {
        return Stream.of(
            Arguments.of("Minimum Size", """
                deck edit Name
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                add Grass-IV 2
                add Grass-V
                exit
                playerDeck --equip Name
                """, IllegalArgumentException.class),
            Arguments.of("Rank Balance", """
                deck edit Name
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                add Grass-IV 2
                add Grass-V
                add Fire-III 2
                add Water-III 2
                exit
                playerDeck --equip Name
                """, IllegalArgumentException.class)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testGameplay(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testGameplay() {
        return Stream.of(
            Arguments.of("Map", """
                play --war
                map
                """, new Object[] { "P F\n   \nG W" }),
            Arguments.of("Move Valid", """
                play --war
                move e
                move s
                move e
                """, new Object[] { "(2, 1)" }),
            Arguments.of("Move Invalid", """
                play --war
                move n
                """, new Object[] { IllegalArgumentException.class }),
            Arguments.of("Combat Victory", """
                playerDeck Water-V Water-V
                play --war
                move e
                move e
                """, new Object[] { "(2, 0)" }),
            Arguments.of("Combat Defeat", """
                playerDeck Grass-I Grass-I Grass-I
                play --war
                move e
                move e
                """, new Object[] { "(1, 0)" }),
            Arguments.of("Roll Credits", """
                playerDeck Water-V Water-V Water-V
                play --war
                move e
                move e
                move w
                move s
                move w
                move s
                move e
                move e
                exit
                """, new Object[] { "20 (3-0)" })
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
            }
        });
    }

}
