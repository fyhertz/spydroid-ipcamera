package net.majorkernelpanic.spydroid.api;

import net.majorkernelpanic.streaming.misc.RtspServer;

public class CustomRtspServer extends RtspServer {
	public CustomRtspServer() {
		super();
		mEnabled = false;
	}
}
