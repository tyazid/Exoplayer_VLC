package com.google.android.exoplayer.raw;

import android.util.Log;

import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DataSpec;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created by ataldir on 26/02/2015.
 */
public class UdpSampleSource implements DataSource {

    private final String TAG = "UdpSampleSource";
    private final int BUFFER_LENGTH = 10*1316*1024;
    private final int SAMPLE_LENGTH = 4*1024;

    private static final Object LOCK = new Object();

    private DataSource dataSource;
    private byte[] buffer;
    private int read_pos = 0;
    private int write_pos = 0;
    private boolean running = false;
    private Thread t;
    private int packet_length = 0;

    public UdpSampleSource(DataSource dataSource) { this.dataSource = dataSource; }

    /*
    *
    * Implements DataSource
    *
    */

    @Override
    public long open(DataSpec dataSpec) throws IOException {

        if( this.running )
            return 0;

        Log.d(TAG, "open(): --> <--");
        long result = this.dataSource.open(dataSpec);
        this.read_pos = 0;
        this.write_pos = 0;
        this.buffer = new byte[BUFFER_LENGTH];
        this.running = true;
        this.packet_length = 0;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        internalReadFromSource();
                    }
                    catch(UnknownHostException e){}
                    catch(IOException e) {}
                }
            }
        };
        t = new Thread(runnable);
        t.start();
        return result;
    }

    @Override
    public void close() throws IOException {

        if( !this.running )
            return;

        this.running = false;
        this.dataSource.close();
        try {
            t.join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.buffer = null;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {

        int to_read = 0;

        if( !this.running )
            return to_read;

        // Get the current content length (debug purpose only)
        long content_length = getContentLength();
        //Log.d(TAG, "read(): ... buffer used="+content_length+", free="+(BUFFER_LENGTH-content_length)+"/"+BUFFER_LENGTH);

        synchronized(LOCK) {
            if( write_pos == read_pos ) {
                //Log.d(TAG, "read(): Not enough data to read from, readLength="+readLength+", write_pos-read_pos="+(write_pos-read_pos)+", available="+content_length);
            }
            else if( write_pos > read_pos ) {
                to_read = Math.min(write_pos-read_pos, readLength);
                System.arraycopy(this.buffer, read_pos, buffer, offset, to_read);
                read_pos += to_read;
            }
            else {
                to_read = Math.min(BUFFER_LENGTH-read_pos, readLength);
                System.arraycopy(this.buffer, read_pos, buffer, offset, to_read);
                read_pos += to_read;
                if( read_pos >= BUFFER_LENGTH ) // read position has reached the end of buffer, loop to the beginning
                    read_pos = 0;
                if( (to_read < readLength) && (write_pos > read_pos) ) {
                    // We have still data available: continue to copy data from the beginning of the buffer
                    int rest = Math.min(write_pos-read_pos, readLength-to_read);
                    System.arraycopy(this.buffer, read_pos, buffer, offset+to_read, rest);
                    read_pos += rest;
                    to_read += rest;
                }
            }
        }
        return to_read;
    }

    /*
    *
    * private methods
    *
    */

    private long getContentLength() {
        long content_length = 0;
        synchronized(LOCK) {
            if (write_pos >= read_pos)
                content_length = write_pos - read_pos;
            else
                content_length = BUFFER_LENGTH - read_pos + write_pos;
        }
        return content_length;
    }

    private int internalReadFromSource() throws IOException {

        if( !this.running )
            return 0;

        // Read one UDP packet from the source
        byte[] tmp = new byte[SAMPLE_LENGTH];
        int read = this.dataSource.read(tmp, 0, SAMPLE_LENGTH);

        this.packet_length = read;

        //Log.d(TAG, "internalReadFromSource(): read "+(read/188)+" TS packet, used="+getContentLength());

        synchronized(LOCK) {
            if( write_pos >= read_pos ) {
                int len = Math.min(BUFFER_LENGTH-write_pos, read);
                System.arraycopy(tmp, 0, this.buffer, write_pos, len);
                write_pos += len;
                if( write_pos >= BUFFER_LENGTH )  // write position has reached the end of buffer, loop to the beginning
                    write_pos = 0;
                if( len < read ) {
                    Log.d(TAG, "internalReadFromSource(): ###reach end of buffer, len="+len+", rest="+(read-len));
                    // We have still data to write
                    int rest = Math.min(read-len, read_pos-write_pos-1);
                    if( rest < read-len ) {
                        skipOlderPacket();
                        rest = Math.min(read-len, read_pos-write_pos-1);
                    }
                    if( rest < read-len ) // TODO: in this case we would like to return a discontinuity to the caller
                        Log.e(TAG, "internalReadFromSource(): can't write all data... Buffer full?");
                    System.arraycopy(tmp, len, this.buffer, write_pos, rest);
                    write_pos += rest;
                }
            }
            else if( write_pos < read_pos-1 ) {
                int len = Math.min(read_pos-write_pos-1, read);
                if( len < read ) {
                    skipOlderPacket();
                    if( write_pos >= read_pos )
                        len = Math.min(BUFFER_LENGTH-write_pos, read);
                    else
                        len = Math.min(read_pos-write_pos-1, read);
                }
                if( len < read ) // TODO: in this case we would like to return a discontinuity to the caller
                    Log.e(TAG, "internalReadFromSource(): can't write all data... Buffer full?");
                System.arraycopy(tmp, 0, this.buffer, write_pos, len);
                write_pos += len;
            }
        }

        return read;
    }

    void skipOlderPacket()
    {
        // forward read pointer by packet_length
        Log.d(TAG, "skipOlderPacket(): free "+this.packet_length+" bytes (write="+write_pos+", read="+read_pos+")");
        int to_read;
        if( write_pos < read_pos ) {
            to_read = Math.min(BUFFER_LENGTH-read_pos, this.packet_length);
            read_pos += to_read;
            if( read_pos >= BUFFER_LENGTH ) // read position has reached the end of buffer, loop to the beginning
                read_pos = 0;
            if( (to_read < this.packet_length) && (write_pos > read_pos) ) {
                int rest = Math.min(write_pos-read_pos, this.packet_length-to_read);
                read_pos += rest;
                to_read += rest;
                if (to_read < this.packet_length)
                    Log.e(TAG, "skipOlderPacket(): ***ERROR*** we don't expect to reach this case (1)");
            }
        }
        else {
            to_read = Math.min(write_pos-read_pos, this.packet_length);
            read_pos += to_read;
            if( to_read < this.packet_length ) {
                Log.e(TAG, "skipOlderPacket(): ***ERROR*** we don't expect to reach this case (2)");
            }
        }
    }

}
