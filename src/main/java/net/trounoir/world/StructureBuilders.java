package net.trounoir.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.trounoir.data.SimpleDefs;
import net.trounoir.registry.ModRegistry;
import net.trounoir.util.Lookup;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans des lieux, construits bloc à bloc. Chaque plan est déterministe : rejoué « à sec »,
 * il redonne les mêmes points d'intérêt sans rien poser (utile pour repeupler un lieu déjà bâti).
 * La palette de blocs vient du JSON du lieu.
 */
public final class StructureBuilders {
    private StructureBuilders() {}

    /** Résultat d'un plan : point focal (trône, autel…), emplacements d'habitants, point d'arrivée. */
    public record Built(BlockPos focus, List<BlockPos> spots, BlockPos arrival, BlockPos chest) {}

    private static BlockState b(String vanillaId) {
        return Lookup.block(ResourceLocation.withDefaultNamespace(vanillaId));
    }

    /** Bloc avec propriétés, ex. b("oak_stairs", "facing=south"). Une propriété inconnue est ignorée. */
    private static BlockState b(String vanillaId, String... properties) {
        BlockState state = b(vanillaId);
        for (String property : properties) {
            int eq = property.indexOf('=');
            if (eq > 0) state = with(state, property.substring(0, eq), property.substring(eq + 1));
        }
        return state;
    }

    private static BlockState with(BlockState state, String name, String value) {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().equals(name)) return apply(state, property, value);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState apply(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    /** Surface de travail relative au centre du lieu (y = 0 : niveau du sol). */
    private static final class Canvas {
        final ServerLevel level;
        final BlockPos center;
        final boolean dry;
        final List<BlockPos> spots = new ArrayList<>();
        BlockPos focus;
        BlockPos arrival;
        /** Emplacement du coffre de relique, si le plan en prévoit un. */
        BlockPos chest;
        /** Hasard propre au lieu : le même à chaque construction, différent d'un lieu à l'autre. */
        final java.util.Random rng;
        final List<BlockPos> shaped = new ArrayList<>();
        final String wood;
        final BlockState primary;
        final BlockState secondary;
        final BlockState accent;

        Canvas(ServerLevel level, BlockPos center, SimpleDefs.SiteDef site, boolean dry) {
            this.level = level;
            this.center = center;
            this.dry = dry;
            this.primary = Lookup.block(site.primary());
            this.secondary = Lookup.block(site.secondary());
            this.accent = Lookup.block(site.accent());
            this.rng = new java.util.Random(site.id().toString().hashCode() * 31L + 7);
            String primaryPath = site.primary().getPath();
            this.wood = primaryPath.endsWith("_planks") ? primaryPath.substring(0, primaryPath.length() - 7) : "oak";
            this.focus = center;
            this.arrival = center;
        }

        void set(int x, int y, int z, BlockState state) {
            if (!dry) level.setBlock(center.offset(x, y, z), state, 2);
        }

        /** Bloc dont la forme dépend des voisins (clôture, vitre) : raccordé à la fin. */
        void setShaped(int x, int y, int z, BlockState state) {
            set(x, y, z, state);
            if (!dry) shaped.add(center.offset(x, y, z));
        }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            if (dry) return;
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                        level.setBlock(center.offset(x, y, z), state, 2);
        }

        /** Murs d'un rectangle (périmètre seulement). */
        void walls(int x1, int z1, int x2, int z2, int y1, int y2, BlockState state) {
            fill(x1, y1, z1, x2, y2, z1, state);
            fill(x1, y1, z2, x2, y2, z2, state);
            fill(x1, y1, z1, x1, y2, z2, state);
            fill(x2, y1, z1, x2, y2, z2, state);
        }

        void disc(int y, int radius, BlockState state) {
            if (dry) return;
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++)
                    if (x * x + z * z <= radius * radius) level.setBlock(center.offset(x, y, z), state, 2);
        }

