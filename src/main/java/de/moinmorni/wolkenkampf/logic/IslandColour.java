package de.moinmorni.wolkenkampf.logic;

/**
 * Farbe einer Startinsel – nach Alpenblumen benannt. Ballon-Gondel, Name im Chat und Team tragen sie. Reihenfolge =
 * Reihenfolge der Inseln im Ring (Nachbarn sehen deutlich verschieden aus).
 */
public enum IslandColour {
    ENZIAN("enzian", "blue", 0x3C44AA),
    ALPENROSE("alpenrose", "red", 0xD8434E),
    EDELWEISS("edelweiss", "white", 0xF2F2F2),
    TROLLBLUME("trollblume", "yellow", 0xF5D33B),
    ALPENLEIN("alpenlein", "cyan", 0x2CB5B5),
    ARNIKA("arnika", "orange", 0xF28C28),
    KROKUS("krokus", "purple", 0x9B59D0),
    FRAUENMANTEL("frauenmantel", "lime", 0x86D33A),
    VERGISSMEINNICHT("vergissmeinnicht", "light_blue", 0x6FB7F0),
    SEIDELBAST("seidelbast", "magenta", 0xD45BC8),
    LATSCHE("latsche", "green", 0x4E8A2E),
    MEHLPRIMEL("mehlprimel", "pink", 0xF49AC1);

    private final String id;
    private final String dye;
    private final int rgb;

    IslandColour(String id, String dye, int rgb) {
        this.id = id;
        this.dye = dye;
        this.rgb = rgb;
    }

    public String id() {
        return id;
    }

    /** Minecraft-Farbname für Wolle, Banner, Glas („blue“, „light_blue“ …). */
    public String dye() {
        return dye;
    }

    /** Farbe für Texte (#rrggbb). */
    public int rgb() {
        return rgb;
    }

    /** Hellere Farbe für die zweite Bahn der Ballonhülle (Weiß bekommt Hellgrau, damit man die Bahnen sieht). */
    public String stripe() {
        return this == EDELWEISS ? "light_gray" : "white";
    }

    /** Farbe der Bauchbinde am Ballon. */
    public String band() {
        return switch (this) {
            case ENZIAN, VERGISSMEINNICHT, ALPENLEIN -> "yellow";
            case TROLLBLUME, ARNIKA -> "red";
            case EDELWEISS -> "yellow";
            case FRAUENMANTEL, LATSCHE -> "brown";
            default -> "white".equals(stripe()) ? "light_blue" : "white";
        };
    }

    public static IslandColour of(int island) {
        IslandColour[] all = values();
        return all[Math.floorMod(island, all.length)];
    }
}
