package de.dragon.RFS.file;

import de.dragon.RFS.auth.Session;

import java.io.File;
import java.util.UUID;

public class FileShare extends TimeStamp {

    private File file;
    private long maxAge;
    private Session sender;
    private Session receiver;
    private String id;

    public FileShare(File file, long maxAge, Session sender, Session receiver) {
        super();

        this.file = file;
        this.maxAge = maxAge;
        this.sender = sender;
        this.receiver = receiver;
        this.id = UUID.randomUUID().toString();
    }

    public File getFile() {
        return file;
    }

    public String getId() {
        return id;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public Session getSender() {
        return sender;
    }

    public Session getReceiver() {
        return receiver;
    }
}
