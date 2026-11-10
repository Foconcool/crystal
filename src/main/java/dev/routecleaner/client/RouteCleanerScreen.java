package dev.routecleaner.client;

import dev.routecleaner.core.ConnectionReport;
import dev.routecleaner.core.Point3;
import dev.routecleaner.core.Route;
import dev.routecleaner.core.ScanReport;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Native Fabric/Minecraft GUI. No other menu library or Polar dependency. */
public final class RouteCleanerScreen extends Screen {
    public enum Tab { DASHBOARD, ROUTES, SCANNER, SETTINGS }

    private static final int BACKDROP = 0xD909101A;
    private static final int PANEL = 0xFF142031;
    private static final int CARD = 0xFF1B2B3E;
    private static final int ACCENT = 0xFF64D5E5;
    private static final int WHITE = 0xFFF3F8FE;
    private static final int MUTED = 0xFF93AFC0;
    private static final int WARN = 0xFFFFC070;
    private static final int OK = 0xFF88DBB0;

    private final RouteCleanerClient app;
    private final Tab tab;
    private final int page;
    private int x, y, w, h;

    public RouteCleanerScreen(RouteCleanerClient app) { this(app, Tab.DASHBOARD, 0); }
    private RouteCleanerScreen(RouteCleanerClient app, Tab tab, int page) {
        super(Component.literal("Crystal Route Cleaner"));
        this.app = app;
        this.tab = tab;
        this.page = page;
    }

