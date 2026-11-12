package net.trounoir.suke;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.config.TNConfig;
import net.trounoir.data.SimpleDefs;
import net.trounoir.faction.Reputation;
import net.trounoir.network.Net;
import net.trounoir.util.Fx;
import net.trounoir.world.Crossing;
import net.trounoir.world.SiteManager;
import net.trounoir.world.WorldState;

/**
 * Suke, le tireur qui veille sur tout Diviniarth.
 * <p>
 * Tant qu'il vit, la notoriété du joueur sur le continent attire ses tirs. La mécanique est lisible et déjouable :
 * <ol>
 *   <li>un REFLET apparaît dans le ciel, du côté de sa tour, accompagné d'un tintement ;</li>
 *   <li>peu avant la fin du reflet, Suke fige sa visée sur la position du joueur ;</li>
 *   <li>le tir part : il est arrêté par tout bloc entre le reflet et la cible (se mettre à couvert),
 *       et manque si le joueur a quitté le point visé (pas de côté de dernière seconde).</li>
 * </ol>
 * Tuer Suke éteint le système ; l'absorber donne sa portée.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SukeSystem {
    private SukeSystem() {}

    public static final String SUKE = "trounoir:suke";
    private static final String TOWER = "trounoir:aiguille_de_veille";
    private static final int AIM_LOCK_TICKS = 7;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || !player.isAlive()) return;
        LifeData life = LifeManager.getOrNull(player);
        if (life == null) return;
        ServerLevel level = player.serverLevel();
        boolean onContinent = level.dimension().location().equals(Crossing.DIVINIARTH);
        long now = level.getGameTime();

        if (player.tickCount % 200 == 0) updateNotoriety(player, life, onContinent);

        if (!onContinent || player.isCreative() || player.isSpectator() || WorldState.get(player.server).isDead(SUKE)) {
            life.sukeFireAt = 0;
            life.sukeGlint = null;
            return;
        }
        int[] tower = SiteManager.positionOf(player.server, TOWER);
        if (tower == null) return;
        Vec3 top = new Vec3(tower[0] + 0.5, tower[1] + 60, tower[2] + 0.5);
        double duel = TNConfig.SUKE_DUEL_RADIUS.get();
        double dx = top.x - player.getX();
        double dz = top.z - player.getZ();
        if (dx * dx + dz * dz < duel * duel) {
            // Au pied de la tour, Suke se bat lui-même : le tir systémique se tait.
            life.sukeFireAt = 0;
            life.sukeGlint = null;
            return;
        }

        if (life.sukeFireAt > 0) {
            aimAndFire(player, life, level, now);
            return;
        }
        if (life.notoriety < TNConfig.SUKE_THRESHOLD.get()) {
            life.sukeNextShot = 0;
            return;
        }
        if (life.sukeNextShot == 0) life.sukeNextShot = now + interval(life) / 2;
        if (now >= life.sukeNextShot) startGlint(player, life, top, now);
    }

    private static long interval(LifeData life) {
        double threshold = TNConfig.SUKE_THRESHOLD.get();
        double t = Math.max(0, Math.min(1, (life.notoriety - threshold) / Math.max(1, 100 - threshold)));
        return Math.round(TNConfig.SUKE_MAX_INTERVAL.get() + (TNConfig.SUKE_MIN_INTERVAL.get() - TNConfig.SUKE_MAX_INTERVAL.get()) * t);
    }

    private static void startGlint(ServerPlayer player, LifeData life, Vec3 towerTop, long now) {
        Vec3 eye = player.getEyePosition();
        Vec3 toTower = towerTop.subtract(eye);
        double distance = toTower.length();
        // Le reflet est montré à portée de vue, sur la ligne qui mène à la tour.
        Vec3 glint = eye.add(toTower.normalize().scale(Math.min(distance, 44)));
        if (glint.y < eye.y + 10) glint = new Vec3(glint.x, eye.y + 10, glint.z);
        life.sukeGlint = new double[]{glint.x, glint.y, glint.z};
        life.sukeAim = null;
        life.sukeFireAt = now + TNConfig.SUKE_TELEGRAPH_TICKS.get();
        Fx.playTo(player, "block.amethyst_block.resonate", 1.0F, 2.0F);
        Fx.playTo(player, "block.note_block.bell", 0.8F, 1.8F);
    }

    private static void aimAndFire(ServerPlayer player, LifeData life, ServerLevel level, long now) {
        if (life.sukeGlint == null) {
            life.sukeFireAt = 0;
            return;
        }
        Vec3 glint = new Vec3(life.sukeGlint[0], life.sukeGlint[1], life.sukeGlint[2]);
        long left = life.sukeFireAt - now;
        if (left > 0) {
            if (left % 2 == 0) {
                level.sendParticles(player, Fx.particle("end_rod"), true, glint.x, glint.y, glint.z, 3, 0.15, 0.15, 0.15, 0.0);
                level.sendParticles(player, Fx.particle("flash"), true, glint.x, glint.y, glint.z, 1, 0, 0, 0, 0.0);
            }
            if (left == AIM_LOCK_TICKS) {
                Vec3 aim = player.position().add(0, player.getBbHeight() * 0.5, 0);
                life.sukeAim = new double[]{aim.x, aim.y, aim.z};
                Fx.playTo(player, "item.crossbow.loading_end", 1.0F, 0.6F);
            }
            return;
        }

        // Le tir part.
        life.sukeFireAt = 0;
        life.sukeNextShot = now + interval(life);
        Vec3 aim = life.sukeAim == null
                ? player.position().add(0, player.getBbHeight() * 0.5, 0)
                : new Vec3(life.sukeAim[0], life.sukeAim[1], life.sukeAim[2]);
        life.sukeGlint = null;
        life.sukeAim = null;

        Vec3 direction = aim.subtract(glint).normalize();
        Vec3 through = aim.add(direction.scale(3.0));
        BlockHitResult block = level.clip(new ClipContext(glint, through, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? through : block.getLocation();
        boolean hits = player.getBoundingBox().inflate(0.35).clip(glint, end).isPresent();

        Fx.line(level, Fx.particle("end_rod"), glint, end, 1.5);
        Fx.play(level, end, "entity.firework_rocket.blast", 2.0F, 0.5F);
        Fx.burst(level, "crit", end, 14, 0.25, 0.3);

        if (hits) {
            life.lastSniper = SUKE;
            player.hurt(level.damageSources().magic(), TNConfig.SUKE_DAMAGE.get().floatValue() * (float) rankFactor(life));
            if (player.isAlive()) life.lastSniper = null;
        } else {
            life.sukeDodged++;
            if (life.sukeDodged == 1 || life.sukeDodged == 5 || life.sukeDodged == 20) {
                life.feat("feat.trounoir.dodged_suke", Integer.toString(life.sukeDodged));
            }
            player.displayClientMessage(Component.translatable("trounoir.suke.missed"), true);
        }
    }

    /** Suke est un Hypernova (rang VI) : le tir obéit aux mêmes écarts de rang que le reste du combat. */
    private static double rankFactor(LifeData life) {
        return TNConfig.gapMultiplier(TNConfig.INCOMING_GAP_MULTIPLIERS, 6 - life.rank);
    }

    private static void updateNotoriety(ServerPlayer player, LifeData life, boolean onContinent) {
        double before = life.notoriety;
        if (onContinent) {
            SimpleDefs.SiteDef near = SiteManager.nearest(player, 80);
            boolean inImperialPlace = near != null && near.faction().equals("azalee");
            if (inImperialPlace && Reputation.hostile(player.server, "azalee")) {
                life.notoriety = Math.min(100, life.notoriety + 2);       // repéré en ville
            } else if (!inImperialPlace) {
                life.notoriety = Math.max(0, life.notoriety - 1);         // on se fait oublier loin des cités
            }
        } else {
            life.notoriety = Math.max(0, life.notoriety - 2);
        }
        if ((int) before != (int) life.notoriety) Net.syncLife(player);
    }
}