        void circleWall(int radius, int y1, int y2, BlockState state) {
            if (dry) return;
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++) {
                    int d = x * x + z * z;
                    if (d <= radius * radius && d > (radius - 1) * (radius - 1)) {
                        for (int y = y1; y <= y2; y++) level.setBlock(center.offset(x, y, z), state, 2);
                    }
                }
        }

        /** Aplanit l'emprise : fondations sous le sol, sol uniforme, air au-dessus. */
        void prepare(int rx, int rz, int clear, BlockState ground) {
            if (dry) return;
            BlockState air = b("air");
            BlockState foundation = b("stone");
            for (int x = -rx; x <= rx; x++) {
                for (int z = -rz; z <= rz; z++) {
                    for (int y = -2; y >= -10; y--) {
                        BlockPos pos = center.offset(x, y, z);
                        BlockState existing = level.getBlockState(pos);
                        if (existing.isAir() || !existing.getFluidState().isEmpty() || existing.canBeReplaced()) level.setBlock(pos, foundation, 2);
                        else break;
                    }
                    level.setBlock(center.offset(x, -1, z), ground, 2);
                    for (int y = 0; y <= clear; y++) {
                        BlockPos pos = center.offset(x, y, z);
                        if (!level.getBlockState(pos).isAir()) level.setBlock(pos, air, 2);
                    }
                }
            }
        }

        void spot(int x, int y, int z) {
            spots.add(center.offset(x, y, z));
        }

        void spotRing(int radius, int count, int y) {
            for (int i = 0; i < count; i++) {
                double a = Math.PI * 2 * i / count;
                spot((int) Math.round(Math.cos(a) * radius), y, (int) Math.round(Math.sin(a) * radius));
            }
        }

        Built done() {
            if (!dry) {
                // Clôtures et vitres se raccordent à leurs voisins une fois tout posé.
                for (BlockPos pos : shaped) {
                    BlockState now = level.getBlockState(pos);
                    level.setBlock(pos, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(now, level, pos), 2);
                }
            }
            return new Built(focus, List.copyOf(spots), arrival, chest);
        }
    }

    public static Built build(ServerLevel level, SimpleDefs.SiteDef site, BlockPos center) {
        return run(new Canvas(level, center, site, false), site);
    }

    /** Recalcule les points d'intérêt sans poser de bloc. */
    public static Built layout(ServerLevel level, SimpleDefs.SiteDef site, BlockPos center) {
        return run(new Canvas(level, center, site, true), site);
    }

    private static Built run(Canvas c, SimpleDefs.SiteDef site) {
        switch (site.blueprint()) {
            case "castle" -> castle(c);
            case "harbor" -> harbor(c);
            case "city" -> city(c);
            case "fortress" -> fortress(c);
            case "tower" -> tower(c);
            case "palace" -> palace(c);
            case "lair" -> lair(c);
            case "sanctuary" -> sanctuary(c);
            case "ruin" -> ruin(c);
            default -> village(c);
        }
        return c.done();
    }

    // ------------------------------------------------------------------ plans

    private static void hut(Canvas c, int cx, int cz, int half, int height, BlockState wall, BlockState roof) {
        c.fill(cx - half, -1, cz - half, cx + half, -1, cz + half, c.secondary);
        c.walls(cx - half, cz - half, cx + half, cz + half, 0, height - 1, wall);
        c.fill(cx - half, height, cz - half, cx + half, height, cz + half, roof);
        c.fill(cx, 0, cz + half, cx, 1, cz + half, b("air"));           // porte au sud
        c.set(cx - half, 1, cz, b("glass"));
        c.set(cx + half, 1, cz, b("glass"));
        c.set(cx - half + 1, 0, cz - half + 1, c.accent);
    }

    private static final String[] FLOWERS = {"poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium"};
    private static final String[] CROPS = {"wheat", "carrots", "potatoes", "beetroots"};
    private static final String[] STALL_WOOL = {"red_wool", "yellow_wool", "blue_wool", "green_wool", "orange_wool", "white_wool"};

    /** Maison à toit en pente : soubassement de pierre, poteaux en rondins, vitres, porte, lanterne. */
    private static void house(Canvas c, int cx, int cz, int hx, int hz, int doorSide, BlockState roofBlock, String roofStairs) {
        String wood = c.wood;
        int h = 3;
        c.fill(cx - hx, -1, cz - hz, cx + hx, -1, cz + hz, c.secondary);
        c.walls(cx - hx, cz - hz, cx + hx, cz + hz, 0, 0, b("cobblestone"));
        c.walls(cx - hx, cz - hz, cx + hx, cz + hz, 1, h - 1, c.primary);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) c.fill(cx + sx * hx, 0, cz + sz * hz, cx + sx * hx, h - 1, cz + sz * hz, b(wood + "_log"));
        // Toit à deux pans, avec débord ; les pignons sont fermés.
        for (int i = 0; ; i++) {
            int z1 = cz - hz - 1 + i;
            int z2 = cz + hz + 1 - i;
            int y = h - 1 + i;
            if (z1 > z2) break;
            for (int x = cx - hx - 1; x <= cx + hx + 1; x++) {
                if (z1 == z2) {
                    c.set(x, y, z1, roofBlock);
                } else {
                    c.set(x, y, z1, b(roofStairs, "facing=south"));
                    c.set(x, y, z2, b(roofStairs, "facing=north"));
                }
            }
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                c.set(cx - hx, y, z, c.primary);
                c.set(cx + hx, y, z, c.primary);
            }
        }
        // Porte (côté tourné vers la place) et fenêtres.
        int dx = doorSide == 2 ? -hx : doorSide == 3 ? hx : 0;
        int dz = doorSide == 0 ? -hz : doorSide == 1 ? hz : 0;
        String facing = new String[]{"south", "north", "east", "west"}[doorSide];
        c.set(cx + dx, 0, cz + dz, b(wood + "_door", "facing=" + facing, "half=lower"));
        c.set(cx + dx, 1, cz + dz, b(wood + "_door", "facing=" + facing, "half=upper"));
        if (doorSide < 2) {
            c.setShaped(cx - hx, 1, cz, b("glass_pane"));
            c.setShaped(cx + hx, 1, cz, b("glass_pane"));
        } else {
            c.setShaped(cx, 1, cz - hz, b("glass_pane"));
            c.setShaped(cx, 1, cz + hz, b("glass_pane"));
        }
        // Dehors : un pas de porte, une lanterne sur son poteau, un objet du quotidien.
        int ox = Integer.signum(dx);
        int oz = Integer.signum(dz);
        c.set(cx + dx + ox, -1, cz + dz + oz, b("gravel"));
        int lx = cx + dx + ox + (oz != 0 ? 1 : 0);
        int lz = cz + dz + oz + (ox != 0 ? 1 : 0);
        c.setShaped(lx, 0, lz, b(wood + "_fence"));
        c.set(lx, 1, lz, b("lantern"));
        String[] props = {"barrel", "hay_block", "composter", "cauldron", "crafting_table"};
        c.set(cx + dx + ox - (oz != 0 ? 1 : 0), 0, cz + dz + oz - (ox != 0 ? 1 : 0), b(props[c.rng.nextInt(props.length)]));
        c.set(cx - hx + 1, 0, cz - hz + 1, b("lantern"));
        c.spot(cx + dx + ox * 2, 0, cz + dz + oz * 2);
    }

    /**
     * Village : aucun n'est pareil. Le nombre et la taille des maisons, le cœur de la place, les cultures,
     * les toits et les décorations sont tirés d'un hasard propre au lieu.
     */
    private static void village(Canvas c) {
        java.util.Random r = c.rng;
        String wood = c.wood;
        c.prepare(27, 27, 12, b("grass_block"));

        // Chemins irréguliers : terre battue et gravier.
        for (int i = -24; i <= 24; i++) {
            int wobble = (int) Math.round(Math.sin(i * 0.35) * 1.2);
            for (int w = -1; w <= 1; w++) {
                c.set(i, -1, wobble + w, b(r.nextInt(5) == 0 ? "gravel" : "dirt_path"));
                c.set(wobble + w, -1, i, b(r.nextInt(5) == 0 ? "gravel" : "dirt_path"));
            }
        }

        // Le cœur de la place change d'un village à l'autre.
        int heart = r.nextInt(4);
        if (heart == 0) {                                   // puits couvert
            c.walls(-1, -1, 1, 1, 0, 0, b("mossy_cobblestone"));
            c.set(0, -1, 0, b("water"));
            for (int[] p : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
                c.setShaped(p[0], 1, p[1], b(wood + "_fence"));
                c.setShaped(p[0], 2, p[1], b(wood + "_fence"));
            }
            c.fill(-1, 3, -1, 1, 3, 1, b(wood + "_slab"));
        } else if (heart == 1) {                            // marché : étals aux toiles de couleur
            for (int k = 0; k < 3; k++) {
                int sx = -5 + k * 5;
                String wool = STALL_WOOL[r.nextInt(STALL_WOOL.length)];
                for (int[] p : new int[][]{{sx - 1, -1}, {sx + 1, -1}, {sx - 1, 1}, {sx + 1, 1}}) {
                    c.setShaped(p[0], 0, p[1], b(wood + "_fence"));
                    c.setShaped(p[0], 1, p[1], b(wood + "_fence"));
                }
                c.fill(sx - 1, 2, -1, sx + 1, 2, 1, b(wool));
                c.set(sx, 0, 0, b(r.nextBoolean() ? "barrel" : "hay_block"));
            }
        } else if (heart == 2) {                            // grand arbre et bancs
            c.fill(0, 0, 0, 0, 5, 0, b("oak_log"));
            for (int x = -3; x <= 3; x++)
                for (int z = -3; z <= 3; z++)
                    for (int y = 4; y <= 7; y++) {
                        int d = x * x + z * z + (y - 5) * (y - 5) * 2;
                        if (d <= 11 && !(x == 0 && z == 0 && y <= 5)) c.set(x, y, z, b("oak_leaves", "persistent=true"));
                    }
            c.set(-2, 0, 0, b(wood + "_stairs", "facing=east"));
            c.set(2, 0, 0, b(wood + "_stairs", "facing=west"));
            c.set(0, 0, 2, b(wood + "_stairs", "facing=north"));
        } else {                                            // fontaine
            c.fill(-2, -1, -2, 2, -1, 2, b("stone_bricks"));
            c.walls(-2, -2, 2, 2, 0, 0, b("stone_brick_slab"));
            c.fill(-1, 0, -1, 1, 0, 1, b("water"));
            c.fill(0, 0, 0, 0, 2, 0, b("mossy_stone_bricks"));
            c.set(0, 3, 0, b("lantern"));
        }
        c.set(4, 0, 4, b("bell"));

        // Maisons en couronne, tournées vers la place.
        String[] roofs = {wood, "spruce", "dark_oak", "brick", "cobblestone"};
        String roof = roofs[r.nextInt(roofs.length)];
        boolean stone = roof.equals("brick") || roof.equals("cobblestone");
        BlockState roofBlock = b(stone ? (roof.equals("brick") ? "bricks" : "cobblestone") : roof + "_planks");
        int houses = 5 + r.nextInt(3);
        double start = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < houses; i++) {
            double a = start + Math.PI * 2 * i / houses + (r.nextDouble() - 0.5) * 0.25;
            int radius = 14 + r.nextInt(5);
            int cx = (int) Math.round(Math.cos(a) * radius);
            int cz = (int) Math.round(Math.sin(a) * radius);
            int doorSide = Math.abs(cz) >= Math.abs(cx) ? (cz > 0 ? 0 : 1) : (cx > 0 ? 2 : 3);
            house(c, cx, cz, 2 + r.nextInt(2), 2 + r.nextInt(2), doorSide, roofBlock, roof + "_stairs");
        }

        // Champs clos, aux cultures variées, et un enclos à foin.
        for (int k = 0; k < 2; k++) {
            double a = start + Math.PI / houses + Math.PI * k + 0.4;
            int fx = (int) Math.round(Math.cos(a) * 21);
            int fz = (int) Math.round(Math.sin(a) * 21);
            String crop = CROPS[r.nextInt(CROPS.length)];
            String age = crop.equals("beetroots") ? "age=3" : "age=" + (4 + r.nextInt(4));
            for (int x = -3; x <= 3; x++)
                for (int z = -3; z <= 3; z++) {
                    boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
                    if (edge) {
                        if (!(x == 0 && z == 3)) c.setShaped(fx + x, 0, fz + z, b(wood + "_fence"));
                    } else if (x == 0) {
                        c.set(fx + x, -1, fz + z, b("water"));
                    } else {
                        c.set(fx + x, -1, fz + z, b("farmland", "moisture=7"));
                        c.set(fx + x, 0, fz + z, b(crop, age));
                    }
                }
        }

        // Petites choses qui font un lieu habité : lampadaires, fleurs, buissons, feu de camp.
        for (int k = 0; k < 4; k++) {
            int lx = (k % 2 == 0 ? 1 : -1) * (8 + r.nextInt(3));
            int lz = (k < 2 ? 1 : -1) * 3;
            c.setShaped(lx, 0, lz, b(wood + "_fence"));
            c.setShaped(lx, 1, lz, b(wood + "_fence"));
            c.set(lx, 2, lz, b("lantern"));
        }
        for (int k = 0; k < 26; k++) {
            int x = r.nextInt(49) - 24;
            int z = r.nextInt(49) - 24;
            if (Math.abs(x) < 4 || Math.abs(z) < 4) continue;
            if (!c.dry && !c.level.getBlockState(c.center.offset(x, 0, z)).isAir()) continue;
            if (!c.dry && c.level.getBlockState(c.center.offset(x, -1, z)).getBlock() != net.minecraft.world.level.block.Blocks.GRASS_BLOCK) continue;
            c.set(x, 0, z, k % 5 == 0 ? b("oak_leaves", "persistent=true") : b(FLOWERS[r.nextInt(FLOWERS.length)]));
        }
        if (r.nextBoolean()) {
            c.set(-6, 0, 6, b("campfire"));
            c.set(-8, 0, 6, b(wood + "_log", "axis=z"));
            c.set(-6, 0, 8, b(wood + "_log", "axis=x"));
        }
        c.spotRing(6, 6, 0);
        c.focus = c.center.offset(0, 0, 5);
    }

    /** Vestiges : un cercle de murs effondrés autour d'un coffre oublié. */
    private static void ruin(Canvas c) {
        java.util.Random r = c.rng;
        c.prepare(9, 9, 8, b("grass_block"));
        c.disc(-1, 7, c.secondary);
        String[] stones = {"mossy_stone_bricks", "cracked_stone_bricks", "stone_bricks", "mossy_cobblestone"};
        for (int x = -7; x <= 7; x++)
            for (int z = -7; z <= 7; z++) {
                int d = x * x + z * z;
                if (d <= 49 && d > 36) {
                    int height = r.nextInt(5);
                    for (int y = 0; y < height; y++) c.set(x, y, z, r.nextInt(3) == 0 ? c.primary : b(stones[r.nextInt(stones.length)]));
                } else if (d <= 36 && r.nextInt(9) == 0) {
                    c.set(x, -1, z, b("coarse_dirt"));
                }
            }
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + Math.PI / 2 * i;
            int x = (int) Math.round(Math.cos(a) * 4);
            int z = (int) Math.round(Math.sin(a) * 4);
            c.fill(x, 0, z, x, 1 + r.nextInt(3), z, c.accent);
        }
        c.fill(-1, -1, -1, 1, -1, 1, c.accent);
        c.chest = c.center;
        c.focus = c.center.offset(0, 0, 3);
        c.arrival = c.center.offset(0, 0, 9);
    }

    private static void castle(Canvas c) {
        c.prepare(27, 27, 20, b("grass_block"));
        int w = 23;
        c.walls(-w, -w, w, w, 0, 6, c.primary);
        c.walls(-w + 1, -w + 1, w - 1, w - 1, 0, 5, c.primary);
        for (int i = -w; i <= w; i += 2) {                                  // créneaux
            c.set(i, 7, -w, c.primary);
            c.set(i, 7, w, c.primary);
            c.set(-w, 7, i, c.primary);
            c.set(w, 7, i, c.primary);
        }
        c.fill(-1, 0, w - 1, 1, 3, w, b("air"));                            // porte sud
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {                           // tours d'angle
                int tx = sx * w;
                int tz = sz * w;
                c.walls(tx - 2, tz - 2, tx + 2, tz + 2, 0, 11, c.primary);
                c.fill(tx - 2, 12, tz - 2, tx + 2, 12, tz + 2, c.secondary);
                c.set(tx, 13, tz, c.accent);
            }
        c.fill(-1, -1, 8, 1, -1, w, b("stone_bricks"));                     // allée
        // Donjon.
        int k = 7;
        c.fill(-k, -1, -k, k, -1, k, c.secondary);
        c.walls(-k, -k, k, k, 0, 9, c.primary);
        c.fill(-k, 10, -k, k, 10, k, c.primary);
        c.fill(-1, 0, k, 1, 2, k, b("air"));
        for (int z = -k + 1; z <= k - 1; z++) c.set(0, -1, z, b("red_wool"));
        for (int[] p : new int[][]{{-k + 1, -k + 1}, {k - 1, -k + 1}, {-k + 1, k - 1}, {k - 1, k - 1}}) c.set(p[0], 0, p[1], c.accent);
        c.fill(-2, 0, -k + 1, 2, 0, -k + 2, b("polished_andesite"));        // estrade
        c.set(0, 1, -k + 1, b("gold_block"));                               // trône
        for (int x = -k + 2; x <= k - 2; x += 4) {
            c.set(x, 3, -k, b("glass"));
            c.set(x, 3, k, b("glass"));
        }
        c.focus = c.center.offset(0, 1, -k + 3);
        c.chest = c.center.offset(5, 0, -6);
        c.spot(-3, 0, -3);
        c.spot(3, 0, -3);
        c.spot(-3, 0, 2);
        c.spot(3, 0, 2);
        c.spotRing(14, 10, 0);
        c.spot(-2, 0, w - 3);
        c.spot(2, 0, w - 3);
        c.arrival = c.center.offset(0, 0, w + 3);
    }

    private static void harbor(Canvas c) {
        c.prepare(18, 20, 14, b("grass_block"));
        BlockState planks = c.secondary;
        // Bassin.
        c.fill(-13, -4, 3, 13, -1, 17, c.primary);
        c.fill(-12, -3, 4, 12, -1, 16, b("water"));
        // Quai et maison du port.
        c.fill(-13, -1, -3, 13, -1, 2, planks);
        c.fill(-1, 0, 3, 1, 0, 7, planks);                                  // passerelle
        hut(c, -10, -10, 3, 4, c.primary, planks);
        hut(c, 10, -10, 2, 3, planks, c.primary);
        for (int x = -12; x <= 12; x += 6) c.fill(x, 0, 2, x, 1, 2, b("oak_fence"));
        // Navire.
        for (int x = -7; x <= 7; x++) {
            int halfWidth = Math.abs(x) >= 6 ? 1 : 2;
            c.fill(x, -2, 10 - halfWidth, x, -1, 10 + halfWidth, planks);
            c.fill(x, 0, 10 - halfWidth, x, 0, 10 + halfWidth, planks);       // pont
            c.set(x, 1, 10 - halfWidth, b("oak_fence"));
            c.set(x, 1, 10 + halfWidth, b("oak_fence"));
        }
        c.fill(-1, 1, 8, 1, 1, 8, b("air"));                                // coupée
        c.fill(0, 1, 10, 0, 9, 10, b("oak_log"));                           // mât
        c.fill(-3, 4, 10, 3, 8, 10, b("white_wool"));
        c.fill(0, 4, 10, 0, 9, 10, b("oak_log"));
        c.set(6, 1, 10, c.dry ? b("air") : ModRegistry.HELM.get().defaultBlockState());
        c.set(-6, 1, 10, c.accent);
        c.focus = c.center.offset(6, 1, 10);
        c.arrival = c.center.offset(0, 0, 0);
        c.spot(-6, 0, 0);
        c.spot(6, 0, 0);
        c.spot(-10, 0, -5);
        c.spot(10, 0, -5);
        c.spot(-3, 0, -6);
        c.spot(3, 0, -6);
        c.spot(0, 0, -9);
        c.spot(-7, 0, -1);
    }

    private static void city(Canvas c) {
        c.prepare(33, 33, 16, b("stone_bricks"));
        int w = 30;
        c.walls(-w, -w, w, w, 0, 6, c.primary);
        for (int i = -w; i <= w; i += 2) {
            c.set(i, 7, -w, c.primary);
            c.set(i, 7, w, c.primary);
            c.set(-w, 7, i, c.primary);
            c.set(w, 7, i, c.primary);
        }
        c.fill(-2, 0, w, 2, 4, w, b("air"));
        c.fill(-2, 0, -w, 2, 4, -w, b("air"));
        c.fill(w, 0, -2, w, 4, 2, b("air"));
        c.fill(-w, 0, -2, -w, 4, 2, b("air"));
        c.fill(-w + 1, -1, -2, w - 1, -1, 2, c.secondary);                  // avenues
        c.fill(-2, -1, -w + 1, 2, -1, w - 1, c.secondary);
        int[] grid = {-19, 19};
        int n = 0;
        for (int gx : grid)
            for (int gz : grid) {
                for (int dx = -5; dx <= 5; dx += 10)
                    for (int dz = -5; dz <= 5; dz += 10) {
                        int height = 4 + (n++ % 3);
                        hut(c, gx + dx, gz + dz, 3, height, c.primary, c.secondary);
                    }
            }
        // Place et statue.
        c.walls(-3, -3, 3, 3, 0, 0, c.primary);
        c.fill(-2, -1, -2, 2, -1, 2, b("water"));
        c.fill(0, -1, 0, 0, 6, 0, c.accent);
        c.set(0, 7, 0, b("sea_lantern"));
        for (int[] p : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) c.fill(p[0], 0, p[1], p[0], 3, p[1], c.accent);
        c.chest = c.center.offset(-24, 0, -26);
        c.focus = c.center.offset(0, 0, 6);
        c.spotRing(7, 8, 0);
        c.spotRing(12, 8, 0);
        c.spot(-3, 0, w - 2);
        c.spot(3, 0, w - 2);
        c.spot(-3, 0, -w + 2);
        c.spot(3, 0, -w + 2);
        c.arrival = c.center.offset(0, 0, w + 3);
    }

    private static void fortress(Canvas c) {
        c.prepare(21, 21, 18, b("gravel"));
        int w = 17;
        c.walls(-w, -w, w, w, 0, 9, c.primary);
        c.walls(-w + 1, -w + 1, w - 1, w - 1, 0, 8, c.primary);
        c.walls(-w + 2, -w + 2, w - 2, w - 2, 8, 8, c.secondary);           // chemin de ronde
        for (int i = -w; i <= w; i += 2) {
            c.set(i, 10, -w, c.primary);
            c.set(i, 10, w, c.primary);
            c.set(-w, 10, i, c.primary);
            c.set(w, 10, i, c.primary);
        }
        c.fill(-2, 0, w - 1, 2, 4, w, b("air"));
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                int tx = sx * w;
                int tz = sz * w;
                c.walls(tx - 2, tz - 2, tx + 2, tz + 2, 0, 14, c.primary);
                c.fill(tx - 2, 15, tz - 2, tx + 2, 15, tz + 2, c.secondary);
                c.set(tx, 16, tz, c.accent);
            }
        // Cour d'armes.
        c.fill(-8, -1, -8, 8, -1, 8, c.secondary);
        c.walls(-4, -4, 4, 4, -1, -1, c.accent);
        for (int[] p : new int[][]{{-9, -9}, {9, -9}, {-9, 9}, {9, 9}}) {
            c.fill(p[0], 0, p[1], p[0], 4, p[1], c.primary);
            c.set(p[0], 5, p[1], c.accent);
        }
        c.chest = c.center.offset(-13, 0, -13);
        c.focus = c.center.offset(0, 0, -2);
        c.spot(3, 0, -2);
        c.spot(-3, 0, -2);
        c.spotRing(11, 10, 0);
        c.spot(-3, 0, w - 3);
        c.spot(3, 0, w - 3);
        c.arrival = c.center.offset(0, 0, w + 3);
    }

    private static void tower(Canvas c) {
        c.prepare(9, 9, 6, b("stone_bricks"));
        int h = 56;
        c.walls(-4, -4, 4, 4, 0, h, c.primary);
        c.fill(-3, 0, -3, 3, h, 3, b("air"));
        c.fill(0, 0, 4, 0, 1, 4, b("air"));                                 // entrée sud
        // Escalier intérieur : une marche par bloc, le long des murs.
        int[][] loop = new int[24][2];
        int idx = 0;
        for (int x = -3; x <= 3; x++) loop[idx++] = new int[]{x, 3};
        for (int z = 2; z >= -3; z--) loop[idx++] = new int[]{3, z};
        for (int x = 2; x >= -3; x--) loop[idx++] = new int[]{x, -3};
        for (int z = -2; z <= 2; z++) loop[idx++] = new int[]{-3, z};
        // Plate-forme de tir.
        c.fill(-6, h + 1, -6, 6, h + 1, 6, c.primary);
        c.walls(-6, -6, 6, 6, h + 2, h + 2, c.secondary);
        for (int[] p : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) c.set(p[0], h + 3, p[1], c.accent);
        // Les marches montent jusqu'au niveau de la plate-forme ; le décalage laisse l'entrée dégagée.
        for (int y = 0; y <= h + 1; y++) {
            int[] p = loop[(y + 5) % 24];
            c.set(p[0], y, p[1], c.secondary);
            if (y % 8 == 4 && y < h) c.set(0, y, 0, c.accent);                // lumière suspendue dans le puits
        }
        for (int y = h - 2; y <= h; y++) {                                  // trémie : dégage la tête au sommet de l'escalier
            int[] p = loop[(y + 5) % 24];
            c.set(p[0], h + 1, p[1], b("air"));
        }
        for (int y = 6; y < h; y += 6) {
            c.set(-4, y, 0, b("glass"));
            c.set(4, y, 0, b("glass"));
        }
        c.chest = c.center.offset(-2, 0, -2);
        c.focus = c.center.offset(0, h + 2, 0);
        c.spotRing(8, 6, 0);
        c.arrival = c.center.offset(0, 0, 8);
    }

    private static void palace(Canvas c) {
        c.prepare(28, 28, 18, c.primary);
        int w = 24;
        c.fill(-w, 0, -w, w, 0, w, c.primary);                              // parvis surélevé
        c.fill(-2, 0, w - 3, 2, 0, w, c.secondary);
        for (int i = -w + 2; i <= w - 2; i += 6) {                          // colonnade
            for (int[] p : new int[][]{{i, -w + 2}, {i, w - 2}, {-w + 2, i}, {w - 2, i}}) {
                c.fill(p[0], 1, p[1], p[0], 9, p[1], c.accent);
            }
        }
        c.walls(-w + 2, -w + 2, w - 2, w - 2, 10, 10, c.secondary);         // entablement
        c.fill(-1, 0, -w + 6, 1, 0, w - 4, b("red_wool"));
        // Estrade et trône.
        c.fill(-4, 1, -w + 5, 4, 1, -w + 11, c.secondary);
        c.fill(-2, 2, -w + 5, 2, 2, -w + 8, c.secondary);
        c.fill(-1, 3, -w + 5, 1, 5, -w + 5, c.accent);
        c.set(0, 3, -w + 6, b("gold_block"));
        for (int sx = -1; sx <= 1; sx += 2) {
            c.fill(sx * 6, 1, -w + 6, sx * 6, 6, -w + 6, c.accent);
            c.set(sx * 6, 7, -w + 6, b("sea_lantern"));
        }
        c.chest = c.center.offset(2, 3, -w + 6);
        c.focus = c.center.offset(0, 2, -w + 10);
        c.spot(-5, 1, -w + 14);
        c.spot(5, 1, -w + 14);
        c.spot(-9, 1, -6);
        c.spot(9, 1, -6);
        c.spotRing(15, 8, 1);
        c.arrival = c.center.offset(0, 1, w - 2);
    }

    private static void lair(Canvas c) {
        c.prepare(17, 17, 12, c.secondary);
        c.disc(-1, 15, c.secondary);
        c.circleWall(15, 0, 3, c.primary);
        c.fill(-1, 0, 13, 1, 3, 16, b("air"));
        c.fill(-1, 0, -16, 1, 3, -13, b("air"));
        for (int i = 0; i < 7; i++) {                                       // piliers d'os
            double a = Math.PI * 2 * i / 7 + 0.4;
            int x = (int) Math.round(Math.cos(a) * 10);
            int z = (int) Math.round(Math.sin(a) * 10);
            c.fill(x, 0, z, x, 2 + (i % 3), z, c.accent);
        }
        c.disc(-1, 3, c.accent);
        c.chest = c.center.offset(0, 0, -11);
        c.focus = c.center;
        c.arrival = c.center.offset(0, 0, 18);
    }

    private static void sanctuary(Canvas c) {
        c.prepare(15, 15, 16, b("grass_block"));
        c.disc(0, 12, c.primary);
        c.disc(0, 6, c.secondary);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = (int) Math.round(Math.cos(a) * 10);
            int z = (int) Math.round(Math.sin(a) * 10);
            c.fill(x, 1, z, x, 7, z, c.accent);
            c.set(x, 8, z, c.secondary);
        }
        c.fill(-1, 1, -1, 1, 1, 1, c.accent);                               // autel
        c.set(0, 2, 0, c.secondary);
        c.focus = c.center.offset(0, 1, 4);
        c.arrival = c.center.offset(0, 1, 13);
    }
}
