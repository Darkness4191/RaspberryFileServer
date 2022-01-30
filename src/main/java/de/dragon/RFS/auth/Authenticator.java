package de.dragon.RFS.auth;

import com.sun.net.httpserver.HttpExchange;
import de.dragon.RFS.Main;
import de.dragon.RFS.file.FileShare;
import de.dragon.RFS.file.TimeStamp;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Authenticator {

    private HashMap<String, Session> map = new HashMap<>();

    private String allowedChars = "0123456789";

    public Authenticator() {
        Main.executor.submit(() -> {
            try {
                while(true) {
                    TimeUnit.SECONDS.sleep(1);
                    new HashMap<>(map).forEach((k, v) -> {
                        if(Instant.now().getEpochSecond() - v.getTimestamp() >= TimeStamp.SESSION_TIMEOUT) {
                            map.remove(k);
                        }
                        for(FileShare share : v.getFiles().values()) {
                            if(Instant.now().getEpochSecond() - share.getTimestamp() >= share.getMaxAge()) {
                                v.removeFile(share.getId());
                                share.getFile().delete();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    public Session authConnection(HttpExchange exchange) throws IOException {
        if(!isConnectionAuth(exchange)) {
            String uuid = UUID.randomUUID().toString();
            exchange.getResponseHeaders().add("Set-Cookie", "session-id=" + uuid);
            map.put(uuid, new Session());

            return map.get(uuid);
        } else {
            return getSession(exchange);
        }
    }

    public boolean isConnectionAuth(HttpExchange exchange) {
        return getSession(exchange) != null;
    }

    public HashMap<String, Session> getSessions() {
        return new HashMap<>(map);
    }

    public Session getSession(HttpExchange exchange) {
        //Determine Session
        List<String> cookiesClient = exchange.getRequestHeaders().get("Cookie");
        if (cookiesClient == null || !containsSession(cookiesClient)) {
            return null;
        } else {
            String sessionid = "";

            for(String c : cookiesClient) {
                if(c.contains("session-id")) {
                    sessionid = c.split("=")[1];
                }
            }

            return map.get(sessionid);
        }
    }

    private boolean containsSession(List<String> list) {
        for(String c : list) {
            if(c.contains("session-id")) {
                return true;
            }
        }
        return false;
    }

    public Session getSessionByName(String name) {
        for(Session session : map.values()) {
            if(session.getName().equals(name)) {
                return session;
            }
        }
        return null;
    }

    public String getClosestName(String name) {
        while (getSessionByName(name) != null) {
            name = generateID();
        }
        return name;
    }

    public String generateID() {
        StringBuilder id = new StringBuilder();
        Random r = new Random();
        for(int i = 0; i < 6; i++) {
            id.append(allowedChars.toCharArray()[r.nextInt(6)]);
        }
        return id.toString();
    }
}
