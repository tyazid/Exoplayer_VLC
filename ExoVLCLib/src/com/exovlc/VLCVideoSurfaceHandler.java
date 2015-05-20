package com.exovlc;

import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcUtil;

import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;// Fixe me plz

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
 

public class VLCVideoSurfaceHandler implements IVideoPlayer {
	public enum Start_Type {
		START_DELAYED, STARTED_IMMEDIATELY
	};

	public static interface SurfaceLayoutHandler {
		int SURFACE_BEST_FIT = 0;
		int SURFACE_FIT_HORIZONTAL = 1;
		int SURFACE_FIT_VERTICAL = 2;
		int SURFACE_FILL = 3;
		int SURFACE_16_9 = 4;
		int SURFACE_4_3 = 5;
		int SURFACE_ORIGINAL = 6;
	
		/**
		 * This method is called by native vout to request a new layout.
		 * 
		 * @param width
		 *            Frame width
		 * @param height
		 *            Frame height
	
		 */
		void setSurfaceLayout(int layout_width,int layout_wheight);
		
		SurfaceHolder getHolder();
	
		int getPreferedSurfaceHeight();
	
		int getPreferedSurfaceWidth();
	
		int getSurfaceFit();
		
		void setNewSurface(Surface surface);
		
		int configureSurface(Surface surface, final int width, final int height, final int hal);
	}

	private final LibVLC libvlc;
	private final Handler eventHandler;
	private final MediaCodecVideoTrackRenderer.EventListener eventListener; 

	private Surface mSurface;
	private boolean mSurfaceReady;
	private Runnable postStartJob;
	private SurfaceLayoutHandler layoutHandler;

	public VLCVideoSurfaceHandler( LibVLC libvlc, Handler eventHandler,
			MediaCodecVideoTrackRenderer.EventListener eventListener, SurfaceLayoutHandler layoutHandler) throws ExoPlaybackException {
		super();
		this.libvlc = libvlc;
		this.eventHandler = eventHandler;
		this.eventListener = eventListener;
 
		
		if (layoutHandler != null) {
			this.layoutHandler = layoutHandler;
 			SurfaceHolder surfaceHolder = layoutHandler.getHolder();
			ExoVlcUtil.log(this, "VLCIVideoSurfaceHandler.VLCIVideoSurfaceHandler()");
			ExoVlcUtil.log(this, "## ====> rect= " + surfaceHolder.getSurfaceFrame());
			if (com.exovlc.ExoVlcUtil.validSurface(surfaceHolder))
				setVlcSurface(surfaceHolder.getSurface(), false);

		} else
			throw new ExoPlaybackException("layout handler null.");

	}

