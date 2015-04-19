package org.prevoz.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.events.Events;

import java.io.IOException;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

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

    public void requestAuthentication(Activity parentActivity, int requestCode)
    {
        Observable.defer(() -> {
            AccountManager am = AccountManager.get(parentActivity);
            AccountManagerFuture<Bundle> result = am.addAccount(parentActivity.getString(R.string.account_type), null, null, null, null, null, null);

            try {
                return Observable.just(result.getResult());
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                throw OnErrorThrowable.from(e);
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Bundle>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Crashlytics.logException(e);
                Log.e(LOG_TAG, "Something went very wrong when trying to authenticate!", e);
            }

            @Override
            public void onNext(Bundle bundle) {
                Intent i = (Intent) bundle.get(AccountManager.KEY_INTENT);
                parentActivity.startActivityForResult(i, requestCode);
            }
        });
    }

    public boolean isAuthenticated()
    {
        return getUserAccount() != null;
    }

    public Observable<String> getUsername()
    {
        return Observable.defer(() -> {
            Account acc = getUserAccount();
            return Observable.just(acc == null ? null : acc.name);
        });
    }

    public void removeExistingAccounts()
    {
        AccountManager am = AccountManager.get(ctx);
        Account[] accounts = am.getAccountsByType(ctx.getString(R.string.account_type));
        for (Account acc : accounts)
        {
            AccountManagerFuture<Boolean> future = am.removeAccount(acc, null, null);
            try
            {
                future.getResult();
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                // Nothing TBD
            }
        }
    }

    /**
     * This updates authentication cookie in retrofit at startup
     */
    public void updateRetrofitAuthenticationCookie()
    {
        // Check if the app was migrated to OAuth2
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (!preferences.getBoolean(PrevozAccountAuthenticator.PREF_OAUTH2, false))
        {
            removeExistingAccounts();
            return;
        }

        Account acc = getUserAccount();
        if (acc == null)
            return;

        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>()
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
                    ApiClient.setBearer(cookies);
                }
                catch (OperationCanceledException | IOException | AuthenticatorException e)
                {
                    // Nothing TBD
                }
            }

        };

        // Check for expiry
        final AccountManager am = AccountManager.get(ctx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            am.getAuthToken(acc, "default", null, false, callback, null);
        }
        else
        {
            am.getAuthToken(acc, "default", false, callback, null);
        }
    }

    public Observable<Void> logout()
    {
        return Observable.defer(() -> {
            removeExistingAccounts();
            ApiClient.setBearer(null);
            EventBus.getDefault().post(new Events.LoginStateChanged());
            return Observable.empty();
        });
    }
}
