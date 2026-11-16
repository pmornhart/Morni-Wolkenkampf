package de.moinmorni.wolkenkampf.listen;

import de.moinmorni.wolkenkampf.Items;
import de.moinmorni.wolkenkampf.Texts;
import de.moinmorni.wolkenkampf.Ui;
import de.moinmorni.wolkenkampf.WolkenkampfPlugin;
import de.moinmorni.wolkenkampf.game.Game;
import de.moinmorni.wolkenkampf.hall.HallMenus;
import de.moinmorni.wolkenkampf.world.HallPlan;
import de.moinmorni.wolkenkampf.world.MapType;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Betreten, Verlassen, Knöpfe in der Schnellleiste, Absturz im Wolkenhafen. */
public final class PlayerListener implements Listener {

    private final WolkenkampfPlugin plugin;

    public PlayerListener(WolkenkampfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.joinMessage(null);
        plugin.data(p);
        Game g = plugin.games().of(p.getUniqueId());
        if (g != null && g.fighter(p.getUniqueId()) != null && g.phase() != Game.Phase.OVER) {
            g.rejoin(p);
            return;
        }
        plugin.hall().enter(p);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "hall.welcome"));
        p.sendMessage(Texts.msg(lang, "hall.how"));
        Ui.title(p, Texts.text(lang, "hall.title"), Texts.text(lang, "hall.subtitle"), 2500);
        // Kämpft die Party gerade? Dann Bescheid sagen (zuschauen geht mit /zuschauen).
        UUID uuid = p.getUniqueId();
        plugin.net().party(uuid).thenAccept(party -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player q = Bukkit.getPlayer(uuid);
            if (party.isEmpty() || q == null || !plugin.hall().inHall(uuid)) {
                return;
            }
            for (var m : party.get().members()) {
                Game mate = m.uuid().equals(uuid) ? null : plugin.games().of(m.uuid());
                if (mate != null) {
                    q.sendMessage(Texts.msg(plugin.lang(q), "hall.party-playing"));
                    return;
                }
            }
        }));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.quitMessage(null);
        plugin.hall().leave(p.getUniqueId());
        Game g = plugin.games().of(p.getUniqueId());
        if (g != null) {
            g.quit(p);
        }
        plugin.boards().sidebar().forget(p.getUniqueId());
        plugin.store().unload(p.getUniqueId());
        plugin.forgetLanguage(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = e.getPlayer();
        String hall = Items.hallTag(e.getItem());
        if (hall == null) {
            return;
        }
        e.setCancelled(true);
        switch (hall) {
            case "vote" -> HallMenus.vote(plugin, p);
            case "ready" -> plugin.hall().toggleReady(p);
            case "kit" -> HallMenus.kits(plugin, p);
            case "stats" -> HallMenus.stats(plugin, p);
            case "lobby" -> {
                // Das Netzwerk fragt nach, falls jemand gerade spielt (im Wolkenhafen geht es sofort).
                p.sendMessage(Texts.msg(plugin.lang(p), "lobby.going"));
                plugin.net().askToLobby(p, Texts.text(plugin.lang(p), "lobby.kick"));
            }
            default -> {
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld() == plugin.world() && !plugin.builder(p) && plugin.games().fighting(p.getUniqueId()) == null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (Items.hallTag(e.getMainHandItem()) != null || Items.hallTag(e.getOffHandItem()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }
        Game g = plugin.games().fighting(p.getUniqueId());
        if (plugin.hall().inHall(p.getUniqueId()) || (g != null && g.phase() != Game.Phase.FIGHT)) {
            e.setCancelled(true);
        }
    }

    /** Wer im Wolkenhafen doch hinunterfällt, landet wieder am Ankunftsplatz. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo().getY() < MapType.FLOOR_Y + HallPlan.FALL_Y && plugin.hall().inHall(e.getPlayer().getUniqueId())) {
            e.getPlayer().setFallDistance(0);
            e.getPlayer().teleport(plugin.hall().spawn());
        }
    }
}
