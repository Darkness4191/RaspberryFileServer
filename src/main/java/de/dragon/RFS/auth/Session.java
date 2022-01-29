package de.dragon.RFS.auth;

import de.dragon.RFS.file.FileShare;
import de.dragon.RFS.file.TimeStamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class Session extends TimeStamp {

    private Operation operation;
    private HashMap<String, FileShare> files;
    private long operationLastChanged = 0;
    private String name;

    public Session() {
        super();

        this.files = new HashMap<>();
        this.name = UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
        operationLastChanged = Instant.now().getEpochSecond();
    }

    public long getOperationLastChanged() {
        return operationLastChanged;
    }

    public void clearFiles() {
        files.clear();
    }

    public void putFile(FileShare f) {
        files.put(f.getId(), f);
    }

    public void removeFile(FileShare f) {
        files.remove(f);
    }

    public HashMap<String, FileShare> getFiles() {
        return new HashMap<>(files);
    }
}
