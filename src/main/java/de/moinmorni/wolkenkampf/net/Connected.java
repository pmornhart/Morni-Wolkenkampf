package de.moinmorni.wolkenkampf.net;

import de.moinmorni.api.MoinMorni;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Wolkenkampf im Netzwerk: alles über die Schnittstelle des Netzwerk-Plugins. Die Schnittstelle wird bei jedem Aufruf neu
 * geholt – lädt jemand das Netzwerk-Plugin neu, stimmt sie trotzdem.
 */
final class Connected implements Network {

    private static final String GERMAN = "de";

    private Connected() {
    }

    /** null, wenn das Netzwerk-Plugin keine Schnittstelle anbietet. */
    static Network create() {
        return MoinMorni.get().isPresent() ? new Connected() : null;
    }

    private static Optional<MoinMorni> api() {
        return MoinMorni.get();
    }

    @Override
    public boolean connected() {
        return true;
    }

    @Override
    public String language(UUID player) {
        String lang = api().map(a -> a.language(player)).orElse(GERMAN);
        return "en".equals(lang) ? "en" : GERMAN;
    }

    @Override
    public void stats(UUID player, Map<String, Long> add, Map<String, Long> best) {
        api().ifPresent(a -> a.stats(MODE, player, add, best, false));
    }

    @Override
    public void state(String state, int players) {
        api().ifPresent(a -> a.state(MODE, state, players));
    }

    @Override
    public void toLobby(Player player, Component message) {
        api().ifPresentOrElse(a -> a.toLobby(player), () -> player.kick(message));
    }

    @Override
    public CompletableFuture<Optional<Party>> party(UUID player) {
        Optional<MoinMorni> api = api();
        if (api.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return api.get().party(player).thenApply(p -> p.map(Connected::convert))
                .exceptionally(e -> Optional.empty());
    }

    private static Party convert(MoinMorni.Party p) {
        return new Party(p.leader(), p.members().stream()
                .map(m -> new Party.Member(m.uuid(), m.name(), m.server()))
                .toList());
    }

    @Override
    public String serverName() {
        return api().map(MoinMorni::serverName).orElse("");
    }
}
