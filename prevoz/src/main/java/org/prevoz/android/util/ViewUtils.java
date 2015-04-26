package org.prevoz.android.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import org.prevoz.android.R;

public class ViewUtils
{
    private static Typeface snackbarTypeface;

    public static void setupEmptyView(RecyclerView listView, View emptyView, String text) {
        TextView textView = (TextView) emptyView.findViewById(R.id.empty_text);
        textView.setText(text);
        emptyView.setVisibility(View.VISIBLE);
    }

    public static void setupEmptyView(ListView listView, View emptyView, String text)
    {
        TextView textView = (TextView) emptyView.findViewById(R.id.empty_text);
        textView.setText(text);
        listView.setEmptyView(emptyView);
    }

    public static void hideKeyboard(Activity ctx)
    {
        if (ctx == null) return;

        InputMethodManager inputManager = (InputMethodManager)ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = ctx.getCurrentFocus();
        if (currentFocus != null)
            inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static void showMessage(Activity context, int messageTextResId, boolean failure) {
        showMessage(context, context.getResources().getString(messageTextResId), failure);
    }

    public static void showMessage(Activity context, String messageText, boolean failure) {
        if (context == null) return;

        if (snackbarTypeface == null)
            snackbarTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);

        Snackbar snackbar = Snackbar.with(context)
                                    .text(messageText)
                                    .textTypeface(snackbarTypeface)
                                    .color(failure ? Color.RED : context.getResources().getColor(R.color.prevoztheme_color_dark))
                                    .duration(Snackbar.SnackbarDuration.LENGTH_SHORT);

        SnackbarManager.show(snackbar, context);
    }
}
