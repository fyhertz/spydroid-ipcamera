/*
 * Copyright (C) 2011 GUIGUI Simon fyhertz@gmail.com
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

package net.mkp.spydroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/*
 * Main activity
 */

public class SpydroidActivity extends Activity {
    
    public ViewGroup topLayout;
    public Button startButton, quitButton;
    
    static final public String LOG_TAG = "SPYDROID";
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        setContentView(R.layout.main);
        topLayout = (ViewGroup) findViewById(R.id.mainlayout);
        startButton = (Button) findViewById(R.id.streambutton);
        startButton.setOnClickListener(new OnClickListener() 
        {
        	public void onClick(View v) {
        		
        		Intent intent = new Intent(v.getContext(),SecondActivity.class);
        		intent.putExtra("ip", ((EditText) findViewById(R.id.ip)).getText().toString() );
        		startActivityForResult(intent, 0);
        		
        	}
        });
        
        
        
    }
    
    
}