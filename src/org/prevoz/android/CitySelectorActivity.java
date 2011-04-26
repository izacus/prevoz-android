package org.prevoz.android;

import org.prevoz.android.util.Database;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class CitySelectorActivity extends FragmentActivity implements TextWatcher, OnItemClickListener, OnEditorActionListener
{
	private SQLiteDatabase database = null;	
	private ListView cityList;
	private EditText cityText;
	private ImageButton gpsButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.city_selector_activity);
		
		// Disable status bar in landscape layout
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		// Prepare view items
		cityList = (ListView) findViewById(R.id.city_list);
		cityList.setOnItemClickListener(this);
		cityText = (EditText) findViewById(R.id.city_text);
		cityText.addTextChangedListener(this);
		cityText.setOnEditorActionListener(this);
		cityText.requestFocus();
		
		gpsButton = (ImageButton)findViewById(R.id.gps_button);
		gpsButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				fillInCurrentLocation(cityText);
			}
		});
		
		// Prepare database connection
		database = Database.getSettingsDatabase(this);
		populateCityList("");
	}
	
	
	private void populateCityList(String pattern)
	{
		final String[] column = { "name" };
		final int[] ids = { android.R.id.text1 };
		
		Cursor cities = Database.getCitiesStartingWith(database, pattern);
		startManagingCursor(cities);
		
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, 
															  android.R.layout.simple_list_item_1, 
															  cities, 
															  column, 
															  ids);
		cityList.setAdapter(adapter);

		Log.d(this.toString(), "Populating city list with pattern " + pattern);
	}

	private void fillInCurrentLocation(final TextView view)
	{
		view.setEnabled(false);
		
		final String currentText = view.getText().toString();
		final String currentHint = view.getHint().toString();
		
		view.setText("");
		view.setHint(getResources().getString(R.string.search_gps_locating));
		
		// Determine location
		final GPSManager gpsManager = new GPSManager();
		Handler callback = new Handler()
		{
			@Override
			public void handleMessage(Message msg) 
			{
				view.setHint(currentHint);
				
				if (msg.what == GPSManager.GPS_PROVIDER_UNAVALABLE)
				{
					Toast.makeText(CitySelectorActivity.this, R.string.search_gps_error, Toast.LENGTH_SHORT).show();
					// Restore old entry
					view.setText(currentText);
					return;
				}
				
				view.setText(gpsManager.getCurrentCity());
				view.requestFocus();
			}
		};
		
		gpsManager.findCurrentCity(this, callback);
	}
	

	public void afterTextChanged(Editable s) 
	{
		// Nothing TBD
	}


	public void beforeTextChanged(CharSequence s, 
								  int start, 
								  int count,
								  int after) 
	{
		// Nothing TBD
	}


	public void onTextChanged(CharSequence s, int start, int before, int count) 
	{
		populateCityList(s.toString());
	}


	public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	{
		String name = ((TextView)view.findViewById(android.R.id.text1)).getText().toString();
		returnCity(name);
	}

	public boolean onEditorAction(TextView v, 
								  int actionId, 
								  KeyEvent event) 
	{
		if (cityList.getCount() > 1)
		{
			setResult(RESULT_CANCELED);
			finish();
		}
		else
		{
			String name = ((TextView)cityList.getChildAt(0)).getText().toString();
			returnCity(name);
		}
		
		return false;
	}
	

	@Override
	protected void onStop() 
	{
		super.onStop();
		database.close();
	}


	private void returnCity(String name) {
		Intent result = new Intent();
		result.putExtra("city", name);
		
		setResult(RESULT_OK, result);
		finish();
	}	
}
