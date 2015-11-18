package com.exovlc;

import org.videolan.libvlc.LibVLC;

import android.os.Handler;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;

public class VLCAudioTrackRenderer extends VLCTrackRenderer {

	public VLCAudioTrackRenderer(VLCSampleSource source, Handler eventHandler,
			MediaCodecAudioTrackRenderer.EventListener eventListener, LibVLC vlc) {
		super(source, eventHandler, eventListener, vlc);
	}

	@Override
	protected boolean isSupportedMime(String mimeType) {
		System.out.println("VLCAudioTrackRenderer.isSupportedMime(" + mimeType + ")");
		return com.exovlc.ExoVlcUtil.isVLCAudioMimeType(mimeType);

	}

	public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
		if (messageType == com.google.android.exoplayer.MediaCodecAudioTrackRenderer.MSG_SET_VOLUME) {
			/* the volume in percents (0 = mute, 100 = 0dB) */
			int v = com.exovlc.ExoVlcUtil.media2vlcVolume((Float) message);
			if (vlc.setVolume(v) == -1)
				ExoVlcUtil.log(this, "Err when setting VLC audio level :" + v);
			;
		} else {
			super.handleMessage(messageType, message);
		}
	}

	@Override
	protected void onStarted() throws ExoPlaybackException {
		if (!source.hasVideo()) // TODO : depending on selected track !!
			super.onStarted();
	}

}
