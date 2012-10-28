/*
 * Copyright (C) 2011 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package net.majorkernelpanic.spydroid;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;

import net.majorkernelpanic.networking.HttpServer;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Handler;

public class CustomHttpServer extends HttpServer {

	/** Adds some request handler to allow the user to remotly launch souds on the phone **/
	public CustomHttpServer(int port, Context context, Handler handler) {
		super(port, context, handler);
		addRequestHandler("/sound.htm*", new SoundRequestHandler(context, handler));
		addRequestHandler("/js/params.js", new SoundsListRequestHandler(handler));
	}

	private static boolean screenState = true;
	
	public void setScreenState(boolean state) {
		screenState  = state;
	}
	
	/** Send an array with all available sounds, and the screenState boolean that indicates if the app is on the foreground */
	static class SoundsListRequestHandler implements HttpRequestHandler {

		private Handler handler;
		private Field[] raws = R.raw.class.getFields();
		
		public SoundsListRequestHandler(Handler handler) {
			this.handler = handler;
		}
		
		public void handle(HttpRequest request, HttpResponse response, HttpContext arg2) throws HttpException, IOException {
			EntityTemplate body = new EntityTemplate(new ContentProducer() {
				public void writeTo(final OutputStream outstream) throws IOException {
					OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
					writer.write("var sounds = [");
					for(int i=0; i < raws.length-1; i++) {
						writer.write("'"+raws[i].getName() + "',");
					}
					writer.write("'"+raws[raws.length-1].getName() + "'];");
					writer.write("var screenState = "+(screenState?"1":"0")+";");
					writer.flush();
				}
			});

			response.setStatusCode(HttpStatus.SC_OK);
        	body.setContentType("application/json; charset=UTF-8");
        	response.setEntity(body);
        	
        	if (!screenState) {
    			Intent i = new Intent(context,SpydroidActivity.class);
    			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    			context.startActivity(i);
        	}
        	
		}
	}
	
	/**	Play a sound on the phone based on the uri */
	static class SoundRequestHandler implements HttpRequestHandler {

		private Handler handler;
		private Context context;
		private SoundPool soundPool = new SoundPool(4,AudioManager.STREAM_MUSIC,0);
		private Field[] raws = R.raw.class.getFields();
		
		public SoundRequestHandler(Context context, Handler handler) {
			this.handler = handler;
			this.context = context;
			
			soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
				public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
					soundPool.play(sampleId, 0.99f, 0.99f, 1, 0, 1);
				}
			});
		}
		
		public void handle(HttpRequest request, HttpResponse response, HttpContext arg2)
				throws HttpException, IOException {
			
			final String uri = URLDecoder.decode(request.getRequestLine().getUri());
			final List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uri),"UTF-8");
			final String[] content = {"Error"};
			int soundID;
			
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);

			if (params.size()>0) {
	        	try {
	        		for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
	        			NameValuePair param = it.next();
	        			// Load sound with appropriate name
	        			if (param.getName().equals("name")) {
	        				for(int i=0; i < raws.length; i++) {
	        					if (raws[i].getName().equals(param.getValue())) {
	        						soundID = soundPool.load(context, raws[i].getInt(null), 0);
	        						response.setStatusCode(HttpStatus.SC_OK);
	        						content[0] = "OK";
	        					}
	        				}
	        			}
	        		}
				} catch (IllegalArgumentException e) {

				} catch (IllegalAccessException e) {

				}
			}
			
        	EntityTemplate body = new EntityTemplate(new ContentProducer() {
        		public void writeTo(final OutputStream outstream) throws IOException {
        			OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
        			writer.write(content[0]);
        			writer.flush();
        		}
        	});
        	body.setContentType("text/plain; charset=UTF-8");
        	response.setEntity(body);
			
			
		}
		
		
	}
	
}
