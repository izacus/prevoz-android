package org.prevoz.android.util;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

import org.prevoz.android.ApplicationComponent;
import org.prevoz.android.PrevozApplication;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.push.PushManager;

import java.util.Locale;

import javax.inject.Inject;

@SuppressLint("Registered")
public class PrevozActivity extends AppCompatActivity {

    @Inject protected AuthenticationUtils authUtils;
    @Inject protected PushManager pushManager;
    @Inject protected PrevozDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleUtil.checkSetLocale(this, getResources().getConfiguration());
        super.onCreate(savedInstanceState);
        getApplicationComponent().inject(this);
    }

    public ApplicationComponent getApplicationComponent() {
        return ((PrevozApplication)getApplication()).component();
    }
}
