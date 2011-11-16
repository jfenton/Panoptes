package com.jfenton.panoptes;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class PanoptesTabWidget extends TabActivity {
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);

	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    Intent mapIntent = new Intent().setClass(this, PanoptesMapActivity.class);
	    Intent settingsIntent = new Intent().setClass(this, PanoptesSettingsActivity.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("map").setIndicator("Map", res.getDrawable(R.drawable.greendot)).setContent(mapIntent);
	    tabHost.addTab(spec);

	    spec = tabHost.newTabSpec("settings").setIndicator("Settings", res.getDrawable(R.drawable.greendot)).setContent(settingsIntent);
	    tabHost.addTab(spec);
	    
	    tabHost.setCurrentTab(1);
	}
}
