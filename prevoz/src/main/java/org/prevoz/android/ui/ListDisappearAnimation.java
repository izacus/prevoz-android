package org.prevoz.android.ui;

import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.ListView;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ListDisappearAnimation implements ViewTreeObserver.OnPreDrawListener
{
    private final ListView view;

    public ListDisappearAnimation(StickyListHeadersListView view)
    {
        this.view = view.getWrappedList();
    }

    public void animate()
    {
        view.getViewTreeObserver().addOnPreDrawListener(this);
    }

    @Override
    public boolean onPreDraw()
    {
        view.getViewTreeObserver().removeOnPreDrawListener(this);
        for (int i = 1; i < view.getChildCount(); i++ )
        {
            final View child = view.getChildAt(i);
            ViewCompat.setLayerType(child, ViewCompat.LAYER_TYPE_HARDWARE, null);
            int position = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200.0f, view.getResources().getDisplayMetrics());
            ViewCompat.animate(child).withLayer().translationY(position).alpha(0).setDuration(150).setInterpolator(new AccelerateInterpolator());
        }

        return true;
    }
}
