package de.moinmorni.wolkenkampf.data;

import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Werte einer Person: Runden, Siege, Kills, Ausscheiden, geöffnete Truhen, beste Runde und die gewählte Ausrüstung. */
public final class PlayerData {

    private final UUID uuid;
    private String name;
    private int games;
    private int wins;
    private int kills;
    private int deaths;
    private int chests;
    private int bestKills;
    private String kit = "";

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String n) {
        if (n != null && !n.isBlank()) {
            name = n;
        }
    }

    public int games() {
        return games;
    }

    public int wins() {
        return wins;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    public int chests() {
        return chests;
    }

    public int bestKills() {
        return bestKills;
    }

    /** Zuletzt gewählte Start-Ausrüstung (leer = noch keine). */
    public String kit() {
        return kit;
    }

    public void kit(String id) {
        kit = id == null ? "" : id;
    }

    /** Eine Runde zu Ende: Werte hochzählen; liefert {@code true} bei neuer Bestleistung (meiste Kills in einer Runde). */
    public boolean addGame(boolean won, int kills, boolean died, int chests) {
        this.games++;
        if (won) {
            this.wins++;
        }
        this.kills += Math.max(0, kills);
        if (died) {
            this.deaths++;
        }
        this.chests += Math.max(0, chests);
        if (kills > bestKills) {
            bestKills = kills;
            return true;
        }
        return false;
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("runden", games);
        y.set("siege", wins);
        y.set("kills", kills);
        y.set("ausgeschieden", deaths);
        y.set("truhen", chests);
        y.set("beste-kills", bestKills);
        y.set("ausruestung", kit);
        return y.saveToString();
    }

    public static PlayerData fromYaml(UUID uuid, String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        PlayerData d = new PlayerData(uuid, y.getString("name", "?"));
        d.games = y.getInt("runden");
        d.wins = y.getInt("siege");
        d.kills = y.getInt("kills");
        d.deaths = y.getInt("ausgeschieden");
        d.chests = y.getInt("truhen");
        d.bestKills = y.getInt("beste-kills");
        d.kit = y.getString("ausruestung", "");
        return d;
    }
}
