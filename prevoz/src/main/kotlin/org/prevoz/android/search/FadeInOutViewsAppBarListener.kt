package org.prevoz.android.search

import android.support.design.widget.AppBarLayout
import android.view.View

/**
 * Fades in a view and fades out another as they scroll
 */
class FadeInOutViewsAppBarListener(val fadeOutViews: List<View>, val fadeInViews: List<View>) : AppBarLayout.OnOffsetChangedListener {

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        if (appBarLayout == null) return
        val ratio = -verticalOffset.toFloat() / appBarLayout.totalScrollRange.toFloat()

        for (fadeOutView in fadeOutViews) {
            fadeOutView.alpha = 1f - ratio
        }

        for (fadeInView in fadeInViews) {
            fadeInView.alpha = ratio
        }
    }
}