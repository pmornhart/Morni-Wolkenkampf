package de.moinmorni.wolkenkampf.game;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Eine Person in einer Runde: Team (Insel), Zustand, Kills, geöffnete Truhen, wer zuletzt getroffen hat. */
public final class Fighter {

    public enum State {
        ALIVE, OUT
    }

    /**
     * So lange (in Spielticks, 20 pro Sekunde) zählt ein Treffer noch, wenn jemand danach in die Tiefe fällt oder im
     * Nebel scheitert.
     */
    static final long CREDIT_TICKS = 15L * 20L;

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
    private boolean started;
    /** Höhe des eigenen Gondelbodens: erst Bodenkontakt darunter zählt als Landung. */
    private double landingFloor;

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

    /** Treffer merken; {@code now} = aktueller Spieltick. */
    void hitBy(UUID attacker, long now) {
        lastAttacker = attacker;
        lastHitAt = now;
    }

    /**
     * Wer zuletzt (innerhalb von {@link #CREDIT_TICKS}) getroffen hat – oder null. Gezählt wird in Spielticks, nicht
     * nach der Uhr: hängt der Server kurz, verfällt die Gutschrift nicht vorzeitig.
     */
    UUID creditTo(long now) {
        return lastAttacker != null && now - lastHitAt <= CREDIT_TICKS ? lastAttacker : null;
    }

    /**
     * Hat die Person den Start mitbekommen (Gondel offen, Überlebensmodus, Start-Ausrüstung)? Wer beim Öffnen gerade die
     * Verbindung verloren hatte, bekommt ihn beim Wiederkommen.
     */
    public boolean started() {
        return started;
    }

    void started(boolean s) {
        started = s;
    }

    /** Noch auf dem Weg aus der Gondel nach unten (kein Fallschaden). */
    public boolean landing() {
        return landing;
    }

    /**
     * Landeschutz an, wenn sich die Gondel öffnet.
     *
     * @param floorY Höhe des eigenen Gondelbodens (Unterkante des Blocks, auf dem man in der Gondel steht)
     */
    void startLanding(double floorY) {
        landing = true;
        landingFloor = floorY;
    }

    /**
     * Landeschutz nachführen, jeden Tick mit dem Bodenkontakt, den der Client meldet, und der Höhe der Füße. Gelandet
     * ist erst, wer <em>unterhalb</em> des Gondelbodens Boden unter den Füßen hat. Auf Gondelhöhe zählt kein
     * Bodenkontakt: Direkt nach dem Öffnen meldet der Client noch „am Boden“ (er stand ja auf dem Gondelboden), nach
     * einem Teleport kurz „in der Luft“ und gleich wieder „am Boden“ – und wer auf dem Rand der Gondel stehen bleibt,
     * steigt mit dem Ballon und fällt erst später. In all diesen Fällen bleibt der Schutz, bis man wirklich unten ist.
     */
    void landingTick(boolean onGround, double feetY) {
        if (landing && onGround && feetY < landingFloor) {
            landing = false;
        }
    }
}
