package net.trounoir.util;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Retours visuels et sonores. Sons, particules et effets vanilla sont désignés par leur identifiant
 * (ex. "entity.enderman.teleport") : un identifiant inconnu retombe sur une valeur neutre.
 */
public final class Fx {
    private Fx() {}

    /** Poussière colorée : la couleur en RGB (0xRRGGBB), la taille en blocs. */
    public static ParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new org.joml.Vector3f(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F), size);
    }

    /** Tremblement d'écran chez un joueur ; power entre 0 et 1. */
    public static void shake(ServerPlayer player, double power) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("power", Math.max(0.05, Math.min(1.0, power)));
        net.trounoir.network.Net.toClient(player, "hit", json);
    }

    /** Tremblement d'écran chez tous les joueurs proches, d'autant plus fort qu'ils sont près. */
    public static void shake(ServerLevel level, Vec3 at, double radius, double power) {
        for (ServerPlayer player : level.players()) {
            double d = player.position().distanceTo(at);
            if (d <= radius) shake(player, power * (1.0 - d / (radius + 1)));
        }
    }

    public static SoundEvent sound(String vanillaId) {
        return Lookup.sound(ResourceLocation.withDefaultNamespace(vanillaId));
    }

    public static ParticleOptions particle(String vanillaId) {
        return Lookup.particle(ResourceLocation.withDefaultNamespace(vanillaId));
    }

    public static void play(ServerLevel level, Vec3 at, String sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound(sound), SoundSource.NEUTRAL, volume, pitch);
    }

    public static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.NEUTRAL, volume, pitch);
    }

    /** Son entendu du seul joueur visé, où qu'il soit. */
    public static void playTo(ServerPlayer player, String sound, float volume, float pitch) {
        player.playNotifySound(sound(sound), SoundSource.NEUTRAL, volume, pitch);
    }

    public static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    public static void burst(ServerLevel level, String particle, Vec3 at, int count, double spread, double speed) {
        burst(level, particle(particle), at, count, spread, speed);
    }

    /** Cercle horizontal de particules. */
    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius) {
        int points = (int) Math.max(12, Math.min(72, radius * 7));
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.15, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0.0);
        }
    }

    /** Segment de particules. */
    public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double perBlock) {
        int points = (int) Math.max(2, Math.min(200, from.distanceTo(to) * perBlock));
        for (int i = 0; i <= points; i++) {
            Vec3 p = from.lerp(to, i / (double) points);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);
        }
    }

    /** Titre bref à l'écran (apparition 10, maintien selon l'appel, disparition 20 ticks). */
    public static void title(ServerPlayer player, Component title, Component subtitle, int stay) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, stay, 20));
        if (subtitle != null) player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    public static void effect(LivingEntity target, String vanillaId, int duration, int amplifier) {
        Holder<MobEffect> holder = Lookup.effect(ResourceLocation.withDefaultNamespace(vanillaId));
        if (holder != null) target.addEffect(new MobEffectInstance(holder, duration, amplifier, true, false, false));
    }
}
