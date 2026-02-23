package oop.practical.techdeque;

import oop.practical.techdeque.game.GameManager;
import oop.practical.techdeque.game.Input;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Scanner;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Practical1Tests {

    @ParameterizedTest
    @MethodSource
    public void testCard(String name, String card, Object expected) {
        test("card " + card, expected);
    }

    public static Stream<Arguments> testCard() {
        return Stream.of(
            Arguments.of("Type Fire", "Fire-I", "Fire-I"),
            Arguments.of("Type Grass", "Grass-I", "Grass-I"),
            Arguments.of("Type Water", "Water-I", "Water-I"),

            Arguments.of("Rank I", "Grass-I", "Grass-I"),
            Arguments.of("Rank II", "Grass-II", "Grass-II"),
            Arguments.of("Rank III", "Grass-III", "Grass-III"),
            Arguments.of("Rank IV", "Grass-IV", "Grass-IV"),
            Arguments.of("Rank V", "Grass-V", "Grass-V"),

            Arguments.of("Invalid", "Invalid", IllegalArgumentException.class),
            Arguments.of("Invalid Type", "Electric-I", IllegalArgumentException.class),
            Arguments.of("Invalid Rank (Numeric)", "Grass-1", IllegalArgumentException.class),
            Arguments.of("Invalid Rank (Bounds)", "Grass-VI", IllegalArgumentException.class),

            Arguments.of("Simplified Format (Bonus)", "fire3", "Fire-III")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDeck(String name, String command, Object expected) {
        test(command, expected);
    }

    public static Stream<Arguments> testDeck() {
        return Stream.of(
            Arguments.of("Empty", """
                playerDeck
                """, "[]"),
            Arguments.of("Single", """
                playerDeck Grass-IV
                """, "[Grass-IV]"),
            Arguments.of("Multiple", """
                playerDeck Grass-IV Water-II Fire-III
                """, "[Grass-IV, Water-II, Fire-III]"),
            Arguments.of("Enemy", """
                enemyDeck Grass-IV Water-II Fire-III
                """, "[Grass-IV, Water-II, Fire-III]")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testWarCombat(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testWarCombat() {
        return Stream.of(
            Arguments.of("Type Win", """
                playerDeck Grass-I
                enemyDeck Water-I
                combat --war
                """, "3-0"),
            Arguments.of("Rank Win", """
                playerDeck Grass-II
                enemyDeck Grass-I
                combat --war
                """, "1-0")
                //TODO: Test coverage
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testRegularCombat(String name, String commands, Object expected) {
        test(commands, expected);
    }

    public static Stream<Arguments> testRegularCombat() {
        return Stream.of(
            //TODO
            Arguments.of("Rank Win", """
                playerDeck Grass-II
                enemyDeck Grass-I
                combat --war
                """, "1-0")
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
            } else if (expected instanceof String) {
                Assertions.assertEquals(expected, received.get(i).toString());
            } else {
                Assertions.assertEquals(expected, received.get(i));
            }
        }));
    }

}
