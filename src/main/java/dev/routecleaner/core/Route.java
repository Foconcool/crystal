package dev.routecleaner.core;

import java.util.List;

public record Route(String name, String type, String location, List<Waypoint> positions) {
    public Route {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Route name is required");
        if (!"gemstone".equalsIgnoreCase(type)) throw new IllegalArgumentException("Only gemstone routes are supported");
        if (!"CRYSTAL_HOLLOWS".equalsIgnoreCase(location)) throw new IllegalArgumentException("Route must be in Crystal Hollows");
        if (positions == null || positions.size() < 2 || positions.size() > 1000)
            throw new IllegalArgumentException("Route needs 2-1000 VEIN waypoints");
        positions = List.copyOf(positions);
        if (positions.stream().anyMatch(p -> !"VEIN".equalsIgnoreCase(p.action())))
            throw new IllegalArgumentException("Only VEIN waypoints are supported");
    }
    public record Waypoint(int x, int y, int z, String action) {
        public Point3 block() { return new Point3(x, y, z); }
    }
}
