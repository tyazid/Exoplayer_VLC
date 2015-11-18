package com.google.android.exoplayer.raw;

import android.util.Log;

import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DataSpec;

import java.io.IOException;

/**
 * Created by ataldir on 26/02/2015.
 */
public class RawBufferedSource implements DataSource {

    private final String TAG = "RawBufferedSource";
    private final int BUFFER_LENGTH = 16*1024;
    private final int SAMPLE_LENGTH = 4*1024;

    private DataSource dataSource;
    private byte[] buffer;
    private int read_pos = 0;
    private int write_pos = 0;

    public RawBufferedSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /*
    *
    * Implements DataSource
    *
    */

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        Log.d(TAG, "open(): --> <--");
        long result = this.dataSource.open(dataSpec);
        this.read_pos = 0;
        this.write_pos = 0;
        this.buffer = new byte[BUFFER_LENGTH];
        return result;
    }

    @Override
    public void close() throws IOException {
        this.buffer = null;
        this.dataSource.close();
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {

        //return this.dataSource.read(buffer, offset, readLength);

        //Log.d(TAG, "read(): want size="+readLength+", on available="+getContentLength());
        while( getContentLength()< readLength ) {
            int read = internalReadFromSource();
            //Log.d(TAG, "read(): ... read="+read+", getContentLength()="+getContentLength());
        }

        if( write_pos >= read_pos ) {
            if( write_pos-read_pos >= readLength ) {
                System.arraycopy(this.buffer, read_pos, buffer, 0, readLength);
                read_pos += readLength;
                return readLength;
            }
            else {
                Log.d(TAG, "Not enough data to read from");
                return 0;
            }
        }
        else {
            int len = Math.min(BUFFER_LENGTH-read_pos, readLength);
            System.arraycopy(this.buffer, read_pos, buffer, 0, len);
            read_pos += len;
            if( read_pos >= BUFFER_LENGTH )
                read_pos = 0;
            if( len < readLength ) {
                int rest = readLength-len;
                System.arraycopy(this.buffer, read_pos, buffer, len, rest);
                read_pos += rest;
            }
            return readLength;
        }
    }

    /*
    *
    * private methods
    *
    */

    private long getContentLength() {
        long content_length = 0;
        if( write_pos >= read_pos )
            content_length = write_pos-read_pos;
        else
            content_length = BUFFER_LENGTH-read_pos + write_pos;
        return content_length;
    }

    private int internalReadFromSource() throws IOException {

        int read = 0;
        if( write_pos >= read_pos ) {
            int len = Math.min(BUFFER_LENGTH-write_pos, SAMPLE_LENGTH);
            read = this.dataSource.read(this.buffer, write_pos, len);
            write_pos += read;
            if( write_pos >= BUFFER_LENGTH )
                write_pos = 0;
        }
        else if( write_pos < read_pos-1 ) {
            int len = Math.min(read_pos-write_pos-1, SAMPLE_LENGTH);
            read = this.dataSource.read(this.buffer, write_pos, len);
            write_pos += read;
        }

        return read;
    }

}
