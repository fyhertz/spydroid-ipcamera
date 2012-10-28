package net.majorkernelpanic.spydroid;

import android.app.Activity;
import android.os.Bundle;

/** 
 * Read the stream from another phone running spydroid !
 * Not ready yet, obvioulsy :) 
 **/
public class ClientActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.client);
        
    }
    
    public void onStart() {
    	super.onStart();
    }
    
    public void onStop() {
    	super.onStop();
    }
	
}
