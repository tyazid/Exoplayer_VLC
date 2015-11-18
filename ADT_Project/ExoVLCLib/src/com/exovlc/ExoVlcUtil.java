package com.exovlc;

import java.util.ArrayList;
import java.util.Arrays;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.Media.AudioTrack;
import org.videolan.libvlc.Media.SubtitleTrack;
import org.videolan.libvlc.Media.VideoTrack;
import org.videolan.libvlc.MediaList;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.media.MediaFormat;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.TrackInfo;

public class ExoVlcUtil {
	static final int MS_2_MICRO = 1000;
	static final String DUMM_VDO_MIME = "video/uncknown";
	private static final int[] MEDIA_TYPES = { Media.Track.Type.Audio, Media.Track.Type.Video, Media.Track.Type.Text };// should
																														// be
																														// sorted
																														// !!
	static {
		Arrays.sort(MEDIA_TYPES);
	}

	private ExoVlcUtil() {
	}

	static void releaseVLC(LibVLC lib) {
		synchronized (ExoVlcUtil.class) {
			if (lib != null) {
				lib.destroy();
				lib = null;
			}
			libCtx = null;
		}
	}

	static boolean validSurface(SurfaceHolder holder) {
		if (holder.getSurface() != null) {
			Rect r = holder.getSurfaceFrame();
			System.out.println("ExoVlcUtil.validSurface() r = " + r);
			return (r.width() * r.height()) > 0;
		}
		return false;
	}

	static synchronized void updateLibVlcSettings(SharedPreferences pref, LibVLC sLibVLC) {

		sLibVLC.setSubtitlesEncoding(pref.getString("subtitle_text_encoding", ""));
		sLibVLC.setTimeStretching(pref.getBoolean("enable_time_stretching_audio", false));
		sLibVLC.setFrameSkip(pref.getBoolean("enable_frame_skip", false));
		sLibVLC.setChroma(pref.getString("chroma_format", ""));
		// sLibVLC.setVerboseMode(pref.getBoolean("enable_verbose_mode", true));
		sLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_FULL);// HW_ACCELERATION_AUTOMATIC);//
																		// HW_ACCELERATION_FULL);//HW_ACCELERATION_DISABLED);
		System.out.println("######VideoActivity..updateLibVlcSettings() setChroma="
				+ pref.getString("chroma_format", ""));
		System.out.println("######VideoActivity..updateLibVlcSettings() enable_frame_skip="
				+ pref.getBoolean("enable_frame_skip", false));
		System.out.println("######VideoActivity..updateLibVlcSettings() enable_time_stretching_audio = "
				+ pref.getBoolean("enable_time_stretching_audio", false));
		System.out.println("######VideoActivity..updateLibVlcSettings() subtitle_text_encoding = "
				+ pref.getString("subtitle_text_encoding", ""));
		System.out.println("######VideoActivity..updateLibVlcSettings() equalizer_enabled = "
				+ pref.getBoolean("equalizer_enabled", false));

		// if (pref.getBoolean("equalizer_enabled", false))
		// sLibVLC.setEqualizer(Preferences.getFloatArray(pref,
		// "equalizer_values"));

		int aout;
		try {
			aout = Integer.parseInt(pref.getString("aout", "-1"));
		} catch (NumberFormatException nfe) {
			aout = -1;
		}
		int vout;
		try {
			vout = Integer.parseInt(pref.getString("vout", "-1"));
		} catch (NumberFormatException nfe) {
			vout = -1;
		}
		int deblocking;
		try {
			deblocking = Integer.parseInt(pref.getString("deblocking", "-1"));
		} catch (NumberFormatException nfe) {
			deblocking = -1;
		}
		int hardwareAcceleration;
		try {
			hardwareAcceleration = Integer.parseInt(pref.getString("hardware_acceleration", "-1"));
		} catch (NumberFormatException nfe) {
			hardwareAcceleration = -1;
		}
		int devHardwareDecoder;
		try {
			devHardwareDecoder = Integer.parseInt(pref.getString("dev_hardware_decoder", "-1"));
		} catch (NumberFormatException nfe) {
			devHardwareDecoder = -1;
		}
		int networkCaching = pref.getInt("network_caching_value", 0);
		if (networkCaching > 60000)
			networkCaching = 60000;
		else if (networkCaching < 0)
			networkCaching = 0;
		System.out.println("###### VLCInstance.updateLibVlcSettings()aout=" + aout);
		sLibVLC.setAout(aout);
		System.out.println("######VideoActivity..updateLibVlcSettings()vout=" + vout);
		/************/
		/************/
		/************/
		/************/

		/*vout = org.videolan.libvlc.LibVLC.VOUT_OPEGLES2;*/
		/************/
		/************/
		/************/
		/************/

		if (vout != -1)
			sLibVLC.setVout(vout);
		else
			sLibVLC.setVout(LibVlcUtil.isGingerbreadOrLater() ? LibVLC.VOUT_ANDROID_WINDOW
					: LibVLC.VOUT_ANDROID_SURFACE);

