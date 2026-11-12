package net.trounoir.absorption;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;
import net.trounoir.network.Net;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.util.Fx;
import net.trounoir.util.Text;

import java.util.List;

/** Le cœur de l'Absorption : accorder un trait, une technique ou un savoir au Trou-Noir. */
public final class Absorption {
    private Absorption() {}

    /**
     * Absorbe une liste de gains (profil d'une créature ou d'un personnage).
     *
     * @param origin position de la victime, pour le retour visuel ; peut être nul (commande de debug)
     */
    public static void absorbAll(ServerPlayer player, List<SimpleDefs.Grant> grants, Vec3 origin) {
        boolean any = false;
        for (SimpleDefs.Grant grant : grants) {
            if (grant.chance() < 1.0 && player.getRandom().nextDouble() >= grant.chance()) continue;
            any |= absorb(player, grant.id().toString(), true);
        }
        if (any) {
            if (origin != null) feedback(player, origin);
            Bonuses.recompute(player);
            LifeManager.save(player);
            Net.syncLife(player);
        }
    }

    /** Absorbe un élément. Retourne vrai si quelque chose a changé. Le recalcul est à la charge de l'appelant. */
    public static boolean absorb(ServerPlayer player, String id, boolean notify) {
        AbsorbableDef def = TNData.absorbable(id);
        if (def == null) return false;
        LifeData life = LifeManager.get(player);
        int before = life.level(id);
        int count = life.xp.getOrDefault(id, 0);
        if (count >= AbsorbableDef.xpForLevel(def.maxLevel)) return false; // plafond atteint : plus rien à gagner
        life.xp.put(id, count + 1);
        life.hasEverAbsorbed = true;
        int after = life.level(id);

        if (def.kind == AbsorbableDef.Kind.KNOWLEDGE) rememberKnowledge(player, def);
        if (def.kind == AbsorbableDef.Kind.TECHNIQUE && before == 0) autoEquip(life, id);

        if (notify && after > before) {
            // Notification courte et diégétique : un nom, un niveau, rien d'autre.
            Component name = Component.translatable(def.nameKey());
            Component line = def.maxLevel > 1
                    ? Component.translatable("trounoir.absorb.gain_level", name, Text.roman(after))
                    : Component.translatable("trounoir.absorb.gain", name);
            player.displayClientMessage(line.copy().withColor(colorFor(def)), true);
        }
        return true;
    }

    private static int colorFor(AbsorbableDef def) {
        if (!def.tags.isEmpty()) {
            SimpleDefs.TagDef tag = TNData.TAGS.get(def.tags.get(0));
            if (tag != null) return tag.color();
        }
        return switch (def.kind) {
            case TECHNIQUE -> 0xC792EA;
            case KNOWLEDGE -> 0x9AD8FF;
            default -> 0xE8E0FF;
        };
    }

    private static void autoEquip(LifeData life, String id) {
        for (String s : life.slots) if (id.equals(s)) return;
        for (int i = 0; i < life.slots.length; i++) {
            if (life.slots[i] == null || life.slots[i].isEmpty()) {
                life.slots[i] = id;
                return;
            }
        }
    }

    /** Les Savoirs sont des souvenirs : ils survivent à la mort. */
    private static void rememberKnowledge(ServerPlayer player, AbsorbableDef def) {
        PersistentStore store = PersistentStore.get(player.server);
        PersistentStore.Memory memory = store.memory(player.getUUID());
        memory.knowledge.add(def.id.toString());
        switch (def.knowledgeType) {
            case "location" -> memory.places.add(def.target);
            case "weakness" -> memory.weaknesses.add(def.target);
            case "recipe" -> player.awardRecipesByKey(def.recipes);
            default -> { }
        }
        store.save();
    }

    private static void feedback(ServerPlayer player, Vec3 origin) {
        ServerLevel level = player.serverLevel();
        Vec3 to = player.position().add(0, player.getBbHeight() * 0.6, 0);
        Vec3 from = origin.add(0, 0.6, 0);
        // Une traînée de matière aspirée de la victime vers le Trou-Noir.
        int steps = (int) Math.min(24, Math.max(4, from.distanceTo(to) * 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(Fx.particle("reverse_portal"), p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.0);
        }
        level.sendParticles(Fx.particle("sculk_soul"), from.x, from.y, from.z, 8, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Fx.sound("block.respawn_anchor.charge"), SoundSource.PLAYERS, 0.6F, 0.6F);
    }
}
