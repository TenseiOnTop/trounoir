package net.trounoir.character;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.TNData;
import net.trounoir.faction.Reputation;
import net.trounoir.util.Fx;
import net.trounoir.util.Lookup;

/**
 * Entité unique de tous les personnages humains du mod. Son identité (statistiques, apparence,
 * comportement, attaques) vient entièrement d'une {@link CharacterDef} chargée depuis le datapack.
 */
public class CharacterEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DEF_ID = SynchedEntityData.defineId(CharacterEntity.class, EntityDataSerializers.STRING);

    private final MoveRunner runner = new MoveRunner(this);
    private BlockPos home;
    private boolean provoked;
    private int phase;
    private ServerBossEvent bossBar;

    public CharacterEntity(EntityType<? extends CharacterEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DEF_ID, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4) {
            @Override
            public boolean canUse() {
                CharacterDef def = def();
                return def != null && def.behavior.equals("civilian") && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.001F) {
            @Override
            public boolean canUse() {
                return getTarget() == null && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                CharacterDef def = def();
                return def != null && !def.behavior.equals("civilian") && super.canUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isHostileTowards));
    }

    // ------------------------------------------------------------------ définition

    public String defId() {
        return this.entityData.get(DEF_ID);
    }

    public CharacterDef def() {
        return TNData.character(defId());
    }

    public void setHome(BlockPos home) {
        this.home = home;
        CharacterDef def = def();
        if (home != null) this.restrictTo(home, def != null && def.isBoss() ? 14 : 24);
    }

    /** Donne au personnage son identité. Appelé une fois, à l'apparition. */
    public void applyDef(CharacterDef def) {
        this.entityData.set(DEF_ID, def.id.toString());
        setBase(Attributes.MAX_HEALTH, def.health);
        setBase(Attributes.ATTACK_DAMAGE, def.damage);
        setBase(Attributes.ARMOR, def.armor);
        setBase(Attributes.MOVEMENT_SPEED, def.speed);
        setBase(Attributes.KNOCKBACK_RESISTANCE, def.knockbackResistance);
        setBase(Attributes.FOLLOW_RANGE, def.followRange);
        setBase(Attributes.SCALE, def.scale);
        this.setHealth(this.getMaxHealth());
        this.setCustomName(def.display());
        this.setCustomNameVisible(def.unique);
        if (def.heldItem != null) {
            this.setItemSlot(EquipmentSlot.MAINHAND, Lookup.stack(def.heldItem));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        this.xpReward = def.rank * 6;
    }

    private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    public int phase() {
        return phase;
    }

    public boolean isProvoked() {
        return provoked;
    }

    public void provoke(LivingEntity by) {
        this.provoked = true;
        CharacterDef def = def();
        if (by != null && def != null && !def.behavior.equals("civilian") && this.getTarget() == null && by.isAlive()) {
            if (!(by instanceof Player player) || (!player.isCreative() && !player.isSpectator())) this.setTarget(by);
        }
    }

    /** Ce personnage attaque-t-il cet être à vue ? */
    public boolean isHostileTowards(LivingEntity other) {
        CharacterDef def = def();
        if (def == null || def.behavior.equals("civilian") || !(other instanceof Player)) return false;
        if (def.alwaysHostile() || provoked) return true;
        return this.getServer() != null && Reputation.hostile(this.getServer(), def.faction);
    }

    // ------------------------------------------------------------------ vie

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        CharacterDef def = def();
        if (def == null) return;
        runner.tick(def);

        int newPhase = 0;
        float ratio = this.getHealth() / this.getMaxHealth();
        for (double threshold : def.phases) if (ratio <= threshold) newPhase++;
        if (newPhase > phase && this.level() instanceof ServerLevel level) {
            // Changement de phase : un signal net, reconnaissable d'une vie à l'autre.
            Vec3 at = this.position().add(0, this.getBbHeight() * 0.5, 0);
            Fx.play(level, at, "entity.wither.spawn", 0.5F, 1.4F);
            Fx.burst(level, "soul_fire_flame", at, 40, 0.8, 0.08);
            runner.interrupt();
        }
        phase = newPhase;

        if (def.bossBar) {
            if (bossBar == null) {
                bossBar = new ServerBossEvent(def.display(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            }
            bossBar.setProgress(Math.max(0F, Math.min(1F, ratio)));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            this.provoke(attacker);
            // Les témoins de la même faction prennent parti.
            CharacterDef def = def();
            if (def != null && attacker instanceof Player) {
                for (CharacterEntity ally : this.level().getEntitiesOfClass(CharacterEntity.class, this.getBoundingBox().inflate(18), e -> e != this)) {
                    CharacterDef other = ally.def();
                    if (other != null && !other.faction.isEmpty() && other.faction.equals(def.faction)) ally.provoke(attacker);
                }
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer && !isHostileTowards(player)) {
            Speech.speak(serverPlayer, this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        CharacterDef def = def();
        if (def != null && def.bossBar) {
            if (bossBar == null) {
                bossBar = new ServerBossEvent(def.display(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            }
            bossBar.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossBar != null) bossBar.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (bossBar != null) bossBar.removeAllPlayers();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return Fx.sound("entity.player.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Fx.sound("entity.player.death");
    }

    // ------------------------------------------------------------------ sauvegarde

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("TnDef", defId());
        tag.putBoolean("TnProvoked", provoked);
        if (home != null) tag.putLong("TnHome", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DEF_ID, tag.getString("TnDef"));
        this.provoked = tag.getBoolean("TnProvoked");
        CharacterDef loaded = def();
        if (loaded != null) this.setCustomName(loaded.display());
        if (tag.contains("TnHome")) setHome(BlockPos.of(tag.getLong("TnHome")));
    }
}
