package net.trounoir.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Création des projectiles vanilla utilisables par les techniques et les attaques de boss. */
public final class Projectiles {
    private Projectiles() {}

    /**
     * @param kind small_fireball, large_fireball, wither_skull, dragon_fireball ou arrow
     */
    public static Entity launch(ServerLevel level, LivingEntity owner, String kind, Vec3 from, Vec3 direction, double speed, double damage, int power) {
        Vec3 dir = direction.normalize();
        Entity shot = switch (kind) {
            case "large_fireball" -> new LargeFireball(level, owner, dir.scale(speed), Math.max(1, power));
            case "wither_skull" -> new WitherSkull(level, owner, dir.scale(speed));
            case "dragon_fireball" -> new DragonFireball(level, owner, dir.scale(speed));
            case "arrow" -> {
                Arrow arrow = new Arrow(level, owner, new ItemStack(Items.ARROW), null);
                arrow.shoot(dir.x, dir.y, dir.z, (float) (speed * 2.0), 0.5F);
                arrow.setBaseDamage(damage);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                yield arrow;
            }
            default -> new SmallFireball(level, owner, dir.scale(speed));
        };
        shot.setPos(from.x, from.y, from.z);
        level.addFreshEntity(shot);
        return shot;
    }
}
