package dev.routecleaner.storage;

public final class Settings {
    public boolean oresEnabled = true;
    public boolean loopRoute = true;
    public boolean spreadAware = true;
    public boolean scanWhilePlaying = false;
    public boolean singleplayerAutoMine = false;
    public double reach = 4.25;
    public int scanIntervalTicks = 60;
    public String selectedRouteFile = "";

    public void sanitize() {
        reach = Math.max(2.5, Math.min(4.5, reach));
        scanIntervalTicks = Math.max(20, Math.min(200, scanIntervalTicks));
        if (selectedRouteFile == null) selectedRouteFile = "";
    }
}
