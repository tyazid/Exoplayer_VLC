package com.google.android.exoplayer.raw;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackInfo;
import com.google.android.exoplayer.raw.parser.RawExtractor;
import com.google.android.exoplayer.upstream.BufferPool;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;


/**
 * Created by ataldir on 26/02/2015.
 */
public class RawSampleSource implements SampleSource {

    private static final String TAG = "RawSampleSource";
    private static final int STATE_UNPREPARED = 0;
    private static final int STATE_PREPARED = 1;

    private final Uri uri;
    private final Context context;
    private final DataSource dataSource;
    private int trackCount;
    private TrackInfo[] trackInfos;
    private MediaFormat[] trackMediaFormats;
    private boolean[] trackEnabledStates;
    private boolean[] pendingDiscontinuities;
    private int state;
    private RawExtractor extractor;
    private long downstreamPositionUs;

    public RawSampleSource(DataSource dataSource, Uri uri, Context context, RawExtractor extractor) {
        Log.d(TAG, "RawSampleSource(): --> <--");

        this.dataSource = dataSource;
        this.context = context;
        this.uri = uri;
        this.state = STATE_UNPREPARED;
        this.extractor = extractor;
    }

    /**
     * Prepares the source.
     * <p>
     * Preparation may require reading from the data source (e.g. to determine the available tracks
     * and formats). If insufficient data is available then the call will return {@code false} rather
     * than block. The method can be called repeatedly until the return value indicates success.
     *
     * @return True if the source was prepared successfully, false otherwise.
     * @throws IOException If an error occurred preparing the source.
     */
    @Override
    public boolean prepare() throws IOException {
        //Log.d(TAG, "prepare(): --> ");

        if( state == STATE_PREPARED )
            return true;

        DataSpec dataSpec = new DataSpec(this.uri);
        this.dataSource.open(dataSpec);

        // Wait for extractor to be prepared (i.e. it must have detected tracks number and all tracks formats)
        while( !this.extractor.isPrepared() ) {
            //Log.d(TAG, "prepare(): read... ");
            this.extractor.read(this.dataSource);
        }

        this.trackCount = this.extractor.getTrackCount();
        this.trackInfos = new TrackInfo[this.trackCount];
        this.trackMediaFormats = new MediaFormat[this.trackCount];
        this.trackEnabledStates = new boolean[this.trackCount];
        pendingDiscontinuities = new boolean[trackCount];
        for( int i=0; i<this.trackCount; i++) {
            this.trackInfos[i] = new TrackInfo(this.extractor.getFormat(i).mimeType, C.UNKNOWN_TIME_US);
            this.trackMediaFormats[i] = this.extractor.getFormat(i);
            this.trackMediaFormats[i].setMaxVideoDimensions(1920, 1080);
            this.trackEnabledStates[i] = false;
        }

        this.state = STATE_PREPARED;

        // For debug only, dump extractor content
        SparseArray<Integer> list = extractor.getStreamTypeList();
        Log.d(TAG, "prepare(): dump stream type content. nb="+list.size());
        for( int i=0; i<list.size(); i++)
        {
            Log.d(TAG, "prepare(): ...[pid="+list.keyAt(i)+", streamId="+list.valueAt(i)+" ("+ Util.getStreamTypeString(list.valueAt(i))+")]");
        }

        Log.d(TAG, "prepare(): <-- this.state="+this.state);
        return true;
    }

    /**
     * Returns the number of tracks exposed by the source.
     *
     * @return The number of tracks.
     */
    @Override
    public int getTrackCount() {
        return this.trackCount;
    }

    /**
     * Returns information about the specified track.
     * <p>
     * This method should not be called until after the source has been successfully prepared.
     *
     * @return Information about the specified track.
     */
    @Override
    public TrackInfo getTrackInfo(int track) {
        return this.trackInfos[track];    }

