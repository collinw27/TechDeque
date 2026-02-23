package oop.practical.techdeque.game;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

/**
 * IMPORTANT: This is an API file and should not be modified by your submission.
 */
public final class Input {

    public static Scanner SCANNER = new Scanner(System.in);
    public static boolean INTERACTIVE = true; //System.in is interactive

    public static List<Object> loop(ArgumentParser parser, Function<Namespace, Object> body) {
        var results = new ArrayList<>();
        while (SCANNER.hasNext()) {
            try {
                var args = Input.prompt(parser, "");
                var result = body.apply(args);
                if (result == null) {
                    break;
                }
                results.add(result);
            } catch (RuntimeException e) {
                System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                results.add(e);
            }
        }
        return results;
    }

    public static Namespace prompt(ArgumentParser parser, String prefix) {
        // Interactive input allows re-prompting users on invalid input. If not
        // interactive (e.g. tests), error accordingly.
        if (INTERACTIVE) {
            while (true) {
                System.out.print(prefix);
                try {
                    return parser.parseArgs(nextArgs());
                } catch (ArgumentParserException e) {
                    //IntelliJ terminal flushes System.err async, which causes
                    //race conditions with System.out. Just always use stdout.
                    parser.handleError(e, new PrintWriter(System.out));
                }
            }
        } else {
            try {
                System.out.print(prefix);
                return parser.parseArgs(nextArgs());
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
