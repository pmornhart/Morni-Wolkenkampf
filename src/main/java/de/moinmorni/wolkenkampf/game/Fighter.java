package de.moinmorni.wolkenkampf.game;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Eine Person in einer Runde: Team (Insel), Zustand, Kills, geöffnete Truhen, wer zuletzt getroffen hat. */
public final class Fighter {

    public enum State {
        ALIVE, OUT
    }

    /** So lange zählt ein Treffer noch, wenn jemand danach hinunterfällt oder im Nebel scheitert. */
    static final long CREDIT_MS = 15_000L;

    private final UUID uuid;
    private final String name;
    private final int team;
    private State state = State.ALIVE;
    private boolean online = true;
    private int kills;
    private int place;
    private final Set<String> chests = new HashSet<>();
    private UUID lastAttacker;
    private long lastHitAt;
    private boolean landing;

    Fighter(UUID uuid, String name, int team) {
        this.uuid = uuid;
        this.name = name;
        this.team = team;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    /** Nummer des Teams in der Runde. */
    public int team() {
        return team;
    }

    public State state() {
        return state;
    }

    void state(State s) {
        state = s;
    }

    public boolean alive() {
        return state == State.ALIVE;
    }

    public boolean online() {
        return online;
    }

    void online(boolean o) {
        online = o;
    }

    public int kills() {
        return kills;
    }

    void kill() {
        kills++;
    }

    /** Platz beim Ausscheiden (1 = Sieg, 0 = noch dabei). */
    public int place() {
        return place;
    }

    void place(int p) {
        place = p;
    }

    /** Wie viele Truhen diese Person geöffnet hat (jede Truhe einmal, nach dem Nachschub wieder neu). */
    public int chests() {
        return chests.size();
    }

    /** Truhe öffnen; true beim ersten Mal. */
    boolean openChest(String key) {
        return chests.add(key);
    }

    void hitBy(UUID attacker, long now) {
        lastAttacker = attacker;
        lastHitAt = now;
    }

    /** Wer zuletzt (innerhalb von {@link #CREDIT_MS}) getroffen hat – oder null. */
    UUID creditTo(long now) {
        return lastAttacker != null && now - lastHitAt <= CREDIT_MS ? lastAttacker : null;
    }

    /** Noch auf dem Weg aus der Gondel nach unten (kein Fallschaden). */
    public boolean landing() {
        return landing;
    }

    void landing(boolean l) {
        landing = l;
    }
}