    /**
     * Enable the specified track. This allows the track's format and samples to be read from
     * {@link #readData(int, long, MediaFormatHolder, SampleHolder, boolean)}.
     * <p>
     * This method should not be called until after the source has been successfully prepared.
     *
     * @param track The track to enable.
     * @param positionUs The player's current playback position.
     */
    @Override
    public void enable(int track, long positionUs) {
        Assertions.checkState(state == STATE_PREPARED);
        Assertions.checkState(!trackEnabledStates[track]);
        Log.d(TAG, "enable(track=" + track + ",pos=" + positionUs + "): --> <--");
        this.trackEnabledStates[track] = true;
        this.trackMediaFormats[track] = null;   // Needed to force the send FORMAT_READ on first "read()"
    }

    /**
     * Disable the specified track.
     * <p>
     * This method should not be called until after the source has been successfully prepared.
     *
     * @param track The track to disable.
     */
    @Override
    public void disable(int track) {
        Assertions.checkState(state == STATE_PREPARED);
        Assertions.checkState(trackEnabledStates[track]);
        Log.d(TAG, "disable(track="+track+"): --> <--");
        this.trackEnabledStates[track] = false;
        pendingDiscontinuities[track] = false;
    }

    /**
     * Indicates to the source that it should still be buffering data.
     *
     * @param positionUs The current playback position.
     * @return True if the source has available samples, or if the end of the stream has been reached.
     *     False if more data needs to be buffered for samples to become available.
     * @throws IOException If an error occurred reading from the source.
     */
    @Override
    public boolean continueBuffering(long positionUs) throws IOException {
        Assertions.checkState(state == STATE_PREPARED);

        this.downstreamPositionUs = positionUs;
        discardSamplesForDisabledTracks(this.extractor, downstreamPositionUs);

        // Read at least one packet
        int read = this.extractor.read(this.dataSource);
        // If we have no samples (i.e. not enough data), continue to buffer until we have one sample.
        int c = 0;
        while( !haveSamplesForEnabledTracks(this.extractor) && read>0 ) {
            //Log.d(TAG, "continueBuffering(pos="+positionUs+"): not enough stuff... c="+c+", read="+read); c++;
            read = this.extractor.read(this.dataSource);
        }

        boolean haveSamples = haveSamplesForEnabledTracks(this.extractor);

        //Log.d(TAG, "continueBuffering(pos="+positionUs+"): --> <-- ret="+haveSamples+", positionUs="+positionUs);
        return haveSamples;
    }

    /**
     * Attempts to read either a sample, a new format or or a discontinuity from the source.
     * <p>
     * This method should not be called until after the source has been successfully prepared.
     * <p>
     * Note that where multiple tracks are enabled, {@link #NOTHING_READ} may be returned if the
     * next piece of data to be read from the {@link SampleSource} corresponds to a different track
     * than the one for which data was requested.
     *
     * @param track The track from which to read.
     * @param positionUs The current playback position.
     * @param formatHolder A {@link MediaFormatHolder} object to populate in the case of a new format.
     * @param sampleHolder A {@link SampleHolder} object to populate in the case of a new sample. If
     *     the caller requires the sample data then it must ensure that {@link SampleHolder#data}
     *     references a valid output buffer.
     * @param onlyReadDiscontinuity Whether to only read a discontinuity. If true, only
     *     {@link #DISCONTINUITY_READ} or {@link #NOTHING_READ} can be returned.
     * @return The result, which can be {@link #SAMPLE_READ}, {@link #FORMAT_READ},
     *     {@link #DISCONTINUITY_READ}, {@link #NOTHING_READ} or {@link #END_OF_STREAM}.
     * @throws IOException If an error occurred reading from the source.
     */
    @Override
    public int readData(int track, long positionUs, MediaFormatHolder formatHolder, SampleHolder sampleHolder, boolean onlyReadDiscontinuity) throws IOException {
        Assertions.checkState(state == STATE_PREPARED);
        Assertions.checkState(trackEnabledStates[track]);
        //Log.d(TAG, "readData(track="+track+", pos="+positionUs+"): --> <-- hasSamples="+this.extractor.hasSamples(track));

        this.downstreamPositionUs = positionUs;

        if (pendingDiscontinuities[track]) {
            pendingDiscontinuities[track] = false;
            Log.d(TAG, "readData(track="+track+", pos="+positionUs+"): pendingDiscontinuities");
            return DISCONTINUITY_READ;
        }
        if (onlyReadDiscontinuity) {
            //Log.d(TAG, "readData(track="+track+", pos="+positionUs+"): onlyReadDiscontinuity");
            return NOTHING_READ;
        }

        MediaFormat mediaFormat = this.extractor.getFormat(track);
        if (mediaFormat != null && !mediaFormat.equals(trackMediaFormats[track], true)) {
            //Log.d(TAG, "readData(track="+track+", pos="+positionUs+"): read mediaformat="+mediaFormat.toString());
            formatHolder.format = mediaFormat;
            this.trackMediaFormats[track] = mediaFormat;
            this.trackMediaFormats[track].setMaxVideoDimensions(mediaFormat.getMaxVideoWidth(), mediaFormat.getMaxVideoHeight());
            return FORMAT_READ;
        }

        //Log.d(TAG, "readData(track="+track+", pos="+positionUs+"): b tr="+haveSamplesForEnabledTracks(this.extractor));
        if( this.extractor.hasSamples(track) ) {
            if (this.extractor.getSample(track, sampleHolder)) {
                //Log.d(TAG, "readData(track="+track+", pos="+positionUs+"): read sample, size="+sampleHolder.size+", ms="+sampleHolder.timeUs);
                sampleHolder.decodeOnly = false; //frameAccurateSeeking && sampleHolder.timeUs < lastSeekPositionUs;
                //positionUs = sampleHolder.timeUs;
                return SAMPLE_READ;
            }
        }
        //Log.d(TAG, "readData(track="+track+", pos="+positionUs+"): NOTHING TO READ ");
        return NOTHING_READ;
    }

