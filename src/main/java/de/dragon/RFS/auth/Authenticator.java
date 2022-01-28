package de.dragon.RFS.auth;

import com.sun.net.httpserver.HttpExchange;
import de.dragon.RFS.Main;
import de.dragon.RFS.file.TimeStamp;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Authenticator {

    private HashMap<String, Session> map = new HashMap<>();

    public Authenticator() {
        Main.executor.submit(() -> {
            try {
                while(true) {
                    TimeUnit.SECONDS.sleep(1);
                    new HashMap<>(map).forEach((k, v) -> {
                        if(Instant.now().getEpochSecond() - v.getTimestamp() >= TimeStamp.SESSION_TIMEOUT) {
                            map.remove(k);
                        }
                        if(Instant.now().getEpochSecond() - v.getOperationLastChanged() >= 3 && v.getOperation() != null) {
                            v.setOperation(null);
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
            exchange.getResponseHeaders().add("Set-Cookie", uuid);
            map.put(uuid, new Session());

            return map.get(uuid);
        } else {
            return getSession(exchange);
        }
    }

    public boolean isConnectionAuth(HttpExchange exchange) {
        return getSession(exchange) != null;
    }

    public Session getSession(HttpExchange exchange) {
        //Determine Session
        List<String> cookiesClient = exchange.getRequestHeaders().get("Cookie");
        if (cookiesClient == null || !cookiesClient.contains("session-id")) {
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

    public String getClosestName(String name) {
        AtomicInteger i = new AtomicInteger();
        map.forEach((k, v) -> {
            if(v.getName() != null && v.getOperation() == Operation.RECEIVE && v.getName().equals(name)) {
                i.getAndIncrement();
            }
        });
        return name + (i.get() != 0 ? Integer.toString(i.get()) : "");
    }
}
