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
                ).toString())
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
                deck edit name
                add Grass-V 1
                exit
                playerDeck --equip name
                """, IllegalArgumentException.class),
            Arguments.of("Type Balance", """
                deck edit name
                add Grass-V 1
                add Solarbeam 1
                add Fire-I 3
                add Fire-II 3
                add Fire-III 2
                exit
                playerDeck --equip name
                """, IllegalArgumentException.class),
            Arguments.of("Valid copies (save test)", """
                deck edit Name
                add Grass-V 1
                add Solarbeam 1
                add Fire-I 2
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                exit
                deck save Name
                playerDeck --equip Name
                """, true),
            Arguments.of("Too many rank I", """
                deck edit name
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
                deck edit name
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
                deck edit name
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
                deck edit name
                add Grass-V 1
                add Tempest 1
                add Fire-I 2
                add Grass-I 3
                add Grass-II 3
                add Grass-III 2
                exit
                playerDeck --equip name
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
                """, "0-0")
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
