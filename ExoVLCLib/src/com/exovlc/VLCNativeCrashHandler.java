package com.exovlc;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVLC.OnNativeCrashListener;

import android.os.Handler;

import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer.EventListener;

public class VLCNativeCrashHandler implements OnNativeCrashListener {

	private final Handler eventHandler;
	private final MediaCodecVideoTrackRenderer.EventListener eventListener;
	private com.exovlc.VLCVideoSurfaceHandler surfacePlayer;

	VLCNativeCrashHandler(Handler eventHandler, EventListener eventListener, LibVLC vlc,
			VLCVideoSurfaceHandler surfacePlayer) {
		super();
		this.eventHandler = eventHandler;
		this.eventListener = eventListener;
		this.surfacePlayer = surfacePlayer;
		LibVLC.setOnNativeCrashListener(this);
	}

	@Override
	public void onNativeCrash() {
		if (eventHandler != null && eventListener != null) {
			eventHandler.post(new Runnable() {

				@Override
				public void run() {

					eventListener.onDecoderInitializationError(new DecoderInitializationException(null, new Exception(
							"VLC Lib native crash occures."), 0));
					if (VLCNativeCrashHandler.this.surfacePlayer != null) {
						VLCNativeCrashHandler.this.surfacePlayer.release();
						VLCNativeCrashHandler.this.surfacePlayer = null;
					}
				}
			});
		}
	}

}
