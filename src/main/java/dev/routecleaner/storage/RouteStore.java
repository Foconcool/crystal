package dev.routecleaner.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.routecleaner.core.Route;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class RouteStore {
    public record StoredRoute(String fileName, Route route) {}
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public final Path folder = FabricLoader.getInstance().getConfigDir().resolve("routecleaner/routes");

    public List<StoredRoute> loadAll() {
        try {
            Files.createDirectories(folder);
            List<StoredRoute> routes = new ArrayList<>();
            try (Stream<Path> paths = Files.list(folder)) {
                paths.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .limit(500)
                    .forEach(p -> {
                        try { routes.add(new StoredRoute(p.getFileName().toString(),
                            fromJson(Files.readString(p, StandardCharsets.UTF_8)))); }
                        catch (Exception ex) { System.err.println("[routecleaner] Invalid " + p + ": " + ex.getMessage()); }
                    });
            }
            return List.copyOf(routes);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't open route directory: " + e.getMessage(), e);
        }
    }

    public String importJson(String json) throws Exception {
        Route route = fromJson(json);
        Files.createDirectories(folder);
        String safe = route.name().toLowerCase().replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        if (safe.isEmpty()) safe = "route";
        safe = safe.length() > 60 ? safe.substring(0, 60) : safe;
        String candidate = safe + ".json";
        int suffix = 2;
        while (Files.exists(folder.resolve(candidate)) && suffix <= 999) candidate = safe + "-" + suffix++ + ".json";
        if (Files.exists(folder.resolve(candidate))) throw new IllegalStateException("Too many copies");
        Files.writeString(folder.resolve(candidate), json, StandardCharsets.UTF_8);
        return candidate;
    }

    public static Route fromJson(String json) {
        if (json == null || json.length() > 2_000_000) throw new IllegalArgumentException("Route JSON is empty or too large");
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String name = require(root, "name");
        String type = require(root, "type");
        String location = require(root, "location");
        JsonArray positions = root.getAsJsonArray("positions");
        if (positions == null) throw new IllegalArgumentException("Missing positions array");
        List<Route.Waypoint> waypoints = new ArrayList<>();
        for (JsonElement e : positions) {
            JsonObject p = e.getAsJsonObject();
            waypoints.add(new Route.Waypoint(p.get("x").getAsInt(), p.get("y").getAsInt(),
                p.get("z").getAsInt(), require(p, "action")));
        }
        return new Route(name, type, location, waypoints);
    }

    private static String require(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) throw new IllegalArgumentException("Missing " + field);
        return json.get(field).getAsString();
    }
}
