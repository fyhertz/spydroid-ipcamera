package net.majorkernelpanic.spydroid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/*
 * Activity that is displayed when clicking on "Quality" in the option menu of the main activity
 */

public class QualityListActivity extends ListActivity {

	public static int DefaultBitRate = -1;
	private ListView listView;
	
	private int fps, br, resX, resY;
	
    /* User can choose a quality among those */
    private String[] resolutions = new String[] {
    		"640x480",
    		"320x240",
    		"176x144"  		
    };
    private String[] framerates = new String[] {
    		"20 fps",
    		"15 fps",
    		"10 fps"
    };
    private String[] bitrates = new String[] {
    		"Default",
    		"1000 kb/s",
    		"500 kb/s",
    		"100 kb/s",
    		"50 kb/s",
    		"10 kb/s"
    };
    
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        setListAdapter(new CustomAdapter(this, new String[] {"Resolution","Framerate","Bitrate"}, new String[][] {resolutions,framerates,bitrates}));
        
        listView = getListView();
        listView.setItemsCanFocus(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
    }
    
    protected void onResume() {
    	super.onResume();

    	Intent data = getIntent();
    	
		resX = data.getIntExtra("net.majorkernelpanic.spydroid.resX", 0);
		resY = data.getIntExtra("net.majorkernelpanic.spydroid.resY", 0);
		fps = data.getIntExtra("net.majorkernelpanic.spydroid.fps", 0);
		br = data.getIntExtra("net.majorkernelpanic.spydroid.br", 0);
		
		updateSelection();
    	
    }
    
    public void onListItemClick(ListView l, View v, int position, long id) {
    	
    	Adapter a = listView.getAdapter();
    	Pattern p;
    	Matcher m;
    	
    	switch ((int )id) {
    	
    	// User changes resolution
    	case 0:
    		p = Pattern.compile("(\\d+)x(\\d+)");
    		m = p.matcher((String) a.getItem(position)); m.find();
    		resX = Integer.parseInt(m.group(1));
    		resY = Integer.parseInt(m.group(2));
    		break;
    		
    	// User changes framerate
    	case 1:
    		p = Pattern.compile("(\\d+)[^\\d]+");
    		m = p.matcher((String) a.getItem(position)); m.find();
    		fps = Integer.parseInt(m.group(1));
    		break;
    		
    	// User changes bitrate
    	case 2:
    		p = Pattern.compile("(\\d+)[^\\d]+");
    		m = p.matcher((String) a.getItem(position));
    		br = m.find()?Integer.parseInt(m.group(1)):DefaultBitRate;
    		break;
    	
    	}
    	
    	updateSelection();
    	
    	setResult(
    			1000,
    			new Intent().putExtra("net.majorkernelpanic.spydroid.resX", resX)
        			.putExtra("net.majorkernelpanic.spydroid.resY", resY)
        			.putExtra("net.majorkernelpanic.spydroid.fps", fps)
        			.putExtra("net.majorkernelpanic.spydroid.br", br)
    	);
    	
    	finish();

    }
	
    private void updateSelection() {
    
    	listView.clearChoices();
    	
		listView.setItemChecked(getId(resX+"x"+resY),true);
		listView.setItemChecked(getId(fps+" fps"),true);
		listView.setItemChecked(getId(br==DefaultBitRate?"Default":br+" kb/s"),true);
    	
    }
    
	private int getId(String s) {
		
		Adapter a = listView.getAdapter();
		
		for (int i=0;i<a.getCount();i++) {
			if (s.equals((String) a.getItem(i))) return i;
		}
		
		return -1;
		
	}
    
    
    private class CustomAdapter extends BaseAdapter {

    	private LayoutInflater inflater = null;
    	
    	private String [] headers;
    	private String [][] items;
    	private int count = 0;
    	
    	CustomAdapter(Context context, String[] headers, String[][] items) {
    		
    		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		this.headers = headers;
    		this.items = items;
    		
    		for (int i=0;i<items.length;i++) 
    			count+=items[i].length+1;
    		
    	}
    	
		@Override
		public int getCount() {
			return count;
		}

		@Override
		public Object getItem(int position) {
			int[] section = getSection(position);
			
			if (section[1] == 0) 
				return headers[section[0]];
			else 
				return items[section[0]][section[1]-1];

		}

		@Override
		public long getItemId(int position) {
			return (long) getSection(position)[0];
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View view;
			TextView text;
			
			int[] section = getSection(position);
			
			if (section[1]==0) {
				view = inflater.inflate(R.layout.quality_header, parent, false);
			}
			else {
				view = inflater.inflate(R.layout.quality_item, parent, false);
			}
			
			text = (TextView) view;
			text.setText( (String) getItem(position));
			
			return view;
		}

		private int[] getSection(int position) {
			
			int p = 0, s = 0, sum = 0;
			
			for (int i=0;i<items.length;i++) {
				if (position>=sum && position<sum+items[i].length+1) {
					s = i;
					p = position-sum;
					break;
				}
				sum += items[i].length+1;
			}
			
			return new int[] {s, p};
			
		}
		
    }



}
