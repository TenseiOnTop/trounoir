package net.trounoir.character;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.util.Fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Le temps s'arrête : des flèches se figent en cercle autour de la victime, pointées sur elle.
 * Quand le temps reprend, toutes partent en même temps.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrozenVolley {
    private FrozenVolley() {}

    private record Volley(List<AbstractArrow> arrows, LivingEntity target, long releaseAt) {}

    private static final List<Volley> VOLLEYS = new ArrayList<>();

    public static void surround(ServerLevel level, LivingEntity owner, LivingEntity target, int count, double damage, int delayTicks) {
        List<AbstractArrow> arrows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            Vec3 at = target.position().add(Math.cos(a) * 3.5, 1.0 + (i % 3) * 0.8, Math.sin(a) * 3.5);
            Arrow arrow = new Arrow(level, owner, new ItemStack(Items.ARROW), null);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setBaseDamage(damage);
            arrow.setNoGravity(true);
            arrow.setPos(at.x, at.y, at.z);
            arrow.setDeltaMovement(Vec3.ZERO);
            Vec3 aim = target.getEyePosition().subtract(at).normalize();
            arrow.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
            arrow.setXRot((float) Math.toDegrees(-Math.asin(aim.y)));
            level.addFreshEntity(arrow);
            Fx.burst(level, Fx.dust(0xFFFFFF, 0.9F), at, 3, 0.1, 0.0);
            arrows.add(arrow);
        }
        Fx.play(level, target.position(), "block.amethyst_block.resonate", 1.0F, 0.6F);
        VOLLEYS.add(new Volley(arrows, target, level.getGameTime() + delayTicks));
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level instanceof ServerLevel level) || VOLLEYS.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Volley> it = VOLLEYS.iterator();
        while (it.hasNext()) {
            Volley volley = it.next();
            if (volley.target.level() != level) continue;
            if (now < volley.releaseAt) {
                for (AbstractArrow arrow : volley.arrows) arrow.setDeltaMovement(Vec3.ZERO);
                continue;
            }
            // Le temps reprend : tout part d'un coup.
            for (AbstractArrow arrow : volley.arrows) {
                if (!arrow.isAlive()) continue;
                Vec3 aim = volley.target.getEyePosition().subtract(arrow.position()).normalize();
                arrow.setNoGravity(false);
                arrow.setDeltaMovement(aim.scale(2.4));
            }
            Fx.play(level, volley.target.position(), "entity.arrow.shoot", 1.0F, 0.7F);
            Fx.play(level, volley.target.position(), "block.beacon.deactivate", 1.0F, 1.6F);
            it.remove();
        }
    }
}
