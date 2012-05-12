package net.majorkernelpanic.libstreaming;

import android.media.MediaRecorder;
import net.majorkernelpanic.librtp.AMRNBPacketizer;

/**
 * This will stream AMRNB from the mic over RTP
 * Just call setDestination(), prepare() & start()
 */
public class AMRNBStream extends MediaStream {

	public AMRNBStream() {
		super();
		
		this.packetizer = new AMRNBPacketizer();
		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
		setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		setAudioChannels(1);
		
	}
	
}
