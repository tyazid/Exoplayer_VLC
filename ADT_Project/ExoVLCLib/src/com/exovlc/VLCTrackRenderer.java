package com.exovlc;

import java.io.IOException;

import org.videolan.libvlc.LibVLC;

import android.os.Handler;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.TrackRenderer;

public abstract class VLCTrackRenderer extends TrackRenderer {

	protected final LibVLC vlc;
	protected final VLCSampleSource source;
	private boolean isEnded;
	private int trackIndex;
	/* event handling in media rendrers way */
	private long currentPositionUs = 0L;
	protected final Handler eventHandler;
	private final MediaCodecTrackRenderer.EventListener eventListener;

	VLCTrackRenderer(VLCSampleSource source, Handler eventHandler, MediaCodecTrackRenderer.EventListener eventListener,
			LibVLC vlc) {
		if (vlc == null)
			throw new NullPointerException("null vlc parm if prohibited");
		this.vlc = vlc;

		this.source = source;
		this.eventHandler = eventHandler;
		this.eventListener = eventListener;
	}

	protected void postError(DecoderInitializationException ex) {
		this.eventListener.onDecoderInitializationError(ex);
	}

	@Override
	protected int doPrepare() throws ExoPlaybackException {

		try {
			boolean sourcePrepared = source.prepare();
			if (!sourcePrepared) {
				return TrackRenderer.STATE_UNPREPARED;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new ExoPlaybackException(e);
		}
		int tc;
		System.out.println(">>>> VLCTrackRenderer.doPrepare(" + getClass().getSimpleName()
				+ ")>> source.getTrackCount():" + source.getTrackCount());
		if ((tc = source.getTrackCount()) > 0)
			while (--tc >= 0) {
				System.out.println(">>>> VLCTrackRenderer.doPrepare(" + getClass().getSimpleName()
						+ ")>> source.getTrackInfo(" + tc + ").mimeType):" + source.getTrackInfo(tc).mimeType);
				if (source.getTrackInfo(tc).mimeType != null)
					try {
						if (isSupportedMime(source.getTrackInfo(tc).mimeType)) {

							trackIndex = tc;
							System.out.println(">>>> VLCTrackRenderer.doPrepare(" + getClass().getSimpleName()
									+ ")>> Bingo! keep track with mime:" + source.getTrackInfo(tc).mimeType);
							return TrackRenderer.STATE_PREPARED;
						}
					} catch (Exception e) {
						// continue the loop.

					}

			}

		return TrackRenderer.STATE_IGNORE;
	}

	protected abstract boolean isSupportedMime(String mimeType);

	@Override
	protected boolean isEnded() {

		return isEnded;
	}

	@Override
	protected boolean isReady() {
		return vlc.isPlaying() || source.isPrepared();
	}

	@Override
	protected void onReleased() throws ExoPlaybackException {
		System.out.println(">>>> VLCTrackRenderer.onReleased()>>>>");
		super.onReleased();
		if (!vlc.isPlaying())
			vlc.stop();
		source.release();
	}

	@Override
	protected void onStarted() throws ExoPlaybackException {
		super.onStarted();
		System.out.println(">>>>>>>>   VLCTrackRenderer.onStarted(" + getClass().getSimpleName() + ") ");
		Thread.dumpStack();
		String uri = source.getUri();

		if (!vlc.isPlaying())
			try {
				System.out.println(">>>>>>>>   VLCTrackRenderer.onStarted( ) uri =  " + uri + " vdo ="
						+ vlc.hasVideoTrack(uri));
				String[] options = vlc.getMediaOptions(!true, !vlc.hasVideoTrack(uri));
				System.out.println(">>VLCTrackRenderer.onStarted() OPTIONS:");
				for (int i = 0; i < options.length; i++)
					System.out.println(" o-" + i + ":" + options[i]);
				vlc.playMRL(uri, options);
				System.out.println(">>> VLCTrackRenderer.onStarted() playMRL done");
			} catch (IOException e) {
				e.printStackTrace();
				throw new ExoPlaybackException(e);
			}
	}

	@Override
	protected void onStopped() throws ExoPlaybackException {
		super.onStopped();
		System.out.println(">>>> VLCTrackRenderer.onStopped() >>>> ");
		if (!vlc.isPlaying())
			vlc.pause();

	}

	@Override
	protected void doSomeWork(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
		if (vlc != null) {
			// if(!vlc.isPlaying() && !isEnded) // was playing
			// isEnded = !vlc.isPlaying();
		}
		// System.out.println(">> VLCTrackRenderer.doSomeWork() is ended ="+isEnded
		// +" is play ="+vlc.isPlaying());
		// if (vlc != null && vlc.isPlaying())
		// vlc.getPosition()

	}

	@Override
	protected long getDurationUs() {
		return source.getTrackInfo(trackIndex).durationUs;
	}

	@Override
	protected long getCurrentPositionUs() {
		if (vlc != null && vlc.isPlaying()) {
			float per = vlc.getPosition();
			currentPositionUs = (long) ((double) getDurationUs() * per);
		}
		return currentPositionUs;
	}

	@Override
	protected long getBufferedPositionUs() {

		long sourceBufferedPosition = source.getBufferedPositionUs();
		return sourceBufferedPosition == UNKNOWN_TIME_US || sourceBufferedPosition == END_OF_TRACK_US ? sourceBufferedPosition
				: Math.max(sourceBufferedPosition, getCurrentPositionUs());

	}

	@Override
	protected void seekTo(long positionUs) throws ExoPlaybackException {
		System.out.println("VLCTrackRenderer.seekTo(positionUs=" + positionUs + ")");
		Thread.dumpStack();
		source.seekToUs(positionUs);
		float per = vlc.getPosition();
		System.out.println("VLCTrackRenderer.seekTo() new per_pos==" + positionUs + " dur =" + getDurationUs());
		currentPositionUs = (long) ((double) getDurationUs() * per);
		System.out.println("VLCTrackRenderer.seekTo(currentPositionUs=" + currentPositionUs + ")");
		// sourceState = SOURCE_STATE_NOT_READY;
		isEnded = false;
	}

	@Override
	protected void onEnabled(long positionUs, boolean joining) throws ExoPlaybackException {
		System.out.println("VLCTrackRenderer.onEnabled()");
		super.onEnabled(positionUs, joining);
		source.enable(trackIndex, positionUs);
		// sourceState = SOURCE_STATE_NOT_READY;
		isEnded = false;
		float per = vlc.getPosition();
		currentPositionUs = (long) ((double) getDurationUs() * per);

	}

}
