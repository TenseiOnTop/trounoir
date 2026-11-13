package net.trounoir.character;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.trounoir.config.TNConfig;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.MoveDef;
import net.trounoir.data.TNData;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.util.Fx;
import net.trounoir.util.Lookup;
import net.trounoir.util.Projectiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Déroulé des attaques d'un personnage : choisir, ANNONCER, puis frapper.
 * Pendant l'annonce, la forme exacte de l'attaque (cercle, ligne, cône) est dessinée au sol,
 * figée à l'endroit visé : sortir de la forme, c'est esquiver. C'est ce qui rend les boss apprenables.
 */
final class MoveRunner {
    private enum State { IDLE, TELEGRAPH, RECOVER }

    private final CharacterEntity self;
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private State state = State.IDLE;
    private MoveDef current;
    private int timer;
    private int total = 1;
    private int globalCooldown = 20;
    private final List<Vec3> anchors = new ArrayList<>();
    private Vec3 direction = new Vec3(0, 0, 1);

    MoveRunner(CharacterEntity self) {
        this.self = self;
    }

    void interrupt() {
        state = State.RECOVER;
        timer = 20;
        current = null;
    }

    void tick(CharacterDef def) {
        if (!(self.level() instanceof ServerLevel level)) return;
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        if (globalCooldown > 0) globalCooldown--;
        LivingEntity target = self.getTarget();

        switch (state) {
            case IDLE -> {
                if (target == null || !target.isAlive()) return;
                List<MoveDef> moves = def.moves.isEmpty() ? List.of(MoveDef.basic(def.damage)) : def.moves;
                double distance = self.distanceTo(target);
                double reach = Double.MAX_VALUE;
                for (MoveDef move : moves) {
                    if (usableInPhase(move)) reach = Math.min(reach, move.maxRange);
                }
                if (distance > reach * 0.85) self.getNavigation().moveTo(target, 1.0);
                else self.getNavigation().stop();
                self.getLookControl().setLookAt(target);
                if (globalCooldown > 0) return;

                MoveDef chosen = choose(moves, distance);
                if (chosen != null) begin(level, chosen, target);
            }
            case TELEGRAPH -> {
                self.getNavigation().stop();
                if (!anchors.isEmpty() && current != null && current.shape == MoveDef.Shape.CIRCLE) {
                    Vec3 a = anchors.get(0);
                    self.getLookControl().setLookAt(a.x, a.y + 1, a.z);
                }
                timer--;
                if (current != null) drawTelegraph(level, current, timer <= 6);
                if (timer <= 0) {
                    if (current != null) execute(level, def, current, target);
                    state = State.RECOVER;
                    timer = 10;
                }
            }
            case RECOVER -> {
                if (--timer <= 0) {
                    state = State.IDLE;
                    current = null;
                }
            }
        }
    }

    private boolean usableInPhase(MoveDef move) {
        return self.phase() >= move.minPhase && self.phase() <= move.maxPhase;
    }

    private MoveDef choose(List<MoveDef> moves, double distance) {
        int total = 0;
        List<MoveDef> candidates = new ArrayList<>();
        for (MoveDef move : moves) {
            if (!usableInPhase(move) || cooldowns.getOrDefault(move.id, 0) > 0) continue;
            if (distance < move.minRange || distance > move.maxRange) continue;
            candidates.add(move);
            total += move.weight;
        }
        if (candidates.isEmpty()) return null;
        int roll = self.getRandom().nextInt(total);
        for (MoveDef move : candidates) {
            roll -= move.weight;
            if (roll < 0) return move;
        }
        return candidates.get(0);
    }

