#Exoplayer_VLC
Google Android ExoPlayer with VLC tracks rendering. 
##Introduction
Exoplayer_VLC is an implementation of ExoPlayer Tracks renderer framwork based on android LibVLC. 

The principle is that the LibVLC is used to render audio/video/subtitle tracks within different ExoPlayer TrackRenderers implemetations.

##Why LibVLC:
(please see https://wiki.videolan.org/LibVLC/#libVLC_on_Android)

-VLC is free

-VLC is open source

-VLC support most audio/video codecs and various media protocols. 

-VLC uses android OMX HW interface.

-VLC is well integrated in android (build, runtime & dbg)

##How it works?:
Android ExoPlayer provides default TrackRenderer implementations for presenting audio and video contents for various format and protocols. It also support a customized track renderers through a provided  renderer builder (see https://developer.android.com/guide/topics/media/exoplayer.html). ExoPlayer_VLC is a supply of an implementation of the customizable part of the exo-player  media content rendering  based on LibVLC. this implementation will used the appliaction player surface and set it as VLC surface video redering, it will also translate available VLC tracks to exoplayer tracks and export them trought its com.google.android.exoplayer.SampleSource implementation and so allow their selection using its  com.google.android.exoplayer.source.SampleExtractor.selectTrack(int) metho implemetation.

Android default exoplayer implemetation:

![alt tag](https://developer.android.com/images/exoplayer/object-model.png)

VLC implementation of Exoplayer framwork:

![alt tag](https://github.com/tyazid/Exoplayer_VLC/blob/master/doc/diag1.png)

Programmatically, the appliaction who want to use exoplayer with VLC renderers implementation should provide the appropriate RendererBuilder implementation ( com.google.android.exoplayer.demo.player.vlc.VLCRendererBuilder is the default one) as folowing:
 

```
DemoPlayer player = new DemoPlayer(getRendererBuilder());
//...
player.prepare();
//...
player.setSurface(surfaceView.getHolder().getSurface());
player.setPlayWhenReady(true);
//...
//...
private RendererBuilder getRendererBuilder() {
 //...
 if(some_conditions)
   {	
        Properties p  = new Properties();
				p.put(VLCRendererBuilder.SURFACE_VIEW_RES_ID_PROP,  Integer.valueOf(R.id.surface_view) );
				//this 1st version need to have a handler on the surface holder, so we will provide the app ctx 
				//and the resource id of the surface view.
				return  new VLCRendererBuilder(this,   contentUri ,p);
   }
 //...
}
```
##Configuration & Build:
VLC renderer for exo-player is (currently) integrated as an additional package to ExoplayerDemo project (source or as an additional jar lib). So assuming that Exoplayer (Lib & Demo) project  (see https://github.com/google/ExoPlayer) and android LibVLC (see https://wiki.videolan.org/AndroidCompile/) are correctly configured, when adding the package com.google.android.exoplayer.demo.player.vlc (from source code or jar file) to ExoplyerDemo project, the application will be able to instantiate a DemoPlayer by providing a VLCRendererBuilder object. 

Exoplayer_VLC project (this one) includes ExoVLCLib which deal (and also depends on) with LibVLC and ExoPlayerLib, and com.exovlc.demo.VLCRendererBuilder located under ExoVLCLibDemo directory who serves as track renders builder  for ExoplayerDemo  project (see https://github.com/google/ExoPlayer). Indeed, ExoVLCLibDemo should be added to ExoplayerDemo project (additional package) and update (as seen above) the implementation of “com.google.android.exoplayer.demo.PlayerActivity.getRendererBuilder()” method which will  build the appropriate VLCRendererBuilder instance (see above). The ExoVLCLib should be also set as a reference library in ExoplayerDemo project setting. Finally, the LibVLC native libs location should be added to ExoplayerDemo native library location.

##For test:
bin/ directory contains an exported eclipse LibVLC project containing armv7 jni libs. This project will used as project dpendency(jar lib && jni lib) 
