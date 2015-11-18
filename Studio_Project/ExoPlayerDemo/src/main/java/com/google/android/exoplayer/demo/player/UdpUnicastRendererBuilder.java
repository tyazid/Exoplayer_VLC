package com.google.android.exoplayer.demo.player;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.raw.RawBufferedSource;
import com.google.android.exoplayer.raw.RawSampleSource;
import com.google.android.exoplayer.raw.RtpSampleSource;
import com.google.android.exoplayer.raw.parser.RawExtractor;
import com.google.android.exoplayer.raw.parser.TsExtractor;
import com.google.android.exoplayer.upstream.BufferPool;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.UdpUnicastDataSource;

/**
 * Created by yychu on 11/03/2015.
 */
public class UdpUnicastRendererBuilder implements DemoPlayer.RendererBuilder {


    private static final String TAG = "UdpUnicastRB";
    private static final int BUFFER_POOL_LENGTH = 256*1024;

    private final Context context;
    private final Uri uri;
    private final TextView debugTextView;
    private final String userAgent;
    private final int playerType;

    public UdpUnicastRendererBuilder(Context context, String userAgent, String uri, TextView debugTextView, int playerType) {
        this.context = context;
        this.uri = Uri.parse(uri);
        this.debugTextView = debugTextView;
        this.userAgent = userAgent;
        this.playerType = playerType;
    }

    @Override
    public void buildRenderers(DemoPlayer player, DemoPlayer.RendererBuilderCallback callback) {

        // Build the video and audio renderers.
        Log.d(TAG, "buildRenderers(): uri=" + uri.toString());

        Handler mainHandler = player.getMainHandler();

        RawExtractor extractor = null;
        BufferPool bufferPool = new BufferPool(this.BUFFER_POOL_LENGTH);
        extractor = new TsExtractor(false, 0, bufferPool);

        DataSource videoDataSource = new UdpUnicastDataSource();
        DataSource rawSource = null;
        if (this.uri.getScheme().equals("rtp")) {
            rawSource = new RtpSampleSource(videoDataSource);
        }
        else {
            rawSource = new RawBufferedSource(videoDataSource);
        }
        SampleSource videoSampleSource = new RawSampleSource(rawSource, this.uri, this.context, extractor);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(videoSampleSource, null, true, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, null, mainHandler, player, 50);

        // Build the debug renderer.
        TrackRenderer debugRenderer = debugTextView != null
                ? new DebugTrackRenderer(debugTextView, videoRenderer)
                : null;

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
        renderers[DemoPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[DemoPlayer.TYPE_AUDIO] = null; // audioRenderer;
        renderers[DemoPlayer.TYPE_DEBUG] = debugRenderer;
        callback.onRenderers(null, null, renderers);
    }
}
