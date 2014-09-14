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

public class ViewUtils
{
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
        Crouton.makeText(context, messageText, failure ? Style.ALERT : Style.CONFIRM).show();
    }
}
