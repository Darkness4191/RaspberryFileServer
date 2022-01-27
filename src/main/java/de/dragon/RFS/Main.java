package de.dragon.RFS;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

    public static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    private Authenticator auth;

    public static void main(String[] args) throws IOException {
        new Main();
    }

    public Main() throws IOException {
        auth = new Authenticator();

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/", this::root);
        server.setExecutor(null);
        server.start();
    }

    private void root(HttpExchange httpExchange) throws IOException {
        String response = "This is the response";
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

}
