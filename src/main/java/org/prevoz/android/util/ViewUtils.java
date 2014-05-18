package org.prevoz.android.util;

import android.view.View;
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
}
