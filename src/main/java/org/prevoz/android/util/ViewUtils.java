package org.prevoz.android.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.TextView;

import org.prevoz.android.R;

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
}
