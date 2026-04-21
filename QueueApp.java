import java.io.IOException;

/**
 * Main launcher - starts API server.
 */
public class QueueApp {
    public static void main(String[] args) {
        // Init DB early
        QueueService.getInstance();
        
        try {
            QueueApiServer apiServer = new QueueApiServer();
            apiServer.start();
            
            System.out.println("=== Smart Queue Management System v3.0 - WEB API ===");
            System.out.println("API Server running at http://localhost:8080/api/");
            System.out.println("Web UI at http://localhost:8080/index.html");
            System.out.println("Live Queue: http://localhost:8080/live.html");
            System.out.println("Display (HDMI): http://localhost:8080/display.html (fullscreen)");
            System.out.println("Open browsers and test!");
            
            // Graceful shutdown on Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\\nShutting down server...");
                apiServer.stop();
                QueueService.getInstance().close();
            }));
            
            // Keep alive
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

