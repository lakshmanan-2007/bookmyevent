package com.lakshmanan.bookmyevent.lock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Distributed lock backed by our own Mini-Redis (Project 2) over its HTTPS gateway.
 *
 * Why HTTP and not the RESP/TCP protocol here? Free-tier hosts (Render) expose
 * HTTPS to the public internet but not a raw Redis TCP port. So the live
 * BookMyEvent talks to the live Mini-Redis over HTTPS:
 *   acquire -> POST {url}/api/lock/acquire?key=lock:{key}&ttl=10000   (SET NX PX inside)
 *   release -> POST {url}/api/lock/release?key=lock:{key}&token={token} (safe unlock)
 *
 * Enable with:  app.lock.provider=miniredis  and  app.miniredis.url=https://<your-miniredis>
 */
@Component
@ConditionalOnProperty(name = "app.lock.provider", havingValue = "miniredis")
public class MiniRedisHttpLock implements DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(MiniRedisHttpLock.class);
    private static final long TTL_MS = 10_000;
    private static final long MAX_WAIT_MS = 5000;
    private static final long RETRY_MS = 50;

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public MiniRedisHttpLock(@Value("${app.miniredis.url:}") String baseUrl) {
        // strip a trailing slash so URL building is predictable
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public <T> T runLocked(String key, Supplier<T> action) {
        String lockKey = "lock:" + key;
        String enc = URLEncoder.encode(lockKey, StandardCharsets.UTF_8);
        String token = null;
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        try {
            while (System.currentTimeMillis() < deadline) {
                token = tryAcquire(enc);
                if (token != null) {
                    break;
                }
                try {
                    Thread.sleep(RETRY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (token == null) {
                // Mini-Redis was unreachable or the lock stayed contended past the wait.
                // Degrade gracefully: the DB pessimistic lock inside the transaction is the
                // final oversell guarantee, so proceed instead of failing the booking.
                log.warn("MiniRedis lock not acquired for '{}'; proceeding (DB lock is the guard)", key);
            }
            return action.get();
        } finally {
            if (token != null) {
                release(enc, token);
            }
        }
    }

    private String tryAcquire(String encKey) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/lock/acquire?key=" + encKey + "&ttl=" + TTL_MS))
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(res.body());
            if (node.path("acquired").asBoolean(false)) {
                return node.path("token").asText(null);
            }
            return null;
        } catch (Exception e) {
            // Network hiccup: treat as "not acquired" and let the retry loop continue.
            return null;
        }
    }

    private void release(String encKey, String token) {
        try {
            String encToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/lock/release?key=" + encKey + "&token=" + encToken))
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Lock will auto-expire via TTL even if release fails.
        }
    }
}
