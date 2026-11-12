package net.trounoir.character;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.TNData;
import net.trounoir.registry.ModRegistry;
import net.trounoir.world.WorldState;

import java.util.Optional;

/** Apparition et identification des personnages définis par données. */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Characters {
    private Characters() {}

    /** Marque posée sur une entité vanilla qui incarne un personnage (demi-dieux des repaires). */
    public static final String TAG_DEF = "tn_unique";
    public static final String TAG_EPOCH = "tn_epoch";
    public static final String TAG_GENERATION = "tn_generation";

    /** Définition de personnage portée par une entité, ou nul pour une créature ordinaire. */
    public static CharacterDef defOf(LivingEntity entity) {
        if (entity instanceof CharacterEntity character) return character.def();
        CompoundTag tag = entity.getPersistentData();
        return tag.contains(TAG_DEF) ? TNData.character(tag.getString(TAG_DEF)) : null;
    }

    /**
     * Fait apparaître un personnage. Un unique déjà mort ou déjà présent dans cette vie n'apparaît pas.
     *
     * @return l'entité créée, ou nul
     */
    public static LivingEntity spawn(ServerLevel level, CharacterDef def, BlockPos pos, BlockPos home) {
        WorldState state = WorldState.get(level.getServer());
        String id = def.id.toString();
        if (def.unique) {
            if (state.isDead(id) || state.spawnedUniques.contains(id)) return null;
        }
        LivingEntity entity;
        if (def.isModEntity()) {
            CharacterEntity character = ModRegistry.CHARACTER.get().create(level);
            if (character == null) return null;
            character.applyDef(def);
            character.setHome(home);
            entity = character;
        } else {
            Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(def.baseEntity);
            if (type.isEmpty()) return null;
            Entity raw = type.get().create(level);
            if (!(raw instanceof Mob mob)) return null;
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            setBase(mob, Attributes.MAX_HEALTH, def.health);
            setBase(mob, Attributes.ATTACK_DAMAGE, def.damage);
            setBase(mob, Attributes.ARMOR, def.armor);
            setBase(mob, Attributes.MOVEMENT_SPEED, def.speed);
            setBase(mob, Attributes.KNOCKBACK_RESISTANCE, def.knockbackResistance);
            setBase(mob, Attributes.FOLLOW_RANGE, def.followRange);
            setBase(mob, Attributes.SCALE, def.scale);
            mob.setHealth(mob.getMaxHealth());
            mob.setCustomName(def.display());
            mob.setCustomNameVisible(true);
            mob.setPersistenceRequired();
            mob.restrictTo(home, 16);
            mob.getPersistentData().putString(TAG_DEF, id);
            entity = mob;
        }
        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        CompoundTag tag = entity.getPersistentData();
        tag.putInt(TAG_EPOCH, state.epoch);
        tag.putInt(TAG_GENERATION, state.generation(id));
        if (!level.addFreshEntity(entity)) return null;
        if (def.unique) {
            state.spawnedUniques.add(id);
            state.homes.put(id, new int[]{home.getX(), home.getY(), home.getZ()});
            state.homeDimensions.put(id, level.dimension().location().toString());
        }
        return entity;
    }

    private static void setBase(Mob mob, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    /**
     * Un personnage d'une vie passée (renaissance douce) ou une copie périmée d'un unique
     * (déplacé par une vengeance) ne doit pas réapparaître quand son tronçon se recharge.
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        CharacterDef def = defOf(living);
        if (def == null) return;
        WorldState state = WorldState.get(level.getServer());
        CompoundTag tag = living.getPersistentData();
        boolean stale = tag.getInt(TAG_EPOCH) != state.epoch
                || (def.unique && (tag.getInt(TAG_GENERATION) != state.generation(def.id.toString()) || state.isDead(def.id.toString())));
        if (stale) event.setCanceled(true);
    }
}
