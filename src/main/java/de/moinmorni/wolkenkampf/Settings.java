package de.moinmorni.wolkenkampf;

import de.moinmorni.wolkenkampf.logic.Timeline;
import org.bukkit.configuration.file.FileConfiguration;

/** Werte aus der config.yml (Erklärungen stehen dort). */
public record Settings(
        String world,
        int hallCountdown,
        int hallMinPlayers,
        int maxPlayers,
        int gondolaSeconds,
        int afterSeconds,
        Timeline timeline) {

    static Settings load(FileConfiguration c) {
        int refill = clamp(c.getInt("ereignisse.nachschub", 150), 10, 3600);
        int storm = clamp(c.getInt("ereignisse.gewitter", 270), refill + 1, 3600);
        int fog = clamp(c.getInt("ereignisse.nebel", 390), storm + 1, 3600);
        int fogDuration = clamp(c.getInt("ereignisse.nebel-dauer", 180), 10, 3600);
        return new Settings(
                c.getString("welt", "wolkenkampf"),
                clamp(c.getInt("wolkenhafen.countdown-sekunden", 30), 3, 300),
                clamp(c.getInt("wolkenhafen.mindestens-spieler", 2), 2, 12),
                clamp(c.getInt("runde.hoechstens-spieler", 12), 2, 12),
                clamp(c.getInt("runde.gondel-sekunden", 10), 3, 60),
                clamp(c.getInt("runde.nachspiel-sekunden", 8), 1, 60),
                new Timeline(refill, storm, fog, fogDuration));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
