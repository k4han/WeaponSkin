package me.k4han.weaponSkin.pack;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Small HTTP server running inside the plugin, serves resource pack ZIP file to Minecraft clients.
 * Uses com.sun.net.httpserver.HttpServer built into JDK.
 */
public class PackHostServer {

    private final JavaPlugin plugin;
    private HttpServer httpServer;
    private ExecutorService executor;
    private final AtomicReference<File> packFileRef = new AtomicReference<>();
    private final int port;

    public PackHostServer(JavaPlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    /**
     * Initialize and start HTTP server.
     * @throws IOException if port is occupied or cannot bind
     */
    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/pack", new PackHandler());
        executor = Executors.newFixedThreadPool(4);
        httpServer.setExecutor(executor);
        httpServer.start();
        plugin.getLogger().info("Pack HTTP server started on port " + port);
    }

    /**
     * Stop HTTP server when plugin disables.
     */
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            plugin.getLogger().info("Pack HTTP server stopped.");
        }
    }

    /**
     * Update current ZIP file to serve.
     */
    public void updatePackFile(File packFile) {
        packFileRef.set(packFile);
        plugin.getLogger().info("Pack file updated: " + packFile.getPath());
    }

    /**
     * Get full URL for client to download pack.
     */
    public String getPackUrl(String serverHost) {
        return "http://" + serverHost + ":" + port + "/pack";
    }

    /**
     * Check if valid pack file is available.
     */
    public boolean hasPackFile() {
        File file = packFileRef.get();
        return file != null && file.exists();
    }

    /**
     * Internal handler for processing HTTP requests to /pack endpoint.
     */
    private class PackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            try {
                // Only accept GET and HEAD
                if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                    String response = "Method Not Allowed";
                    exchange.sendResponseHeaders(405, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }

                // Check if pack file exists (thread-safe read)
                File currentPack = packFileRef.get();
                if (currentPack == null || !currentPack.exists()) {
                    String response = "Not Found";
                    exchange.sendResponseHeaders(404, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }

                long fileSize = currentPack.length();

                // Set headers
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"WeaponSkin-pack.zip\"");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");

                if ("HEAD".equalsIgnoreCase(method)) {
                    // HEAD request: only return headers, no body
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileSize));
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    // GET request: stream ZIP file to response body
                    exchange.sendResponseHeaders(200, fileSize);
                    try (InputStream is = new FileInputStream(currentPack);
                         OutputStream os = exchange.getResponseBody()) {
                        is.transferTo(os);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error handling pack request: " + e.getMessage());
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (Exception ignored) {}
            } finally {
                exchange.close();
            }
        }
    }
}
