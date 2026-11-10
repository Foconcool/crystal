package dev.routecleaner.core;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Spread is applied by the server. This only ranks initial blocks. */
public final class SpreadRanker {
    private static final int[][] SIDES = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public List<Point3> sort(Set<Point3> targets, boolean spreadAware) {
        if (!spreadAware) return targets.stream().toList();
        return targets.stream().sorted(Comparator.comparingInt(
            (Point3 p) -> neighborCount(p, targets)).reversed()
            .thenComparingInt(Point3::y).thenComparingInt(Point3::x).thenComparingInt(Point3::z)).toList();
    }
    public int neighborCount(Point3 p, Set<Point3> targets) {
        int count = 0;
        for (int[] d : SIDES) if (targets.contains(p.offset(d[0], d[1], d[2]))) count++;
        return count;
    }
}
