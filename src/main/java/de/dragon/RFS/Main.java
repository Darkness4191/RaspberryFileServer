package de.dragon.RFS;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.dragon.RFS.auth.Authenticator;
import de.dragon.RFS.auth.Operation;
import de.dragon.RFS.auth.Session;
import de.dragon.RFS.file.FileShare;

import java.io.*;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

    public static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private Authenticator authenticator;

    private ArrayList<HttpContext> fileContexts = new ArrayList<>();
    private HttpServer server;

    public static void main(String[] args) throws IOException {
        new Main();
    }

    public Main() throws IOException {
        authenticator = new Authenticator();

        server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/", this::root);
        server.createContext("/receive", this::receive);
        server.createContext("/send", this::send);
        server.setExecutor(null);
        server.start();
    }

    private void root(HttpExchange httpExchange) throws IOException {
        authenticator.authConnection(httpExchange);

        sendFile(new File("./webroot/index.html"), httpExchange);
    }

    private void receive(HttpExchange httpExchange) throws IOException {
        Session session = authenticator.authConnection(httpExchange);
        JsonObject info = new JsonObject();

        if(httpExchange.getRequestMethod().equals("GET")) {
            sendFile(new File("./webroot/receive.html"), httpExchange);
        } else if(httpExchange.getRequestMethod().equals("POST")) {
            JsonObject object = JsonObject.readFrom(new InputStreamReader(httpExchange.getRequestBody()));
            if(object.get("status").toString().equals("check")) {
                session.setOperation(Operation.RECEIVE);
                JsonArray array = new JsonArray();
                for(FileShare share : session.getFiles().values()) {
                    JsonObject file = new JsonObject();
                    file.add("id", share.getId());
                    file.add("name", share.getFile().getName());
                    file.add("size", share.getFile().length());
                    file.add("sender", share.getSender().getName());
                    array.add(file);
                }

                info.add("files", array);
            } else if(object.get("status").toString().equals("accept")) {
                FileShare file = session.getFiles().get(object.get("file").asObject().get("id"));
                info.add("link", "/" + file.getId());

                HttpContext create = server.createContext("/" + file.getId(), new HttpHandler() {
                    @Override
                    public void handle(HttpExchange httpExchange) throws IOException {
                        Session session1 = authenticator.authConnection(httpExchange);
                        if(session1.getName().equals(session.getName())) {
                            sendFile(file.getFile(), httpExchange);
                        }
                    }
                });

                create.getAttributes().put("timestamp", Instant.now().getEpochSecond());
            } else if(object.get("status").toString().equals("deny")) {
                session.removeFile(session.getFiles().get(object.get("file").asObject().get("id")));
            }

            try {
                if(session.getName() != null) {
                    info.add("device-name", session.getName());
                } else {
                    session.setName(authenticator.getClosestName(object.get("device-wish-name").toString()));
                    info.add("device-name", session.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void send(HttpExchange httpExchange) throws IOException {
        authenticator.authConnection(httpExchange);
    }

    private void sendFile(File f, HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(200, f.length());
        OutputStream os = httpExchange.getResponseBody();
        FileInputStream fileInputStream = new FileInputStream(f);
        int j = 0;
        byte[] buffer = new byte[8 * 1024];
        while((j = fileInputStream.read(buffer)) > 0) {
            os.write(buffer, 0, j);
        }
        os.close();
    }

}
