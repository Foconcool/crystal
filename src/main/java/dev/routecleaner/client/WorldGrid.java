package dev.routecleaner.client;

import dev.routecleaner.core.BlockGrid;
import dev.routecleaner.core.BlockKind;
import dev.routecleaner.core.Point3;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Works with loaded client world blocks only. No Polar integration is necessary. */
public final class WorldGrid implements BlockGrid {
    private final Minecraft mc;
    public WorldGrid(Minecraft mc) { this.mc = mc; }
    public static BlockPos toBlockPos(Point3 p) { return new BlockPos(p.x(), p.y(), p.z()); }

    @Override
    public BlockKind get(Point3 point) {
        if (mc.level == null) return BlockKind.UNLOADED;
        BlockPos pos = toBlockPos(point);
        if (!mc.level.hasChunkAt(pos)) return BlockKind.UNLOADED;
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir() || state.getCollisionShape(mc.level, pos).isEmpty()) return BlockKind.AIR;
        if (state.is(Blocks.COBBLESTONE)) return BlockKind.PLATFORM;
        if (state.is(Blocks.STONE) || state.is(Blocks.CYAN_TERRACOTTA)) return BlockKind.HARDSTONE;
        // Hypixel represents gemstone blocks using stained glass/panes.
        // Don't directly target *any* glass, even if it isn't a gemstone.
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (name.contains("glass")) return BlockKind.GEMSTONE;
        if (name.endsWith("_ore") || name.endsWith("_ores") || name.equals("ancient_debris"))
            return BlockKind.ORE;
        return BlockKind.OTHER_SOLID;
    }
}
