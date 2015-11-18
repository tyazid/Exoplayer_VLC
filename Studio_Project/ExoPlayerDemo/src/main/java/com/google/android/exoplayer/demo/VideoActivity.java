package com.google.android.exoplayer.demo;

import java.lang.ref.WeakReference;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;

import com.google.android.exoplayer.ExoPlaybackException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;

public class VideoActivity extends Activity implements SurfaceHolder.Callback,
        IVideoPlayer {
    public final static String TAG = "LibVLCAndroidSample/VideoActivity";

    public final static String LOCATION = "com.compdigitec.libvlcandroidsample.VideoActivity.location";

    private String mFilePath;

    // display surface
    private SurfaceView mSurfaceView;
    private Surface mSurface;
    private SurfaceHolder mSurfaceHolder;

    // media player
    private LibVLC libvlc;
    private int mVideoWidth;
    private int mVideoHeight;
    private final static int VideoSizeChanged = -1;
    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;
 //   private FrameLayout mSurfaceFrame;
    /*************
     * Activity
     *************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);
        // Receive path to play from intent
        Intent intent = getIntent();
        if(intent==null|| intent.getExtras()==null || (mFilePath = intent.getExtras().getString(LOCATION))!=null)
         mFilePath =LibVLC.PathToURI("/data/local/tmp/air_stunt_1.mp4");
         System.out.println("###########################################################");
         System.out.println("###########################################################");
         System.out.println("###########################################################");
         System.out.println("###########################################################");
         System.out.println("###########################################################");
         System.out.println(">>>> VideoActivity.onCreate() File PATH = "+mFilePath);
         System.out.println("###########################################################");
         System.out.println("###########################################################");
         System.out.println("###########################################################");
         System.out.println("###########################################################");
         
         
        //   mFilePath = LibVLC.PathToURI("/data/local/tmp/air_stunt_1.mp4");// intent.getExtras().getString(LOCATION);
        //  mFilePath = LibVLC.PathToURI("/data/local/tmp/5.0_de.ac3");
         
         
        Log.d(TAG, "Playing back " + mFilePath);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceHolder = mSurfaceView.getHolder();
		setVlcSurface(mSurfaceHolder);

       
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onResume() {
        super.onResume();
      //  createPlayer(mFilePath);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    /*************
     * Surface
     *************/

    public void surfaceCreated(SurfaceHolder holder) {
    	System.out.println(">>>>>>> VideoActivity.surfaceCreated()");
    	 System.out.println("## ====> rec = "+holder.getSurfaceFrame());
    }
    boolean mSurfaceReady ;

	private int mVideoVisibleHeight;

	private int mVideoVisibleWidth;

	private int mSarNum;

	private int mSarDen;
	private void setVlcSurface(SurfaceHolder surfaceholder){
		surfaceholder.addCallback(this);
    	   final Surface newSurface = surfaceholder.getSurface();
           if (mSurface != newSurface) {
               mSurface = newSurface;
               Log.d(TAG, "surfaceChanged: " + mSurface);
               getLibVLC().attachSurface(mSurface, this);
               mSurfaceReady = true;
           }
            System.out.println(">>>> VideoActivity.onCreate() start playing \""+mFilePath+"\"in 3 sec ...holder = "+surfaceholder);
            new Handler().postDelayed(new Runnable() {
            			@Override
            			public void run() {
            				System.out
            					.println(" ## RUN TASK now playing...");
            				createPlayer(mFilePath);
            			}
            		}, 3000L);
	}
    public void surfaceChanged(SurfaceHolder surfaceholder, int format,
            int width, int height) { 
    //	System.out.println(">>>>>>> VideoActivity.surfaceChanged() s = "+surfaceholder.getSurface());

    	//setVlcSurface(surfaceholder);
    }

    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
    	System.out.println(">>>>>> VideoActivity.surfaceDestroyed()");
    	   if(libvlc != null) {
    		   libvlc = null;
    		   libvlc.detachSurface();
               mSurfaceReady = false;
           }
    }

    private void setSize(int width, int height) {
    	System.out.println(">>>>> VideoActivity.setSize( "+width +","+  height+")");
    	Thread.dumpStack();
    	
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if(mSurfaceHolder == null || mSurfaceView == null)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurfaceView.setLayoutParams(lp);
        mSurfaceView.invalidate();
    }

    
    @Override
    public void setSurfaceLayout(int width, int height, int visible_width,
            int visible_height, int sar_num, int sar_den) {
    	   mVideoHeight = height;
           mVideoWidth = width;
           mVideoVisibleHeight = visible_height;
           mVideoVisibleWidth  = visible_width;
           mSarNum = sar_num;
           mSarDen = sar_den;
        Message msg = Message.obtain(mHandler, VideoSizeChanged, width, height);
        msg.sendToTarget();
        System.out.println(">>>> VideoActivity.setSurfaceLayout( "+ width+", " + height+", " + visible_width+","+
              visible_height+", " + sar_num+",  "+ sar_den+")");
        
    }

    /*************
     * Player
     *************/
    LibVLC getLibVLC(){
    	if(true)
			try {
				return com.exovlc.ExoVlcUtil.getVLC(this);
			} catch (ExoPlaybackException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				finish();
			}
   throw new IllegalStateException();
    }
    
    private synchronized void createPlayer(String media  ) {
        releasePlayer();
     //   mSurfaceView.setKeepScreenOn(true);
        LibVLC libvlc = getLibVLC(); 
    	 System.out.println("## Attache vlc lib to surface rec = "+mSurfaceHolder.getSurfaceFrame());
    	 
        // libvlc.attachSurface(mSurfaceHolder.getSurface(), this);
        
        try {
            if (media.length() > 0) {
                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,     0);
                toast.show();
            }
 
            String[] options = libvlc.getMediaOptions(!true, !libvlc.hasVideoTrack(media));
            System.out.println(">>VideoActivity.createPlayer() OPTIONS:");
            for (int i = 0; i < options.length; i++) 
				System.out.println(" o-"+i+":"+options[i]);
			
            libvlc.playMRL(media,options);
            System.out.println(" CREATED OPTION WITH : noHardwareAcceleration = !true  & noVideo ="+ ! libvlc.hasVideoTrack(media));
            System.out.println(">>>>>>>VideoActivity.createPlayer() AUDIOS = "+libvlc.getAudioTracksCount()+ "\n\tAudio track ="+libvlc.getAudioTrack()+ "\n\t VIDEOS = "+ libvlc.getVideoTracksCount() +
            		"\n\t AUDIO DESCs = " + libvlc. getAudioTrackDescription()+"\n\t HAS VIDEO  DESCs = " + libvlc.hasVideoTrack(media)
            		+"\n\t HAS HardwareAcceleration = " + libvlc.getHardwareAcceleration());
            
        } catch (Exception e) {
        	e.printStackTrace();
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        Thread.dumpStack();
        EventHandler.getInstance(). removeHandler(mHandler);
        libvlc.stop();
 
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }
    private void changeSurfaceLayout() {
        int sw;
        int sh;
        // get screen size
            sw = getWindow().getDecorView().getWidth();
            sh = getWindow().getDecorView().getHeight();
      
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
        if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }
        // compute the aspect ratio
        double ar, vw;
        if (mSarDen == mSarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth;
            ar = (double)mVideoVisibleWidth / (double)mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * (double)mSarNum / mSarDen;
            ar = vw / mVideoVisibleHeight;
        }
        // compute the display aspect ratio
        double dar = dw / dh;
        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_FIT_HORIZONTAL:
                dh = dw / ar;
                break;
            case SURFACE_FIT_VERTICAL:
                dw = dh * ar;
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_ORIGINAL:
                dh = mVideoVisibleHeight;
                dw = vw;
                break;
        }

        SurfaceView surface; 
        FrameLayout surfaceFrame;
            surface = mSurfaceView;
          //   surfaceFrame = mSurfaceFrame;
        // set display size
        LayoutParams lp = surface.getLayoutParams();
        lp.width  = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
        surface.setLayoutParams(lp); 

        // set frame size (crop if necessary)
     //   lp = surfaceFrame.getLayoutParams();
      //  lp.width = (int) Math.floor(dw);
     //   lp.height = (int) Math.floor(dh);
     //   surfaceFrame.setLayoutParams(lp);

        surface.invalidate(); 
    }
    /*************
     * Events
     *************/

    private Handler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private WeakReference<VideoActivity> mOwner;

        public MyHandler(VideoActivity owner) {
            mOwner = new WeakReference<VideoActivity>(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoActivity player = mOwner.get();

            // SamplePlayer events
             
            if (msg.what == VideoSizeChanged) {
               // player.setSize(msg.arg1, msg.arg2);
				player.changeSurfaceLayout();
                return;
            }
            

            // Libvlc events
            Bundle b = msg.getData();
            switch (b.getInt("event")) {
            case EventHandler.MediaPlayerEndReached:
            	System.out.println(">>> VideoActivity.MyHandler.handleMessage():::MediaPlayerEndReached");
                Log.d(TAG, "MediaPlayerEndReached");
                player.releasePlayer();
                break;
            case EventHandler.MediaPlayerPlaying:
            case EventHandler.MediaPlayerPaused:
            case EventHandler.MediaPlayerStopped:
            default:
                break;
            }
        }
    }

    @Override
    public void eventHardwareAccelerationError() {
        // Handle errors with hardware acceleration
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    
    
    private static class ConfigureSurfaceHolder {
        private final Surface surface;
        private boolean configured;

        private ConfigureSurfaceHolder(Surface surface) {
            this.surface = surface;
        }
    }
    
    
    @Override
    public int configureSurface(Surface surface,final int width,final int height, final int hal) {

    	System.out.println("VideoActivity.configureSurface() :"+width +" x "+height+" ; hal="+hal+"; >=ICS:"+LibVlcUtil.isICSOrLater());
        if (LibVlcUtil.isICSOrLater() || surface == null)
            return -1;
        if (width * height == 0)
            return 0;
        Log.d(TAG, "configureSurface: " + width +"x"+height);

        final ConfigureSurfaceHolder holder = new ConfigureSurfaceHolder(surface);

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mSurface == holder.surface && mSurfaceHolder != null) {
                    if (hal != 0)
                        mSurfaceHolder.setFormat(hal);
                    mSurfaceHolder.setFixedSize(width, height);
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
    
    	
    	
    	
    	/********************************************************************************/
    	
    	
    	
      /*  Log.d(TAG, "configureSurface: width = " + width + ", height = " + height);
        if (LibVlcUtil.isICSOrLater() || surface == null)
            return -1;
        if (width * height == 0)
            return 0;
        if(hal != 0)
            holder.setFormat(hal);
        holder.setFixedSize(width, height);
        return 1;*/
    }


}
