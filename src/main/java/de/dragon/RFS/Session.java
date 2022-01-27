package de.dragon.RFS;

import java.net.Socket;
import java.sql.Time;

public class Session extends TimeStamp {

    private Socket connection;

    public Session(Socket connection) {
        super();

        this.connection = connection;
    }

    public Socket getConnection() {
        return connection;
    }
}
