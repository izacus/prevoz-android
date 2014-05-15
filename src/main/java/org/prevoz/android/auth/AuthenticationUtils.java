package org.prevoz.android.auth;

import android.accounts.*;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.api.Scope;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;

import java.io.IOException;

@EBean(scope = Scope.Singleton)
public class AuthenticationUtils
{
    private static final String LOG_TAG = "Prevoz.Authentication";
    private final Context ctx;
    public AuthenticationUtils(Context ctx)
    {
        this.ctx = ctx;
    }

    private Account getUserAccount()
    {
        AccountManager am = AccountManager.get(ctx);
        Account[] accounts = am.getAccountsByType(ctx.getString(R.string.account_type));
        if (accounts.length > 0)
            return accounts[0];

        return null;
    }

    public boolean isAuthenticated()
    {
        return getUserAccount() != null;
    }

    public String getUsername()
    {
        Account acc = getUserAccount();
        return acc == null ? null : acc.name;
    }

    /**
     * This updates authentication cookie in retrofit at startup
     */
    public void updateRetrofitAuthenticationCookie()
    {
        Account acc = getUserAccount();
        if (acc == null)
            return;

        final AccountManager am = AccountManager.get(ctx);
        am.getAuthToken(acc, "default", null, false, new AccountManagerCallback<Bundle>()
        {
            @Override
            public void run(AccountManagerFuture<Bundle> future)
            {
                try
                {
                    Bundle b = future.getResult();
                    if (b == null)
                        return;

                    String cookies = b.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.d(LOG_TAG, "Updating authentication bearer to " + cookies);
                    ApiClient.setBearer(cookies);
                }
                catch (OperationCanceledException e)
                {
                    // Nothing TBD
                }
                catch (IOException e)
                {
                    // Nothing TBD
                }
                catch (AuthenticatorException e)
                {
                    // Nothing TBD
                }
            }

        }, null);
    }
}