package com.google.android.exoplayer.raw;

import android.util.Log;

import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DataSpec;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

/**
 * Created by yychu on 26/02/2015.
 */
public class RawCircularBufferedSource implements DataSource {

    private final int BUFFER_LENGTH = 10*1024*1024; //128*1024;
    private final String TAG = "RawBufferedSource";
    private final int SAMPLE_LENGTH = 4*1024;

    private DataSource dataSource;
    private byte[] buffer;
    private int read_pos = 0;
    private int write_pos = 0;

    private int last_packet_nb = 0;
    private boolean running = false;
    private Thread t;
    private static final Semaphore lock = new Semaphore(1, true);

    public RawCircularBufferedSource(DataSource dataSource) {
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
        this.last_packet_nb = 0;
        this.buffer = new byte[BUFFER_LENGTH];
        this.running = true;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        internalReadFromSource();
                    }
                    catch(UnknownHostException e){}
                    catch(IOException e) {}
                    catch(InterruptedException e) {}
                }
            }
        };
        t = new Thread(runnable);
        t.start();
        return result;
    }

    @Override
    public void close() throws IOException {
        this.buffer = null;
        this.dataSource.close();
        try {
            lock.acquire();
            this.running = false;
            lock.release();
        }
        catch (InterruptedException e) {}
        try {
            t.join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        int ret = 0;

        //System.out.println(">>>>>>>>>>>>>  readpos:"+ (read_pos/188) + " writepos:"+(write_pos/188) + " len:"+getContentLength()+ " reqlen:"+readLength );
        //Util.printByteArray("..........in store ", this.buffer, read_pos, 20);
//        try {
//            lock.acquire();

            if (write_pos >= read_pos) {
                if (write_pos - read_pos >= readLength) {
                    System.arraycopy(this.buffer, read_pos, buffer, offset, readLength);
                    read_pos += readLength;
                    ret = readLength;
                } else {
                    //Log.d(TAG, "Not enough data to read from");
                    ret = 0;
                }
            } else {
                int len = Math.min(BUFFER_LENGTH - read_pos, readLength);
                System.arraycopy(this.buffer, read_pos, buffer, offset, len);
                read_pos += len;
                if (read_pos >= BUFFER_LENGTH-1)
                    read_pos = 0;
                if (len < readLength) {
                    int rest = readLength - len;
                    System.arraycopy(this.buffer, read_pos, buffer, len + offset, rest);
                    read_pos += rest;
                }
                ret = readLength;
            }
//            lock.release();
//            Thread.sleep(5);
//        }
//        catch (InterruptedException e) {
//        }
        return ret;
    }

    private int internalReadFromSource() throws IOException, InterruptedException {

        lock.acquire();
        if (!this.running) {
            lock.release();
            return 0;
        }
        //lock.release();

        byte[] inbuffer = new byte[SAMPLE_LENGTH];
        int inread = this.dataSource.read(inbuffer, 0, SAMPLE_LENGTH);

        //

        int len = Math.min(BUFFER_LENGTH - write_pos, inread);
        System.arraycopy(inbuffer, 0, this.buffer, write_pos, len);
        write_pos += len;
        if (write_pos >= BUFFER_LENGTH-1) {
            write_pos = 0;
            if (len < inread) {
                System.arraycopy(inbuffer, len, this.buffer, write_pos, inread-len);
                write_pos += inread-len;
            }
        }

        lock.release();

        return inread;
    }

}
