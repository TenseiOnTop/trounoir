package net.trounoir.item;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeManager;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.MoveDef;
import net.trounoir.data.TNData;
import net.trounoir.technique.TechniqueExecutor;
import net.trounoir.util.Fx;
import net.trounoir.util.JsonUtil;
import net.trounoir.util.Lookup;

import java.util.List;

/**
 * Arme signature d'un personnage unique, ou relique d'un Dieu.
 * Clic gauche : coup de l'arme (avec son effet au toucher). Clic droit : son pouvoir spécial, à recharge.
 * Le pouvoir et l'effet au toucher sont décrits dans data/trounoir/trounoir/weapons/&lt;id&gt;.json.
 */
public class ArtifactItem extends Item {
    /** Identité fixe d'un objet (les dégâts et la cadence font partie de l'objet lui-même). */
    public record Spec(String id, double damage, double speed, boolean relic, boolean zoom) {}

    public final Spec spec;

    public ArtifactItem(Spec spec) {
        super(properties(spec));
        this.spec = spec;
    }

    private static Properties properties(Spec spec) {
        Properties p = new Properties().stacksTo(1).fireResistant().rarity(spec.relic() ? Rarity.EPIC : Rarity.RARE);
        if (spec.damage() > 0) {
            p.attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, spec.damage() - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, spec.speed() - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .build());
        }
        return p;
    }

    private AbsorbableDef def() {
        return TNData.WEAPONS.get(TrouNoir.rl(spec.id()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user instanceof ServerPlayer player && !player.getCooldowns().isOnCooldown(this)) {
            AbsorbableDef def = def();
            if (def != null && TechniqueExecutor.execute(player, LifeManager.get(player), def, 1.0)) {
                player.getCooldowns().addCooldown(this, Math.max(10, def.cooldownTicks));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Effet au toucher du clic gauche : feu, givre, poison… et une gerbe de particules lisible. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        AbsorbableDef def = def();
        if (def != null && target.level() instanceof ServerLevel level) {
            JsonObject hit = JsonUtil.obj(def.raw, "on_hit");
            double ignite = JsonUtil.num(hit, "ignite", 0);
            if (ignite > 0) target.igniteForSeconds((float) ignite);
            Lookup.applyEffects(target, MoveDef.EffectSpec.parseList(hit, "effects"));
            double heal = JsonUtil.num(hit, "heal", 0);
            if (heal > 0) attacker.heal((float) heal);
            String particle = JsonUtil.str(hit, "particle", "");
            if (!particle.isEmpty()) {
                Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
                Fx.burst(level, Lookup.particle(JsonUtil.id(particle)), at, 10, 0.3, 0.04);
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = this.getDescriptionId();
        if (spec.damage() > 0) tooltip.add(Component.translatable(key + ".left").withColor(0xB8AEDC));
        tooltip.add(Component.translatable(key + ".right").withColor(0xE6D08A));
        if (spec.zoom()) tooltip.add(Component.translatable("trounoir.item.zoom").withColor(0x8F86A8));
    }
}
