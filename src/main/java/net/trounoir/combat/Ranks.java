package net.trounoir.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.character.Characters;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;

/** Rang de toute entité vivante : 0 (le Trou-Noir à sa naissance) à 10 (Higgs). */
public final class Ranks {
    private Ranks() {}

    public static int of(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            LifeData life = LifeManager.getOrNull(player);
            return life == null ? 0 : life.rank;
        }
        CharacterDef def = Characters.defOf(entity);
        if (def != null) return def.rank;
        SimpleDefs.ProfileDef profile = TNData.PROFILES.get(EntityType.getKey(entity.getType()));
        return profile == null ? 1 : profile.rank();
    }
}
