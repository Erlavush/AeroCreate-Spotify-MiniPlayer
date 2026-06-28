package dev.aerocreate.spotify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class LocalCallbackServer {
    private final int port;
    private ServerSocket serverSocket;
    private Thread thread;
    private volatile boolean running;

    public LocalCallbackServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        running = true;
        thread = new Thread(this::acceptLoop, "AeroCreate Spotify Callback");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void acceptLoop() {
        while (running) {
            try (Socket socket = serverSocket.accept()) {
                handle(socket);
                stop();
            } catch (IOException e) {
                if (running) {
                    AeroCreateSpotify.LOGGER.warn("Spotify callback server failed", e);
                }
            }
        }
    }

    private void handle(Socket socket) throws IOException {
        socket.setSoTimeout(5000);
        BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        String requestLine = reader.readLine();
        if (requestLine == null || !requestLine.startsWith("GET ")) {
            writeResponse(socket, 400, "Invalid Spotify callback.");
            return;
        }

        String target = requestLine.split(" ")[1];
        Map<String, String> query = parseQuery(target);
        SpotifyController.handleCallback(query.get("code"), query.get("state"), query.get("error"));
        writeResponse(socket, 200, "Spotify linked. You can close this tab and return to Minecraft.");
    }

    private static Map<String, String> parseQuery(String target) throws IOException {
        Map<String, String> result = new HashMap<>();
        int question = target.indexOf('?');
        if (question < 0 || question + 1 >= target.length()) {
            return result;
        }
        String query = target.substring(question + 1);
        try (BufferedReader reader = new BufferedReader(new StringReader(query.replace("&", "\n")))) {
            String pair;
            while ((pair = reader.readLine()) != null) {
                int equals = pair.indexOf('=');
                if (equals > 0) {
                    String key = URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    private static void writeResponse(Socket socket, int status, String message) throws IOException {
        byte[] body = ("<!doctype html><title>AeroCreate Spotify</title><p>" + message + "</p>").getBytes(StandardCharsets.UTF_8);
        String reason = status >= 200 && status < 300 ? "OK" : "Bad Request";
        String header = "HTTP/1.1 " + status + " " + reason + "\r\n"
            + "Content-Type: text/html; charset=utf-8\r\n"
            + "Content-Length: " + body.length + "\r\n"
            + "Connection: close\r\n\r\n";
        OutputStream output = socket.getOutputStream();
        output.write(header.getBytes(StandardCharsets.UTF_8));
        output.write(body);
        output.flush();
    }
}
