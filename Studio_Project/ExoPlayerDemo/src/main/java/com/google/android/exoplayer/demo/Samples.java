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
package com.google.android.exoplayer.demo;

import java.util.Locale;

/**
 * Holds statically defined sample definitions.
 */
/* package */ class Samples {

  public static class Sample {

    public final String name;
    public final String contentId;
    public final String uri;
    public final int type;

    public Sample(String name, String uri, int type) {
      this(name, name.toLowerCase(Locale.US).replaceAll("\\s", ""), uri, type);
    }

    public Sample(String name, String contentId, String uri, int type) {
      this.name = name;
      this.contentId = contentId;
      this.uri = uri;
      this.type = type;
    }
  }
     public static final Sample[] CISCO = new Sample[]{
            new Sample("NY (HTTP, SD, h264/ts)", "http://10.60.61.88/ny.ts", DemoUtil.TYPE_RAW_HTTP_TS),
            new Sample("UDP (udp://239.10.10.16:10000) h264/ts", "udp://239.10.10.16:10000", DemoUtil.TYPE_UDP),
            new Sample("UDP (udp://239.10.10.24:10000) h264/ts", "udp://239.10.10.24:10000", DemoUtil.TYPE_UDP),
            new Sample("UDP/RTP (rtp://239.10.10.117:10000) h264/ts", "rtp://239.10.10.117:10000", DemoUtil.TYPE_RTP),
            new Sample("UDP/RTP (rtp://239.10.10.118:10000) h264/mp4", "rtp://239.10.10.118:10000", DemoUtil.TYPE_RTP),
    };

    public static final Sample[] DIVERS = new Sample[]{
          //  new Sample("NY (File, 720p, h264/mp4)", "file:///storage/usb3host/newyork_720p.mp4", DemoUtil.TYPE_OTHER),
       //     new Sample("Heima (HTTP, 1080p, h264/mp4)", "http://10.60.61.88/HEIMA1080p1500.mp4", DemoUtil.TYPE_RAW_HTTP_TS),
            //file:///data/local/tmp/air_stunt_1.mp4
        //    new Sample("Air_stunt_1 (FILE, 1080p, h264/mp4)", "/data/local/tmp/air_stunt_1.mp4", DemoUtil.TYPE_RAW_HTTP_MP4),
       //     new Sample("Hello kitty (HTTP/ Youtube URI)", "https://www.youtube.com/watch?v=JSdiZj63ec0", DemoUtil.TYPE_RAW_HTTP_MP4),
           // new Sample("Trailer (FILE, 1080p, h264/mp4)", "/data/local/tmp/trailer.avi", DemoUtil.TYPE_RAW_HTTP_MP4),
          //  new Sample("Hallo (FILE, 1080p, h264/mp4)", "/data/local/tmp/halo.mp4", DemoUtil.TYPE_RAW_HTTP_MP4),
          //  new Sample("TVHeadEnd/SAT RAW TS (SD/ Raw mpeg 2 TS)", "http://10.60.63.178:9981/stream/channelid/4", DemoUtil.TYPE_RAW_HTTP_MP4),
           new Sample("Big Buck Bunny-MP4 (HTTP/mp4, 640x360)", "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4",    DemoUtil.TYPE_RAW_HTTP_MP4) ,
           new Sample("Big Buck Bunny-OGG (HTTP/ogg, 640x360)", "http://clips.vorwaerts-gmbh.de/big_buck_bunny.ogv",    DemoUtil.TYPE_RAW_HTTP_MP4),
           //rtsp://184.72.239.149/vod/mp4:BigBuckBunny_175k.mov
           new Sample("HLS m3u8 P.L.(HTTP/mp4, 640x352)", "http://content.jwplatform.com/manifests/vM7nH0Kl.m3u8",DemoUtil.TYPE_RAW_HTTP_MP4),
           new Sample("Big Buck Bunny-RTSP (RTSP/MOV, 240x180)", "rtsp://184.72.239.149/vod/mp4:BigBuckBunny_175k.mov",    DemoUtil.TYPE_RAW_HTTP_MP4),
           new Sample("Jellyfish-10-Mbps.mkv (HTTP, H264, 1280x720)", "http://jell.yfish.us/media/Jellyfish-10-Mbps.mkv",    DemoUtil.TYPE_RAW_HTTP_MP4),
            new Sample("Jellyfish-5-Mbps.mkv (HTTP, H264, 1920x1080)", "http://jell.yfish.us/media/Jellyfish-5-Mbps.mkv",    DemoUtil.TYPE_RAW_HTTP_MP4),
             new Sample("Jellyfish-3-Mbps.mkv (HTTP, H264, 1920x1080)", "http://jell.yfish.us/media/Jellyfish-3-Mbps.mkv",    DemoUtil.TYPE_RAW_HTTP_MP4),
             new Sample("**Dreambox-2 (HTTP, H264/Ts)", "http://10.60.51.131/files/dreambox-2.ts",    DemoUtil.TYPE_RAW_HTTP_MP4),
             new Sample("-->Big Bunny (HTTP, mp4 + ac3)", "http://download.blender.org/peach/bigbuckbunny_movies/big_buck_bunny_480p_surround-fix.avi",    DemoUtil.TYPE_RAW_HTTP_MP4),
             new Sample("**AC3 only", "http://techslides.com/demos/samples/sample.ac3",    DemoUtil.TYPE_RAW_HTTP_MP4),
             //http://techslides.com/demos/samples/sample.ac3
             //
             //http://10.60.51.131/files/dreambox-2.ts
         //  new Sample("Crazy (YT) ",        "http://static-cdn1.ustream.tv/swf/live/viewerqos:24.swf",    DemoUtil.TYPE_RAW_HTTP_MP4),
           
         //  new Sample("SWF1 (http) ",        "http://static-cdn1.ustream.tv/swf/live/viewerqos:24.swf",    DemoUtil.TYPE_RAW_HTTP_MP4),
         //  new Sample("DASH VLC (http) ",        "http://www-itec.uni-klu.ac.at/ftp/datasets/mmsys12/BigBuckBunny/MPDs/BigBuckBunnyNonSeg_1s_isoffmain_DIS_23009_1_v_2_1c2_2011_08_30.mpd",    DemoUtil.TYPE_RAW_HTTP_MP4),
//http://r3---sn-j5ou8-jjns.googlevideo.com/videoplayback?ratebypass=yes&sver=3&initcwndbps=106250&ipbits=0&mm=31&fexp=905024%2C916657%2C927606%2C9407122%2C9407720%2C9407992%2C9408079%2C9408142%2C9408588%2C9408706%2C9408710%2C9412774%2C9413150%2C945137%2C946008%2C948124%2C952612%2C952637%2C952642&id=o-ABYgq8tupaDkajYfuACTxi6gkEhKDC2XJQ6tTuVZGOM2&signature=908A0E90D7E8CF6A98531FCF052BAC9B09EDD848.5BFDE61D7F31860BEA7EF65F0C5DCCAC4DD74A1B&key=yt5&source=youtube&mv=m&dur=49.110&itag=18&ms=au&ip=190.72.179.176&upn=SRxCThKHkt0&expire=1431670600&sparams=dur%2Cid%2Cinitcwndbps%2Cip%2Cipbits%2Citag%2Cmime%2Cmm%2Cms%2Cmv%2Cpl%2Cratebypass%2Csource%2Cupn%2Cexpire&mt=1431648904&mime=video%2Fmp4&pl=19&title=How+to+get+link+MP4%2C+FLV+in+Youtube
            //http://www-itec.uni-klu.ac.at/ftp/datasets/mmsys12/BigBuckBunny/MPDs/BigBuckBunnyNonSeg_1s_isoffmain_DIS_23009_1_v_2_1c2_2011_08_30.mpd
            // http://dagobah.net//flashswf/unlockinggoosehoward.swf
          //  new Sample("UDP/RTP (rtp://239.10.10.116:10000)", "rtp://239.10.10.116:10000", DemoUtil.TYPE_RTP),
          //  new Sample("UDP (rtp unicast 8888)", "rtp://127.0.0.1:8888", DemoUtil.TYPE_UDP_UNICAST),
          //  new Sample("UDP (udp unicast 8888)", "udp://127.0.0.1:8888", DemoUtil.TYPE_UDP_UNICAST),
    };

    public static final Sample[] YOUTUBE_DASH_MP4 = new Sample[] {
    new Sample("Google Glass",
        "http://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?"
        + "as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,as&ip=0.0.0.0&"
        + "ipbits=0&expire=19000000000&signature=255F6B3C07C753C88708C07EA31B7A1A10703C8D."
        + "2D6A28B21F921D0B245CDCF36F7EB54A2B5ABFC2&key=ik0", DemoUtil.TYPE_RAW_HTTP_MP4),
    new Sample("Google Play",
        "http://www.youtube.com/api/manifest/dash/id/3aa39fa2cc27967f/source/youtube?"
        + "as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,as&ip=0.0.0.0&ipbits=0&"
        + "expire=19000000000&signature=7181C59D0252B285D593E1B61D985D5B7C98DE2A."
        + "5B445837F55A40E0F28AACAA047982E372D177E2&key=ik0", DemoUtil.TYPE_DASH),
  };

  public static final Sample[] YOUTUBE_DASH_WEBM = new Sample[] {
    new Sample("Google Glass",
        "http://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?"
        + "as=fmp4_audio_clear,webm2_sd_hd_clear&sparams=ip,ipbits,expire,as&ip=0.0.0.0&ipbits=0&"
        + "expire=19000000000&signature=A3EC7EE53ABE601B357F7CAB8B54AD0702CA85A7."
        + "446E9C38E47E3EDAF39E0163C390FF83A7944918&key=ik0", DemoUtil.TYPE_DASH),
    new Sample("Google Play",
        "http://www.youtube.com/api/manifest/dash/id/3aa39fa2cc27967f/source/youtube?"
        + "as=fmp4_audio_clear,webm2_sd_hd_clear&sparams=ip,ipbits,expire,as&ip=0.0.0.0&ipbits=0&"
        + "expire=19000000000&signature=B752B262C6D7262EC4E4EB67901E5D8F7058A81D."
        + "C0358CE1E335417D9A8D88FF192F0D5D8F6DA1B6&key=ik0", DemoUtil.TYPE_DASH),
  };

  public static final Sample[] SMOOTHSTREAMING = new Sample[] {
    new Sample("Super speed",
        "http://playready.directtaps.net/smoothstreaming/SSWSS720H264/SuperSpeedway_720.ism",
        DemoUtil.TYPE_SS),
    new Sample("Super speed (PlayReady)",
        "http://playready.directtaps.net/smoothstreaming/SSWSS720H264PR/SuperSpeedway_720.ism",
        DemoUtil.TYPE_SS),
  };

  public static final Sample[] WIDEVINE_GTS = new Sample[] {
    new Sample("WV: HDCP not specified", "d286538032258a1c",
        "http://www.youtube.com/api/manifest/dash/id/d286538032258a1c/source/youtube?"
        + "as=fmp4_audio_cenc,fmp4_sd_hd_cenc&sparams=ip,ipbits,expire,as&ip=0.0.0.0&ipbits=0"
        + "&expire=19000000000&signature=41EA40A027A125A16292E0A5E3277A3B5FA9B938."
        + "0BB075C396FFDDC97E526E8F77DC26FF9667D0D6&key=ik0", DemoUtil.TYPE_DASH),
    new Sample("WV: HDCP not required", "48fcc369939ac96c",
        "http://www.youtube.com/api/manifest/dash/id/48fcc369939ac96c/source/youtube?"
        + "as=fmp4_audio_cenc,fmp4_sd_hd_cenc&sparams=ip,ipbits,expire,as&ip=0.0.0.0&ipbits=0"
        + "&expire=19000000000&signature=315911BDCEED0FB0C763455BDCC97449DAAFA9E8."
        + "5B41E2EB411F797097A359D6671D2CDE26272373&key=ik0", DemoUtil.TYPE_DASH),
    new Sample("WV: HDCP required", "e06c39f1151da3df",
        "http://www.youtube.com/api/manifest/dash/id/e06c39f1151da3df/source/youtube?"
        + "as=fmp4_audio_cenc,fmp4_sd_hd_cenc&sparams=ip,ipbits,expire,as&ip=0.0.0.0&ipbits=0"
        + "&expire=19000000000&signature=A47A1E13E7243BD567601A75F79B34644D0DC592."
        + "B09589A34FA23527EFC1552907754BB8033870BD&key=ik0", DemoUtil.TYPE_DASH),
    new Sample("WV: Secure video path required", "0894c7c8719b28a0",
        "http://www.youtube.com/api/manifest/dash/id/0894c7c8719b28a0/source/youtube?"
        + "as=fmp4_audio_cenc,fmp4_sd_hd_cenc&sparams=ip,ipbits,expire,as&ip=0.0.0.0&ipbits=0"
        + "&expire=19000000000&signature=2847EE498970F6B45176766CD2802FEB4D4CB7B2."
        + "A1CA51EC40A1C1039BA800C41500DD448C03EEDA&key=ik0", DemoUtil.TYPE_DASH),
    new Sample("WV: HDCP + secure video path required", "efd045b1eb61888a",
        "http://www.youtube.com/api/manifest/dash/id/efd045b1eb61888a/source/youtube?"
        + "as=fmp4_audio_cenc,fmp4_sd_hd_cenc&sparams=ip,ipbits,expire,as&ip=0.0.0.0&ipbits=0"
        + "&expire=19000000000&signature=61611F115EEEC7BADE5536827343FFFE2D83D14F."
        + "2FDF4BFA502FB5865C5C86401314BDDEA4799BD0&key=ik0", DemoUtil.TYPE_DASH),
    new Sample("WV: 30s license duration", "f9a34cab7b05881a",
        "http://www.youtube.com/api/manifest/dash/id/f9a34cab7b05881a/source/youtube?"
        + "as=fmp4_audio_cenc,fmp4_sd_hd_cenc&sparams=ip,ipbits,expire,as&ip=0.0.0.0&ipbits=0"
        + "&expire=19000000000&signature=88DC53943385CED8CF9F37ADD9E9843E3BF621E6."
        + "22727BB612D24AA4FACE4EF62726F9461A9BF57A&key=ik0", DemoUtil.TYPE_DASH),
  };

  public static final Sample[] HLS = new Sample[] {
    new Sample("Apple master playlist",
        "https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/"
        + "bipbop_4x3_variant.m3u8", DemoUtil.TYPE_HLS),
    new Sample("Apple master playlist advanced",
        "https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/"
        + "bipbop_16x9_variant.m3u8", DemoUtil.TYPE_HLS),
    new Sample("Apple TS media playlist",
        "https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear1/"
        + "prog_index.m3u8", DemoUtil.TYPE_HLS),
    new Sample("Apple AAC media playlist",
        "https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear0/"
        + "prog_index.m3u8", DemoUtil.TYPE_HLS),
  };

  public static final Sample[] MISC = new Sample[] {
    new Sample("Dizzy", "http://html5demos.com/assets/dizzy.mp4",
        DemoUtil.TYPE_OTHER),
    new Sample("Dizzy (https->http redirect)", "https://goo.gl/MtUDEj",
        DemoUtil.TYPE_OTHER),
    new Sample("Apple AAC 10s", "https://devimages.apple.com.edgekey.net/"
        + "streaming/examples/bipbop_4x3/gear0/fileSequence0.aac",
        DemoUtil.TYPE_OTHER),
  };

  private Samples() {}

}
