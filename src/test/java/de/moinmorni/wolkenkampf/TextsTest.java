package de.moinmorni.wolkenkampf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.wolkenkampf.logic.IslandColour;
import de.moinmorni.wolkenkampf.logic.Mode;
import de.moinmorni.wolkenkampf.logic.StartKit;
import de.moinmorni.wolkenkampf.world.MapType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

class TextsTest {

    /** Formatierungen, die keine Platzhalter sind. */
    private static final Set<String> STYLES = Set.of("gray", "white", "sky", "au", "em", "bl", "red", "green", "dark_gray",
            "b", "bold", "gold", "yellow");
    // Tags wie <name> oder </gray> – aber nicht \<spieler> (maskiert).
    // https://regex101.com/?flavor=java&regex=(%3F%3C!%5C%5C)%3C%2F%3F(%5Ba-z_%5D%2B)%3E&testString=%3Cgray%3EHi%20%3Cname%3E%3C%2Fgray%3E%20%5C%3Cspieler%3E
    private static final Pattern TAG = Pattern.compile("(?<!\\\\)</?([a-z_]+)>");
    /** Text-Schlüssel als feste Zeichenkette im Quelltext, z. B. "elim.void-by" (nicht "map." + id). */
    // https://regex101.com/?flavor=java&regex=%22((%3F%3Ahall%7Cmenu%7Cgame%29%5C.%5Ba-z0-9.%5C-%5D*%5Ba-z0-9%5D)%22&testString=Texts.msg(lang%2C%20%22hall.ready%22)%20%22map.%22
    private static final Pattern KEY = Pattern.compile("\"((?:hall|item|menu|game|event|elim|out|end|spectate|leave|bar|side|stats"
            + "|top|board|kill|fog|border|lobby|only|unknown|kit|map|mode|colour)[.\\-][a-z0-9.\\-]*[a-z0-9])\"");

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = TAG.matcher(text);
        while (m.find()) {
            if (!STYLES.contains(m.group(1))) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    @Test
    void everyTextExistsInBothLanguagesWithTheSamePlaceholders() {
        for (String key : Texts.keys()) {
            String de = Texts.raw("de", key);
            String en = Texts.raw("en", key);
            assertFalse(de.isBlank(), key);
            assertFalse(en.isBlank(), key);
            assertEquals(placeholders(de), placeholders(en), "Platzhalter in " + key);
        }
    }

    @Test
    void everyTextParsesStrictly() {
        MiniMessage strict = MiniMessage.builder().strict(true)
                .editTags(t -> t.resolvers(
                        TagResolver.resolver("sky", Tag.styling(TextColor.color(Texts.SKY))),
                        TagResolver.resolver("au", Tag.styling(TextColor.color(Texts.GOLD))),
                        TagResolver.resolver("em", Tag.styling(TextColor.color(Texts.EMERALD))),
                        TagResolver.resolver("bl", Tag.styling(TextColor.color(Texts.BLUE)))))
                .build();
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                TagResolver.Builder resolvers = TagResolver.builder();
                placeholders(text).forEach(p -> resolvers.resolver(Placeholder.unparsed(p, "x")));
                TagResolver all = resolvers.build();
                for (String line : text.split("\n")) {
                    assertDoesNotThrow(() -> strict.deserialize(line, all), lang + " " + key + ": " + line);
                }
            }
        }
    }

    /** Minecraft zeigt nur Zeichen der „Basic Multilingual Plane“ – Emoji wie 🎈 wären ein leeres Kästchen. */
    @Test
    void onlyCharactersMinecraftCanShow() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                assertEquals(text.length(), text.codePointCount(0, text.length()), lang + " " + key);
            }
        }
    }

    @Test
    void englishIsReallyEnglish() {
        assertNotEquals(Texts.raw("de", "end.win.title"), Texts.raw("en", "end.win.title"));
        assertEquals(Texts.raw("de", "map.wolkenalm"), Texts.raw("fr", "map.wolkenalm"), "unbekannte Sprache → Deutsch");
        assertEquals("missing.key", Texts.raw("de", "missing.key"));
    }

    /** Jede Karte, Spielart, Ausrüstung, Inselfarbe und jedes Ereignis hat seine Texte in beiden Sprachen. */
    @Test
    void everyGameThingHasAName() {
        Set<String> keys = Texts.keys();
        for (MapType m : MapType.values()) {
            for (String s : new String[] {"", ".desc", ".short"}) {
                assertTrue(keys.contains("map." + m.id() + s), m.id() + s);
            }
        }
        for (Mode m : Mode.values()) {
            assertTrue(keys.contains("mode." + m.id()), m.id());
            assertTrue(keys.contains("mode." + m.id() + ".desc"), m.id());
        }
        for (StartKit k : StartKit.values()) {
            assertTrue(keys.contains("kit." + k.id()), k.id());
            assertTrue(keys.contains("kit." + k.id() + ".desc"), k.id());
        }
        for (IslandColour c : IslandColour.values()) {
            assertTrue(keys.contains("colour." + c.id()), c.id());
        }
        for (String e : new String[] {"refill", "storm", "fog"}) {
            for (String s : new String[] {".warn", ".title", ".sub", ".now"}) {
                assertTrue(keys.contains("event." + e + s), e + s);
            }
            assertTrue(keys.contains("bar." + e), e);
            assertTrue(keys.contains("side.next." + e), e);
        }
    }

    /** Jeder Schlüssel, der im Quelltext steht, hat einen Text. */
    @Test
    void everyKeyUsedInTheCodeExists() throws IOException {
        Set<String> missing = new TreeSet<>();
        int found = 0;
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = KEY.matcher(Files.readString(f, StandardCharsets.UTF_8));
                while (m.find()) {
                    found++;
                    if (!Texts.keys().contains(m.group(1))) {
                        missing.add(m.group(1) + " (" + f.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(found > 100, "nur " + found + " Schlüssel gefunden – stimmt das Muster?");
        assertEquals(Set.of(), missing);
    }

    @Test
    void numbersAndClock() {
        assertEquals("▮▮▮▯▯", Ui.bar(0.6, 5));
        assertEquals("1:05", Ui.clock(65));
        assertEquals("0:00", Ui.clock(-3));
        assertEquals("3,5", Ui.hearts(7.0, "de"));
        assertEquals("3.5", Ui.hearts(7.0, "en"));
    }
}
