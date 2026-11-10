package dev.routecleaner.core;

public record Point3(int x, int y, int z) {
    public Point3 above() { return new Point3(x, y + 1, z); }
    public Point3 offset(int dx, int dy, int dz) { return new Point3(x + dx, y + dy, z + dz); }
    public double distanceSquared(Point3 other) {
        double dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
