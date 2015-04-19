package org.prevoz.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.push.PushManager;
import org.prevoz.android.util.PrevozActivity;

import javax.inject.Inject;

public class PrevozFragment extends Fragment {

    @Inject protected AuthenticationUtils authUtils;
    @Inject protected PushManager pushManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((PrevozActivity)getActivity()).getApplicationComponent().inject(this);
    }
}
