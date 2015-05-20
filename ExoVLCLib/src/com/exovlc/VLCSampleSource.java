/**
 * 
 */
package com.exovlc;

import java.io.IOException;

import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackInfo;
import com.google.android.exoplayer.util.Assertions;

/**
 * Source of media sample handled by VLCLib.
 * 
 * @author tyazid
 *
 */
public class VLCSampleSource implements SampleSource {

	private boolean prepared;
	private VLCSampleExtractor extractor;

	public VLCSampleSource(VLCSampleExtractor extractor) {
		this.extractor = Assertions.checkNotNull(extractor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#prepare()
	 */
	@Override
	public synchronized boolean prepare() throws IOException {
		System.out.println(">> VLCSampleSource.prepare() is prepared = " + prepared);
		if (!prepared) {
			extractor.prepare();
			prepared = true;
		}

		return prepared;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#getTrackCount()
	 */
	@Override
	public int getTrackCount() {
		Assertions.checkState(prepared);
		return extractor.getTrackCount();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#getTrackInfo(int)
	 */
	@Override
	public TrackInfo getTrackInfo(int track) {
		Assertions.checkState(prepared);
		TrackInfo[] infos = extractor.getTrackInfos();
		if (track >= 0 && track < infos.length)
			return infos[track];
		ExoVlcUtil.log(this, " getTrackInfo : track=" + track + " is out of range [strat:0,lenght:" + infos.length
				+ "] => ret NULL");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#enable(int, long)
	 */
	@Override
	public void enable(int track, long positionUs) {
		Assertions.checkState(prepared);
		if (!extractor.isActiveTrack(track))
			extractor.selectTrack(track);
		extractor.seekTo(positionUs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#disable(int)
	 */
	@Override
	public void disable(int track) {
		// do nothing

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#continueBuffering(long)
	 */
	@Override
	public boolean continueBuffering(long positionUs) throws IOException {
		// do nothing
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#readData(int, long,
	 * com.google.android.exoplayer.MediaFormatHolder,
	 * com.google.android.exoplayer.SampleHolder, boolean)
	 */
	@Override
	public int readData(int track, long positionUs, MediaFormatHolder formatHolder, SampleHolder sampleHolder,
			boolean onlyReadDiscontinuity) throws IOException {
		Assertions.checkState(prepared);
		return extractor.readSample(track, sampleHolder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#seekToUs(long)
	 */
	@Override
	public void seekToUs(long positionUs) {
		Assertions.checkState(prepared);
		extractor.seekTo(positionUs);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#getBufferedPositionUs()
	 */
	@Override
	public long getBufferedPositionUs() {
		Assertions.checkState(prepared);
		return extractor.getBufferedPositionUs();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.exoplayer.SampleSource#release()
	 */
	@Override
	public synchronized void release() {
		Assertions.checkState(prepared);
		if (extractor != null)
			extractor.release();
		prepared = false;
		extractor = null;

	}

	public synchronized boolean isPrepared() {
		return prepared;
	}

	String getUri() {
		return extractor.getUri();
	}

	boolean hasVideo() {
		return extractor.hasVideo();
	}

}
