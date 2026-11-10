package dev.routecleaner.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Geometrical approximation of a line of sight between VEIN waypoint platforms.
 * The game server decides actual Etherwarp success; this is only a planner.
 */
public final class RouteAnalyzer {
    public static final double MAX_WARP = 57.0;
    private static final double[][] ENDPOINTS = {
        {0.5, 0.5}, {0.25, 0.25}, {0.25, 0.75}, {0.75, 0.25},
        {0.75, 0.75}, {0.5, 0.25}, {0.5, 0.75}, {0.25, 0.5}, {0.75, 0.5}
    };

    public ScanReport analyze(Route route, BlockGrid grid, boolean loop, boolean ores) {
        List<ConnectionReport> connections = new ArrayList<>();
        Set<Point3> targets = new LinkedHashSet<>();
        int n = route.positions().size();
        for (int i = 0; i < n - (loop ? 0 : 1); i++) {
            int to = (i + 1) % n;
            ConnectionReport report = connection(i, to,
                route.positions().get(i).block(), route.positions().get(to).block(), grid, ores);
            connections.add(report);
            targets.addAll(report.targets());
        }
        return new ScanReport(connections, targets);
    }

    private ConnectionReport connection(int i, int j, Point3 from, Point3 to,
                                        BlockGrid grid, boolean ores) {
        if (Math.sqrt(from.distanceSquared(to)) > MAX_WARP)
            return new ConnectionReport(i, j, ConnectionReport.Status.OUT_OF_RANGE, Set.of(), Set.of());

        BlockKind fromKind = grid.get(from);
        BlockKind toKind = grid.get(to);
        boolean platformUnloaded = fromKind == BlockKind.UNLOADED || toKind == BlockKind.UNLOADED;
        boolean missing = fromKind != BlockKind.PLATFORM || toKind != BlockKind.PLATFORM;
        PathChoice best = null;
        for (double[] endpoint : ENDPOINTS) {
            PathChoice candidate = trace(from.x() + .5, from.y() + 2.62, from.z() + .5,
                to.x() + endpoint[0], to.y() + 1.001, to.z() + endpoint[1], from, to, grid, ores);
            // Headroom at both platforms must exist for the route to be useful.
            for (Point3 p : List.of(from.above(), from.offset(0, 2, 0),
                                    to.above(), to.offset(0, 2, 0))) {
                addBlock(candidate, p, grid.get(p), ores);
            }
            if (best == null || candidate.score() < best.score()) best = candidate;
        }
        if (best == null) throw new IllegalStateException("No candidate paths");
        ConnectionReport.Status status = (platformUnloaded || best.unloaded) ? ConnectionReport.Status.UNLOADED
            : missing ? ConnectionReport.Status.MISSING_PLATFORM
            : !best.protectedBlocks.isEmpty() ? ConnectionReport.Status.PROTECTED
            : !best.targets.isEmpty() ? ConnectionReport.Status.BLOCKED
            : ConnectionReport.Status.CLEAR;
        // A connection cannot be scheduled for mining until its chunks and platforms exist.
        return new ConnectionReport(i, j, status,
            (platformUnloaded || missing || best.unloaded) ? Set.of() : best.targets,
            best.protectedBlocks);
    }

    // A small fixed-step voxel sampler, not server-side Etherwarp simulation.
    private PathChoice trace(double sx, double sy, double sz, double ex, double ey, double ez,
                             Point3 from, Point3 to, BlockGrid grid, boolean ores) {
        PathChoice choice = new PathChoice();
        double dx = ex - sx, dy = ey - sy, dz = ez - sz;
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx*dx + dy*dy + dz*dz) * 8));
        Point3 prior = null;
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            Point3 p = new Point3((int)Math.floor(sx + dx*t),
                (int)Math.floor(sy + dy*t), (int)Math.floor(sz + dz*t));
            if (p.equals(prior)) continue;
            prior = p;
            if (p.equals(from) || p.equals(to)) continue;
            addBlock(choice, p, grid.get(p), ores);
        }
        return choice;
    }

    private void addBlock(PathChoice c, Point3 p, BlockKind kind, boolean ores) {
        if (!kind.blocksRay()) return;
        if (kind == BlockKind.UNLOADED) { c.unloaded = true; return; }
        if (kind.mineable(ores)) c.targets.add(p);
        else c.protectedBlocks.add(p);
    }

    private static final class PathChoice {
        final Set<Point3> targets = new LinkedHashSet<>();
        final Set<Point3> protectedBlocks = new LinkedHashSet<>();
        boolean unloaded;
        int score() { return (unloaded ? 100000 : 0) + protectedBlocks.size()*1000 + targets.size(); }
    }
}
