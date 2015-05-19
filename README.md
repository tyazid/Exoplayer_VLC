# Exoplayer_VLC
Google Android ExoPlayer with VLC tracks rendering. 
#Introduction
Exoplayer_VLC is an implementation of ExoPlayer Tracks renderer framwork based on android LibVLC. 

The principle is that the LibVLC is used to render audio/video/subtitle tracks within different ExoPlayer TrackRenderers implemetations.

#Why LibVLC:
(please see https://wiki.videolan.org/LibVLC/#libVLC_on_Android)

-VLC is free

-VLC is open source

-VLC support most audio/video codecs and various media protocols. 

-VLC uses android OMX HW interface.

-VLC is well integrated in android (build, runtime & dbg)

#How it works?:
Android Exoplayer 
ExoPlayer provides default TrackRenderer implementations for presenting audio and video contents for various format and protocols. It also support a customized track renderers through a provided  renderer builder (see https://developer.android.com/guide/topics/media/exoplayer.html). VLC implementation is a supply of and implemetation the customizable part of the exo-player based on LibVLC. this implementation will used the appliaction player surface and set it as VLC surface video redering, it will also translate available VLC tracks to exoplayer tracks and export them trought its com.google.android.exoplayer.SampleSource implementation and so allow their selection using its  com.google.android.exoplayer.source.SampleExtractor.selectTrack(int) metho implemetation.

Android default exoplayer implemetation:
![alt tag](https://developer.android.com/images/exoplayer/object-model.png)
