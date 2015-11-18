package com.google.android.exoplayer.upstream;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.android.exoplayer.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

/**
 * Created by yychu on 27/02/2015.
 */
public class UdpMulticastDataSource implements DataSource {

    private MulticastSocket socket = null;
    private InetAddress groupAddr;
    private WifiManager.MulticastLock mLock;
    private static final String TAG = "UdpMulticastDataSource";
    private boolean opened;

    // Multicast packets are filtered out in the Wifi stack this is to disable it,
    // however this increases power consumption on the device
    public void turnOffWifiFilter(Context ctx) {
        WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            mLock = wm.createMulticastLock("multicastLock");
            mLock.acquire();
        }
    }

    public void restoreWifiFilter() {
        if (mLock != null) {
            mLock.release();
            mLock = null;
        }
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        socket = new MulticastSocket(dataSpec.uri.getPort());
        InetSocketAddress socketAddress = new InetSocketAddress(dataSpec.uri.getHost(), dataSpec.uri.getPort());
        groupAddr = socketAddress.getAddress();
        socket.joinGroup(groupAddr);
        opened = true;
        return 0;
    }

    @Override
    public void close() throws IOException {
        if( opened ) {
            socket.leaveGroup(groupAddr);
            socket.close();
            opened = false;
        }
        socket = null;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {

        if( !opened )
            return 0;

        DatagramPacket packet = new DatagramPacket(buffer, offset, readLength);
        socket.receive(packet);

        return packet.getLength();
    }


}
