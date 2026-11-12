package net.trounoir.util;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.trounoir.data.MoveDef;

import java.util.List;
import java.util.Optional;

/** Résolution d'identifiants venant des JSON vers les objets du jeu, avec repli sûr. */
public final class Lookup {
    private Lookup() {}

    public static SoundEvent sound(ResourceLocation id) {
        SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
        return sound != null ? sound : SoundEvents.PLAYER_ATTACK_SWEEP;
    }

    public static ParticleOptions particle(ResourceLocation id) {
        ParticleType<?> type = id == null ? null : BuiltInRegistries.PARTICLE_TYPE.get(id);
        return type instanceof SimpleParticleType simple ? simple : ParticleTypes.CRIT;
    }

    public static Holder<MobEffect> effect(ResourceLocation id) {
        if (id == null) return null;
        Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(id);
        return holder.orElse(null);
    }

    public static BlockState block(ResourceLocation id) {
        Block block = id == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(id);
        return block.defaultBlockState();
    }

    public static ItemStack stack(ResourceLocation id) {
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item);
    }

    public static void applyEffects(LivingEntity target, List<MoveDef.EffectSpec> effects) {
        for (MoveDef.EffectSpec spec : effects) {
            Holder<MobEffect> holder = effect(spec.effect());
            if (holder != null) target.addEffect(new MobEffectInstance(holder, spec.duration(), spec.amplifier()));
        }
    }
}