    /**
     * Seeks to the specified time in microseconds.
     * <p>
     * This method should not be called until after the source has been successfully prepared.
     *
     * @param positionUs The seek position in microseconds.
     */
    @Override
    public void seekToUs(long positionUs) {
        Log.d(TAG, "seekToUs(pos="+positionUs+"): --> <--");
        if (downstreamPositionUs == positionUs) {
            return;
        }
        downstreamPositionUs = positionUs;
        for (int i = 0; i < pendingDiscontinuities.length; i++) {
            pendingDiscontinuities[i] = true;
        }
    }

    /**
     * Returns an estimate of the position up to which data is buffered.
     * <p>
     * This method should not be called until after the source has been successfully prepared.
     *
     * @return An estimate of the absolute position in microseconds up to which data is buffered,
     *     or {@link com.google.android.exoplayer.TrackRenderer#END_OF_TRACK_US} if data is buffered to the end of the stream, or
     *     {@link com.google.android.exoplayer.TrackRenderer#UNKNOWN_TIME_US} if no estimate is available.
     */
    @Override
    public long getBufferedPositionUs() {
        Assertions.checkState(state == STATE_PREPARED);
        long largestSampleTimestamp = this.extractor.getLargestSampleTimestamp();
        //Log.d(TAG, "getBufferedPositionUs(): --> <-- largestSampleTimestamp="+largestSampleTimestamp);
        return largestSampleTimestamp;
    }

    /**
     * Releases the {@link SampleSource}.
     */
    @Override
    public void release() {
        Log.d(TAG, "release(): --> <--");
        try {
            this.dataSource.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void discardSamplesForDisabledTracks(RawExtractor extractor, long timeUs) {
        if (!extractor.isPrepared()) {
            return;
        }
        for (int i = 0; i < trackEnabledStates.length; i++) {
            if (!trackEnabledStates[i]) {
                extractor.discardUntil(i, timeUs);
            }
        }
    }

    private boolean haveSamplesForEnabledTracks(RawExtractor extractor) {
        if (!extractor.isPrepared()) {
            return false;
        }
        for (int i = 0; i < trackEnabledStates.length; i++) {
            if (trackEnabledStates[i] && extractor.hasSamples(i)) {
                return true;
            }
        }
        return false;
    }

}

