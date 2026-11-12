package net.trounoir.absorption;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.character.Characters;
import net.trounoir.config.TNConfig;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;

import java.util.List;

/**
 * Règles de l'Absorption : seules comptent les morts causées par le joueur lui-même,
 * et les créatures de spawner ou tuées « par le décor » (fermes) ne donnent rien.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AbsorptionHandler {
    private AbsorptionHandler() {}

    public static final String FROM_SPAWNER = "tn_spawner";
    public static final String PLAYER_DAMAGE = "tn_pdmg";
    private static final SimpleDefs.Grant UNKNOWN = new SimpleDefs.Grant(TrouNoir.rl("essence_inconnue"), 1.0);

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (MobSpawnType.isSpawner(event.getSpawnType())) {
            event.getEntity().getPersistentData().putBoolean(FROM_SPAWNER, true);
        }
    }

    /** Comptabilise les dégâts réellement infligés par un joueur à chaque créature. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || victim instanceof Player) return;
        if (event.getSource().getEntity() instanceof ServerPlayer) {
            CompoundTag tag = victim.getPersistentData();
            tag.putFloat(PLAYER_DAMAGE, tag.getFloat(PLAYER_DAMAGE) + event.getAmount());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || victim instanceof Player) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LifeData life = LifeManager.getOrNull(player);
        if (life == null) return;
        life.kills++;

        CompoundTag tag = victim.getPersistentData();
        if (tag.getBoolean(FROM_SPAWNER) && player.getRandom().nextDouble() >= TNConfig.SPAWNER_ABSORPTION_CHANCE.get()) return;
        if (tag.getFloat(PLAYER_DAMAGE) < victim.getMaxHealth() * TNConfig.MIN_PLAYER_DAMAGE_SHARE.get()) return;

        List<SimpleDefs.Grant> grants;
        CharacterDef character = Characters.defOf(victim);
        if (character != null) {
            grants = character.grants;
        } else {
            SimpleDefs.ProfileDef profile = TNData.PROFILES.get(EntityType.getKey(victim.getType()));
            grants = profile != null ? profile.grants() : List.of(UNKNOWN);
        }
        Absorption.absorbAll(player, grants, victim.position());
    }
}
