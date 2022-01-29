package de.dragon.RFS.file;

import org.apache.commons.fileupload.RequestContext;

import java.io.IOException;
import java.io.InputStream;

public class DefinedRequestContext implements RequestContext {

    private String encoding;
    private String contentType;
    private int contentLength;
    private InputStream inputStream;

    public DefinedRequestContext(String encoding, String contentType, int contentLength, InputStream inputStream) {
        this.encoding = encoding;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.inputStream = inputStream;
    }

    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }
}
