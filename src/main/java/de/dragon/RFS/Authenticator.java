package de.dragon.RFS;

import javax.xml.ws.spi.http.HttpExchange;
import java.net.Socket;
import java.sql.Time;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    public Session authConnection(HttpExchange exchange, Socket socket) {
        if(!isConnectionAuth(exchange)) {
            String uuid = UUID.randomUUID().toString();
            exchange.addResponseHeader("Set-Cookie", "session-id=" + uuid);
            map.put(uuid, new Session(socket));

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
        String cookiesClient = exchange.getRequestHeader("Cookie");
        if (cookiesClient == null || !cookiesClient.contains("session-id")) {
            return null;
        } else {
            int index = cookiesClient.indexOf("session-id");
            String sessionid = cookiesClient.substring(index + "session-id".length() + 1, cookiesClient.substring(index).contains(";") ? cookiesClient.indexOf(";", index) : cookiesClient.length());

            return map.get(sessionid);
        }
    }

}
