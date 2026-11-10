package dev.routecleaner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.routecleaner.core.Route;
import dev.routecleaner.core.RouteAnalyzer;
import dev.routecleaner.core.ScanReport;
import dev.routecleaner.storage.RouteStore;
import dev.routecleaner.storage.Settings;
import dev.routecleaner.storage.SettingsStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class RouteCleanerClient implements ClientModInitializer {
    public static final RouteCleanerClient INSTANCE = new RouteCleanerClient();
    public static final String MOD_ID = "routecleaner";
    public final SettingsStore configStore = new SettingsStore();
    public final RouteStore routeStore = new RouteStore();
    public final RouteAnalyzer analyzer = new RouteAnalyzer();
    public final CleanerController controller = new CleanerController(this);
    public Settings settings;
    public List<RouteStore.StoredRoute> routes = List.of();
    public int routeIndex = -1;
    public ScanReport report;
    public String message = "Import a route to begin.";
    private int ticks;

    private KeyMapping open;
    private KeyMapping stop;

    @Override
    public void onInitializeClient() {
        settings = configStore.load();
        refreshRoutes();
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "general"));
        open = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.routecleaner.open", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));
        stop = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.routecleaner.stop", InputConstants.Type.KEYSYM, InputConstants.KEY_J, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.consumeClick()) client.gui.setScreen(new RouteCleanerScreen(this));
            while (stop.consumeClick()) controller.stop(client);
            controller.tick(client);
            if (++ticks >= settings.scanIntervalTicks) {
                ticks = 0;
                if (settings.scanWhilePlaying && !controller.running()
                    && client.level != null && selected() != null) scan(client);
            }
        });
    }

    public void refreshRoutes() {
        String remember = selected() != null ? routes.get(routeIndex).fileName() : settings.selectedRouteFile;
        routes = routeStore.loadAll();
        routeIndex = routes.isEmpty() ? -1 : 0;
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).fileName().equals(remember)) { routeIndex = i; break; }
        }
        settings.selectedRouteFile = selected() == null ? "" : routes.get(routeIndex).fileName();
        configStore.save();
        report = null;
    }

    public void select(int index) {
        controller.stop(Minecraft.getInstance());
        if (routes.isEmpty()) { routeIndex = -1; report = null; return; }
        routeIndex = Math.floorMod(index, routes.size());
        settings.selectedRouteFile = routes.get(routeIndex).fileName();
        configStore.save();
        report = null;
        message = "Selected " + selected().name();
    }

    public Route selected() {
        return routeIndex >= 0 && routeIndex < routes.size() ? routes.get(routeIndex).route() : null;
    }

    public void scan(Minecraft client) {
        if (selected() == null) { message = "Import a route first."; return; }
        if (client.level == null) { message = "Join a world before scanning."; return; }
        controller.stop(client);
        report = analyzer.analyze(selected(), new WorldGrid(client), settings.loopRoute, settings.oresEnabled);
        message = report.summary();
    }

    public void importClipboard(Minecraft client) {
        try {
            String json = client.keyboardHandler.getClipboard();
            String file = routeStore.importJson(json);
            refreshRoutes();
            for (int i = 0; i < routes.size(); i++) {
                if (routes.get(i).fileName().equals(file)) select(i);
            }
            message = "Imported: " + file;
        } catch (Exception e) {
            message = "Import error: " + e.getMessage();
        }
    }

    public void notifyPlayer(Minecraft client, String text) {
        message = text;
        if (client.player != null) client.player.displayClientMessage(Component.literal("[Route Cleaner] " + text), false);
    }
}
