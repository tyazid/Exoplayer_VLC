package com.google.android.exoplayer.raw.parser;

import com.google.android.exoplayer.upstream.BufferPool;
import com.google.android.exoplayer.util.ParsableByteArray;

/**
 * Created by yychu on 18/03/2015.
 */
public class DVBTxtReader extends ElementaryStreamReader {
    public DVBTxtReader(BufferPool bufferPool) {
        super(bufferPool);
    }

    @Override
    public void consume(ParsableByteArray data, long pesTimeUs, boolean startOfPacket) {

    }

    @Override
    public void packetFinished() {

    }
}
