package org.prevoz.android.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.TextView;

import org.prevoz.android.R;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ViewUtils
{
    private static Style confirmStyle = null;

    public static void setupEmptyView(StickyListHeadersListView listView, View emptyView, String text) {
        TextView textView = (TextView) emptyView.findViewById(R.id.empty_text);
        textView.setText(text);
        listView.setEmptyView(emptyView);
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

        if (confirmStyle == null)
            confirmStyle = new Style.Builder().setBackgroundColor(R.color.prevoztheme_color).build();

        Crouton.makeText(context, messageText, failure ? Style.ALERT : confirmStyle).show();
    }
}
