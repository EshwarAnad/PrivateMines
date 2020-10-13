package me.bristermitten.privatemines;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public final class Util
{

    private Util()
    {

    }

    public static Vector toBukkitVector(com.sk89q.worldedit.Vector weVector)
    {
        return new Vector(weVector.getX(), weVector.getY(), weVector.getZ());
    }

    public static com.sk89q.worldedit.Vector toWEVector(Vector bukkitVector)
    {
        return new com.sk89q.worldedit.Vector(bukkitVector.getX(), bukkitVector.getY(), bukkitVector.getZ());
    }

    public static Location toLocation(com.sk89q.worldedit.Vector weVector, World world)
    {
        return new Location(world, weVector.getX(), weVector.getY(), weVector.getZ());
    }

    public static com.sk89q.worldedit.Vector deserializeWorldEditVector(Map<String, Object> map)
    {
        return toWEVector(Vector.deserialize(map));
    }

    public static ItemStack deserializeStack(Map<String, Object> map, Object... placeholders)
    {
        Map<String, String> replacements = arrayToMap(placeholders);

        ItemStack s = new ItemStack(Material.AIR);
        s.setAmount((int) map.getOrDefault("Amount", 1));
        String type = (String) map.getOrDefault("Type", Material.STONE.name());
        type = replacements.getOrDefault(type, type);
        s.setType(Material.matchMaterial(type));
        s.setDurability((short) (int) map.getOrDefault("Data", 0));
        ItemMeta itemMeta = s.getItemMeta();

        itemMeta.setDisplayName(color((String) map.get("Name")));


        //noinspection unchecked
        itemMeta.setLore(color((List<String>) map.get("Lore")));

        s.setItemMeta(itemMeta);
        return s;
    }

    public static String color(String s)
    {
        if (s == null)
        {
            return null;
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static List<String> color(List<String> lines)
    {
        if (lines == null)
        {
            return Collections.emptyList();
        }

        return lines.stream().map(Util::color).collect(toList());
    }

    public static Map<String, String> arrayToMap(Object... array)
    {
        if (array.length == 0)
        {
            return Collections.emptyMap();
        }
        if (array.length % 2 != 0)
        {
            throw new IllegalArgumentException("Array size must be a multiple of 2");
        }

        Map<String, String> map = new LinkedHashMap<>(array.length / 2);
        int i = 0;
        while (i < array.length - 1)
        {
            map.put(array[i].toString(), array[i + 1].toString());
            i += 2;
        }
        return map;
    }

    public static void replaceMeta(ItemMeta meta, Object... replacements)
    {
        Map<String, String> replace = arrayToMap(replacements);

        if (meta.hasDisplayName())
        {
            replace.forEach((k, v) -> meta.setDisplayName(meta.getDisplayName().replace(k, v)));
        }
        if (meta.hasLore())
        {
            meta.setLore(meta.getLore().stream().map(line -> {
                String[] mutableLine = {line};

                replace.forEach((k, v) -> mutableLine[0] = mutableLine[0].replace(k, v));

                return mutableLine[0];
            }).collect(toList()));
        }

    }

    public static String prettify(String s)
    {
        return WordUtils.capitalize(s.toLowerCase().replace("_", " "));
    }

    public static Float getYaw(BlockFace face)
    {
        switch (face)
        {
            case WEST:
                return 90f;
            case NORTH:
                return 180f;
            case EAST:
                return -90f;
            case SOUTH:
                return -180f;
            default:
                return 0f;
        }
    }
}
