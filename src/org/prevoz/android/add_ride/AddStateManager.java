package org.prevoz.android.add_ride;

import android.widget.ViewFlipper;

public class AddStateManager
{
	public static enum Views
	{
		LOADING,
		FORM,
		PREVIEW
	};
	
	private ViewFlipper flipper;
	private Views currentView;
	
	public AddStateManager(ViewFlipper viewFlipper)
	{
		this.flipper = viewFlipper;
	}
	
	public void showView(Views view)
	{
		switch(view)
		{
			case FORM:
				flipper.setDisplayedChild(1);
				break;
			case LOADING:
				flipper.setDisplayedChild(0);
				break;
			case PREVIEW:
				flipper.setDisplayedChild(2);
				break;
			default:
				return;
		}
		
		currentView = view;
	}
	
	public boolean handleBackKey()
	{
		if (currentView == Views.PREVIEW)
		{
			showView(Views.FORM);
			return true;
		}
		
		return false;
	}
};
