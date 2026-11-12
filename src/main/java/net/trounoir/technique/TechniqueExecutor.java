package net.trounoir.technique;

import com.google.gson.JsonObject;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.trounoir.absorption.LifeData;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.MoveDef;
import net.trounoir.util.Fx;
import net.trounoir.util.JsonUtil;
import net.trounoir.util.Lookup;
import net.trounoir.util.Projectiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Exécution des techniques actives. Les types sont codés ici ; chaque technique du datapack
 * choisit un type et ses paramètres (portée, dégâts, effets…).
 */
public final class TechniqueExecutor {
    private TechniqueExecutor() {}

    public static boolean execute(ServerPlayer player, LifeData life, AbsorbableDef def, double potency) {
        JsonObject p = def.params;
        return switch (def.techniqueType) {
            case "dash" -> dash(player, life, p, potency);
            case "leap" -> leap(player, life, p);
            case "blink" -> blink(player, p);
            case "projectile" -> projectile(player, p, potency);
            case "aoe" -> aoe(player, p, potency);
            case "beam" -> beam(player, p, potency);
            case "buff" -> buff(player, p, potency);
            case "time_stop" -> timeStop(player, p);
            default -> false;
        };
    }

    /** Une technique peut-elle toucher cette entité ? (ni soi-même, ni ses familiers, ni un décor.) */
    public static boolean canHit(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive() || target instanceof ArmorStand) return false;
        if (target instanceof TamableAnimal pet && pet.isOwnedBy(player)) return false;
        if (target instanceof Player other) return player.canHarmPlayer(other);
        return true;
    }

    private static DamageSource source(ServerPlayer player, JsonObject p) {
        return "physical".equals(JsonUtil.str(p, "damage_type", "magic"))
                ? player.damageSources().playerAttack(player)
                : player.damageSources().indirectMagic(player, player);
    }

    private static void sound(ServerPlayer player, JsonObject p, String fallback) {
        String id = JsonUtil.str(p, "sound", "");
        SoundEvent sound = id.isEmpty() ? Fx.sound(fallback) : Lookup.sound(JsonUtil.id(id));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static ParticleOptions particle(JsonObject p, ParticleOptions fallback) {
        String id = JsonUtil.str(p, "particle", "");
        return id.isEmpty() ? fallback : Lookup.particle(JsonUtil.id(id));
    }

    private static void hit(ServerPlayer player, LivingEntity target, JsonObject p, double potency) {
        double damage = JsonUtil.num(p, "damage", 0) * potency;
        if (damage > 0) target.hurt(source(player, p), (float) damage);
        double ignite = JsonUtil.num(p, "ignite", 0);
        if (ignite > 0) target.igniteForSeconds((float) ignite);
        Lookup.applyEffects(target, MoveDef.EffectSpec.parseList(p, "effects"));
        Vec3 away = target.position().subtract(player.position());
        double knockback = JsonUtil.num(p, "knockback", 0);
        double pull = JsonUtil.num(p, "pull", 0);
        if (knockback > 0) target.knockback(knockback, -away.x, -away.z);
        if (pull > 0) target.knockback(pull, away.x, away.z);
    }

    private static boolean dash(ServerPlayer player, LifeData life, JsonObject p, double potency) {
        Vec3 look = player.getLookAngle();
        double strength = JsonUtil.num(p, "strength", 1.6);
        player.setDeltaMovement(look.x * strength, Math.max(0.15, look.y * strength * 0.5), look.z * strength);
        player.hurtMarked = true;
        life.dashTicks = JsonUtil.integer(p, "ticks", 8);
        life.dashDamage = JsonUtil.num(p, "damage", 4) * potency;
        life.dashHit.clear();
        life.noFallUntil = player.serverLevel().getGameTime() + 40;
        sound(player, p, "entity.player.attack.sweep");
        player.serverLevel().sendParticles(Fx.particle("cloud"), player.getX(), player.getY() + 0.5, player.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        return true;
    }

    private static boolean leap(ServerPlayer player, LifeData life, JsonObject p) {
        Vec3 look = player.getLookAngle();
        double up = JsonUtil.num(p, "strength", 1.0);
        double forward = JsonUtil.num(p, "forward", 0.6);
        player.setDeltaMovement(look.x * forward, up, look.z * forward);
        player.hurtMarked = true;
        life.noFallUntil = player.serverLevel().getGameTime() + Math.round(JsonUtil.num(p, "safe_fall_seconds", 4) * 20);
        sound(player, p, "entity.slime.jump");
        return true;
    }

    private static boolean blink(ServerPlayer player, JsonObject p) {
        double range = JsonUtil.num(p, "range", 12);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(player.getLookAngle().scale(0.6));
        ServerLevel level = player.serverLevel();
        level.sendParticles(Fx.particle("portal"), player.getX(), player.getY() + 1, player.getZ(), 24, 0.3, 0.6, 0.3, 0.1);
        player.teleportTo(target.x, target.y - 1.0, target.z);
        player.fallDistance = 0;
        level.sendParticles(Fx.particle("portal"), target.x, target.y, target.z, 24, 0.3, 0.6, 0.3, 0.1);
        sound(player, p, "entity.enderman.teleport");
        return true;
    }

    private static boolean projectile(ServerPlayer player, JsonObject p, double potency) {
        ServerLevel level = player.serverLevel();
        String kind = JsonUtil.str(p, "projectile", "small_fireball");
        int count = Math.max(1, JsonUtil.integer(p, "count", 1));
        double spread = JsonUtil.num(p, "spread", 6);
        double speed = JsonUtil.num(p, "speed", 1.5);
        Vec3 eye = player.getEyePosition();
        for (int i = 0; i < count; i++) {
            float offset = count == 1 ? 0 : (float) ((i - (count - 1) / 2.0) * spread);
            Vec3 dir = player.getLookAngle().yRot((float) Math.toRadians(offset)).normalize();
            Vec3 start = eye.add(dir.scale(1.2));
            Projectiles.launch(level, player, kind, start.add(0, -0.2, 0), dir, speed, JsonUtil.num(p, "damage", 2) * potency, JsonUtil.integer(p, "power", 1));
        }
        sound(player, p, "entity.blaze.shoot");
        return true;
    }

    private static boolean aoe(ServerPlayer player, JsonObject p, double potency) {
        ServerLevel level = player.serverLevel();
        double radius = JsonUtil.num(p, "radius", 4);
        double cone = JsonUtil.num(p, "cone_angle", 0);
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> {
            if (!canHit(player, e) || e.distanceToSqr(player) > radius * radius) return false;
            if (cone <= 0) return true;
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.lengthSqr() < 1.0E-4) return true;
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, look.dot(to.normalize())))));
            return angle <= cone / 2.0;
        });
        for (LivingEntity target : targets) hit(player, target, p, potency);
        selfEffects(player, p, potency);

        ParticleOptions particle = particle(p, Fx.particle("sweep_attack"));
        if (cone > 0) {
            for (int i = 1; i <= 12; i++) {
                Vec3 at = eye.add(look.scale(radius * i / 12.0));
                level.sendParticles(particle, at.x, at.y - 0.3, at.z, 3, 0.15 * i / 3.0, 0.15, 0.15 * i / 3.0, 0.01);
            }
        } else {
            for (int i = 0; i < 32; i++) {
                double a = Math.PI * 2 * i / 32;
                level.sendParticles(particle, player.getX() + Math.cos(a) * radius, player.getY() + 0.4, player.getZ() + Math.sin(a) * radius, 1, 0.1, 0.1, 0.1, 0.0);
            }
            level.sendParticles(particle, player.getX(), player.getY() + 0.8, player.getZ(), 12, radius / 3, 0.3, radius / 3, 0.02);
        }
        sound(player, p, "entity.dragon_fireball.explode");
        return true;
    }

    private static boolean beam(ServerPlayer player, JsonObject p, double potency) {
        ServerLevel level = player.serverLevel();
        double range = JsonUtil.num(p, "range", 24);
        double width = JsonUtil.num(p, "width", 0.6);
        boolean pierce = JsonUtil.bool(p, "pierce", false);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 far = eye.add(look.scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? far : block.getLocation();

        List<LivingEntity> hits = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(width + 1.0), t -> canHit(player, t))) {
            Optional<Vec3> clip = e.getBoundingBox().inflate(width).clip(eye, end);
            if (clip.isPresent() || e.getBoundingBox().inflate(width).contains(eye)) hits.add(e);
        }
        hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        if (!pierce && hits.size() > 1) {
            LivingEntity first = hits.get(0);
            end = first.position().add(0, first.getBbHeight() * 0.5, 0);
            hits = List.of(first);
        }
        for (LivingEntity target : hits) hit(player, target, p, potency);

        ParticleOptions particle = particle(p, Fx.particle("end_rod"));
        double length = eye.distanceTo(end);
        int points = (int) Math.min(160, Math.max(4, length * 1.5));
        for (int i = 1; i <= points; i++) {
            Vec3 at = eye.lerp(end, i / (double) points);
            level.sendParticles(particle, at.x, at.y - 0.15, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        sound(player, p, "entity.warden.sonic_boom");
        return true;
    }

    private static void selfEffects(ServerPlayer player, JsonObject p, double potency) {
        Lookup.applyEffects(player, MoveDef.EffectSpec.parseList(p, "self_effects"));
        double heal = JsonUtil.num(p, "heal", 0);
        if (heal > 0) player.heal((float) (heal * potency));
    }

    private static boolean buff(ServerPlayer player, JsonObject p, double potency) {
        selfEffects(player, p, potency);
        ParticleOptions particle = particle(p, Fx.particle("enchant"));
        player.serverLevel().sendParticles(particle, player.getX(), player.getY() + 1, player.getZ(), 24, 0.4, 0.6, 0.4, 0.05);
        sound(player, p, "entity.illusioner.cast_spell");
        return true;
    }

    private static boolean timeStop(ServerPlayer player, JsonObject p) {
        TimeStop.start(player.serverLevel(), player.position(), JsonUtil.num(p, "radius", 24),
                JsonUtil.integer(p, "duration", 100), JsonUtil.integer(p, "resist_rank", 8), player.getUUID());
        sound(player, p, "block.beacon.deactivate");
        return true;
    }
}
