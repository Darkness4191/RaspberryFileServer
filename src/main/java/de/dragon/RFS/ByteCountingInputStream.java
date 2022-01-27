package de.dragon.RFS;

import java.io.IOException;
import java.io.InputStream;

public class ByteCountingInputStream extends InputStream {

    private final InputStream in;
    private long bytesRead = 0L;
    private long bytesSkipped = 0L;
    private long bytesReadSinceMark = 0L;
    private long bytesSkippedSinceMark = 0L;

    public ByteCountingInputStream(InputStream in) {
        this.in = in;
    }

    public ByteCountingInputStream(InputStream in, long initialOffset) {
        this.in = in;
        this.bytesSkipped = initialOffset;
    }

    public int read() throws IOException {
        int fromSuper = this.in.read();
        if (fromSuper >= 0) {
            ++this.bytesRead;
            ++this.bytesReadSinceMark;
        }

        return fromSuper;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int fromSuper = this.in.read(b, off, len);
        if (fromSuper >= 0) {
            this.bytesRead += (long)fromSuper;
            this.bytesReadSinceMark += (long)fromSuper;
        }

        return fromSuper;
    }

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    public long skip(long n) throws IOException {
        long skipped = this.in.skip(n);
        if (skipped >= 0L) {
            this.bytesSkipped += skipped;
            this.bytesSkippedSinceMark += skipped;
        }

        return skipped;
    }

    public long getBytesRead() {
        return this.bytesRead;
    }

    public long getBytesSkipped() {
        return this.bytesSkipped;
    }

    public long getBytesConsumed() {
        return this.getBytesRead() + this.getBytesSkipped();
    }

    public void mark(int readlimit) {
        this.in.mark(readlimit);
        this.bytesReadSinceMark = 0L;
        this.bytesSkippedSinceMark = 0L;
    }

    public boolean markSupported() {
        return this.in.markSupported();
    }

    public void reset() throws IOException {
        this.in.reset();
        this.bytesRead -= this.bytesReadSinceMark;
        this.bytesSkipped -= this.bytesSkippedSinceMark;
        this.bytesReadSinceMark = 0L;
        this.bytesSkippedSinceMark = 0L;
    }

    public void close() throws IOException {
        this.in.close();
    }

    public int available() throws IOException {
        return this.in.available();
    }
}

