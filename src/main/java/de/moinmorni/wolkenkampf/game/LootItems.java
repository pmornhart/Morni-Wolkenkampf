package de.moinmorni.wolkenkampf.game;

import de.moinmorni.wolkenkampf.logic.Loot;
import de.moinmorni.wolkenkampf.logic.StartKit;
import de.moinmorni.wolkenkampf.world.MapType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/** Aus den Namen der Truhen-Tabelle ({@link Loot}) echte Gegenstände machen – mit den Baublöcken der Karte. */
public final class LootItems {

    private LootItems() {
    }

    public static ItemStack of(Loot.Stack s, MapType map) {
        String id = s.id();
        int n = Math.max(1, s.amount());
        return switch (id) {
            case "planks" -> new ItemStack(material(map.planks()), n);
            case "stone" -> new ItemStack(material(map.stone()), n);
            case "iron_sword_sharp" -> enchant(new ItemStack(Material.IRON_SWORD), Enchantment.SHARPNESS, 1);
            case "bow_power" -> enchant(new ItemStack(Material.BOW), Enchantment.POWER, 1);
            case "leather_boots_feather" -> enchant(new ItemStack(Material.LEATHER_BOOTS), Enchantment.FEATHER_FALLING, 2);
            case "iron_chestplate_prot" -> enchant(new ItemStack(Material.IRON_CHESTPLATE), Enchantment.PROTECTION, 1);
            case "potion_healing" -> potion(Material.POTION, PotionType.HEALING, n, 0xF82423);
            case "potion_speed" -> potion(Material.POTION, PotionType.SWIFTNESS, n, 0x33EBFF);
            case "potion_regeneration" -> potion(Material.POTION, PotionType.REGENERATION, n, 0xCD5CAB);
            case "potion_strength" -> potion(Material.POTION, PotionType.STRENGTH, n, 0xFFC700);
            case "potion_fire" -> potion(Material.POTION, PotionType.FIRE_RESISTANCE, n, 0xFF9900);
            case "splash_healing" -> potion(Material.SPLASH_POTION, PotionType.HEALING, n, 0xF82423);
            default -> new ItemStack(material(id), n);
        };
    }

    public static List<ItemStack> kit(StartKit kit, MapType map) {
        List<ItemStack> out = new ArrayList<>();
        for (Loot.Stack s : kit.items()) {
            out.add(of(s, map));
        }
        return out;
    }

    static Material material(String name) {
        Material m = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return m == null ? Material.STONE : m;
    }

    private static ItemStack enchant(ItemStack item, Enchantment e, int level) {
        item.editMeta(meta -> meta.addEnchant(e, level, true));
        return item;
    }

    /** Trank ohne eigenen Namen – den Namen zeigt das Spiel in der Sprache des Spielers. */
    private static ItemStack potion(Material m, PotionType type, int n, int rgb) {
        ItemStack item = new ItemStack(m, n);
        item.editMeta(PotionMeta.class, meta -> {
            meta.setBasePotionType(type);
            meta.setColor(Color.fromRGB(rgb));
        });
        return item;
    }
}
