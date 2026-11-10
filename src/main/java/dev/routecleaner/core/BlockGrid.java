package dev.routecleaner.core;

@FunctionalInterface
public interface BlockGrid {
    BlockKind get(Point3 point);
}
