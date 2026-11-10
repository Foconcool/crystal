package dev.routecleaner.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SettingsStore {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path path = FabricLoader.getInstance().getConfigDir().resolve("routecleaner/settings.json");
    private Settings current;

    public Settings load() {
        try {
            Files.createDirectories(path.getParent());
            if (Files.isRegularFile(path)) {
                current = gson.fromJson(Files.readString(path, StandardCharsets.UTF_8), Settings.class);
            }
        } catch (Exception e) {
            System.err.println("[routecleaner] Couldn't read config: " + e.getMessage());
        }
        if (current == null) current = new Settings();
        current.sanitize();
        return current;
    }
    public Settings current() { return current == null ? load() : current; }
    public void save() {
        try {
            current().sanitize();
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(current), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[routecleaner] Couldn't save config: " + e.getMessage());
        }
    }
}
