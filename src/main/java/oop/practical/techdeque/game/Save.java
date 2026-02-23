package oop.practical.techdeque.game;

import com.google.common.base.Preconditions;
import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * IMPORTANT: This is an API file and should not be modified by your submission.
 */
public final class Save {

    private static final String FIRE_DECK = loadResource("FireDeck.txt");
    private static final String GRASS_DECK = loadResource("GrassDeck.txt");
    private static final String WATER_DECK = loadResource("WaterDeck.txt");

    public static List<Object> load(GameManager game, String source) throws IOException {
        var save = switch (source) {
            case "Fire" -> FIRE_DECK;
            case "Grass" -> GRASS_DECK;
            case "Water" -> WATER_DECK;
            default -> {
                Preconditions.checkArgument(source.matches("[A-Z][A-Za-z]*\\.txt"));
                yield Files.readString(Paths.get("saves").resolve(source));
            }
        };
        var originalScanner = Input.SCANNER;
        var originalInteractive = Input.INTERACTIVE;
        try {
            Input.SCANNER = new Scanner(save);
            Input.INTERACTIVE = false;
            return game.loop();
        } catch (AssertionError e) {
            return List.of(e);
        } finally {
            Input.SCANNER = originalScanner;
            Input.INTERACTIVE = originalInteractive;
        }
    }

    public static void save(String file, String save) throws IOException {
        Preconditions.checkArgument(file.matches("[A-Z][A-Za-z]*\\.txt"));
        Files.createDirectories(Paths.get("saves"));
        Files.writeString(Paths.get("saves").resolve(file), save);
    }

    private static String loadResource(String name) {
        try {
            var resource = Preconditions.checkNotNull(Save.class.getResource(name));
            return Resources.toString(resource, Charset.defaultCharset());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

}
