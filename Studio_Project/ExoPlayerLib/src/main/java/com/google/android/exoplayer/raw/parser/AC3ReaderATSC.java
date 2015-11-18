package com.google.android.exoplayer.raw.parser;

import android.util.Log;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.upstream.BufferPool;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableBitArray;
import com.google.android.exoplayer.util.ParsableByteArray;
import com.google.android.exoplayer.util.Util;

/**
 * Created by yychu on 18/03/2015.
 */
public class AC3ReaderATSC extends ElementaryStreamReader {

    ParsableBitArray bitparser;
    int audiodesclen = 0;

    public AC3ReaderATSC(BufferPool bufferPool) {
        super(bufferPool);
        bitparser = new ParsableBitArray(new byte[5]);
    }

    private int findByte(byte b, byte[] data, int offset) {
        for(int i=offset; i<data.length; i++) {
            if (b == data[i])
                return i;
        }
        return -1;
    }
    @Override
    public void consume(ParsableByteArray data, long pesTimeUs, boolean startOfPacket) {
        Util.printByteArray("AC3 reader !!!!!!!!!!", data.data, 0, 30);
        data.setPosition(1);
        if (!hasMediaFormat()) {
            parseAudioDescriptor(data);
        }
        else {
            data.skip(audiodesclen);
        }

        // move to access unit
        if (writingSample())
            appendData(data, data.bytesLeft());

        // ISO/IEC 13818-1 [1] specifies a fixed value for BSn (3584 bytes)

    }

    @Override
    public void packetFinished() {
        commitSample(true);
    }

    private void parseAudioDescriptor(ParsableByteArray data) {
        int streamidpos = findByte((byte) 0xBD, data.data, 1);
        if (streamidpos == -1) {
            return;
        }
        data.setPosition(streamidpos+1);
        data.readBytes(bitparser, 5);
        int desctag = bitparser.readBits(8);
        audiodesclen = bitparser.readBits(8);
        int sampleratecode = bitparser.readBits(3);
        bitparser.skipBits(5); // bsid
        int bit_rate_code = bitparser.readBits(6);
        int surroundmode = bitparser.readBits(2);
        bitparser.skipBits(3); // bsmod
        int numchannel = bitparser.readBits(4);
        int fullsvc = bitparser.readBits(1);

        int samplerate = (sampleratecode > 4)? 48000 : 44100;

        setMediaFormat(MediaFormat.createAudioFormat("audio/ac3", MediaFormat.NO_VALUE, samplerate, numchannel, null));

    }

}
