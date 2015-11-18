/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer.raw.parser;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.upstream.BufferPool;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.UdpMulticastDataSource;
import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.ParsableBitArray;
import com.google.android.exoplayer.util.ParsableByteArray;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;

/**
 * Facilitates the extraction of data from the MPEG-2 TS container format.
 */
public final class TsExtractor extends RawExtractor {

    private static final String TAG = "TsExtractor";

    private static final int TS_PACKET_SIZE = 188;
    private static final int TS_SYNC_BYTE = 0x47; // First byte of each TS packet.
    private static final int TS_PAT_PID = 0;

    // see http://www.etherguidesystems.com/help/sdos/mpeg/semantics/mpeg-2/stream_type.aspx
    private static final int TS_STREAM_TYPE_VIDEO_MPEG2 = 0x02;
    private static final int TS_STREAM_TYPE_AUDIO_MPEG1 = 0x03;
    private static final int TS_STREAM_TYPE_AUDIO_MPEG2 = 0x04;
    private static final int TS_STREAM_TYPE_MPEG2_PES = 0x06;
    private static final int TS_STREAM_TYPE_AAC = 0x0F;
    private static final int TS_STREAM_TYPE_H264 = 0x1B;
    private static final int TS_STREAM_TYPE_ID3 = 0x15;
    private static final int TS_STREAM_TYPE_AUDIO_ATSC_AC3 = 0x81;
    private static final int TS_STREAM_TYPE_EIA608 = 0x100; // 0xFF + 1
    private static final int TS_STREAM_TYPE_AC3 = 0x81;
    private static final int TS_STREAM_TYPE_MP2 = 0x04;
    private static final int TS_STREAM_TYPE_DVBTXT = 0x06;


    private static final long MAX_PTS = 0x1FFFFFFFFL;

    private final ParsableByteArray tsPacketBuffer;
    private final SparseArray<SampleQueue> sampleQueues; // Indexed by streamType
    private final SparseArray<TsPayloadReader> tsPayloadReaders; // Indexed by pid
    private final SparseArray<Integer> tsStreamTypes; // Indexed by pid
    private final BufferPool bufferPool;
    private final long firstSampleTimestamp;
    private final ParsableBitArray tsScratch;

    // Accessed only by the loading thread.
    private int tsPacketBytesRead;
    private long timestampOffsetUs;
    private long lastPts;

    // Accessed by both the loading and consuming threads.
    private volatile boolean prepared;

    public TsExtractor(boolean shouldSpliceIn, long firstSampleTimestamp, BufferPool bufferPool) {
        this.firstSampleTimestamp = firstSampleTimestamp;
        this.bufferPool = bufferPool;
        tsScratch = new ParsableBitArray(new byte[3]);
        tsPacketBuffer = new ParsableByteArray(TS_PACKET_SIZE);
        sampleQueues = new SparseArray<SampleQueue>();
        tsPayloadReaders = new SparseArray<TsPayloadReader>();
        tsPayloadReaders.put(TS_PAT_PID, new PatReader());
        lastPts = Long.MIN_VALUE;
        tsStreamTypes = new SparseArray<Integer>();
    }

    @Override
    public int getTrackCount() {
        Assertions.checkState(prepared);
        return sampleQueues.size();
    }

    @Override
    public MediaFormat getFormat(int track) {
        Assertions.checkState(prepared);
        return sampleQueues.valueAt(track).getMediaFormat();
    }

    @Override
    public boolean isPrepared() {
        return prepared;
    }

    @Override
    public void release() {
        for (int i = 0; i < sampleQueues.size(); i++) {
            sampleQueues.valueAt(i).release();
        }
    }

    @Override
    public long getLargestSampleTimestamp() {
        long largestParsedTimestampUs = Long.MIN_VALUE;
        for (int i = 0; i < sampleQueues.size(); i++) {
            largestParsedTimestampUs = Math.max(largestParsedTimestampUs,
                    sampleQueues.valueAt(i).getLargestParsedTimestampUs());
        }
        return largestParsedTimestampUs;
    }

