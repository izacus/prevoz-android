package org.prevoz.android.ui;

import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ListView;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ListFlyupAnimator implements ViewTreeObserver.OnPreDrawListener {
    private final StickyListHeadersListView view;

    public ListFlyupAnimator(StickyListHeadersListView view)
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

        int delay = 0;

        for (int i = 1; i < view.getChildCount(); i++ )
        {
            final View child = view.getChildAt(i);
            child.setTranslationY(300);
            child.setAlpha(0);
            child.animate().translationY(0).alpha(1).setStartDelay(delay).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
            delay += 50;
        }

        return true;
    }
}
