package com.google.android.exoplayer.upstream;

import android.util.Log;

import com.google.android.exoplayer.util.Util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.DatagramSocket;

/**
 * Created by yychu on 10/03/2015.
 */
public class UdpUnicastDataSource implements DataSource {
    DatagramSocket socket;
    int rcvBufferSize;
    private static final String TAG = "UdpUnicastDataSource";

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        socket = new DatagramSocket(dataSpec.uri.getPort());
        // connect
        return 0;
    }

    @Override
    public void close() throws IOException {
        socket.close();
        socket = null;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength > rcvBufferSize) {
            rcvBufferSize = readLength+1;
            socket.setReceiveBufferSize(rcvBufferSize);
        }
        DatagramPacket packet = new DatagramPacket(buffer, offset, readLength);
        socket.receive(packet);
        Log.i(TAG, ">>>> read "+packet.getLength() + " MMMMMMMMMMM " + readLength);
        Util.printByteArray(TAG, packet.getData(), offset, 50);
        Util.findThisByte("sync interval ", (byte) 0x47, packet.getData());
        return packet.getLength();
    }
}
