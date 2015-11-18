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
package com.exovlc.demo;

import java.util.Properties;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcUtil;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;

import com.exovlc.ExoVlcUtil;
import com.exovlc.VLCAudioTrackRenderer;
import com.exovlc.VLCSampleExtractor;
import com.exovlc.VLCSampleSource;
import com.exovlc.VLCVideoSurfaceHandler;
import com.exovlc.VLCVideoTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.demo.player.DemoPlayer;
import com.google.android.exoplayer.demo.player.DemoPlayer.RendererBuilder;
import com.google.android.exoplayer.demo.player.DemoPlayer.RendererBuilderCallback;

/**
 * A {@link RendererBuilder} for VLC implementation
 */
public class VLCRendererBuilder implements RendererBuilder {
	public static final String SURFACE_VIEW_RES_ID_PROP = "surface.resource.id";

	private final Context context;
	private final String uri;
	private final LibVLC vlc;
	private final Properties props;

	public VLCRendererBuilder(Context context, Uri uri) throws ExoPlaybackException {

		this(context, uri, null);

	}

	public VLCRendererBuilder(Context context, Uri uri, Properties props) throws ExoPlaybackException {
		this.context = context;
		this.uri = LibVLC.PathToURI(uri.toString());
		this.vlc = ExoVlcUtil.getVLC(context);
		this.props = props;

	}

	@Override
	public void buildRenderers(DemoPlayer player, RendererBuilderCallback callback) {
		TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
		try {
			VLCSampleExtractor extractor = new VLCSampleExtractor(vlc, uri);
			VLCSampleSource vlcsource = new VLCSampleSource(extractor);

			MySurfaceLayoutHandler msh = null;
			System.out.println("VLCRendererBuilder.buildRenderers() props = "+props);
				Object v = props.get(SURFACE_VIEW_RES_ID_PROP);
		  
				if (v instanceof Integer)
					msh = new MySurfaceLayoutHandler(this.context, ((Integer) v).intValue());
			 
			if (msh == null)
				callback.onRenderersError(new ExoPlaybackException("property " + SURFACE_VIEW_RES_ID_PROP + " not set"));
			
			
			VLCVideoSurfaceHandler ivdoplayer = new VLCVideoSurfaceHandler( extractor.getLibVLC(),
					player.getMainHandler(), player, msh);

			renderers[DemoPlayer.TYPE_VIDEO] = new VLCVideoTrackRenderer(vlcsource, player.getMainHandler(), player,
					ivdoplayer, extractor.getLibVLC());
			renderers[DemoPlayer.TYPE_AUDIO] = new VLCAudioTrackRenderer(vlcsource, player.getMainHandler(), player,
					extractor.getLibVLC());
		} catch (ExoPlaybackException e) {
			e.printStackTrace();
		}
		callback.onRenderers(null, null, renderers);
	}

	static class MySurfaceLayoutHandler implements
			com.exovlc.VLCVideoSurfaceHandler.SurfaceLayoutHandler {
		private final Context activityCtx;
		private SurfaceView view;
		private int mCurrentSize = SURFACE_BEST_FIT;
		private int pw, ph;
		private Surface mSurface;

		MySurfaceLayoutHandler(Context context, int sv_id) throws ExoPlaybackException {
			super();
			this.activityCtx = context;
			try {
				view = (SurfaceView) ((Activity) activityCtx).findViewById(sv_id);// R.id.surface_view);
			} catch (Exception e) {

				e.printStackTrace();
				throw new ExoPlaybackException(e);
			}

			pw = ((Activity) activityCtx).getWindow().getDecorView().getWidth();
			ph = ((Activity) activityCtx).getWindow().getDecorView().getHeight();
		}

		@Override
		public int getPreferedSurfaceHeight() {
			return ph;
		}

		@Override
		public int getPreferedSurfaceWidth() {
			return pw;
		}

		@Override
		public int getSurfaceFit() {
			return mCurrentSize;
		}

		@Override
		public void setNewSurface(Surface surface) {
			mSurface = surface;

		}

		@Override
		public void setSurfaceLayout(int width, int height) {

			LayoutParams lp = view.getLayoutParams();
			lp.width = width;
			lp.height = height;
			view.setLayoutParams(lp);
			view.invalidate();

		}

		private static class ConfigureSurfaceHolder {
			private final Surface surface;
			private boolean configured;

			private ConfigureSurfaceHolder(Surface surface) {
				this.surface = surface;
			}
		}

		@Override
		public SurfaceHolder getHolder() {
			return view.getHolder();
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
						if (hal != 0)
							view.getHolder().setFormat(hal);
						view.getHolder().setFixedSize(width, height);
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

	}
}
