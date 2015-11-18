package com.google.android.exoplayer.demo.player;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.demo.DemoUtil;
import com.google.android.exoplayer.raw.RawSampleSource;
import com.google.android.exoplayer.raw.RawBufferedSource;
import com.google.android.exoplayer.raw.parser.RawExtractor;
import com.google.android.exoplayer.raw.parser.TsExtractor;
import com.google.android.exoplayer.upstream.BufferPool;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.RawHttpDataSource;

/**
 * Created by ataldir on 26/02/2015.
 */
public class RawHttpRendererBuilder implements DemoPlayer.RendererBuilder {

    private static final String TAG = "RawHttpRendererBuilder";
    private static final int BUFFER_POOL_LENGTH = 256*1024;

    private final Context context;
    private final Uri uri;
    private final TextView debugTextView;
    private final String userAgent;
    private final int playerType;

    public RawHttpRendererBuilder(Context context, String userAgent, String uri, TextView debugTextView, int playerType) {
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
        if( playerType == DemoUtil.TYPE_RAW_HTTP_TS) {
            BufferPool bufferPool = new BufferPool(this.BUFFER_POOL_LENGTH);
            extractor = new TsExtractor(false, 0, bufferPool);
        }

        DataSource videoDataSource = new RawHttpDataSource(userAgent, RawHttpDataSource.REJECT_PAYWALL_TYPES);
        DataSource rawSource = new RawBufferedSource(videoDataSource);
        SampleSource sampleSource = new RawSampleSource(rawSource, this.uri, this.context, extractor);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource, null, true, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, null, mainHandler, player, 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);

        // Build the debug renderer.
        TrackRenderer debugRenderer = debugTextView != null
                ? new DebugTrackRenderer(debugTextView, videoRenderer)
                : null;

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
        renderers[DemoPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[DemoPlayer.TYPE_AUDIO] = audioRenderer; // audioRenderer;
        renderers[DemoPlayer.TYPE_DEBUG] = debugRenderer;
        callback.onRenderers(null, null, renderers);
    }
}
