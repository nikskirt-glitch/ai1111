package com.chatai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure-geometry structure templates. Each structure is a list of relative block placements
 * (dx, dy, dz, material key). The adapter maps material keys to real blocks. All sizes are
 * hard-bounded so a request can never produce an unbounded number of blocks.
 */
public final class Structures {
    private Structures() {}

    public static final class Placement {
        public final int dx, dy, dz;
        public final String material;
        Placement(int dx, int dy, int dz, String material) { this.dx = dx; this.dy = dy; this.dz = dz; this.material = material; }
    }

    /** Material keys understood by adapters: PLANKS, LOG, COBBLE, STONE_BRICK, GLASS, AIR, TORCH, DOOR_*, WOOL, QUARTZ, LANTERN. */
    public static final String[] KINDS = {"house", "tower", "castle", "bridge", "pyramid", "platform"};

    public static boolean known(String id) {
        for (String k : KINDS) if (k.equals(id)) return true;
        return false;
    }

    public static int blockCount(String id) {
        return build(id).size();
    }

    public static List<Placement> build(String id) {
        if (id == null) return new ArrayList<>();
        switch (id.toLowerCase(Locale.ROOT)) {
            case "house": return house();
            case "tower": return tower();
            case "castle": return castle();
            case "bridge": return bridge();
            case "pyramid": return pyramid();
            case "platform": return platform();
            default: return new ArrayList<>();
        }
    }

    private static void box(List<Placement> out, int x0, int y0, int z0, int x1, int y1, int z1, String wall, String floor, String roof, boolean hollow) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++) {
                    boolean edgeX = x == x0 || x == x1, edgeZ = z == z0 || z == z1;
                    String m;
                    if (y == y0) m = floor;
                    else if (y == y1) m = roof;
                    else if (edgeX || edgeZ) m = wall;
                    else m = hollow ? "AIR" : wall;
                    out.add(new Placement(x, y, z, m));
                }
    }

    private static List<Placement> house() {
        List<Placement> p = new ArrayList<>();
        // 7x7 footprint, 4 high walls, flat roof; front door on z = 0 side (relative +z faces the builder)
        box(p, 0, 0, 0, 6, 4, 6, "PLANKS", "COBBLE", "LOG", true);
        // door opening (2 high) in the middle of the z=0 wall
        p.add(new Placement(3, 1, 0, "AIR"));
        p.add(new Placement(3, 2, 0, "AIR"));
        // windows
        for (int[] w : new int[][]{{1, 2, 0}, {5, 2, 0}, {0, 2, 3}, {6, 2, 3}, {3, 2, 6}})
            p.add(new Placement(w[0], w[1], w[2], "GLASS"));
        p.add(new Placement(1, 1, 1, "TORCH"));
        p.add(new Placement(5, 1, 5, "TORCH"));
        return p;
    }

    private static List<Placement> tower() {
        List<Placement> p = new ArrayList<>();
        int h = 14;
        for (int y = 0; y <= h; y++)
            for (int x = 0; x <= 4; x++)
                for (int z = 0; z <= 4; z++) {
                    boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
                    boolean corner = (x == 0 || x == 4) && (z == 0 || z == 4);
                    if (y == 0) p.add(new Placement(x, y, z, "STONE_BRICK"));
                    else if (edge) p.add(new Placement(x, y, z, corner ? "COBBLE" : "STONE_BRICK"));
                    else p.add(new Placement(x, y, z, "AIR"));
                }
        // battlements
        for (int x = 0; x <= 4; x++)
            for (int z = 0; z <= 4; z++) {
                boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
                if (edge && (x + z) % 2 == 0) p.add(new Placement(x, h + 1, z, "STONE_BRICK"));
            }
        p.add(new Placement(2, 1, 0, "AIR"));
        p.add(new Placement(2, 2, 0, "AIR"));
        for (int y = 3; y <= h; y += 4) p.add(new Placement(0, y, 2, "GLASS"));
        p.add(new Placement(2, 1, 2, "TORCH"));
        return p;
    }

    private static List<Placement> castle() {
        List<Placement> p = new ArrayList<>();
        // 15x15 curtain wall 5 high + 4 corner towers 8 high
        for (int y = 0; y <= 5; y++)
            for (int x = 0; x <= 14; x++)
                for (int z = 0; z <= 14; z++) {
                    boolean edge = x == 0 || x == 14 || z == 0 || z == 14;
                    if (y == 0) p.add(new Placement(x, y, z, "STONE_BRICK"));
                    else if (edge) p.add(new Placement(x, y, z, "STONE_BRICK"));
                    else p.add(new Placement(x, y, z, "AIR"));
                }
        for (int x = 0; x <= 14; x++)
            for (int z = 0; z <= 14; z++) {
                boolean edge = x == 0 || x == 14 || z == 0 || z == 14;
                if (edge && (x + z) % 2 == 0) p.add(new Placement(x, 6, z, "STONE_BRICK"));
            }
        int[][] corners = {{0, 0}, {12, 0}, {0, 12}, {12, 12}};
        for (int[] c : corners)
            for (int y = 1; y <= 9; y++)
                for (int x = 0; x <= 2; x++)
                    for (int z = 0; z <= 2; z++) {
                        boolean edge = x == 0 || x == 2 || z == 0 || z == 2;
                        if (edge) p.add(new Placement(c[0] + x, y, c[1] + z, "COBBLE"));
                    }
        // gate
        for (int dx = 6; dx <= 8; dx++) for (int dy = 1; dy <= 3; dy++) p.add(new Placement(dx, dy, 0, "AIR"));
        // keep inside
        box(p, 5, 1, 5, 9, 5, 9, "STONE_BRICK", "STONE_BRICK", "COBBLE", true);
        p.add(new Placement(7, 1, 5, "AIR"));
        p.add(new Placement(7, 2, 5, "AIR"));
        p.add(new Placement(7, 2, 7, "TORCH"));
        return p;
    }

    private static List<Placement> bridge() {
        List<Placement> p = new ArrayList<>();
        for (int z = 0; z < 20; z++) {
            for (int x = 0; x <= 2; x++) p.add(new Placement(x, 0, z, "PLANKS"));
            if (z % 3 == 0) { p.add(new Placement(0, 1, z, "LOG")); p.add(new Placement(2, 1, z, "LOG")); }
        }
        return p;
    }

    private static List<Placement> pyramid() {
        List<Placement> p = new ArrayList<>();
        int n = 9;
        for (int y = 0; y < n; y++) {
            int lo = y, hi = 2 * n - 2 - y;
            for (int x = lo; x <= hi; x++) for (int z = lo; z <= hi; z++) p.add(new Placement(x, y, z, "SANDSTONE"));
        }
        return p;
    }

    private static List<Placement> platform() {
        List<Placement> p = new ArrayList<>();
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) p.add(new Placement(x, 0, z, "QUARTZ"));
        return p;
    }
}