		System.out.println("######VideoActivity..updateLibVlcSettings()deblocking=" + deblocking);
		sLibVLC.setDeblocking(deblocking);
		System.out.println("######VideoActivity..updateLibVlcSettings()networkCaching=" + networkCaching);
		sLibVLC.setNetworkCaching(networkCaching);
		// System.out.println("######VideoActivity..updateLibVlcSettings()hardwareAcceleration="
		// + hardwareAcceleration);
		// sLibVLC.setHardwareAcceleration(hardwareAcceleration);
		System.out.println("######VideoActivity..updateLibVlcSettings()devHardwareDecoder=" + devHardwareDecoder);
		sLibVLC.setDevHardwareDecoder(devHardwareDecoder);
	}

	private static LibVLC lib;
	private static Context libCtx;

	public static LibVLC getVLC(Context context) throws com.google.android.exoplayer.ExoPlaybackException {
		synchronized (ExoVlcUtil.class) {
			if (lib != null) {
				if (context.equals(libCtx))
					return lib;
				try {
					lib.detachSurface();
				} catch (Exception e) {
				}
				lib.destroy();
			}
			lib = new LibVLC();
			updateLibVlcSettings(PreferenceManager.getDefaultSharedPreferences(context), lib);
			try {
				lib.init(libCtx = context);
			} catch (LibVlcException e) {
				e.printStackTrace();
				throw new com.google.android.exoplayer.ExoPlaybackException(e.getCause());
			}
			return lib;
		}
	}

	public static void log(Object o, String msg) {
		String tag = o != null ? o.getClass().getSimpleName() : "";
		log(tag, msg);
	}

	static void log(String tag, String msg) {
		Log.d(tag, msg);
	}

	public static org.videolan.libvlc.Media getMedia(LibVLC vlc, String uri) throws ExoPlaybackException {
		System.out.println(">> ExoVlcUtil.getMedia() uri = " + uri);

		org.videolan.libvlc.Media media = new org.videolan.libvlc.Media(vlc, uri);

		boolean parsed = media.parse(org.videolan.libvlc.Media.Parse.FetchNetwork);
		media.release();
		System.out.println("ExoVlcUtil.getMedia() is parsed media=" + media.isParsed());
		System.out.println("ExoVlcUtil.getMedia() media duration =");

		MediaList mlist = media.subItems();
		System.out.println("ExoVlcUtil.getMedia() sub item =" + mlist);
		if (mlist != null) {
			System.out.println(">>> ExoVlcUtil.getMedia() parsed media subItems count : " + mlist.getCount());
			for (int i = 0; i < mlist.getCount(); i++) {
				System.out.println(">>> ExoVlcUtil.getMedia() parsed media subItems item (" + i + ") : "
						+ mlist.getMediaAt(i).getType() + " track count " + mlist.getMediaAt(i).getTrackCount());

			}
		}
		System.out.println(">>> ExoVlcUtil.getMedia() parsed media  dump tracks : " + media.getTrackCount());
		for (int i = 0; i < media.getTrackCount(); i++) {
			System.out.println(">>> ExoVlcUtil.getMedia() parsed media  track[" + i + "]:" + media.getTrack(i).type);

		}

		// try {
		// boolean hv =vlc.hasVideoTrack(uri);
		// System.out.println(">>> ExoVlcUtil.getMedia() HAS VDO="+hv);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		if (!parsed)
			throw new ExoPlaybackException("Unable to parse media " + uri);
		return media;
	}

	/**
	 * 
	 * @param media
	 * @return
	 */
	static org.videolan.libvlc.Media.Track[] getAvailableTracks(org.videolan.libvlc.Media media) {
		ArrayList<Media.Track> l = new ArrayList<Media.Track>();
		int c = media.getTrackCount();

		while (--c >= 0) {
			Media.Track t = media.getTrack(c);
			System.out.println("ExoVlcUtil.getAvailableTracks() ?? type : " + t.type + " search "
					+ Arrays.binarySearch(MEDIA_TYPES, t.type));
			if (Arrays.binarySearch(MEDIA_TYPES, t.type) >= 0)
				l.add(t);
		}

		return l.toArray(new org.videolan.libvlc.Media.Track[l.size()]);
	}

	static TrackInfo[] getDummyVdoTrack(org.videolan.libvlc.Media media, String mime) {
		System.out.println(">>> ExoVlcUtil.getDummyVdoTrack() ---> duration = " + media.getDuration());
		return new TrackInfo[] { new TrackInfo(mime, media.getDuration()) };
	}

	/**
	 * 
	 * @param media
	 * @param vlcTracks
	 * @param lib
	 * @return
	 */
	static com.google.android.exoplayer.TrackInfo[] vlc2exoTracks(long duration,
			org.videolan.libvlc.Media.Track[] vlcTracks, LibVLC lib) {
		com.google.android.exoplayer.TrackInfo[] res = new com.google.android.exoplayer.TrackInfo[vlcTracks.length];
		System.out.println("ExoVlcUtil.vlc2exoTracks() vlcTracks = " + vlcTracks.length);
		// Media.Track
		for (int i = 0; i < res.length; i++) {
			MediaFormat mf = track2mediaFormat(vlcTracks[i]);

			res[i] = new TrackInfo(mf.getString(MediaFormat.KEY_MIME), duration);
			System.out.println("\t track " + i + " mime type =" + mf.getString(MediaFormat.KEY_MIME) + " duration ="
					+ duration);
		}
		/*
		 * System.out.println(">>>> ExoVlcUtil.vlc2exoTracks() vlcTracks.length = "
		 * +vlcTracks.length); long l; for (int i = 0; i < vlcTracks.length;
		 * i++) { org.videolan.libvlc.TrackInfo vt = vlcTracks[i];
		 * System.out.println("\t\t >>>>>Codec("+i+") "+vlcTracks[i].Codec);
		 * res[i] = new TrackInfo( vt.Codec, (l=lib.getLength()) == -1 ?
		 * com.google.android.exoplayer.C.UNKNOWN_TIME_US : l * MS_2_MICRO); }
		 */
		return res;
	}

	/**
	 * 
	 * @param code
	 */
	static void nativeVlcMsg(int code) {
		if (code == -1)
			log(" Lib VLC", "Err msg:" + LibVlcUtil.getErrorMsg());
	}

	static float pos2percentage(long microsec, LibVLC lib) {
		float fp = .0f;

		long lms = lib.getLength();
		if (lms != -1) {
			lms *= MS_2_MICRO;
			fp = (((float) microsec) / lms);
		}
		return fp;
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	static android.media.MediaFormat track2mediaFormat(Media.Track track) {

		android.media.MediaFormat mf;
		// TODO fix track
		switch (track.type) {
		case Media.Track.Type.Video: {
			VideoTrack vt = (VideoTrack) track;
			mf = android.media.MediaFormat.createVideoFormat(track.codec, vt.width, vt.height);
			mf.setInteger(android.media.MediaFormat.KEY_WIDTH, vt.width);
			mf.setInteger(android.media.MediaFormat.KEY_HEIGHT, vt.height);
			mf.setFloat(android.media.MediaFormat.KEY_FRAME_RATE, vt.frameRateNum);
		}
			break;
		case Media.Track.Type.Audio: {
			AudioTrack at = (AudioTrack) track;
			mf = android.media.MediaFormat.createAudioFormat(track.codec, at.rate, at.channels);
		}
			break;
		case Media.Track.Type.Text: {
			SubtitleTrack st = (SubtitleTrack) track;
			mf = android.media.MediaFormat.createSubtitleFormat(track.codec, track.language);
			mf.setString(VLCTrackKeys.VLC_SUBTITLE_TRACK_ECODING, st.encoding);
		}
			break;

		default:
			throw new IllegalArgumentException("Unknown track type.");
		}

		mf.setInteger(android.media.MediaFormat.KEY_BIT_RATE, track.bitrate);
		mf.setString(android.media.MediaFormat.KEY_LANGUAGE, track.language);// VLC_TRACK_ORIGINAL_CODEC
		mf.setString(VLCTrackKeys.VLC_TRACK_DESCRIPTION, track.description);
		mf.setString(VLCTrackKeys.VLC_TRACK_ORIGINAL_CODEC, track.originalCodec);

		// TODO
		/*
		 * Missing maxInputSize = getOptionalIntegerV16(format,
		 * android.media.MediaFormat.KEY_MAX_INPUT_SIZE); pixelWidthHeightRatio
		 * = getOptionalFloatV16(format, KEY_PIXEL_WIDTH_HEIGHT_RATIO);
		 */

		return mf;

	}

	static int media2vlcVolume(float mediaVolume) {
		// VLC : 0 .. 100 db
		// Android Media: .0f .. MaxVomule

		int v = (int) (mediaVolume * 100);
		v /= android.media.AudioTrack.getMaxVolume();
		return v;
	}

	private static final String[] AUDIO_WITNESS = { "aac", "audio", "mp3", "ac3", "wav" };
	private static final String[] VIDEO_WITNESS = { "video", "mp4", "h26", "ogg", "avi", "divx" };

	private static boolean is_part_of(String[] parts, String of) {
		for (int i = 0; i < parts.length; i++)
			if (of.indexOf(parts[i]) != -1)
				return true;

		return false;

	}

	static boolean isVLCVideoMimeType(String mimeType) {
		return is_part_of(VIDEO_WITNESS, mimeType.toLowerCase());

	}

	static boolean isVLCAudioMimeType(String mimeType) {
		return is_part_of(AUDIO_WITNESS, mimeType.toLowerCase());

	}
}
