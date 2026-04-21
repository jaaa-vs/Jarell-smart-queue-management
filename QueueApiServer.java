import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;

/**
 * Embedded HTTP API Server for QueueService.
 * Runs on http://localhost:8080/api/*
 * Serves web/ static files at root.
 */
public class QueueApiServer {
    private static final int PORT = 8080;
    private HttpServer server;

    public QueueApiServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/", new ApiHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(null);
        System.out.println("Queue API Server ready at http://localhost:" + PORT + "/");
        System.out.println("Static web files at http://localhost:" + PORT + "/index.html");
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String response = "";
            int status = 200;

            try {
                QueueService service = QueueService.getInstance();
                if (!service.isConnected()) {
                    response = "{\"error\":\"DB Offline\"}";
                    status = 503;
                } else {
                    switch (path) {
                        case "/api/waiting":
                            response = jsonList(service.getWaitingQueue());
                            break;
                        case "/api/calling":
                            response = jsonList(service.getCallingQueue());
                            break;
                        case "/api/nextbatch":
                            response = jsonList(service.getNextBatch(5));
                            break;
                        case "/api/history":
                            response = jsonHistory(service.getHistory());
                            break;
                        case "/api/stats":
                            response = jsonStats(service.getStats());
                            break;
                        case "/api/status":
                            response = service.isConnected() ? "{\"connected\":true}" : "{\"connected\":false}";
                            break;
                        default:
                            if (method.equals("POST")) {
                                switch (path) {
                                    case "/api/generate":
                                        service.generateQueueNum();
                                        status = 204;
                                        break;
                                    case "/api/callnext":
                                        String called = service.callNext();
                                        if (called != null) response = "{\"called\":\"" + called + "\"}";
                                        else status = 404;
                                        break;
                                    case "/api/reset":
                                        service.resetQueue();
                                        status = 204;
                                        break;
                                    default:
                                        if (path.startsWith("/api/serve/")) {
                                            String num = path.substring("/api/serve/".length());
                                            if (service.serveCurrent(num)) status = 204;
                                            else status = 404;
                                        } else {
                                            status = 404;
                                        }
                                }
                            } else {
                                status = 405;
                            }
                    }
                }
            } catch (Exception e) {
                response = "{\"error\":\"" + e.getMessage() + "\"}";
                status = 500;
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST");
            exchange.sendResponseHeaders(status, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            File file = new File("web" + path);
            String contentType = guessContentType(path);

            if (file.exists() && file.isFile()) {
                long length = file.length();
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, length);
                try (InputStream is = new FileInputStream(file); OutputStream os = exchange.getResponseBody()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
            }
        }
    }

    private static String jsonList(List<String> list) {
        return "{\"data\":" + list.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",", "[", "]")) + "}";
    }

    private static String jsonHistory(List<Object[]> history) {
        StringBuilder sb = new StringBuilder("{\"data\":[");
        for (int i = 0; i < history.size(); i++) {
            Object[] row = history.get(i);
            sb.append("{\"num\":\"").append(row[0])
              .append("\",\"gen\":\"").append(row[1])
              .append("\",\"called\":\"").append(row[2])
              .append("\",\"status\":\"").append(row[3]).append("\"}");
            if (i < history.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String jsonStats(Map<String, Integer> stats) {
        return "{\"waiting\":" + stats.getOrDefault("waiting", 0) +
               ",\"nextBatch\":" + stats.getOrDefault("nextBatch", 0) +
               ",\"servedToday\":" + stats.getOrDefault("servedToday", 0) + "}";
    }

    private static String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }
}

