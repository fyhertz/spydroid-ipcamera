package net.majorkernelpanic.networking;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import net.majorkernelpanic.streaming.video.VideoQuality;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.hardware.Camera.CameraInfo;

/**
 * Parse a URI and configure a Session accordingly
 */
public class UriParser {

	public static void parse(String uri, Session session) throws IllegalStateException, IOException {
		boolean flash = false;
		int camera = CameraInfo.CAMERA_FACING_BACK;
		
		List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uri),"UTF-8");
		if (params.size()>0) {
			
			// Those parameters must be parsed first or else they won't necessarily be taken into account
			for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
				NameValuePair param = it.next();
				
				// FLASH ON/OFF
				if (param.getName().equals("flash")) {
					if (param.getValue().equals("on")) flash = true;
					else flash = false;
				}
				
				// CAMERA -> client can choose between the front facing camera and the back facing camera
				else if (param.getName().equals("camera")) {
					if (param.getValue().equals("back")) camera = CameraInfo.CAMERA_FACING_BACK;
					else if (param.getValue().equals("front")) camera = CameraInfo.CAMERA_FACING_FRONT;
				}
				
			}
			
			for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
				NameValuePair param = it.next();
				
				// H264
				if (param.getName().equals("h264")) {
					VideoQuality quality = VideoQuality.parseQuality(param.getValue());
					session.addVideoTrack(Session.VIDEO_H264, camera, quality, flash);
				}
				
				// H263
				else if (param.getName().equals("h263")) {
					VideoQuality quality = VideoQuality.parseQuality(param.getValue());
					session.addVideoTrack(Session.VIDEO_H263, camera, quality, flash);
				}
				
				// AMRNB
				else if (param.getName().equals("amrnb")) {
					session.addAudioTrack(Session.AUDIO_AMRNB);
				}
				
				// AMR -> just for convenience: does the same as AMRNB
				else if (param.getName().equals("amr")) {
					session.addAudioTrack(Session.AUDIO_AMRNB);
				}
				
				// AAC -> experimental
				else if (param.getName().equals("aac")) {
					session.addAudioTrack(Session.AUDIO_AAC);
				}
				
				// Generic Audio Stream -> make use of api level 12
				// TODO: Doesn't work :/
				else if (param.getName().equals("testnewapi")) {
					session.addAudioTrack(Session.AUDIO_ANDROID_AMR);
				}
				
			}
		} 
		// Uri has no parameters: the default behavior is to add one h264 track and one amrnb track
		else {
			session.addVideoTrack();
			//session.addAudioTrack();
		}
	}
	
}
