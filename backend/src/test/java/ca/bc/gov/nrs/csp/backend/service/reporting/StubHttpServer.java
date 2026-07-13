package ca.bc.gov.nrs.csp.backend.service.reporting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Minimal scripted HTTP/1.1 server bound to the loopback interface, for exercising
 * {@code HttpURLConnection} / {@code RestClient} based code without external dependencies.
 *
 * <p>Responses are enqueued in the order they should be served (one response per
 * connection, always sent with {@code Connection: close}) and every request received
 * is recorded for later assertions.</p>
 *
 * <p>Implemented over a raw {@link ServerSocket} rather than {@code com.sun.net.httpserver}
 * because the latter normalizes response header names (e.g. {@code Set-Cookie} becomes
 * {@code Set-cookie}), while the production code looks up the literal {@code "Set-Cookie"}
 * key in {@code HttpURLConnection#getHeaderFields()}.</p>
 */
final class StubHttpServer implements AutoCloseable {

    /** A single recorded HTTP request. Header names are lower-cased. */
    record RecordedRequest(String method, String path, Map<String, String> headers, String body) {
        String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }
    }

    private final ServerSocket serverSocket;
    private final Thread acceptThread;
    private final BlockingQueue<byte[]> responses = new LinkedBlockingQueue<>();
    private final List<RecordedRequest> requests = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean running = true;

    private StubHttpServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.acceptThread = new Thread(this::acceptLoop, "stub-http-server-" + serverSocket.getLocalPort());
        this.acceptThread.setDaemon(true);
    }

    static StubHttpServer start() throws IOException {
        StubHttpServer server = new StubHttpServer(
                new ServerSocket(0, 50, InetAddress.getLoopbackAddress()));
        server.acceptThread.start();
        return server;
    }

    String baseUrl() {
        return "http://" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort();
    }

    /**
     * Enqueues the next response to serve.
     *
     * @param status      HTTP status code
     * @param body        response body (may be empty)
     * @param headerLines extra header lines, e.g. {@code "Set-Cookie: JSESSIONID=X; Path=/"}
     */
    void enqueue(int status, String body, String... headerLines) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        StringBuilder head = new StringBuilder("HTTP/1.1 ").append(status).append(" Stubbed\r\n");
        for (String headerLine : headerLines) {
            head.append(headerLine).append("\r\n");
        }
        head.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        head.append("Connection: close\r\n\r\n");
        byte[] headBytes = head.toString().getBytes(StandardCharsets.UTF_8);
        byte[] raw = new byte[headBytes.length + bodyBytes.length];
        System.arraycopy(headBytes, 0, raw, 0, headBytes.length);
        System.arraycopy(bodyBytes, 0, raw, headBytes.length, bodyBytes.length);
        responses.add(raw);
    }

    RecordedRequest request(int index) {
        return requests.get(index);
    }

    int requestCount() {
        return requests.size();
    }

    @Override
    public void close() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // shutting down
        }
        try {
            acceptThread.join(2_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void acceptLoop() {
        while (running) {
            try (Socket socket = serverSocket.accept()) {
                RecordedRequest request = readRequest(socket.getInputStream());
                if (request == null) {
                    continue;
                }
                requests.add(request);
                byte[] response = responses.poll();
                if (response == null) {
                    response = """
                            HTTP/1.1 500 No Scripted Response\r
                            Content-Length: 0\r
                            Connection: close\r
                            \r
                            """.getBytes(StandardCharsets.UTF_8);
                }
                OutputStream out = socket.getOutputStream();
                out.write(response);
                out.flush();
            } catch (IOException | RuntimeException e) {
                if (!running) {
                    return;
                }
                // ignore malformed/aborted connections and keep serving
            }
        }
    }

    private static RecordedRequest readRequest(InputStream in) throws IOException {
        String requestLine = readLine(in);
        if (requestLine.isEmpty()) {
            return null;
        }
        String[] parts = requestLine.split(" ");
        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while (!(line = readLine(in)).isEmpty()) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                headers.put(line.substring(0, idx).trim().toLowerCase(Locale.ROOT),
                        line.substring(idx + 1).trim());
            }
        }
        int contentLength = headers.containsKey("content-length")
                ? Integer.parseInt(headers.get("content-length"))
                : 0;
        byte[] body = in.readNBytes(contentLength);
        return new RecordedRequest(parts[0], parts[1], headers, new String(body, StandardCharsets.UTF_8));
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                buf.write(b);
            }
        }
        return buf.toString(StandardCharsets.UTF_8);
    }
}
