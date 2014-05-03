package org.prevoz.android.auth;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticationService extends Service
{
    @Override
    public IBinder onBind(Intent intent)
    {
        if (AccountManager.ACTION_AUTHENTICATOR_INTENT.equals(intent.getAction()))
            return new PrevozAccountAuthenticator(this).getIBinder();

        return null;
    }
}
