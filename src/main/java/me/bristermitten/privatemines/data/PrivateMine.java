//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package me.bristermitten.privatemines.data;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.MessageType;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.bristermitten.privatemines.PrivateMines;
import me.bristermitten.privatemines.Util;
import me.bristermitten.privatemines.config.LangKeys;
import me.bristermitten.privatemines.service.SchematicStorage;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

public class PrivateMine implements ConfigurationSerializable
{

    public static final int RESET_THRESHOLD = 1200 * 20 * 1000;
    public static final String PLAYER_PLACEHOLDER = "{player}";
    private final UUID owner;
    private final Set<UUID> bannedPlayers;
    private Region region;
    private MineLocations locations;
    private EditSession editSession;
    private ProtectedRegion wgRegion;
    private UUID npcId;
    private boolean open;
    private Material block;
    private double taxPercentage;
    private MineSchematic mineSchematic;
    private long nextResetTime;

    public PrivateMine(UUID owner,
                       Set<UUID> bannedPlayers,
                       boolean open,
                       Material block,
                       Region region,
                       MineLocations locations,
                       ProtectedRegion wgRegion,
                       UUID npc,
                       double taxPercentage,
                       MineSchematic mineSchematic)
    {
        this.owner = owner;
        this.bannedPlayers = bannedPlayers;
        this.open = open;
        this.region = region;
        this.locations = locations;
        this.block = block;
        this.wgRegion = wgRegion;
        this.npcId = npc;
        this.taxPercentage = taxPercentage;
        this.mineSchematic = mineSchematic;

        this.editSession = new EditSessionBuilder(FaweAPI.getWorld(locations.getSpawnPoint().getWorld().getName()))
                .allowedRegions(new Region[]{region}).fastmode(true).build();
        this.fill(block);
    }

    @SuppressWarnings("unchecked")
    public static PrivateMine deserialize(Map<String, Object> map)
    {
        UUID owner = UUID.fromString((String) map.get("Owner"));

        boolean open = (Boolean) map.get("Open");

        Material block = Material.matchMaterial((String) map.get("Block"));
        Vector corner1 = Util.deserializeWorldEditVector((Map<String, Object>) map.get("Corner1"));
        Vector corner2 = Util.deserializeWorldEditVector(((Map<String, Object>) map.get("Corner2")));

        MineLocations locations = MineLocations.deserialize((Map<String, Object>) map.get("Locations"));
        CuboidRegion region = new CuboidRegion(corner1, corner2);
        region.setWorld(new BukkitWorld(locations.getSpawnPoint().getWorld()));
        ProtectedRegion wgRegion =
                WorldGuardPlugin.inst().getRegionManager(locations.getSpawnPoint().getWorld())
                        .getRegion(owner.toString());

        UUID npcId = UUID.fromString((String) map.get("NPC"));
        double taxPercentage = (Double) map.get("Tax");

        String schematicName = (String) map.get("Schematic");
        MineSchematic schematic = SchematicStorage.getInstance().get(schematicName);

        if (schematic == null)
        {
            throw new IllegalArgumentException("Invalid Schematic " + schematicName);
        }

        Set<UUID> bannedPlayers = Optional.ofNullable((List<String>) map.get("BannedPlayers"))
                .map(strings -> strings.stream().map(UUID::fromString).collect(Collectors.toSet()))
                .orElse(new HashSet<>());

        return new PrivateMine(owner, bannedPlayers, open, block, region, locations, wgRegion, npcId, taxPercentage, schematic);
    }

    public double getTaxPercentage()
    {
        return this.taxPercentage;
    }

    public void setTaxPercentage(double amount)
    {
        this.taxPercentage = amount;
    }

    public boolean contains(Player p)
    {
        return this.region.contains(Util.toWEVector(p.getLocation().toVector()));
    }

    public Material getBlock()
    {
        return block;
    }

    public void setBlock(Material block)
    {
        if (block.isBlock())
        {
            this.block = block;
            this.fill(block);
        }
    }

    public Map<String, Object> serialize()
    {
        Map<String, Object> map = new TreeMap<>();
        map.put("Owner", this.owner.toString());
        map.put("Open", this.open);
        map.put("Block", this.block.name());
        map.put("Locations", this.locations.serialize());
        map.put("Corner1", Util.toBukkitVector(this.region.getMinimumPoint()).serialize());
        map.put("Corner2", Util.toBukkitVector(this.region.getMaximumPoint()).serialize());
        map.put("NPC", this.npcId.toString());
        map.put("Tax", this.taxPercentage);
        map.put("Schematic", this.mineSchematic.getName());
        map.put("BannedPlayers", this.bannedPlayers.stream().map(UUID::toString).collect(Collectors.toList()));
        return map;
    }

