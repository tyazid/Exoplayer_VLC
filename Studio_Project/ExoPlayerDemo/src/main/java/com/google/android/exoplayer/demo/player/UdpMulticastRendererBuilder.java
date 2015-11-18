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
import com.google.android.exoplayer.metadata.Id3Parser;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.raw.RawBufferedSource;
import com.google.android.exoplayer.raw.RawSampleSource;
import com.google.android.exoplayer.raw.UdpSampleSource;
import com.google.android.exoplayer.raw.parser.RawExtractor;
import com.google.android.exoplayer.raw.parser.TsExtractor;
import com.google.android.exoplayer.text.eia608.Eia608TrackRenderer;
import com.google.android.exoplayer.upstream.BufferPool;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.UdpMulticastDataSource;

import java.util.Map;


/**
 * Created by yychu on 27/02/2015.
 */
public class UdpMulticastRendererBuilder implements DemoPlayer.RendererBuilder {

    private static final String TAG = "UdpMulticastRndrBldr";
    private static final int BUFFER_POOL_LENGTH = 256*1024;

    private final Context context;
    private final Uri uri;
    private final TextView debugTextView;
    private final String userAgent;
    private final int playerType;

    public UdpMulticastRendererBuilder(Context context, String userAgent, String uri, TextView debugTextView, int playerType) {
        this.context = context;
        this.uri = Uri.parse(uri);
        this.debugTextView = debugTextView;
        this.userAgent = userAgent;
        this.playerType = playerType;
    }


    @Override
    public void buildRenderers(DemoPlayer player, DemoPlayer.RendererBuilderCallback callback) {
        Log.d(TAG, "******************buildRenderers(): uri=" + uri.toString());

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        Handler mainHandler = player.getMainHandler();

        RawExtractor extractor = null;
        BufferPool bufferPool = new BufferPool(this.BUFFER_POOL_LENGTH);
        extractor = new TsExtractor(false, 0, bufferPool);

        DataSource videoDataSource = new UdpMulticastDataSource();
        DataSource rawSource = new UdpSampleSource(videoDataSource);
        SampleSource sampleSource = new RawSampleSource(rawSource, this.uri, this.context, extractor);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource, null, true, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, null, mainHandler, player, 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);

        MetadataTrackRenderer<Map<String, Object>> id3Renderer =
                new MetadataTrackRenderer<Map<String, Object>>(sampleSource, new Id3Parser(),
                        player.getId3MetadataRenderer(), player.getMainHandler().getLooper());

        Eia608TrackRenderer closedCaptionRenderer = new Eia608TrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper());


        // Build the debug renderer.
        TrackRenderer debugRenderer = debugTextView != null
                ? new DebugTrackRenderer(debugTextView, videoRenderer)
                : null;

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
        renderers[DemoPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[DemoPlayer.TYPE_AUDIO] = audioRenderer;
        renderers[DemoPlayer.TYPE_TIMED_METADATA] = id3Renderer;
        renderers[DemoPlayer.TYPE_TEXT] = closedCaptionRenderer;

        renderers[DemoPlayer.TYPE_DEBUG] = debugRenderer;
        callback.onRenderers(null, null, renderers);
    }
}
