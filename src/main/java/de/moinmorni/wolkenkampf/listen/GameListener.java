package de.moinmorni.wolkenkampf.listen;

import de.moinmorni.wolkenkampf.Texts;
import de.moinmorni.wolkenkampf.WolkenkampfPlugin;
import de.moinmorni.wolkenkampf.game.Fighter;
import de.moinmorni.wolkenkampf.game.Game;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Kampf in den Runden: kein Schaden im Wolkenhafen, in der Gondel und unter Teamkameraden; wer zuletzt getroffen hat,
 * bekommt den Kill (auch beim Hinunterstoßen); statt zu sterben scheidet man aus und schaut zu.
 */
public final class GameListener implements Listener {

    private final WolkenkampfPlugin plugin;

    public GameListener(WolkenkampfPlugin plugin) {
        this.plugin = plugin;
    }

    private static Entity source(Entity damager) {
        if (damager instanceof Projectile pr) {
            ProjectileSource s = pr.getShooter();
            return s instanceof Entity e ? e : null;
        }
        return damager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager().getScoreboardTags().contains(Game.FIREWORK_TAG)) {
            e.setCancelled(true);
            return;
        }
        if (!(e.getEntity() instanceof Player victim) || !(source(e.getDamager()) instanceof Player attacker)) {
            return;
        }
        Game g = plugin.games().fighting(victim.getUniqueId());
        if (g == null || g.phase() != Game.Phase.FIGHT || plugin.games().fighting(attacker.getUniqueId()) != g
                || g.sameTeam(victim.getUniqueId(), attacker.getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        g.hitBy(victim, attacker);
    }

    /** Schneebälle und andere Würfe treffen auch ohne Schaden – trotzdem zählt der Treffer für den Kill. */
    @EventHandler
    public void onProjectile(ProjectileHitEvent e) {
        if (e.getHitEntity() instanceof Player victim && e.getEntity().getShooter() instanceof Player attacker) {
            Game g = plugin.games().fighting(victim.getUniqueId());
            if (g != null && !g.sameTeam(victim.getUniqueId(), attacker.getUniqueId())) {
                g.hitBy(victim, attacker);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.getWorld() != plugin.world()) {
            return;
        }
        Game g = plugin.games().fighting(p.getUniqueId());
        if (g == null || g.phase() != Game.Phase.FIGHT) {
            e.setCancelled(true);
            return;
        }
        Fighter f = g.fighter(p.getUniqueId());
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID) {
            e.setCancelled(true);
            return;
        }
        if (cause == EntityDamageEvent.DamageCause.FALL && f.landing()) {
            e.setCancelled(true);
            return;
        }
        if (e.getFinalDamage() >= p.getHealth() + p.getAbsorptionAmount()) {
            e.setCancelled(true);
            Player killer = null;
            if (e instanceof EntityDamageByEntityEvent by && source(by.getDamager()) instanceof Player k) {
                killer = k;
            }
            g.eliminate(p, f, Game.Cause.KILLED, killer, true);
        }
    }

    /** Falls doch jemand stirbt (z. B. /kill): ausscheiden, Sachen bleiben liegen. */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (p.getWorld() != plugin.world()) {
            return;
        }
        e.deathMessage(null);
        Game g = plugin.games().fighting(p.getUniqueId());
        if (g == null) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            return;
        }
        g.eliminate(p, g.fighter(p.getUniqueId()), Game.Cause.OTHER, p.getKiller(), false);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Game g = plugin.games().of(p.getUniqueId());
        if (g != null && g.phase() != Game.Phase.OVER) {
            e.setRespawnLocation(g.spectatorSpot());
            plugin.getServer().getScheduler().runTask(plugin, () -> g.spectate(p));
        } else {
            e.setRespawnLocation(plugin.hall().spawn());
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.hall().enter(p));
        }
    }

    /** Truhen zählen (Statistik „chests“). */
    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) {
            return;
        }
        InventoryHolder holder = e.getInventory().getHolder(false);
        if (!(holder instanceof org.bukkit.block.Chest chest)) {
            return;
        }
        Game g = plugin.games().fighting(p.getUniqueId());
        if (g == null) {
            return;
        }
        Block b = chest.getBlock();
        g.openedChest(p, b);
    }

    /** Keine Enderperle über den Rand hinaus und keine Ausflüge in andere Karten. */
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }
        Game g = plugin.games().fighting(e.getPlayer().getUniqueId());
        if (g != null && !g.buildable(e.getTo())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Texts.msg(plugin.lang(e.getPlayer()), "border.pearl"));
        }
    }
}