	void setVlcSurface(Surface newSurface, boolean forced) {
		if (mSurface != newSurface || forced || !mSurfaceReady) {
			mSurface = newSurface;
			if(this.layoutHandler != null)
				this.layoutHandler.setNewSurface(newSurface);
			ExoVlcUtil.log(this, "setVlcSurface: " + mSurface);
			SurfaceHolder surfaceHolder = layoutHandler.getHolder();
			mSurfaceReady = (surfaceHolder.getSurface() == newSurface && ExoVlcUtil.validSurface(surfaceHolder));
			ExoVlcUtil.log(this, "VLCIVideoSurfaceHandler.setVlcSurface() set surface s =" + newSurface + " , f="
					+ forced + " mSurfaceReady =" + mSurfaceReady + " postStartJob=" + postStartJob);

			
			
			if (mSurfaceReady && postStartJob != null)
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						Thread.dumpStack();
						Runnable r = postStartJob;
						postStartJob = null;
						synchronized (VLCVideoSurfaceHandler.this) {
							if (r != null) {
								VLCVideoSurfaceHandler.this.attachVlcSurface(mSurface);
								r.run();
							}
						}
					}
				});
		}
	}

	Start_Type doStart(Runnable toDo) {
		ExoVlcUtil.log(this, "doStart(): mSurfaceReady " + mSurfaceReady);
		if (isSurfaceReady()) {
			attachVlcSurface(mSurface);
			return Start_Type.STARTED_IMMEDIATELY;
		}
		postStartJob = toDo;
		return Start_Type.START_DELAYED;
	}

	boolean isSurfaceReady() {
		return mSurfaceReady;
	}

	@Override
	public void setSurfaceLayout(final int width, final int height, final int visible_width, final int visible_height,
			final int sar_num, final int sar_den) {
		Thread.dumpStack();
		ExoVlcUtil.log(this, "> VideoActivity.setSurfaceLayout( " + width + ", " + height + ", " + visible_width + ","
				+ visible_height + ", " + sar_num + ",  " + sar_den + ")");
		


		int sw;
		int sh;
		SurfaceLayoutHandler lh =this.layoutHandler;
		if(lh ==null)
			return;
		
		// get screen size

 
		sw = lh.getPreferedSurfaceWidth();
		sh=lh.getPreferedSurfaceHeight();
		System.out.println("VLCIVideoSurfaceHandler.changeSurfaceLayout() window = " + sw + " x " + sh);
		System.out.println("VLCIVideoSurfaceHandler.changeSurfaceLayout() mVideoWidth=" + width
				+ " mVideoHeight=" + height);
		System.out.println("VLCIVideoSurfaceHandler.changeSurfaceLayout() mSarDen=" + sar_den + " mSarNum="
				+ sar_num);

		if (libvlc != null && !libvlc.useCompatSurface())
			libvlc.setWindowSize(sw, sh);

		double dw = sw, dh = sh;
		boolean isPortrait;
		isPortrait = false;

		if (sw > sh && isPortrait || sw < sh && !isPortrait) {
			dw = sh;
			dh = sw;
		}

		// sanity check
		if (dw * dh == 0 || width * height == 0) {
			ExoVlcUtil.log(this, "Invalid surface size");
			return;
		}
		// compute the aspect ratio
		double ar, vw;
		if (sar_den == sar_num) {
			/* No indication about the density, assuming 1:1 */
			vw = visible_width;
			ar = (double) visible_width / (double) visible_height;
		} else {
			/* Use the specified aspect ratio */
			vw = visible_width * (double) sar_num / sar_den;
			ar = vw / visible_height;
		}
		// compute the display aspect ratio
		double dar = dw / dh;
		switch (lh.getSurfaceFit()) {
		case SurfaceLayoutHandler.SURFACE_BEST_FIT:
			if (dar < ar)
				dh = dw / ar;
			else
				dw = dh * ar;
			break;
		case SurfaceLayoutHandler.SURFACE_FIT_HORIZONTAL:
			dh = dw / ar;
			break;
		case SurfaceLayoutHandler.SURFACE_FIT_VERTICAL:
			dw = dh * ar;
			break;
		case SurfaceLayoutHandler.SURFACE_FILL:
			break;
		case SurfaceLayoutHandler.SURFACE_16_9:
			ar = 16.0 / 9.0;
			if (dar < ar)
				dh = dw / ar;
			else
				dw = dh * ar;
			break;
		case SurfaceLayoutHandler.SURFACE_4_3:
			ar = 4.0 / 3.0;
			if (dar < ar)
				dh = dw / ar;
			else
				dw = dh * ar;
			break;
		case SurfaceLayoutHandler.SURFACE_ORIGINAL:
			dh = visible_height;
			dw = vw;
			break;
		}
		// set display size
		
		final int layout_width = (int) Math.ceil(dw * width / visible_width);
		final int layout_wheight = (int) Math.ceil(dh * height / visible_height);

		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				SurfaceLayoutHandler lh = VLCVideoSurfaceHandler.this.layoutHandler;
				if (lh != null)
					lh.setSurfaceLayout(layout_width,layout_wheight);
			}
		});
	}

	private static class ConfigureSurfaceHolder {
		private final Surface surface;
		private boolean configured;

		private ConfigureSurfaceHolder(Surface surface) {
			this.surface = surface;
		}
	}

	@Override
	public int configureSurface(Surface surface, final int width, final int height, final int hal) {

		if (LibVlcUtil.isICSOrLater() || surface == null)
			return -1;
		if (width * height == 0)
			return 0;
		ExoVlcUtil.log(this, "configureSurface: " + width + "x" + height);

		final ConfigureSurfaceHolder holder = new ConfigureSurfaceHolder(surface);

		final Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (mSurface == holder.surface) {
					SurfaceHolder surfaceHolder = layoutHandler.getHolder();
					if (hal != 0)
						surfaceHolder.setFormat(hal);
					surfaceHolder.setFixedSize(width, height);
				}

				synchronized (holder) {
					holder.configured = true;
					holder.notifyAll();
				}
			}
		});

		try {
			synchronized (holder) {
				while (!holder.configured)
					holder.wait();
			}
		} catch (InterruptedException e) {
			return 0;
		}
		return 1;
	}

	@Override
	public void eventHardwareAccelerationError() {
		// TODO Auto-generated method stub
		ExoVlcUtil.log(this, "Error with hardware acceleration");
		release();
		 
		notifyDecoderInitializationError(new DecoderInitializationException(null, new RuntimeException(
				"Error with hardware acceleration"), 0));
	}

	private void handlerPost(Runnable postAction) {

		if (eventHandler != null && eventListener != null) {
			eventHandler.post(postAction);
		}

	}

	private void notifyDecoderInitializationError(final DecoderInitializationException e) {
		handlerPost(new Runnable() {
			@Override
			public void run() {
				eventListener.onDecoderInitializationError(e);
			}
		});
	}

	void release() {
		this.libvlc.detachSurface();
	}

	private void attachVlcSurface(Surface surface) {
		if (mSurfaceReady)
			libvlc.detachSurface();
		if (surface != null) {
			ExoVlcUtil.log(this, " VLCIVideoSurfaceHandler.attachVlcSurface() Setting lib vlc with surface : "
					+ surface);
			libvlc.attachSurface(surface, this);
		}
	}
}
