package com.google.android.exoplayer.raw.parser;

import android.util.SparseArray;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.upstream.DataSource;
import java.io.IOException;

/**
 * Created by ataldir on 26/02/2015.
 */
public abstract class RawExtractor {

    public RawExtractor() {

    }

    /**
     * Gets the number of available tracks.
     * <p>
     * This method should only be called after the extractor has been prepared.
     *
     * @return The number of available tracks.
     */
    public abstract int getTrackCount();

    /**
     * Gets the format of the specified track.
     * <p>
     * This method must only be called after the extractor has been prepared.
     *
     * @param track The track index.
     * @return The corresponding format.
     */
    public abstract MediaFormat getFormat(int track);

    /**
     * Whether the extractor is prepared.
     *
     * @return True if the extractor is prepared. False otherwise.
     */
    public abstract boolean isPrepared();

    /**
     * Releases the extractor, recycling any pending or incomplete samples to the sample pool.
     * <p>
     * This method should not be called whilst {@link #read(DataSource)} is also being invoked.
     */
    public abstract void release();

    /**
     * Gets the largest timestamp of any sample parsed by the extractor.
     *
     * @return The largest timestamp, or {@link Long#MIN_VALUE} if no samples have been parsed.
     */
    public abstract long getLargestSampleTimestamp();

    /**
     * Gets the next sample for the specified track.
     *
     * @param track The track from which to read.
     * @param holder A {@link SampleHolder} into which the sample should be read.
     * @return True if a sample was read. False otherwise.
     */
    public abstract boolean getSample(int track, SampleHolder holder);

    /**
     * Discards samples for the specified track up to the specified time.
     *
     * @param track The track from which samples should be discarded.
     * @param timeUs The time up to which samples should be discarded, in microseconds.
     */
    public abstract void discardUntil(int track, long timeUs);

    /**
     * Whether samples are available for reading from {@link #getSample(int, SampleHolder)} for the
     * specified track.
     *
     * @return True if samples are available for reading from {@link #getSample(int, SampleHolder)}
     *     for the specified track. False otherwise.
     */
    public abstract boolean hasSamples(int track);

    /**
     * Reads up to a single TS packet.
     *
     * @param dataSource The {@link DataSource} from which to read.
     * @throws IOException If an error occurred reading from the source.
     * @return The number of bytes read from the source.
     */
    public abstract int read(DataSource dataSource) throws IOException;

    /**
     * Gets the {@link SampleQueue} for the specified track.
     *
     * @param track The track index.
     * @return The corresponding sample queue.
     */
    protected abstract SampleQueue getSampleQueue(int track);

    /**
     * Gets the list of detected stream type
     *
     * @return The SparseArray correspondint to pid/stream type for ts.
     */
    public abstract SparseArray<Integer> getStreamTypeList();

}
