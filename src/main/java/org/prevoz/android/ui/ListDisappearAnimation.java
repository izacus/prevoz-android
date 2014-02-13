package org.prevoz.android.ui;

import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ListView;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ListDisappearAnimation implements ViewTreeObserver.OnPreDrawListener
{
    private final StickyListHeadersListView view;

    public ListDisappearAnimation(StickyListHeadersListView view)
    {
        this.view = view;
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
            child.animate().translationY(300).alpha(0).setDuration(150).start();
        }

        return true;
    }
}
