package oop.practical.techdeque.game;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class Input {

    public static Scanner SCANNER = new Scanner(System.in);
    public static boolean INTERACTIVE = true; //System.in is interactive

    public static Namespace prompt(ArgumentParser parser, String prefix) {
        // Interactive input allows re-prompting users on invalid input. If not
        // interactive (e.g. tests), error accordingly.
        if (INTERACTIVE) {
            while (true) {
                System.out.print(prefix);
                try {
                    return parser.parseArgs(nextArgs());
                } catch (ArgumentParserException e) {
                    parser.handleError(e);
                }
            }
        } else {
            try {
                var input = nextArgs();
                System.out.println(Arrays.toString(input));
                return parser.parseArgs(input);
            } catch (NoSuchElementException | ArgumentParserException e) {
                throw new AssertionError("Invalid/missing input: " + e.getMessage(), e);
            }
        }
    }

    private static String[] nextArgs() {
        // argparse4j requires a String[] args, as it assumes tokenization is
        // handled by the shell invoking the program. Space-split strings is
        // sufficient for now, but doesn't support quoted arguments.
        return SCANNER.nextLine().split(" ");
    }

}