    @Override
    public boolean getSample(int track, SampleHolder holder) {
        Assertions.checkState(prepared);
        return sampleQueues.valueAt(track).getSample(holder);
    }

    @Override
    public void discardUntil(int track, long timeUs) {
        Assertions.checkState(prepared);
        sampleQueues.valueAt(track).discardUntil(timeUs);
    }

    @Override
    public boolean hasSamples(int track) {
        Assertions.checkState(prepared);
        return !sampleQueues.valueAt(track).isEmpty();
    }

    private boolean checkPrepared() {
        int pesPayloadReaderCount = sampleQueues.size();
        //Log.d("TsExtractor", "checkPrepared(): --> <-- pesPayloadReaderCount=" + pesPayloadReaderCount);

        if (pesPayloadReaderCount == 0) {
            return false;
        }
        for (int i = 0; i < pesPayloadReaderCount; i++) {
            SampleQueue sq = sampleQueues.valueAt(i);
            //Log.d("TsExtractor", "checkPrepared(): --> <-- sampleQueues(" + i + ").hasMediaFormat=" + sq.hasMediaFormat() + ", m=" + (sq.getMediaFormat() == null ? "<null>" : sq.getMediaFormat().toString()));
        }
        for (int i = 0; i < pesPayloadReaderCount; i++) {
            if (!sampleQueues.valueAt(i).hasMediaFormat()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int read(DataSource dataSource) throws IOException {
        int bytesRead = dataSource.read(tsPacketBuffer.data, tsPacketBytesRead,
                TS_PACKET_SIZE - tsPacketBytesRead);
        if (bytesRead == -1) {
            Log.d("TsExtractor", "read(): read=" + bytesRead);
            return -1;
        }

        tsPacketBytesRead += bytesRead;
        if (tsPacketBytesRead < TS_PACKET_SIZE) {
            // We haven't read the whole packet yet.
            //Log.w("TsExtractor", "read(): tsPacketBytesRead("+tsPacketBytesRead+") < TS_PACKET_SIZE, "+Arrays.toString(Thread.currentThread().getStackTrace()));
            return bytesRead;
        }

        // Reset before reading the packet.
        tsPacketBytesRead = 0;
        tsPacketBuffer.setPosition(0);
        tsPacketBuffer.setLimit(TS_PACKET_SIZE);

        //Util.printByteArray("TsExtractor" + "read(): dump=",tsPacketBuffer.data, 0, 15);

        int syncByte = tsPacketBuffer.readUnsignedByte();
        if (syncByte != TS_SYNC_BYTE) {
            Log.w("TsExtractor", "read(): syncByte != TS_SYNC_BYTE");
            return bytesRead;
        }

        tsPacketBuffer.readBytes(tsScratch, 3);
        tsScratch.skipBits(1); // transport_error_indicator
        boolean payloadUnitStartIndicator = tsScratch.readBit();
        tsScratch.skipBits(1); // transport_priority
        int pid = tsScratch.readBits(13);
        tsScratch.skipBits(2); // transport_scrambling_control
        boolean adaptationFieldExists = tsScratch.readBit();
        boolean payloadExists = tsScratch.readBit();
        // Last 4 bits of scratch are skipped: continuity_counter

        // Skip the adaptation field.
        if (adaptationFieldExists) {
            int adaptationFieldLength = tsPacketBuffer.readUnsignedByte();
            tsPacketBuffer.skip(adaptationFieldLength);
        }

        //Util.printByteArray(TAG, tsPacketBuffer.data, tsPacketBuffer.getPosition(), 20);

        // Read the payload.
        if (payloadExists) {
            //Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> pid " + pid);
            TsPayloadReader payloadReader = tsPayloadReaders.get(pid);
            //Log.d("TsExtractor", "read(): --> <-- pid="+pid+", pusi="+payloadUnitStartIndicator+", payloadReader="+payloadReader);
            if (payloadReader != null) {
                payloadReader.consume(tsPacketBuffer, payloadUnitStartIndicator);
            }
        }

        if (!prepared) {
            prepared = checkPrepared();
        }

        return bytesRead;
    }

    public void dumpPmt() {
        Log.d(TAG, "dumpPmt(): --> <--");
        Log.d(TAG, "dumpPmt(): ...size="+tsStreamTypes.size());
        for( int i=0; i<tsStreamTypes.size(); i++)
        {
            Log.d(TAG, "dumpPmt(): ...[pid="+tsStreamTypes.keyAt(i)+", streamId="+tsStreamTypes.valueAt(i)+" ("+Util.getStreamTypeString(tsStreamTypes.valueAt(i))+")]");
        }
    }
    @Override
    public SparseArray<Integer> getStreamTypeList()
    {
        return tsStreamTypes.clone();
    }

    @Override
    protected SampleQueue getSampleQueue(int track) {
        return sampleQueues.valueAt(track);
    }

    /**
     * Adjusts a PTS value to the corresponding time in microseconds, accounting for PTS wraparound.
     *
     * @param pts The raw PTS value.
     * @return The corresponding time in microseconds.
     */
  /* package */ long ptsToTimeUs(long pts) {
        if (lastPts != Long.MIN_VALUE) {
            // The wrap count for the current PTS may be closestWrapCount or (closestWrapCount - 1),
            // and we need to snap to the one closest to lastPts.
            long closestWrapCount = (lastPts + (MAX_PTS / 2)) / MAX_PTS;
            long ptsWrapBelow = pts + (MAX_PTS * (closestWrapCount - 1));
            long ptsWrapAbove = pts + (MAX_PTS * closestWrapCount);
            pts = Math.abs(ptsWrapBelow - lastPts) < Math.abs(ptsWrapAbove - lastPts)
                    ? ptsWrapBelow : ptsWrapAbove;
        }
        // Calculate the corresponding timestamp.
        long timeUs = (pts * C.MICROS_PER_SECOND) / 90000;
        // If we haven't done the initial timestamp adjustment, do it now.
        if (lastPts == Long.MIN_VALUE) {
            timestampOffsetUs = firstSampleTimestamp - timeUs;
        }
        // Record the adjusted PTS to adjust for wraparound next time.
        lastPts = pts;
        return timeUs + timestampOffsetUs;
    }

    /**
     * Parses TS packet payload data.
     */
    private abstract static class TsPayloadReader {

        public abstract void consume(ParsableByteArray data, boolean payloadUnitStartIndicator);

    }

    /**
     * Parses Program Association Table data.
     */
    private class PatReader extends TsPayloadReader {

        private final ParsableBitArray patScratch;

        public PatReader() {
            patScratch = new ParsableBitArray(new byte[4]);
        }

        @Override
        public void consume(ParsableByteArray data, boolean payloadUnitStartIndicator) {
            // Skip pointer.
            //Log.w(TAG, "PatReader::consume(): ##### --> <--");
            if (payloadUnitStartIndicator) {
                int pointerField = data.readUnsignedByte();
                data.skip(pointerField);
            }

            data.readBytes(patScratch, 3);
            patScratch.skipBits(12); // table_id (8), section_syntax_indicator (1), '0' (1), reserved (2)
            int sectionLength = patScratch.readBits(12);
            // transport_stream_id (16), reserved (2), version_number (5), current_next_indicator (1),
            // section_number (8), last_section_number (8)
            data.skip(5);

            int programCount = (sectionLength - 9) / 4;
            for (int i = 0; i < programCount; i++) {
                data.readBytes(patScratch, 4);
                patScratch.skipBits(19); // program_number (16), reserved (3)
                int pid = patScratch.readBits(13);
                tsPayloadReaders.put(pid, new PmtReader());
            }

            // Skip CRC_32.
        }

    }

    /**
     * Parses Program Map Table.
     */
    private class PmtReader extends TsPayloadReader {

        private final ParsableBitArray pmtScratch;

        public PmtReader() {
            pmtScratch = new ParsableBitArray(new byte[5]);
        }

        @Override
        public void consume(ParsableByteArray data, boolean payloadUnitStartIndicator) {
            boolean log_enabled = false;
            if( log_enabled ) Log.w(TAG, "PmtReader::consume(): ##### --> <--");
            // Skip pointer.
            if (payloadUnitStartIndicator) {
                int pointerField = data.readUnsignedByte();
                data.skip(pointerField);
            }

            data.readBytes(pmtScratch, 3);
            pmtScratch.skipBits(12); // table_id (8), section_syntax_indicator (1), '0' (1), reserved (2)
            int sectionLength = pmtScratch.readBits(12);

            // program_number (16), reserved (2), version_number (5), current_next_indicator (1),
            // section_number (8), last_section_number (8), reserved (3), PCR_PID (13)
            // Skip the rest of the PMT header.
            data.skip(7);

            data.readBytes(pmtScratch, 2);
            pmtScratch.skipBits(4);
            int programInfoLength = pmtScratch.readBits(12);

            // Skip the descriptors.
            data.skip(programInfoLength);

            int entriesSize = sectionLength - 9 /* Size of the rest of the fields before descriptors */
                    - programInfoLength - 4 /* CRC size */;
            if( log_enabled ) Log.w(TAG, "PmtReader::consume(): ##### entriesSize="+entriesSize);
            while (entriesSize > 0) {
                data.readBytes(pmtScratch, 5);
                int streamType = pmtScratch.readBits(8);
                pmtScratch.skipBits(3); // reserved
                int elementaryPid = pmtScratch.readBits(13);
                if( log_enabled ) Log.w(TAG, "PmtReader::consume(): ##### ["+entriesSize+"] found es pid="+elementaryPid);
                pmtScratch.skipBits(4); // reserved
                int esInfoLength = pmtScratch.readBits(12);

                // Skip the descriptors.
                data.skip(esInfoLength);
                entriesSize -= esInfoLength + 5;

                if (sampleQueues.get(streamType) != null) {
                    if( log_enabled ) Log.w(TAG, "PmtReader::consume(): stream type already detected");
                    continue;
                }

                ElementaryStreamReader pesPayloadReader = null;
                switch (streamType) {
                    case TS_STREAM_TYPE_AAC:
                        if( log_enabled ) Log.w(TAG, "PmtReader::consume(): detect stream type TS_STREAM_TYPE_AAC");
                        pesPayloadReader = new AdtsReader(bufferPool);
                        break;
                    case TS_STREAM_TYPE_H264:
                        if( log_enabled ) Log.w(TAG, "PmtReader::consume(): detect stream type TS_STREAM_TYPE_H264");
                        SeiReader seiReader = new SeiReader(bufferPool);
                        sampleQueues.put(TS_STREAM_TYPE_EIA608, seiReader);
                        pesPayloadReader = new H264Reader(bufferPool, seiReader);
                        break;
                    case TS_STREAM_TYPE_ID3:
                        if( log_enabled ) Log.w(TAG, "PmtReader::consume(): detect stream type TS_STREAM_TYPE_ID3");
                        pesPayloadReader = new Id3Reader(bufferPool);
                        break;
                    case TS_STREAM_TYPE_AUDIO_MPEG1:
                        if( log_enabled ) Log.w(TAG, "PmtReader::consume(): detect stream type TS_STREAM_TYPE_AUDIO_MPEG1");
                        break;
                    case TS_STREAM_TYPE_VIDEO_MPEG2:    // NOT SUPPORTED
                        if( log_enabled ) Log.w(TAG, "PmtReader::consume(): detect stream type TS_STREAM_TYPE_VIDEO_MPEG2");
                        //pesPayloadReader = new Mpeg2vReader(bufferPool);
                        break;
                    case TS_STREAM_TYPE_AUDIO_MPEG2:
                        if( log_enabled ) Log.w(TAG, "PmtReader::consume(): detect stream type TS_STREAM_TYPE_AUDIO_MPEG2");
                        //pesPayloadReader = new MP2Reader(bufferPool);
                        break;
                    case TS_STREAM_TYPE_AUDIO_ATSC_AC3:
                        if( log_enabled ) Log.w(TAG, "PmtReader::consume(): detect stream type TS_STREAM_TYPE_AUDIO_ATSC_AC3");
                        //pesPayloadReader = new AC3ReaderATSC(bufferPool);
                        break;
                    case TS_STREAM_TYPE_MPEG2_PES:
                        if( log_enabled ) Log.w(TAG, "PmtReader::consume(): detect stream type TS_STREAM_TYPE_MPEG2_PES");
                        break;
                    default:
                        if( log_enabled ) Log.w(TAG, "PmtReader::consume(): unknown stream type ("+streamType+")");
                }
                tsStreamTypes.put(elementaryPid, streamType);

                if (pesPayloadReader != null) {
                    sampleQueues.put(streamType, pesPayloadReader);
                    tsPayloadReaders.put(elementaryPid, new PesReader(pesPayloadReader));
                }
            }

            // Skip CRC_32.
        }

    }

    /**
     * Parses PES packet data and extracts samples.
     */
    private class PesReader extends TsPayloadReader {

        private static final int STATE_FINDING_HEADER = 0;
        private static final int STATE_READING_HEADER = 1;
        private static final int STATE_READING_HEADER_EXTENSION = 2;
        private static final int STATE_READING_BODY = 3;

        private static final int HEADER_SIZE = 9;
        private static final int MAX_HEADER_EXTENSION_SIZE = 5;

        private final ParsableBitArray pesScratch;
        private final ElementaryStreamReader pesPayloadReader;

        private int state;
        private int bytesRead;
        private boolean bodyStarted;

        private boolean ptsFlag;
        private int extendedHeaderLength;

        private int payloadSize;

        private long timeUs;

        public PesReader(ElementaryStreamReader pesPayloadReader) {
            this.pesPayloadReader = pesPayloadReader;
            pesScratch = new ParsableBitArray(new byte[HEADER_SIZE]);
            state = STATE_FINDING_HEADER;
        }

        @Override
        public void consume(ParsableByteArray data, boolean payloadUnitStartIndicator) {
            //Log.d("TsExtractor", "PesReader:consume(): --> <-- payloadUnitStartIndicator="+payloadUnitStartIndicator);
            if (payloadUnitStartIndicator) {
                switch (state) {
                    case STATE_FINDING_HEADER:
                    case STATE_READING_HEADER:
                        // Expected.
                        break;
                    case STATE_READING_HEADER_EXTENSION:
                        Log.w(TAG, "Unexpected start indicator reading extended header");
                        break;
                    case STATE_READING_BODY:
                        // If payloadSize == -1 then the length of the previous packet was unspecified, and so
                        // we only know that it's finished now that we've seen the start of the next one. This
                        // is expected. If payloadSize != -1, then the length of the previous packet was known,
                        // but we didn't receive that amount of data. This is not expected.
                        if (payloadSize != -1) {
                            Log.w(TAG, "Unexpected start indicator: expected " + payloadSize + " more bytes");
                        }
                        // Either way, if the body was started, notify the reader that it has now finished.
                        if (bodyStarted) {
                            pesPayloadReader.packetFinished();
                        }
                        break;
                }
                setState(STATE_READING_HEADER);
            }

            while (data.bytesLeft() > 0) {
                switch (state) {
                    case STATE_FINDING_HEADER:
                        data.skip(data.bytesLeft());
                        break;
                    case STATE_READING_HEADER:
                        if (continueRead(data, pesScratch.getData(), HEADER_SIZE)) {
                            setState(parseHeader() ? STATE_READING_HEADER_EXTENSION : STATE_FINDING_HEADER);
                        }
                        break;
                    case STATE_READING_HEADER_EXTENSION:
                        int readLength = Math.min(MAX_HEADER_EXTENSION_SIZE, extendedHeaderLength);
                        // Read as much of the extended header as we're interested in, and skip the rest.
                        if (continueRead(data, pesScratch.getData(), readLength)
                                && continueRead(data, null, extendedHeaderLength)) {
                            parseHeaderExtension();
                            bodyStarted = false;
                            setState(STATE_READING_BODY);
                        }
                        break;
                    case STATE_READING_BODY:
                        readLength = data.bytesLeft();
                        int padding = payloadSize == -1 ? 0 : readLength - payloadSize;
                        if (padding > 0) {
                            readLength -= padding;
                            data.setLimit(data.getPosition() + readLength);
                        }
                        pesPayloadReader.consume(data, timeUs, !bodyStarted);
                        bodyStarted = true;
                        if (payloadSize != -1) {
                            payloadSize -= readLength;
                            if (payloadSize == 0) {
                                pesPayloadReader.packetFinished();
                                setState(STATE_READING_HEADER);
                            }
                        }
                        break;
                }
            }
        }

        private void setState(int state) {
            this.state = state;
            bytesRead = 0;
        }

        /**
         * Continues a read from the provided {@code source} into a given {@code target}. It's assumed
         * that the data should be written into {@code target} starting from an offset of zero.
         *
         * @param source       The source from which to read.
         * @param target       The target into which data is to be read, or {@code null} to skip.
         * @param targetLength The target length of the read.
         * @return Whether the target length has been reached.
         */
        private boolean continueRead(ParsableByteArray source, byte[] target, int targetLength) {
            int bytesToRead = Math.min(source.bytesLeft(), targetLength - bytesRead);
            if (bytesToRead <= 0) {
                return true;
            } else if (target == null) {
                source.skip(bytesToRead);
            } else {
                source.readBytes(target, bytesRead, bytesToRead);
            }
            bytesRead += bytesToRead;
            return bytesRead == targetLength;
        }

        private boolean parseHeader() {
            pesScratch.setPosition(0);
            int startCodePrefix = pesScratch.readBits(24);
            if (startCodePrefix != 0x000001) {
                //Util.printByteArray("PES HEADER >>>>>>>>>>>>>>>>",pesScratch.getData(), 0, 20 );
                Log.w(TAG, "Unexpected start code prefix: " + String.format("%08X", startCodePrefix));
                payloadSize = -1;
                return false;
            }

            pesScratch.skipBits(8); // stream_id.
            int packetLength = pesScratch.readBits(16);
            // First 8 bits are skipped: '10' (2), PES_scrambling_control (2), PES_priority (1),
            // data_alignment_indicator (1), copyright (1), original_or_copy (1)
            pesScratch.skipBits(8);
            ptsFlag = pesScratch.readBit();
            // DTS_flag (1), ESCR_flag (1), ES_rate_flag (1), DSM_trick_mode_flag (1),
            // additional_copy_info_flag (1), PES_CRC_flag (1), PES_extension_flag (1)
            pesScratch.skipBits(7);
            extendedHeaderLength = pesScratch.readBits(8);

            if (packetLength == 0) {
                payloadSize = -1;
            } else {
                payloadSize = packetLength + 6 /* packetLength does not include the first 6 bytes */
                        - HEADER_SIZE - extendedHeaderLength;
            }
            return true;
        }

        private void parseHeaderExtension() {
            pesScratch.setPosition(0);
            timeUs = 0;
            if (ptsFlag) {
                pesScratch.skipBits(4); // '0010'
                long pts = pesScratch.readBitsLong(3) << 30;
                pesScratch.skipBits(1); // marker_bit
                pts |= pesScratch.readBitsLong(15) << 15;
                pesScratch.skipBits(1); // marker_bit
                pts |= pesScratch.readBitsLong(15);
                pesScratch.skipBits(1); // marker_bit
                timeUs = ptsToTimeUs(pts);
            }
        }

    }

}
