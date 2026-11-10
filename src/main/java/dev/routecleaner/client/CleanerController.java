package dev.routecleaner.client;

import dev.routecleaner.core.BlockKind;
import dev.routecleaner.core.Point3;
import dev.routecleaner.core.SpreadRanker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Only permitted with an integrated single-player server.
 * No spoofed packets, anti-cheat bypass, hidden rotations, or remote-server automation.
 * Requires a clear vanilla ray and a reachable block, and stops on unavailable chunks.
 */
public final class CleanerController {
    private final RouteCleanerClient app;
    private final SpreadRanker ranker = new SpreadRanker();
    private boolean running;
    private Point3 current;
    private int targetTicks;
    private int idleTicks;
    private final Set<Point3> skipped = new HashSet<>();
    public CleanerController(RouteCleanerClient app) { this.app = app; }

    public boolean running() { return running; }
    public Point3 currentTarget() { return current; }

    public void start(Minecraft client) {
        if (client.getSingleplayerServer() == null) {
            app.notifyPlayer(client, "Automatic clearing is only available in single-player worlds. Multiplayer: scan/preview only.");
            return;
        }
        if (client.level == null || app.report == null || app.report.targets().isEmpty()) {
            app.notifyPlayer(client, "Scan a route with mineable targets first.");
            return;
        }
        if (!app.settings.singleplayerAutoMine) {
            app.notifyPlayer(client, "Enable single-player auto mine in Settings first.");
            return;
        }
        skipped.clear();
        running = true;
        current = null;
        idleTicks = 0;
        app.notifyPlayer(client, "Single-player mining started. Close GUI; press J to stop.");
    }

    public void stop(Minecraft client) {
        if (running && client.gameMode != null) client.gameMode.stopDestroyBlock();
        if (running) app.message = "Auto mine stopped.";
        running = false;
        current = null;
        targetTicks = 0;
    }

    public void tick(Minecraft client) {
        if (!running) return;
        if (client.getSingleplayerServer() == null || client.level == null || client.player == null || client.gameMode == null) {
            stop(client);
            return;
        }
        if (client.gui.screen() != null) return;
        WorldGrid world = new WorldGrid(client);
        if (current != null) {
            if (!world.get(current).mineable(app.settings.oresEnabled)) {
                client.gameMode.stopDestroyBlock();
                current = null;
                targetTicks = 0;
                if (app.selected() != null) app.report = app.analyzer.analyze(app.selected(),
                    world, app.settings.loopRoute, app.settings.oresEnabled);
                return;
            }
            BlockHitResult hit = unobstructed(client, current);
            if (hit == null || ++targetTicks > 160) {
                client.gameMode.stopDestroyBlock();
                skipped.add(current);
                current = null;
                targetTicks = 0;
                return;
            }
            client.gameMode.continueDestroyBlock(WorldGrid.toBlockPos(current), hit.getDirection());
            return;
        }
        if (app.report == null || app.report.targets().isEmpty()) {
            stop(client);
            app.notifyPlayer(client, "No remaining obstruction targets.");
            return;
        }
        List<Point3> ranked = ranker.sort(app.report.targets(), app.settings.spreadAware);
        for (Point3 target : ranked) {
            if (skipped.contains(target) || !world.get(target).mineable(app.settings.oresEnabled)) continue;
            BlockHitResult hit = unobstructed(client, target);
            if (hit == null) continue;
            current = target;
            targetTicks = 0;
            client.gameMode.startDestroyBlock(WorldGrid.toBlockPos(target), hit.getDirection());
            return;
        }
        if (++idleTicks > 40) {
            stop(client);
            app.notifyPlayer(client, "No visible obstruction within reach; move closer and rescan.");
        }
    }

    private BlockHitResult unobstructed(Minecraft client, Point3 target) {
        if (client.player == null || client.level == null) return null;
        Vec3 eye = client.player.getEyePosition();
        Vec3 end = new Vec3(target.x() + .5, target.y() + .5, target.z() + .5);
        if (eye.distanceToSqr(end) > app.settings.reach * app.settings.reach) return null;
        BlockHitResult hit = client.level.clip(new ClipContext(eye, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(WorldGrid.toBlockPos(target))
            ? hit : null;
    }
}
