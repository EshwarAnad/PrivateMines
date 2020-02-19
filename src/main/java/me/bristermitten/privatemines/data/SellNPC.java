//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package me.bristermitten.privatemines.data;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public class SellNPC {
    private SellNPC() {
    }

    public static NPC createSellNPC(String name, String skinName, Location location, UUID owner) {
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
        npc.setProtected(true);
        ((SkinnableEntity) npc.getEntity()).setSkinName(skinName);
        npc.getTrait(LookClose.class).toggle();

        SellNPCTrait trait = npc.getTrait(SellNPCTrait.class);
        trait.setOwner(owner);

        npc.spawn(location);
        return npc;
    }

}