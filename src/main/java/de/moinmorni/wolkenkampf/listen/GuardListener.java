package de.moinmorni.wolkenkampf.listen;

import de.moinmorni.wolkenkampf.Texts;
import de.moinmorni.wolkenkampf.WolkenkampfPlugin;
import de.moinmorni.wolkenkampf.game.Game;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * Schutz der eigenen Welt: Bauen und Abbauen nur in einer laufenden Runde, nur für Leute, die noch kämpfen, und nur
 * innerhalb des Randes und unter der Bauhöhe (nach der Runde wird die Karte ohnehin wiederhergestellt). Im
 * Wolkenhafen ändert niemand etwas; nichts brennt, wächst oder verfällt, keine fremden Tiere, kein Wetter.
 * Ausnahme: Admins im Kreativmodus.
 */
public final class GuardListener implements Listener {

    private static final Set<CreatureSpawnEvent.SpawnReason> ALLOWED = Set.of(CreatureSpawnEvent.SpawnReason.CUSTOM,
            CreatureSpawnEvent.SpawnReason.COMMAND, CreatureSpawnEvent.SpawnReason.DEFAULT);

    private final WolkenkampfPlugin plugin;

    public GuardListener(WolkenkampfPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean ours(World w) {
        return w == plugin.world();
    }

    /** Darf dieser Spieler hier etwas verändern? */
    private boolean mayChange(Player p, Location at) {
        if (plugin.builder(p)) {
            return true;
        }
        Game g = plugin.games().fighting(p.getUniqueId());
        return g != null && g.phase() == Game.Phase.FIGHT && g.buildable(at);
    }

    /** Liegt die Stelle in einer Karte, auf der gerade gekämpft wird? */
    private boolean inFight(Location at) {
        for (Game g : plugin.games().all()) {
            if (g.phase() == Game.Phase.FIGHT && g.onMap(at)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (ours(e.getBlock().getWorld()) && !mayChange(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!ours(e.getBlock().getWorld()) || mayChange(e.getPlayer(), e.getBlock().getLocation())) {
            return;
        }
        e.setCancelled(true);
        Game g = plugin.games().fighting(e.getPlayer().getUniqueId());
        if (g != null && g.phase() == Game.Phase.FIGHT) {
            e.getPlayer().sendActionBar(Texts.text(plugin.lang(e.getPlayer()), "border.build"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld()) || plugin.builder(p)) {
            return;
        }
        Game g = plugin.games().fighting(p.getUniqueId());
        boolean fighting = g != null && g.phase() == Game.Phase.FIGHT;
        if (e.getAction() == Action.PHYSICAL && !fighting) {
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !fighting) {
            e.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (ours(e.getBlock().getWorld()) && !mayChange(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketFillEvent e) {
        if (ours(e.getBlock().getWorld()) && !mayChange(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    /** Wasser aus Eimern fließt nur auf Karten mit laufender Runde. */
    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (ours(e.getBlock().getWorld()) && !inFight(e.getToBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (ours(e.getEntity().getWorld())) {
            e.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.blockList().clear();
        }
    }

    /** Fallende Blöcke dürfen in laufenden Runden fallen, sonst ändert nichts die Welt. */
    @EventHandler(ignoreCancelled = true)
    public void onChange(EntityChangeBlockEvent e) {
        if (!ours(e.getBlock().getWorld()) || (e.getEntity() instanceof Player p && plugin.builder(p))) {
            return;
        }
        if (!(e.getEntity() instanceof org.bukkit.entity.FallingBlock) || !inFight(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHanging(HangingBreakEvent e) {
        if (ours(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (ours(e.getLocation().getWorld()) && !ALLOWED.contains(e.getSpawnReason())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent e) {
        if (ours(e.getWorld()) && e.toWeatherState()) {
            e.setCancelled(true);
        }
    }
}
