package net.majorkernelpanic.spydroid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/*
 * Activity that is displayed when clicking on "Quality" in the option menu of the main activity
 */

public class QualityListActivity extends ListActivity {
	
	public static String DefaultQuality = "640x480, 15fps";
	private ListView listView;
	
    /* User can choose a quality among those */
    private String[] ql = new String[] {
    		
    		"640x480, 20fps",
    		"640x480, 15fps",
    		"640x480, 10fps",
    		"320x240, 20fps",
    		"320x240, 12fps",
    		"176x144, 10fps",
    		"176x144, 8fps",
    		
    };
	
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        setListAdapter(new ArrayAdapter<String>( this, R.layout.quality, ql));
        
        listView = getListView();
        listView.setItemsCanFocus(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
    }
    
    protected void onResume() {
    	super.onResume();
    	
    	int i;
    	Intent data = getIntent();
    	
		int resX = data.getIntExtra("net.majorkernelpanic.spydroid.resX", 0);
		int resY = data.getIntExtra("net.majorkernelpanic.spydroid.resY", 0);
		int fps = data.getIntExtra("net.majorkernelpanic.spydroid.fps", 0);
		
		for (i=0;i<ql.length;i++)
			if (ql[i].equals(String.valueOf(resX)+"x"+String.valueOf(resY)+", "+String.valueOf(fps)+"fps")) break;
		
    	listView.setItemChecked(i,true);
    	
    }
    
    public void onListItemClick(ListView l, View v, int position, long id) {
    	
		Pattern p = Pattern.compile("(\\d+)x(\\d+), (\\d+)fps");
		Matcher m = p.matcher(ql[position]); m.find();
    	
    	Intent intent = new Intent();
    	intent.putExtra("net.majorkernelpanic.spydroid.resX", Integer.parseInt(m.group(1))).putExtra("net.majorkernelpanic.spydroid.resY", Integer.parseInt(m.group(2))).putExtra("net.majorkernelpanic.spydroid.fps", Integer.parseInt(m.group(3)));

    	setResult(1000, intent);
    	finish();

    }
	
	
}
