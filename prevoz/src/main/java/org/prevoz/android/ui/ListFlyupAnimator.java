package org.prevoz.android.ui;

import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ListView;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ListFlyupAnimator implements ViewTreeObserver.OnPreDrawListener
{
    private static final DecelerateInterpolator interpolator = new DecelerateInterpolator();

    private final ListView view;

    public ListFlyupAnimator(StickyListHeadersListView view)
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

        int delay = 0;

        for (int i = 1; i < view.getChildCount(); i++ )
        {
            final View child = view.getChildAt(i);
            int position = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200.0f, view.getResources().getDisplayMetrics());
            child.setTranslationY(position);
            child.setAlpha(0);
            ViewCompat.animate(child).withLayer().translationY(0f).alpha(1.0f).setStartDelay(delay).setDuration(300).setInterpolator(interpolator);
            delay += 50;
        }

        return true;
    }
}
