package org.prevoz.android.util;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.prevoz.android.ApplicationComponent;
import org.prevoz.android.PrevozApplication;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.push.PushManager;

import javax.inject.Inject;

public class PrevozActivity extends ActionBarActivity {

    @Inject protected AuthenticationUtils authUtils;
    @Inject protected PushManager pushManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getApplicationComponent().inject(this);
    }

    public ApplicationComponent getApplicationComponent() {
        return ((PrevozApplication)getApplication()).component();
    }
}