    public void teleport(Player player)
    {
        if (bannedPlayers.contains(player.getUniqueId()))
        {
            BukkitCommandManager manager = PrivateMines.getPlugin().getManager();
            BukkitCommandIssuer issuer = manager.getCommandIssuer(player);
            manager.sendMessage(issuer, MessageType.ERROR, LangKeys.ERR_YOU_WERE_BANNED, PLAYER_PLACEHOLDER, Bukkit.getOfflinePlayer(owner).getName());

            return;
        }

        player.teleport(locations.getSpawnPoint());
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
    }

    public UUID getOwner()
    {
        return owner;
    }

    public void teleport()
    {
        Player player = Bukkit.getPlayer(this.owner);
        if (player != null) teleport(player);
    }

    public boolean isOpen()
    {
        return open;
    }

	/*
	   Fills the blocks in the mine.
	 */

    public void setOpen(boolean open)
    {
        this.open = open;
    }

    public void fill(Material m)
    {
        //free any players who might be in the mine
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (locations.getRegion().contains(Util.toWEVector(player.getLocation().toVector())))
            {
                player.teleport(locations.getSpawnPoint());
                player.sendMessage(ChatColor.GREEN + "You've been teleported to the mine spawn point!");
            }
        }

        //noinspection deprecation
        editSession.setBlocks(locations.getRegion(), new BaseBlock(m.getId()));
        editSession.flushQueue();


        nextResetTime = System.currentTimeMillis() + RESET_THRESHOLD;
    }


    public boolean shouldReset()
    {
        return locations.getSpawnPoint().getChunk().isLoaded() && System.currentTimeMillis() >= nextResetTime;
    }

    /*
      Delete the mine.
     */
    public void delete()
    {

        removeAllPlayers();

        //noinspection deprecation
        editSession.setBlocks(region, new BaseBlock(0));
        editSession.flushQueue();

        removeRegion();

        if (PrivateMines.getPlugin().isCitizensEnabled())
        {
            NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcId);
            if (npc != null)
            {
                npc.destroy();
            }
        }
    }

    /*
    Delete the Private Mine Region.
     */
    private void removeRegion()
    {
        World world = locations.getSpawnPoint().getWorld();
        RegionManager regionManager = WorldGuardPlugin.inst().getRegionManager(world);

        if (regionManager == null)
        {
            PrivateMines.getPlugin().getLogger().warning(() -> String.format("RegionManager for world %s is null", world.getName()));
        } else
        {
            regionManager.removeRegion(wgRegion.getId());
            regionManager.removeRegion(locations.getWgRegion().getId());
        }
    }

    /*
      Teleport all the players back to spawn.
     */
    private void removeAllPlayers()
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (this.contains(player))
            {
                player.performCommand("spawn");
            }
        }
    }

    /*
      Sets the new mine schematic (Used when changing themes)
     */
    public void setMineSchematic(MineSchematic mineSchematic)
    {
        fill(Material.AIR);
        boolean mineIsOpen = isOpen();
        setOpen(false);
        delete();

        PrivateMine newMine = PrivateMines.getPlugin().getFactory().create(
                Bukkit.getPlayer(owner),
                mineSchematic,
                Util.toLocation(region.getCenter(), locations.getSpawnPoint().getWorld()));

        this.locations = newMine.locations;
        this.editSession = newMine.editSession;
        this.region = newMine.region;
        this.wgRegion = newMine.wgRegion;
        this.npcId = newMine.npcId;
        this.mineSchematic = mineSchematic;
        setOpen(mineIsOpen);

    }

    public boolean ban(Player player)
    {
        if (this.contains(player))
        {
            BukkitCommandManager manager = PrivateMines.getPlugin().getManager();
            BukkitCommandIssuer issuer = manager.getCommandIssuer(player);
            manager.sendMessage(issuer, MessageType.ERROR, LangKeys.ERR_YOU_WERE_BANNED, PLAYER_PLACEHOLDER, Bukkit.getOfflinePlayer(owner).getName());

            player.performCommand("spawn");
        }

        return bannedPlayers.add(player.getUniqueId());
    }

    public boolean unban(Player player)
    {
        BukkitCommandManager manager = PrivateMines.getPlugin().getManager();
        BukkitCommandIssuer issuer = manager.getCommandIssuer(player);
        manager.sendMessage(issuer, MessageType.ERROR, LangKeys.ERR_YOU_WERE_UNBANNED, PLAYER_PLACEHOLDER, Bukkit.getOfflinePlayer(owner).getName());

        return bannedPlayers.remove(player.getUniqueId());
    }
}
