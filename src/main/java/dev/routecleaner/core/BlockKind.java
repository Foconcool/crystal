package dev.routecleaner.core;

public enum BlockKind {
    AIR, HARDSTONE, ORE, GEMSTONE, PLATFORM, OTHER_SOLID, UNLOADED;

    public boolean mineable(boolean allowOres) {
        return this == HARDSTONE || (this == ORE && allowOres);
    }
    public boolean blocksRay() { return this != AIR; }
}
