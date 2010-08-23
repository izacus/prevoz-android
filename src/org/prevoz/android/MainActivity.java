package org.prevoz.android;

import org.prevoz.android.search.SearchActivity;
import org.prevoz.android.util.TabsUtil;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class MainActivity extends TabActivity 
{
    private TabHost tabHost;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        
        // Prepare tabs for use
        initTabs();
    }
    
    private void initTabs()
    {
	tabHost = getTabHost();
	
	Intent searchIntent = new Intent(this, SearchActivity.class);
	TabsUtil.addNativeLookingTab(this, tabHost, "search", getString(R.string.search_tab), R.drawable.friends_tab, searchIntent);
	tabHost.setCurrentTab(0);
    }
}