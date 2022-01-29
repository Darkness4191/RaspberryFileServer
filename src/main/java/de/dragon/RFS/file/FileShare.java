package de.dragon.RFS.file;

import de.dragon.RFS.auth.Session;
import org.apache.commons.fileupload.FileItem;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;

public class FileShare extends TimeStamp {

    private FileItem fileContent;
    private long maxAge;
    private Session sender;
    private Session receiver;
    private String id;

    public FileShare(FileItem fileContent, long maxAge, Session sender, Session receiver) {
        super();

        this.fileContent = fileContent;
        this.maxAge = maxAge;
        this.sender = sender;
        this.receiver = receiver;
        this.id = UUID.randomUUID().toString();
    }

    public FileItem getFile() {
        return fileContent;
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