    private void begin(ServerLevel level, MoveDef move, LivingEntity target) {
        current = move;
        state = State.TELEGRAPH;
        double length = move.telegraph;
        // Souvenir mécanique : un boss dont on a déjà observé la faiblesse se lit plus facilement.
        if (target instanceof ServerPlayer player) {
            PersistentStore.Memory memory = PersistentStore.get(player.server).memory(player.getUUID());
            if (memory.weaknesses.contains(self.defId())) length *= 1.0 + TNConfig.FAMILIARITY_TELEGRAPH_BONUS.get();
        }
        timer = (int) Math.round(length);
        total = Math.max(1, timer);

        anchors.clear();
        Vec3 origin = self.position();
        Vec3 to = target.position().subtract(origin);
        Vec3 flat = new Vec3(to.x, 0, to.z);
        direction = flat.lengthSqr() < 1.0E-4 ? self.getLookAngle() : flat.normalize();
        if (move.shape == MoveDef.Shape.CIRCLE) {
            Vec3 first = move.anchorTarget ? target.position() : origin;
            anchors.add(first);
            for (int i = 1; i < move.count; i++) {
                double a = self.getRandom().nextDouble() * Math.PI * 2;
                double r = move.spread * (0.4 + 0.6 * self.getRandom().nextDouble());
                anchors.add(first.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
        } else {
            anchors.add(origin);
        }
        Fx.play(level, origin, "block.beacon.activate", 0.8F, 1.5F);
        Fx.play(level, origin, Lookup.sound(move.sound), 1.0F, 0.6F);
        self.swing(InteractionHand.MAIN_HAND);
    }

    /**
     * L'annonce est vivante : un bord coloré qui tourne, un remplissage qui grandit jusqu'au déclenchement,
     * un battement sonore qui monte. À la fin, le bord vire au blanc chaud : c'est l'instant de sortir.
     */
    private void drawTelegraph(ServerLevel level, MoveDef move, boolean imminent) {
        float progress = 1F - timer / (float) total;
        ParticleOptions edge = Fx.dust(imminent ? 0xFFF4D0 : move.color, imminent ? 1.6F : 1.1F);
        ParticleOptions fill = Fx.dust(move.color, 0.8F);
        double spin = (total - timer) * 0.12;
        if (timer % 10 == 0) Fx.play(level, anchors.get(0), "block.note_block.hat", 0.6F, 0.7F + progress);
        if (move.style.equals("za_warudo") && timer % 8 == 0) Fx.play(level, anchors.get(0), "block.note_block.cow_bell", 0.9F, 0.5F);
        if (move.style.equals("judgement_cut")) {
            // Des coupures scintillent déjà dans la zone : l'espace se fend avant le coup.
            for (Vec3 anchor : anchors) flicker(level, anchor, move.size, 2);
        }
        if (move.style.equals("lightning") && timer % 4 == 0) {
            for (Vec3 anchor : anchors) {
                Fx.burst(level, "electric_spark", anchor.add(0, 0.4, 0), 4, move.size * 0.5, 0.1);
            }
        }
        if (imminent && timer == 6) Fx.play(level, anchors.get(0), "block.note_block.pling", 0.9F, 1.9F);
        switch (move.shape) {
            case CIRCLE -> {
                for (Vec3 anchor : anchors) {
                    spinRing(level, edge, anchor, move.size, spin);
                    spinRing(level, fill, anchor, move.size * progress, -spin * 1.4);
                    level.sendParticles(fill, anchor.x, anchor.y + 0.2 + progress * 1.6, anchor.z, 1, 0.05, 0.1, 0.05, 0.0);
                }
            }
            case LINE -> {
                Vec3 origin = anchors.get(0).add(0, 0.15, 0);
                Vec3 side = new Vec3(-direction.z, 0, direction.x).scale(move.width / 2.0);
                Vec3 end = origin.add(direction.scale(move.size));
                Fx.line(level, edge, origin.add(side), end.add(side), 2.2);
                Fx.line(level, edge, origin.subtract(side), end.subtract(side), 2.2);
                // La pointe avance le long de la ligne : elle montre le sens et l'instant de l'attaque.
                Vec3 tip = origin.add(direction.scale(move.size * progress));
                Fx.line(level, fill, origin, tip, 1.6);
                level.sendParticles(Fx.dust(0xFFF4D0, 1.4F), tip.x, tip.y + 0.1, tip.z, 2, move.width / 3, 0.05, move.width / 3, 0.0);
            }
            case CONE -> {
                Vec3 origin = anchors.get(0).add(0, 0.15, 0);
                float half = (float) Math.toRadians(move.angle / 2.0);
                Vec3 left = direction.yRot(half).scale(move.size);
                Vec3 right = direction.yRot(-half).scale(move.size);
                Fx.line(level, edge, origin, origin.add(left), 2.2);
                Fx.line(level, edge, origin, origin.add(right), 2.2);
                arc(level, edge, origin, direction, half, move.size);
                arc(level, fill, origin, direction, half, move.size * progress);
            }
        }
    }

    /** Cercle dont les points tournent d'un tick à l'autre : l'œil voit tout de suite que la zone est armée. */
    private static void spinRing(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, double spin) {
        if (radius < 0.3) return;
        int points = (int) Math.max(10, Math.min(56, radius * 8));
        for (int i = 0; i < points; i++) {
            double a = spin + Math.PI * 2 * i / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.15, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0.0);
        }
    }

    /** Courtes coupures lumineuses tirées au hasard dans la zone (style Judgement Cut). */
    private void flicker(ServerLevel level, Vec3 anchor, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = self.getRandom().nextDouble() * Math.PI * 2;
            double tilt = (self.getRandom().nextDouble() - 0.5) * 1.4;
            double r = self.getRandom().nextDouble() * radius * 0.8;
            Vec3 mid = anchor.add(Math.cos(a) * r, 0.4 + self.getRandom().nextDouble() * 2.2, Math.sin(a) * r);
            Vec3 dir = new Vec3(Math.cos(a + Math.PI / 2), tilt, Math.sin(a + Math.PI / 2)).normalize().scale(1.3);
            Fx.line(level, Fx.dust(0xF4F4FF, 1.1F), mid.subtract(dir), mid.add(dir), 4.0);
        }
    }

