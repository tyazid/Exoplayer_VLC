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

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media.Parse;
import org.videolan.libvlc.MediaList;
import org.videolan.libvlc.VLCObject.Event;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.exovlc.ExoVlcUtil;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer.demo.Samples.Sample;
import com.google.android.exoplayer.util.MimeTypes;

/**
 * An activity for selecting from a number of samples.
 */
public class SampleChooserActivity extends Activity {

  private static final String TAG = "SampleChooserActivity";
  private void atest(){
	  try {
		LibVLC l = ExoVlcUtil.getVLC(this);
		System.out.println("SampleChooserActivity.atest()");
		System.out.println("SampleChooserActivity.atest() lib = "+l);
		final org.videolan.libvlc.Media media = new org.videolan.libvlc.Media(l, ("http://www-itec.uni-klu.ac.at/ftp/datasets/mmsys12/BigBuckBunny/MPDs/BigBuckBunnyNonSeg_1s_isoffmain_DIS_23009_1_v_2_1c2_2011_08_30.mpd"));
		System.out.println("SampleChooserActivity.atest() Media = "+media);
		media.setEventListener( new org.videolan.libvlc.VLCObject.EventListener() {
			
			@Override
			public void onEvent(Event event) {
				// TODO Auto-generated method stub
				System.out.println(">>>>>>  SampleChooserActivity.atest(). E.TYPE="+event.type);
				switch(event.type){
					case org.videolan.libvlc.VLCObject.Events.MediaMetaChanged:
						System.out.println(">>>>>> MediaMetaChanged");
						break;
					case org.videolan.libvlc.VLCObject.Events.MediaSubItemAdded:
						System.out.println(">>>>>> MediaSubItemAdded");
						break;
					case org.videolan.libvlc.VLCObject.Events.MediaDurationChanged:
					
						
						System.out.println(">>>>>> MediaDurationChanged");
						break;
					case org.videolan.libvlc.VLCObject.Events.MediaParsedChanged:
						try {
							System.out.println(">>>>>> MediaParsedChanged***** ");
							System.out.println(">>>>>> dump some info media = "+media);
							media.release();
							System.out.println("ExoVlcUtil.getMedia() is parsed media="+media.isParsed());
							System.out.println("ExoVlcUtil.getMedia() media duration ="+media.getDuration());
							

							
							MediaList mlist = media.subItems();
							System.out.println("ExoVlcUtil.getMedia() sub item ="+mlist);
							if(mlist != null){
							System.out.println(">>> ExoVlcUtil.getMedia() parsed media subItems count : "+mlist .getCount());
							for (int i = 0; i <  mlist.getCount(); i++) {
								System.out.println(">>> ExoVlcUtil.getMedia() parsed media subItems item ("+i+") : "+mlist.getMediaAt(i).getType() +
										" track count "+mlist.getMediaAt(i).getTrackCount());

							}}
							System.out.println(">>> ExoVlcUtil.getMedia() parsed media  dump tracks : "+media.getTrackCount() );
							for (int i = 0; i < media.getTrackCount(); i++) {
								System.out.println(">>> ExoVlcUtil.getMedia() parsed media  track["+i+"]:"+media.getTrack(i).type );

							}
							for (int j = 0; j <org.videolan.libvlc.Media.Meta.MAX; j++) {
								System.out.println( " meta("+j+")="+media.getMeta(j));
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case org.videolan.libvlc.VLCObject.Events.MediaStateChanged:
						System.out.println(">>>>>> MediaStateChanged");
						break;
					case org.videolan.libvlc.VLCObject.Events.MediaSubItemTreeAdded:
						System.out.println(">>>>>> MediaSubItemTreeAdded");
						break;

					case org.videolan.libvlc.VLCObject.Events.MediaListItemAdded:
						System.out.println(">>>>>> MediaListItemAdded");
						break;
					case org.videolan.libvlc.VLCObject.Events.MediaListItemDeleted:
						System.out.println(">>>>>> MediaListItemDeleted");
						break;
					case org.videolan.libvlc.VLCObject.Events.MediaListEndReached:
						System.out.println(">>>>>> MediaListEndReached");
						break;

					case org.videolan.libvlc.VLCObject.Events.MediaDiscovererStarted:
						System.out.println(">>>>>> MediaDiscovererStarted");
						break;
					case org.videolan.libvlc.VLCObject.Events.MediaDiscovererEnded:
						System.out.println(">>>>>> MediaDiscovererEnded ");
						break;
				}
				
			}
		});
		media. parseAsync(Parse.FetchLocal|Parse.ParseLocal|Parse.FetchNetwork|Parse.ParseNetwork);
		
	} catch (ExoPlaybackException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sample_chooser_activity);

    ListView sampleList = (ListView) findViewById(R.id.sample_list);
    final SampleAdapter sampleAdapter = new SampleAdapter(this);

    
    sampleAdapter.add(new Header("DIVERS"));
    sampleAdapter.addAll((Object[]) Samples.DIVERS);
    
//    sampleAdapter.add(new Header("CISCO"));
//    sampleAdapter.addAll((Object[]) Samples.CISCO);
//    sampleAdapter.add(new Header("DIVERS"));
//    sampleAdapter.addAll((Object[]) Samples.DIVERS);
//    sampleAdapter.add(new Header("YouTube DASH"));
//    sampleAdapter.addAll((Object[]) Samples.YOUTUBE_DASH_MP4);
//    sampleAdapter.add(new Header("Widevine GTS DASH"));
//    sampleAdapter.addAll((Object[]) Samples.WIDEVINE_GTS);
//    sampleAdapter.add(new Header("SmoothStreaming"));
//    sampleAdapter.addAll((Object[]) Samples.SMOOTHSTREAMING);
//    sampleAdapter.add(new Header("HLS"));
//    sampleAdapter.addAll((Object[]) Samples.HLS);
//    sampleAdapter.add(new Header("Misc"));
//    sampleAdapter.addAll((Object[]) Samples.MISC);
    
    

    // Add WebM samples if the device has a VP9 decoder.
    try {
      if (MediaCodecUtil.getDecoderInfo(MimeTypes.VIDEO_VP9, false) != null) {
        sampleAdapter.add(new Header("YouTube WebM DASH (Experimental)"));
        sampleAdapter.addAll((Object[]) Samples.YOUTUBE_DASH_WEBM);
      }
    } catch (DecoderQueryException e) {
      Log.e(TAG, "Failed to query vp9 decoder", e);
    }

    sampleList.setAdapter(sampleAdapter);
    sampleList.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object item = sampleAdapter.getItem(position);
        if (item instanceof Sample) {
          onSampleSelected((Sample) item);
        }
      }
    });
    
//    new Handler().post(new Runnable() {
//		
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//		atest();	
//		}
//	});
  }

  private void onSampleSelected(Sample sample) {
    Intent mpdIntent = new Intent(this, PlayerActivity.class)
        .setData(Uri.parse(sample.uri))
        .putExtra(PlayerActivity.CONTENT_ID_EXTRA, sample.contentId)
        .putExtra(PlayerActivity.CONTENT_TYPE_EXTRA, sample.type);
    startActivity(mpdIntent);
  }

  private static class SampleAdapter extends ArrayAdapter<Object> {

    public SampleAdapter(Context context) {
      super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        int layoutId = getItemViewType(position) == 1 ? android.R.layout.simple_list_item_1
            : R.layout.sample_chooser_inline_header;
        view = LayoutInflater.from(getContext()).inflate(layoutId, null, false);
      }
      Object item = getItem(position);
      String name = null;
      if (item instanceof Sample) {
        name = ((Sample) item).name;
      } else if (item instanceof Header) {
        name = ((Header) item).name;
      }
      ((TextView) view).setText(name);
      return view;
    }

    @Override
    public int getItemViewType(int position) {
      return (getItem(position) instanceof Sample) ? 1 : 0;
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

  }

  private static class Header {

    public final String name;

    public Header(String name) {
      this.name = name;
    }

  }

}
