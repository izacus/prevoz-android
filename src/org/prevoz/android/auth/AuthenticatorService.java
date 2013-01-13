package org.prevoz.android.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service
{
    @Override
    public IBinder onBind(Intent intent)
    {
        return new Authenticator(this).getIBinder();
    }
}
