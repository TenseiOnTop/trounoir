package net.trounoir.technique;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.combat.Ranks;
import net.trounoir.util.Fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * L'arrêt du temps du Trou-Blanc : dans une zone, créatures et projectiles se figent.
 * Les êtres de très haut rang (les Dieux) ne sont que ralentis.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeStop {
    private TimeStop() {}

    private static final String FROZEN = "tn_frozen";

    private record Zone(ResourceKey<Level> dimension, Vec3 center, double radius, long until, int resistRank, UUID owner) {
        AABB box() {
            return new AABB(center, center).inflate(radius);
        }
    }

    private static final List<Zone> ZONES = new ArrayList<>();

    public static void start(ServerLevel level, Vec3 center, double radius, int ticks, int resistRank, UUID owner) {
        ZONES.add(new Zone(level.dimension(), center, radius, level.getGameTime() + ticks, resistRank, owner));
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48;
            level.sendParticles(Fx.particle("end_rod"), center.x + Math.cos(a) * radius, center.y + 1, center.z + Math.sin(a) * radius, 1, 0, 0.5, 0, 0.0);
        }
    }

    public static void clearAll() {
        ZONES.clear();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level instanceof ServerLevel level) || ZONES.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Zone> it = ZONES.iterator();
        while (it.hasNext()) {
            Zone zone = it.next();
            if (!zone.dimension().equals(level.dimension())) continue;
            if (now >= zone.until()) {
                release(level, zone);
                it.remove();
                continue;
            }
            for (Mob mob : level.getEntitiesOfClass(Mob.class, zone.box(), m -> m.isAlive() && !m.getUUID().equals(zone.owner()))) {
                if (Ranks.of(mob) >= zone.resistRank()) {
                    Fx.effect(mob, "slowness", 10, 3);
                    continue;
                }
                CompoundTag tag = mob.getPersistentData();
                if (!tag.getBoolean(FROZEN) && !mob.isNoAi()) {
                    tag.putBoolean(FROZEN, true);
                    mob.setNoAi(true);
                }
                mob.setDeltaMovement(Vec3.ZERO);
            }
            for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, zone.box(), p -> p.getOwner() == null || !p.getOwner().getUUID().equals(zone.owner()))) {
                CompoundTag tag = projectile.getPersistentData();
                if (!tag.getBoolean(FROZEN)) {
                    Vec3 v = projectile.getDeltaMovement();
                    tag.putBoolean(FROZEN, true);
                    tag.putDouble("tn_vx", v.x);
                    tag.putDouble("tn_vy", v.y);
                    tag.putDouble("tn_vz", v.z);
                    tag.putBoolean("tn_ng", projectile.isNoGravity());
                    projectile.setNoGravity(true);
                }
                projectile.setDeltaMovement(Vec3.ZERO);
                projectile.hurtMarked = true;
            }
        }
    }

    private static void release(ServerLevel level, Zone zone) {
        for (Entity entity : level.getEntities((Entity) null, zone.box().inflate(8), e -> e.getPersistentData().getBoolean(FROZEN))) {
            unfreeze(entity);
        }
    }

    private static void unfreeze(Entity entity) {
        CompoundTag tag = entity.getPersistentData();
        tag.remove(FROZEN);
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        } else if (entity instanceof Projectile projectile) {
            projectile.setNoGravity(tag.getBoolean("tn_ng"));
            projectile.setDeltaMovement(tag.getDouble("tn_vx"), tag.getDouble("tn_vy"), tag.getDouble("tn_vz"));
            projectile.hurtMarked = true;
        }
    }

    /** Sécurité : une entité restée figée (arrêt du serveur en plein arrêt du temps) est libérée à son retour. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (entity.getPersistentData().getBoolean(FROZEN) && ZONES.isEmpty()) unfreeze(entity);
    }
}
