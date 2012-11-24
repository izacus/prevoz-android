package org.prevoz.android.rideinfo;

import java.text.SimpleDateFormat;

import org.prevoz.android.R;
import org.prevoz.android.util.LocaleUtil;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Paint;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RideInfoUtil
{
	private Activity context;
	
	
	private TextView fromText;
	private TextView toText;
	private TextView timeText;
	private TextView dayText;
	private TextView dateText;
	private TextView priceText;
	private TextView pplText;
	private TextView pplTagText;
	private TextView driverText;
	private TextView contactText;
	private TextView commentText;
	private Button callButton;
	private Button smsButton;
	private Button genButton;

	private OnClickListener callAuthor;
	private OnClickListener sendSMS;
	private String genButtonText;
	private OnClickListener genButtonListener;


	private TextView insuranceText;
	
	public RideInfoUtil(Activity context, String buttonText, OnClickListener buttonListener)
	{
		this.context = context;
		this.genButtonText = buttonText;
		this.genButtonListener = buttonListener;
		prepareUIElements();
	}
	
	public RideInfoUtil(Activity context, OnClickListener callAuthor, OnClickListener sendSMS, String buttonText, OnClickListener buttonListener)
	{
		this.context = context;
		this.callAuthor = callAuthor;
		this.sendSMS = sendSMS;
		this.genButtonText = buttonText;
		this.genButtonListener = buttonListener;
		
		prepareUIElements();
	}
	
	private void prepareUIElements()
	{
		// UI fields
		fromText = (TextView) context.findViewById(R.id.rideinfo_from);
		toText = (TextView) context.findViewById(R.id.rideinfo_to);
		timeText = (TextView) context.findViewById(R.id.rideinfo_time);
		dayText = (TextView) context.findViewById(R.id.rideinfo_day);
		dateText = (TextView) context.findViewById(R.id.rideinfo_date);
		priceText = (TextView) context.findViewById(R.id.rideinfo_price);
		pplText = (TextView) context.findViewById(R.id.rideinfo_people);
		pplTagText = (TextView) context.findViewById(R.id.rideinfo_peopletag);
		driverText = (TextView) context.findViewById(R.id.rideinfo_author);
		contactText = (TextView) context.findViewById(R.id.rideinfo_phone);
		commentText = (TextView) context.findViewById(R.id.rideinfo_comment);
		insuranceText = (TextView) context.findViewById(R.id.rideinfo_insurance);
		
		callButton = (Button)context.findViewById(R.id.rideinfo_call);
		smsButton = (Button)context.findViewById(R.id.rideinfo_sms);
		genButton = (Button) context.findViewById(R.id.rideinfo_delsend);
	}
	
	public void showPeople(Resources res, Ride ride)
	{
		pplText.setText(String.valueOf(ride.getPeople()));
		if (ride.isFull())
		{
			pplText.setPaintFlags(pplText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
		}
		else
		{
			pplText.setPaintFlags(pplText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
		}
			
		pplTagText.setText(LocaleUtil.getStringNumberForm(res, R.array.people_tags, ride.getPeople()));
	}
	
	public void showRide(Ride ride, boolean showControls)
	{
		Resources res = context.getResources();
		
		// From and to
		fromText.setText(ride.getFrom());
		toText.setText(ride.getTo());

		// Time and date
		SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");
		timeText.setText(timeFormatter.format(ride.getTime()));
		dayText.setText(LocaleUtil.getDayName(res, ride.getTime()) + ",");
		dateText.setText(LocaleUtil.getFormattedDate(res, ride.getTime()));

		// Price and number of people
		if (ride.getPrice() != null)
		{
			priceText.setText(String.format(LocaleUtil.getLocale(), "%1.1f €", ride.getPrice()));
		}
		else
		{
			priceText.setText("?");
		}

		showPeople(res, ride);

		// Driver and contact		
		if (ride.getAuthor() == null || ride.getAuthor().trim().length() == 0)
		{
			driverText.setVisibility(View.GONE);
		}
		else
		{
			driverText.setVisibility(View.VISIBLE);
			driverText.setText(ride.getAuthor());
		}

		// Contact info
		contactText.setText(ride.getContact());

		// Comment
		commentText.setText(ride.getComment());
		
		// Insurance
		if (ride.isInsured())
		{
			insuranceText.setText(R.string.driver_has_insurance);
		}
		else
		{
			insuranceText.setText(R.string.driver_hasnt_insurance);
		}
		
		// Setup button callbacks
		if (showControls)
		{
			smsButton.setOnClickListener(sendSMS);
			
			if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
			{
				callButton.setOnClickListener(callAuthor);
			}
			else
			{
				smsButton.setText(context.getString(R.string.add_to_contacts));
				callButton.setVisibility(View.GONE);
			}
		}
		else
		{
			callButton.setVisibility(View.INVISIBLE);
			smsButton.setVisibility(View.INVISIBLE);
		}
		
		if (genButtonText != null && genButtonListener != null)
		{
			genButton.setText(genButtonText);
			genButton.setOnClickListener(genButtonListener);
			genButton.setVisibility(View.VISIBLE);
		}
		else
		{
			genButton.setVisibility(View.GONE);
		}
	}
}