    private static void arc(ServerLevel level, ParticleOptions particle, Vec3 origin, Vec3 direction, float half, double radius) {
        if (radius < 0.3) return;
        int points = (int) Math.max(5, radius * 2.2);
        for (int i = 0; i <= points; i++) {
            Vec3 p = origin.add(direction.yRot(half - 2 * half * i / points).scale(radius));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);
        }
    }

    private boolean inShape(MoveDef move, Vec3 anchor, Vec3 point) {
        Vec3 rel = point.subtract(anchor);
        if (Math.abs(rel.y) > 4.5) return false;
        double flat = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
        switch (move.shape) {
            case CIRCLE:
                return flat <= move.size + 0.3;
            case LINE: {
                double along = rel.x * direction.x + rel.z * direction.z;
                double across = Math.abs(-rel.x * direction.z + rel.z * direction.x);
                return along >= -0.5 && along <= move.size && across <= move.width / 2.0 + 0.4;
            }
            default: {
                if (flat > move.size + 0.3) return false;
                if (flat < 0.6) return true;
                double cos = (rel.x * direction.x + rel.z * direction.z) / flat;
                return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos)))) <= move.angle / 2.0;
            }
        }
    }

    private void execute(ServerLevel level, CharacterDef def, MoveDef move, LivingEntity target) {
        Vec3 origin = anchors.isEmpty() ? self.position() : anchors.get(0);

        // Déplacement préalable : surgir sur la zone annoncée, ou traverser la ligne.
        if (move.pre.equals("teleport_behind") || move.pre.equals("teleport")) {
            Fx.burst(level, "portal", self.position().add(0, 1, 0), 30, 0.4, 0.2);
            self.teleportTo(origin.x, origin.y, origin.z);
            Fx.play(level, origin, "entity.enderman.teleport", 1.0F, 0.8F);
        } else if (move.pre.equals("retreat") && target != null) {
            // Repli : rompre le corps-à-corps pour reprendre ses distances.
            double a = self.getRandom().nextDouble() * Math.PI * 2;
            int x = (int) Math.floor(target.getX() + Math.cos(a) * move.spread);
            int z = (int) Math.floor(target.getZ() + Math.sin(a) * move.spread);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Fx.burst(level, "portal", self.position().add(0, 1, 0), 30, 0.4, 0.2);
            self.teleportTo(x + 0.5, y, z + 0.5);
            Fx.play(level, self.position(), "entity.enderman.teleport", 1.0F, 1.2F);
        } else if (move.pre.equals("dash") && move.shape == MoveDef.Shape.LINE) {
            Vec3 from = origin.add(0, 1, 0);
            Vec3 wanted = from.add(direction.scale(move.size));
            BlockHitResult hit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
            Vec3 end = hit.getType() == HitResult.Type.MISS ? wanted : hit.getLocation().subtract(direction.scale(0.8));
            self.teleportTo(end.x, end.y - 1, end.z);
        }

        ParticleOptions particle = Lookup.particle(move.particle);
        double reach = move.size + move.spread + 4;
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(origin, origin).inflate(reach, 6, reach), this::canStrike);
        for (Vec3 anchor : anchors) {
            Fx.burst(level, particle, anchor.add(0, 0.6, 0), (int) Math.min(120, 12 + move.size * 8), move.size / 2.5, 0.05);
            for (LivingEntity victim : victims) {
                if (!inShape(move, anchor, victim.position())) continue;
                if (move.damage > 0) victim.hurt(self.damageSources().mobAttack(self), (float) move.damage);
                Vec3 away = victim.position().subtract(anchor);
                if (move.knockback > 0) victim.knockback(move.knockback, -away.x, -away.z);
                if (move.pull > 0) victim.knockback(move.pull, away.x, away.z);
                Lookup.applyEffects(victim, move.effects);
                if (move.timeStopTicks > 0) {
                    // Le temps s'arrête pour la victime : elle ne peut plus ni bouger ni frapper juste.
                    Fx.effect(victim, "slowness", move.timeStopTicks, 6);
                    Fx.effect(victim, "weakness", move.timeStopTicks, 3);
                    Fx.effect(victim, "mining_fatigue", move.timeStopTicks, 3);
                    if (move.style.equals("za_warudo")) {
                        if (victim instanceof ServerPlayer frozen) {
                            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                            json.addProperty("ticks", move.timeStopTicks);
                            net.trounoir.network.Net.toClient(frozen, "za_warudo", json);
                        }
                        FrozenVolley.surround(level, self, victim, 10, Math.max(4, move.damage / 3.0), move.timeStopTicks);
                    }
                }
            }
        }
        // L'impact se voit et se sent : triple onde au sol, flash au centre, écran secoué à proximité.
        for (Vec3 anchor : anchors) {
            Fx.ring(level, Fx.dust(move.color, 1.5F), anchor.add(0, 0.2, 0), move.size * 0.55);
            Fx.ring(level, Fx.dust(0xFFF4D0, 1.2F), anchor.add(0, 0.25, 0), Math.max(0.6, move.size * 0.95));
        }
        if (move.damage >= 14) Fx.burst(level, "explosion", origin.add(0, 0.6, 0), 2, 0.4, 0.0);
        Fx.shake(level, origin, Math.max(8, move.size + 6), Math.min(1.0, move.damage / 26.0));
        switch (move.style) {
            case "judgement_cut" -> {
                // Toutes les coupes frappent en même temps : l'espace vole en éclats.
                for (Vec3 anchor : anchors) flicker(level, anchor, move.size, 16);
                Fx.play(level, origin, "entity.player.attack.sweep", 1.0F, 0.6F);
                Fx.play(level, origin, "entity.player.attack.sweep", 1.0F, 1.4F);
                Fx.play(level, origin, "block.glass.break", 1.0F, 0.7F);
            }
            case "lightning" -> {
                for (Vec3 anchor : anchors) {
                    net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt != null) {
                        bolt.moveTo(anchor.x, anchor.y, anchor.z);
                        bolt.setVisualOnly(true);
                        level.addFreshEntity(bolt);
                    }
                }
            }
            case "nova" -> {
                Fx.burst(level, "explosion_emitter", origin.add(0, 1, 0), 1, 0.0, 0.0);
                for (int i = 1; i <= 4; i++) Fx.ring(level, Fx.dust(move.color, 2.0F), origin.add(0, 0.3 + i * 0.25, 0), move.size * i / 4.0);
                Fx.play(level, origin, "entity.generic.explode", 1.0F, 0.6F);
                Fx.shake(level, origin, move.size + 20, 1.0);
            }
            default -> {}
        }
        Fx.play(level, origin, Lookup.sound(move.sound), 1.0F, 1.0F);
        self.swing(InteractionHand.MAIN_HAND);

        if (!move.projectile.isEmpty() && target != null && target.isAlive()) {
            Vec3 from = self.getEyePosition();
            Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from);
            for (int i = 0; i < move.projectileCount; i++) {
                float offset = move.projectileCount == 1 ? 0 : (float) Math.toRadians((i - (move.projectileCount - 1) / 2.0) * 8.0);
                Vec3 dir = aim.yRot(offset).normalize();
                Projectiles.launch(level, self, move.projectile, from.add(dir.scale(1.0 + def.scale)), dir, 1.4, move.damage, 1);
            }
        }
        if (move.summon != null) {
            CharacterDef minion = TNData.CHARACTERS.get(move.summon);
            if (minion != null && !minion.unique) {
                for (int i = 0; i < move.summonCount; i++) {
                    double a = Math.PI * 2 * i / move.summonCount;
                    BlockPos pos = BlockPos.containing(self.getX() + Math.cos(a) * 3, self.getY(), self.getZ() + Math.sin(a) * 3);
                    LivingEntity spawned = Characters.spawn(level, minion, pos, pos);
                    if (spawned instanceof CharacterEntity ally && target != null) ally.provoke(target);
                }
            }
        }
        if (move.heal > 0) self.heal((float) move.heal);

        cooldowns.put(move.id, move.cooldown);
        globalCooldown = Math.max(8, 24 - self.phase() * 6);
    }

    /** Une attaque de personnage touche le joueur et tout ce qui n'est pas de sa faction. */
    private boolean canStrike(LivingEntity entity) {
        if (entity == self || !entity.isAlive()) return false;
        if (entity instanceof Player player) return !player.isCreative() && !player.isSpectator();
        CharacterDef mine = self.def();
        CharacterDef theirs = Characters.defOf(entity);
        if (mine != null && theirs != null && mine.faction.equals(theirs.faction)) return false;
        return entity == self.getTarget();
    }
}
