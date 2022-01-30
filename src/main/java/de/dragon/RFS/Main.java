package de.dragon.RFS;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.sun.net.httpserver.*;
import de.dragon.RFS.auth.Authenticator;
import de.dragon.RFS.auth.Operation;
import de.dragon.RFS.auth.Session;
import de.dragon.RFS.file.DefinedRequestContext;
import de.dragon.RFS.file.FileShare;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

        System.out.println("Server started!");
        System.out.println(new File("./").getAbsolutePath());
    }

    private void root(HttpExchange httpExchange) throws IOException {
        authenticator.authConnection(httpExchange);

        sendFile(new File("./webroot/index.html"), httpExchange);
    }

    private void receive(HttpExchange httpExchange) throws IOException {
        Session session = authenticator.authConnection(httpExchange);
        JsonObject info = new JsonObject();

        if (httpExchange.getRequestMethod().equals("GET")) {
            sendFile(new File("./webroot/receive.html"), httpExchange);
        } else if (httpExchange.getRequestMethod().equals("POST")) {
            JsonObject object = JsonObject.readFrom(new InputStreamReader(httpExchange.getRequestBody()));
            if (object.get("status").toString().equals("check")) {
                session.setOperation(Operation.RECEIVE);
                JsonArray array = new JsonArray();
                for (FileShare share : session.getFiles().values()) {
                    JsonObject file = new JsonObject();
                    file.add("id", share.getId());
                    file.add("name", share.getFile().getName());
                    file.add("size", share.getFile().getSize());
                    file.add("sender", share.getSender().getName());
                    array.add(file);
                }

                info.add("files", array);
            } else if (object.get("status").toString().equals("accept")) {
                FileShare file = session.getFiles().get(object.get("file").asObject().get("id"));
                info.add("link", "/" + file.getId());

                ArrayList<HttpContext> contexts = new ArrayList<>();
                contexts.add(server.createContext("/" + file.getId(), new HttpHandler() {
                    @Override
                    public void handle(HttpExchange httpExchange) throws IOException {
                        Session session1 = authenticator.authConnection(httpExchange);
                        if (session1.getName().equals(session.getName())) {
                            httpExchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + file.getFile().getName());
                            httpExchange.getResponseHeaders().add("accept-ranges", "bytes");
                            httpExchange.getResponseHeaders().add("vary", "Accept-Encoding");
                            sendFile(file.getFile(), httpExchange);
                            server.removeContext(contexts.get(0));
                        }
                    }
                }));

                contexts.get(0).getAttributes().put("timestamp", Instant.now().getEpochSecond());
            } else if (object.get("status").toString().equals("deny")) {
                session.removeFile(session.getFiles().get(object.get("file").asObject().get("id")));
            }

            info.add("device_name", session.getName());
            sendJson(info, httpExchange);
        }
    }

    private void send(HttpExchange httpExchange) throws IOException {
        Session session = authenticator.authConnection(httpExchange);

        JsonObject info = new JsonObject();
        if (httpExchange.getRequestMethod().equals("GET")) {
            sendFile(new File("./webroot/send.html"), httpExchange);
        } else if (httpExchange.getRequestMethod().equals("POST")) {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(1024 * 1024 * 1024 * 4);
            factory.setRepository(new File("./files"));
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(-1);
            upload.setFileSizeMax(-1);

            List<FileItem> files;
            try {
                files = upload.parseRequest(new DefinedRequestContext("UTF-8",
                        httpExchange.getRequestHeaders().get("Content-Type").get(0),
                        Integer.parseInt(httpExchange.getRequestHeaders().get("Content-Length").get(0)),
                        httpExchange.getRequestBody()));
            } catch (FileUploadException e) {
                info.add("status", "encoding error");
                info.add("device_name", session.getName());
                sendJson(info, httpExchange);
                return;
            }

            String receiver = "";
            for (int i = 0; i < files.size(); i++) {
                FileItem f = files.get(i);
                if (i == 0) {
                    InputStreamReader reader = new InputStreamReader(f.getInputStream());
                    JsonObject target = JsonObject.readFrom(reader);
                    receiver = target.get("receiver").toString();
                    f.delete();
                } else if(!receiver.equals("")){
                    Session receiverSession = authenticator.getSessionByName(receiver);
                    if(receiverSession == null) {
                        info.add("status", "unknown receiver");
                        info.add("device_name", session.getName());
                        sendJson(info, httpExchange);
                        return;
                    } else {
                        receiverSession.putFile(new FileShare(f, 0, session, receiverSession));
                        info.add("status", "success, waiting for confirmation");
                    }
                } else {
                    info.add("status", "failed");
                    info.add("device_name", session.getName());
                    sendJson(info, httpExchange);
                    return;
                }
            }

            info.add("device_name", session.getName());
            sendJson(info, httpExchange);
        }
    }

    private void sendFile(FileItem f, HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(200, f.getSize());
        httpExchange.getResponseHeaders().add("Content-Type", getMimeType(f.getName()));
        OutputStream os = httpExchange.getResponseBody();
        InputStream fileInputStream = f.getInputStream();
        int j = 0;
        byte[] buffer = new byte[8 * 1024];
        while ((j = fileInputStream.read(buffer)) > 0) {
            os.write(buffer, 0, j);
        }
        os.close();
    }

    private void sendFile(File f, HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(200, f.length());
        httpExchange.getResponseHeaders().add("Content-Type", getMimeType(f.getName()));
        OutputStream os = httpExchange.getResponseBody();
        InputStream fileInputStream = new FileInputStream(f);
        int j = 0;
        byte[] buffer = new byte[8 * 1024];
        while ((j = fileInputStream.read(buffer)) > 0) {
            os.write(buffer, 0, j);
        }
        os.close();
    }

    private String getMimeType(String s) {
        return URLConnection.guessContentTypeFromName(s);
    }

    private void sendJson(JsonObject object, HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(200, object.size());
        OutputStreamWriter os = new OutputStreamWriter(httpExchange.getResponseBody());
        object.writeTo(os);
        os.close();
    }

}
