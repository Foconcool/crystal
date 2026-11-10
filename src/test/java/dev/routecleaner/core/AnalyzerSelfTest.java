package dev.routecleaner.core;

import java.util.List;
import java.util.Set;

/** Dependency-free test runnable with Java 21+: see README. */
public final class AnalyzerSelfTest {
    private static Route simpleRoute() {
        return new Route("Test", "gemstone", "CRYSTAL_HOLLOWS", List.of(
            new Route.Waypoint(0, 60, 0, "VEIN"),
            new Route.Waypoint(0, 60, 10, "VEIN")));
    }
    private static BlockGrid grid(BlockKind obstacle) {
        return p -> {
            if (p.equals(new Point3(0,60,0)) || p.equals(new Point3(0,60,10)))
                return BlockKind.PLATFORM;
            if (p.x() == 0 && p.z() == 5 && p.y() >= 61 && p.y() <= 63) return obstacle;
            return BlockKind.AIR;
        };
    }
    private static void check(boolean cond, String message) {
        if (!cond) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        RouteAnalyzer engine = new RouteAnalyzer();
        ScanReport clear = engine.analyze(simpleRoute(), grid(BlockKind.AIR), false, true);
        check(clear.connections().get(0).status() == ConnectionReport.Status.CLEAR, "empty ray clear");
        ScanReport hardstone = engine.analyze(simpleRoute(), grid(BlockKind.HARDSTONE), false, true);
        check(hardstone.connections().get(0).status() == ConnectionReport.Status.BLOCKED, "hardstone blocked");
        check(hardstone.targets().stream().allMatch(p -> p.z() == 5), "target only wall blocks");
        ScanReport gems = engine.analyze(simpleRoute(), grid(BlockKind.GEMSTONE), false, true);
        check(gems.connections().get(0).status() == ConnectionReport.Status.PROTECTED, "gemstone protected");
        check(gems.targets().isEmpty(), "no gemstone targets");
        ScanReport oreOff = engine.analyze(simpleRoute(), grid(BlockKind.ORE), false, false);
        check(oreOff.connections().get(0).status() == ConnectionReport.Status.PROTECTED, "ores excluded");
        ScanReport oreOn = engine.analyze(simpleRoute(), grid(BlockKind.ORE), false, true);
        check(oreOn.connections().get(0).status() == ConnectionReport.Status.BLOCKED, "ores enabled");
        ScanReport unloaded = engine.analyze(simpleRoute(), p ->
            p.equals(new Point3(0,60,0)) ? BlockKind.UNLOADED : grid(BlockKind.HARDSTONE).get(p), false, true);
        check(unloaded.connections().get(0).status() == ConnectionReport.Status.UNLOADED,
            "unloaded platform status");
        check(unloaded.targets().isEmpty(), "unloaded connections have no actionable targets");
        var ranker = new SpreadRanker();
        Set<Point3> cluster = Set.of(new Point3(1,1,1),new Point3(2,1,1),new Point3(3,1,1));
        check(ranker.sort(cluster, true).get(0).equals(new Point3(2,1,1)), "spread neighborhood ranking");
        System.out.println("AnalyzerSelfTest: all 9 checks passed");
    }
}
