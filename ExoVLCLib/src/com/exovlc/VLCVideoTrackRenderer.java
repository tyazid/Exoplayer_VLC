package com.exovlc;

import org.videolan.libvlc.LibVLC;

import android.os.Handler;
import android.view.Surface;

import com.exovlc.VLCVideoSurfaceHandler.Start_Type;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;

public class VLCVideoTrackRenderer extends VLCTrackRenderer {
	private final VLCVideoSurfaceHandler surfaceHdl;

	public VLCVideoTrackRenderer(VLCSampleSource source, Handler eventHandler,
			MediaCodecVideoTrackRenderer.EventListener eventListener, VLCVideoSurfaceHandler surfacePlayer, LibVLC vlc) {
		super(source, eventHandler, eventListener, vlc);
		this.surfaceHdl = surfacePlayer;
		new VLCNativeCrashHandler(eventHandler, eventListener, vlc, surfacePlayer);
	}

	@Override
	protected boolean isSupportedMime(String mimeType) {
		System.out.println("VLCVideoTrackRenderer.isSupportedMime() mimetype = " + mimeType);
		return com.exovlc.ExoVlcUtil.isVLCVideoMimeType(mimeType);
	}

	public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
		if (messageType == com.google.android.exoplayer.MediaCodecVideoTrackRenderer.MSG_SET_SURFACE) {
			ExoVlcUtil.log(this, ">>> VLCVideoTrackRenderer.handleMessage() >>> surface = " + (Surface) message);
			Thread.dumpStack();
			this.surfaceHdl.setVlcSurface((Surface) message, false);
		} else {
			super.handleMessage(messageType, message);
		}
	}

	@Override
	protected void onReleased() throws ExoPlaybackException {
		surfaceHdl.release();
		super.onReleased();
	}

	private void p_onStarted() throws ExoPlaybackException {
		super.onStarted();
	}

	@Override
	protected void onStarted() throws ExoPlaybackException {
		System.out.println(">>>VLCVideoTrackRenderer.onStarted()");
		VLCVideoSurfaceHandler.Start_Type t = this.surfaceHdl.doStart(new Runnable() {

			@Override
			public void run() {
				try {
					VLCVideoTrackRenderer.this.p_onStarted();
				} catch (ExoPlaybackException e) {
					e.printStackTrace();
					postError(new com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException(
							null, e, -1));
				}

			}
		});
		System.out.println(">>> VLCVideoTrackRenderer.onStarted() t = " + t);
		if (t == Start_Type.STARTED_IMMEDIATELY)
			super.onStarted();
	}
}
