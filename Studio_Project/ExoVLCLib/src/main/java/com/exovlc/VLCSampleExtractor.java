package com.exovlc;

import java.io.IOException;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media.Track;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackInfo;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.source.SampleExtractor;
import com.google.android.exoplayer.util.Assertions;

public class VLCSampleExtractor implements SampleExtractor {

	private final String uri;
	private TrackInfo[] trackInfos;

	private org.videolan.libvlc.Media.Track[] vlctracks;
	private LibVLC lib;
	private boolean prepared;
	private boolean hasVdo;
	private final org.videolan.libvlc.Media media;

	public VLCSampleExtractor(LibVLC vlc, String uri) throws ExoPlaybackException {
		if (vlc == null || uri == null)
			throw new ExoPlaybackException("null parms!");
		this.uri = uri;
		this.lib = vlc;
		this.media = ExoVlcUtil.getMedia(vlc, uri);
		this.vlctracks = ExoVlcUtil.getAvailableTracks(media);
		if (this.vlctracks.length > 0)
			this.trackInfos = ExoVlcUtil.vlc2exoTracks(media.getDuration(), vlctracks, lib);
		else
			try {
				if ((hasVdo = vlc.hasVideoTrack(uri)))
					this.trackInfos = ExoVlcUtil.getDummyVdoTrack(media,
							com.exovlc.ExoVlcUtil.DUMM_VDO_MIME);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		ExoVlcUtil.log(this, "get vlc tracks for uri = " + uri);

	}

	String getUri() {
		return uri;
	}

	public LibVLC getLibVLC() {
		return lib;
	}

	@Override
	public boolean prepare() throws IOException {
		System.out.println(">>>> VLCSampleExtractor.prepare() prepared = " + prepared);
	
		if (!prepared) {
			if (trackInfos == null || trackInfos.length == 0)
				throw new IOException(" No track is available (vlc read track)");
			return (prepared = true);
		}

		return false;
	}

	@Override
	public TrackInfo[] getTrackInfos() {
		Assertions.checkState(prepared);
		TrackInfo[] r = new TrackInfo[trackInfos.length];
		System.arraycopy(trackInfos, 0, r, 0, trackInfos.length);
		return r;
	}

	/* package */int getTrackCount() {
		return trackInfos.length;
	}

	@Override
	public void selectTrack(int track) {
		Assertions.checkState(prepared);
		if (vlctracks.length != trackInfos.length)
			return;
		if ((track < 0) && (track >= vlctracks.length)) {
			ExoVlcUtil.log(this, "selectTrack() out of range : " + track + "; track len=" + vlctracks.length);
			return;
		}
		Track vt = vlctracks[track];
		ExoVlcUtil.log(this, "selectTrack() track  : " + track);
		switch (vt.type) {
		case Track.Type.Video:
			lib.setVideoTrack(track);
			break;
		case Track.Type.Audio:
			if (lib.getAudioTrack() != track)
				lib.setAudioTrack(track);
			break;
		case Track.Type.Text:
			if (lib.getSpuTrack() != track)
				lib.setSpuTrack(track);
		}

	}

	@Override
	public void deselectTrack(int index) {
		Assertions.checkState(prepared);
		// TODO Auto-generated method stub

	}

	@Override
	public long getBufferedPositionUs() {
		Assertions.checkState(prepared);
		long l = lib.getLength();
		if (l <= 0)
			return TrackRenderer.UNKNOWN_TIME_US;
		return l * ExoVlcUtil.MS_2_MICRO;

	}

	@Override
	public void seekTo(long positionUs) {
		Assertions.checkState(prepared);
		if (lib.isSeekable())
			lib.setPosition(ExoVlcUtil.pos2percentage(positionUs, lib));
	}

	@Override
	public void getTrackMediaFormat(int track, MediaFormatHolder mediaFormatHolder) {
		Assertions.checkState(prepared);
		if (track < 0 || track >= vlctracks.length) {
			ExoVlcUtil.log(this, "getTrackMediaFormat() out of range : " + track + "; track len=" + vlctracks.length);
			return;
		}
		mediaFormatHolder.format = MediaFormat.createFromFrameworkMediaFormatV16(ExoVlcUtil
				.track2mediaFormat(vlctracks[track]));
		mediaFormatHolder.drmInitData = null;
	}

	@Override
	public int readSample(int track, SampleHolder sampleHolder) throws IOException {
		Assertions.checkState(prepared);
		long ct = lib.getTime();
		sampleHolder.timeUs = ct * ExoVlcUtil.MS_2_MICRO;
		sampleHolder.flags = android.media.MediaExtractor.SAMPLE_FLAG_SYNC;
		return ct >= 0L ? SampleSource.SAMPLE_READ : SampleSource.END_OF_STREAM;
	}

	@Override
	public synchronized void release() {
		if (lib != null) {
			ExoVlcUtil.releaseVLC(lib);
			lib = null;
			prepared = false;
		}
	}

	boolean isActiveTrack(int track) {
		Assertions.checkState(prepared);
		System.out.println(">>> VLCSampleExtractor.isActiveTrack() track = " + track);
		System.out.println(">>> VLCSampleExtractor.isActiveTrack() vlctracks.length = " + vlctracks.length);
		System.out.println(">>> VLCSampleExtractor.isActiveTrack() trackInfos.length = " + trackInfos.length);
		if (vlctracks.length != trackInfos.length) {
			if ((track < 0) && (track >= trackInfos.length)) {
				ExoVlcUtil.log(this, "getTrackMediaFormat() out of range : " + track + "; track len="
						+ trackInfos.length);
				return false;
			}
			/*
			 * if(track == trackInfos.length - 1 ) return true;
			 */
			TrackInfo t = trackInfos[track];
			System.out.println(">>> VLCSampleExtractor.isActiveTrack() track mime = " + t.mimeType);
			System.out.println(">>> VLCSampleExtractor.isActiveTrack() isVLCAudioMimeType = "
					+ ExoVlcUtil.isVLCAudioMimeType(t.mimeType));

			System.out.println(">>> VLCSampleExtractor.isActiveTrack() isVLCVideoMimeType= "
					+ ExoVlcUtil.isVLCVideoMimeType(t.mimeType));

			if ((ExoVlcUtil.isVLCAudioMimeType(t.mimeType) && track == lib.getAudioTrack())
					|| (ExoVlcUtil.isVLCVideoMimeType(t.mimeType)))
				return true;

			return false;
		}

		if ((track < 0) && (track >= vlctracks.length)) {
			ExoVlcUtil.log(this, "getTrackMediaFormat() out of range : " + track + "; track len=" + vlctracks.length);
			return false;
		}
		Track vt = vlctracks[track];
		if ((vt.type == Track.Type.Video) || (vt.type == Track.Type.Audio && track == lib.getAudioTrack())
				|| (vt.type == Track.Type.Text && track == lib.getSpuTrack()))
			return true;
		return false;
	}

	boolean hasVideo() {
		return hasVdo;
	}
}