    @Override
    protected void init() {
        w = Math.min(560, Math.max(290, width - 24));
        h = Math.min(390, Math.max(240, height - 20));
        x = (width - w) / 2;
        y = (height - h) / 2;
        int gap = 5;
        int navWidth = (w - 30 - gap * 3) / 4;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab selection = Tab.values()[i];
            btn(selection.name().substring(0, 1) + selection.name().substring(1).toLowerCase(),
                x + 15 + (navWidth + gap) * i, y + 43, navWidth, () -> show(selection, 0));
        }
        switch (tab) {
            case DASHBOARD -> dashboardButtons();
            case ROUTES -> routeButtons();
            case SCANNER -> scanButtons();
            case SETTINGS -> settingButtons();
        }
        btn("Done", x + w - 85, y + h - 29, 70, this::onClose);
    }

    private void dashboardButtons() {
        int contentY = y + 165;
        int width3 = (w - 40) / 3;
        btn("Scan route", x + 15, contentY, width3, () -> {
            app.scan(minecraft); show(Tab.DASHBOARD, 0);
        });
        btn("Start (local)", x + 20 + width3, contentY, width3, () -> {
            app.controller.start(minecraft); show(Tab.DASHBOARD, 0);
        });
        btn("Stop", x + 25 + width3 * 2, contentY, width3, () -> {
            app.controller.stop(minecraft); show(Tab.DASHBOARD, 0);
        });
        btn("Manage routes", x + 15, contentY + 39, (w - 35) / 2, () -> show(Tab.ROUTES, 0));
        btn("Scanner details", x + 20 + (w - 35) / 2, contentY + 39,
            (w - 35) / 2, () -> show(Tab.SCANNER, 0));
    }

    private void routeButtons() {
        btn("Import clipboard", x + 15, y + 114, (w - 40) / 2, () -> {
            app.importClipboard(minecraft); show(Tab.ROUTES, 0);
        });
        btn("Reload folder", x + 25 + (w - 40) / 2, y + 114, (w - 40) / 2, () -> {
            app.refreshRoutes(); app.message = "Route folder reloaded"; show(Tab.ROUTES, 0);
        });
        btn("< Previous", x + 15, y + 150, 104, () -> {
            app.select(app.routeIndex - 1); show(Tab.ROUTES, 0);
        });
        btn("Next >", x + w - 119, y + 150, 104, () -> {
            app.select(app.routeIndex + 1); show(Tab.ROUTES, 0);
        });
        btn("Scan selected route", x + 15, y + h - 68, w - 30, () -> {
            app.scan(minecraft); show(Tab.SCANNER, 0);
        });
    }

    private void scanButtons() {
        btn("Rescan", x + 15, y + 109, (w - 40) / 2, () -> {
            app.scan(minecraft); show(Tab.SCANNER, 0);
        });
        btn("Routes", x + 25 + (w - 40) / 2, y + 109, (w - 40) / 2,
            () -> show(Tab.ROUTES, 0));
        btn("<", x + 15, y + h - 68, 48, () -> show(Tab.SCANNER, Math.max(0, page - 1)));
        btn(">", x + w - 63, y + h - 68, 48, () -> show(Tab.SCANNER, page + 1));
    }

    private void settingButtons() {
        int colW = (w - 40) / 2;
        int x0 = x + 15, x1 = x + 25 + colW;
        int yy = y + 112, d = 35;
        btn("Ores: " + on(app.settings.oresEnabled), x0, yy, colW, () -> {
            app.settings.oresEnabled = !app.settings.oresEnabled; changed();
        });
        btn("Loop: " + on(app.settings.loopRoute), x1, yy, colW, () -> {
            app.settings.loopRoute = !app.settings.loopRoute; changed();
        });
        btn("Spread priority: " + on(app.settings.spreadAware), x0, yy+d, colW, () -> {
            app.settings.spreadAware = !app.settings.spreadAware; changed();
        });
        btn("Periodic scan: " + on(app.settings.scanWhilePlaying), x1, yy+d, colW, () -> {
            app.settings.scanWhilePlaying = !app.settings.scanWhilePlaying; changed();
        });
        btn("Local auto mine: " + on(app.settings.singleplayerAutoMine), x0, yy+d*2, colW, () -> {
            app.settings.singleplayerAutoMine = !app.settings.singleplayerAutoMine; changed();
        });
        btn("Reach: " + String.format("%.2f", app.settings.reach) + "m", x1, yy+d*2, colW, () -> {
            app.settings.reach = app.settings.reach >= 4.5 ? 2.5 : Math.min(4.5, app.settings.reach + .25);
            changed();
        });
        btn("Scan interval: " + app.settings.scanIntervalTicks + " ticks", x0, yy+d*3, w-30,
            () -> { app.settings.scanIntervalTicks = app.settings.scanIntervalTicks >= 200 ? 20
                : app.settings.scanIntervalTicks + 20; changed(); });
    }

    private void changed() {
        app.controller.stop(minecraft);
        app.report = null;
        app.configStore.save();
        app.message = "Settings saved. Rescan to refresh target selection.";
        show(Tab.SETTINGS, 0);
    }

    private static String on(boolean value) { return value ? "ON" : "OFF"; }
    private void btn(String label, int px, int py, int bw, Runnable callback) {
        this.addRenderableWidget(Button.builder(Component.literal(label), button -> callback.run())
            .bounds(px, py, Math.max(25,bw), 20).build());
    }
    private void show(Tab t, int p) {
        minecraft.gui.setScreen(new RouteCleanerScreen(app, t, p));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, BACKDROP);
        graphics.fill(x, y, x+w, y+h, PANEL);
        graphics.outline(x, y, w, h, 0xFF406879);
        graphics.fill(x+8, y+8, x+w-8, y+36, CARD);
        graphics.fill(x+15, y+82, x+w-15, y+83, 0xFF36556C);
        graphics.fill(x+15, y+h-39, x+w-15, y+h-38, 0xFF36556C);
        if (tab == Tab.DASHBOARD) {
            graphics.fill(x+15, y+100, x+w-15, y+153, CARD);
        } else if (tab == Tab.ROUTES) {
            graphics.fill(x+15, y+185, x+w-15, y+h-81, CARD);
        } else if (tab == Tab.SCANNER) {
            graphics.fill(x+15, y+140, x+w-15, y+h-82, CARD);
        } else {
            graphics.fill(x+15, y+96, x+w-15, y+h-65, CARD);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        label(graphics, "CRYSTAL ROUTE CLEANER", x+18, y+17, ACCENT);
        label(graphics, "FABRIC 26.2  |  POLAR-INDEPENDENT", x+w-205, y+18, MUTED);
        switch (tab) {
            case DASHBOARD -> renderDashboard(graphics);
            case ROUTES -> renderRoutes(graphics);
            case SCANNER -> renderScanner(graphics);
            case SETTINGS -> renderSettings(graphics);
        }
        String message = app.message == null ? "" : app.message;
        label(graphics, shorten(message, w-122), x+15, y+h-22, MUTED);
    }

    private void renderDashboard(GuiGraphicsExtractor g) {
        Route route = app.selected();
        label(g, "ACTIVE ROUTE", x+24, y+107, MUTED);
        label(g, route == null ? "No route loaded" : shorten(route.name(), w-65), x+24, y+123, WHITE);
        if (route != null) label(g, route.positions().size() + " VEIN points  |  " + route.location(), x+24, y+139, ACCENT);
        int yy = y+252;
        label(g, "SCAN STATUS", x+18, yy, ACCENT);
        label(g, app.report == null ? "Not scanned" : shorten(app.report.summary(),w-50), x+18, yy+18, WHITE);
        label(g, "Auto mine: " + (app.controller.running() ? "running (single-player)" : "idle"), x+18, yy+37, MUTED);
        label(g, "No remote-server automation or ban-evasion features.", x+18, yy+56, WARN);
    }

    private void renderRoutes(GuiGraphicsExtractor g) {
        label(g, "IMPORT GEMSTONE ROUTES", x+15, y+93, ACCENT);
        Route route = app.selected();
        if (route == null) {
            label(g, "No valid route files found.", x+25, y+194, WHITE);
        } else {
            label(g, "Route " + (app.routeIndex+1) + "/" + app.routes.size() + " : " + shorten(route.name(),w-165), x+130, y+157, WHITE);
            label(g, route.positions().size() + " VEIN waypoints", x+25, y+197, ACCENT);
            for (int i = 0; i < Math.min(6, route.positions().size()); i++) {
                Route.Waypoint p = route.positions().get(i);
                label(g, String.format("#%02d  (%d, %d, %d)", i+1, p.x(), p.y(), p.z()),
                    x+25, y+217+i*15, MUTED);
            }
            if (w >= 490) renderRouteMap(g, route);
        }
        label(g, "Folder: config/routecleaner/routes/*.json", x+15, y+h-91, MUTED);
    }

    /** A compact live route map using the actual waypoint coordinates. */
    private void renderRouteMap(GuiGraphicsExtractor g, Route route) {
        int mx = x + w - 198, my = y + 206, mw = 170, mh = Math.max(50, Math.min(105, h - 290));
        g.fill(mx, my, mx+mw, my+mh, 0xFF101A28);
        g.outline(mx, my, mw, mh, 0xFF406879);
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (Route.Waypoint p : route.positions()) {
            minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
            minZ = Math.min(minZ, p.z()); maxZ = Math.max(maxZ, p.z());
        }
        List<int[]> mapped = new ArrayList<>();
        for (Route.Waypoint p : route.positions()) {
            int px = mx + 5 + (p.x()-minX) * (mw-10) / Math.max(1, maxX-minX);
            int py = my + 5 + (p.z()-minZ) * (mh-10) / Math.max(1, maxZ-minZ);
            mapped.add(new int[]{px,py});
        }
        for (int i=0; i<mapped.size()-(app.settings.loopRoute?0:1); i++) {
            int[] a = mapped.get(i), b = mapped.get((i+1)%mapped.size());
            int steps = Math.max(1, Math.max(Math.abs(b[0]-a[0]), Math.abs(b[1]-a[1])));
            for (int t=0; t<=steps; t++) {
                int px = a[0] + (b[0]-a[0])*t/steps;
                int py = a[1] + (b[1]-a[1])*t/steps;
                g.fill(px,py,px+1,py+1,0xFF387D95);
            }
        }
        for (int[] p : mapped) g.fill(p[0]-1,p[1]-1,p[0]+2,p[1]+2,ACCENT);
    }

    private void renderScanner(GuiGraphicsExtractor g) {
        ScanReport scan = app.report;
        label(g, "ETHERWARP CONNECTION SCANNER", x+15, y+94, ACCENT);
        if (scan == null) {
            label(g, "Run a scan while the route's chunks are loaded.", x+25, y+154, WHITE);
            label(g, "Missing cobble and unloaded chunks are reported.", x+25, y+173, MUTED);
            return;
        }
        label(g, "Mineable: " + scan.targets().size() + " | Clear: "
            + scan.count(ConnectionReport.Status.CLEAR) + " | Protected: "
            + scan.count(ConnectionReport.Status.PROTECTED), x+25, y+149, WHITE);
        int start = page * 8;
        int shown = Math.min(8, Math.max(0, scan.connections().size() - start));
        for (int i = 0; i < shown; i++) {
            ConnectionReport c = scan.connections().get(start+i);
            int yy = y + 171 + i*17;
            int color = switch (c.status()) {
                case CLEAR -> OK;
                case BLOCKED -> ACCENT;
                case UNLOADED, MISSING_PLATFORM, OUT_OF_RANGE -> WARN;
                case PROTECTED -> 0xFFF199B7;
            };
            label(g, String.format("%02d -> %02d  %-16s  mine:%d protect:%d", c.fromIndex()+1,
                c.toIndex()+1, c.status(), c.targets().size(), c.protectedBlocks().size()),
                x+25, yy, color);
        }
        label(g, "Page " + (page+1) + "/" + Math.max(1,(scan.connections().size()+7)/8),
            x + w/2 - 38, y+h-61, MUTED);
    }

    private void renderSettings(GuiGraphicsExtractor g) {
        label(g, "BLOCK FILTERS & CLEARING", x+15, y+88, ACCENT);
        label(g, "Hard Stone is always enabled; gemstones are always excluded.", x+25, y+249, OK);
        label(g, "Mining Spread is server-owned: priority does not force spread.", x+25, y+266, MUTED);
        label(g, "Auto mining works in integrated single-player worlds only.", x+25, y+283, WARN);
    }

    private void label(GuiGraphicsExtractor graphics, String text, int xx, int yy, int color) {
        graphics.text(font, text, xx, yy, color, false);
    }
    private String shorten(String text, int pixels) {
        if (text == null) return "";
        if (font.width(text) <= pixels) return text;
        while (!text.isEmpty() && font.width(text+"...") > pixels) text = text.substring(0,text.length()-1);
        return text+"...";
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(null); }
    @Override
    public boolean isPauseScreen() { return false; }
}
