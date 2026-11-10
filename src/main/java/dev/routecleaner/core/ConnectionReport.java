package dev.routecleaner.core;

import java.util.Set;

public record ConnectionReport(int fromIndex, int toIndex, Status status,
                               Set<Point3> targets, Set<Point3> protectedBlocks) {
    public enum Status { CLEAR, BLOCKED, PROTECTED, UNLOADED, MISSING_PLATFORM, OUT_OF_RANGE }
    public ConnectionReport {
        targets = Set.copyOf(targets);
        protectedBlocks = Set.copyOf(protectedBlocks);
    }
}
