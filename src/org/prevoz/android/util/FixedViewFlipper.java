package org.prevoz.android.util;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ViewFlipper;

public class FixedViewFlipper extends ViewFlipper
{

	public FixedViewFlipper(Context context)
	{
		super(context);
	}

	public FixedViewFlipper(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		if (Build.VERSION.SDK_INT >= 7)
		{
			try
			{
				super.onDetachedFromWindow();
			}
			catch (IllegalArgumentException e)
			{
				Log.w(this.toString(),
					  "Android issue 6191 workaround triggered.");
			}
			finally
			{
				super.stopFlipping();
			}
		}
		else
		{
			super.onDetachedFromWindow();
		}
	}
}
