package org.prevoz.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestAccountStatus;

/**
* ${FILE_NAME}
* <p/>
* Created on 25/05/14
*/
class UpdateAccountInformationTask extends AsyncTask<Void, Void, Void>
{
    private final ProgressDialog dialog;
    private final RestAccountStatus status;
    private final String accessToken;
    private final String refreshToken;
    private final long expires;
    private final Activity context;
    private final AuthenticationUtils authUtils;

    public UpdateAccountInformationTask(Activity context,
                                        AuthenticationUtils authUtils,
                                        ProgressDialog dialog,
                                        RestAccountStatus status,
                                        String accessToken,
                                        String refreshToken,
                                        long expires)
    {
        this.context = context;
        this.authUtils = authUtils;
        this.dialog = dialog;
        this.status = status;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expires = expires;
    }


    @Override
    protected Void doInBackground(Void... params)
    {
        // Try to find existing account
        AccountManager am = AccountManager.get(context);
        authUtils.removeExistingAccounts();
        Account acc = new Account(status.username, context.getString(R.string.account_type));
        am.addAccountExplicitly(acc, refreshToken, null);
        am.setAuthToken(acc, "default", accessToken);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PrevozAccountAuthenticator.PREF_OAUTH2, true)
                 .putLong(PrevozAccountAuthenticator.PREF_KEY_EXPIRES, expires)
                 .apply();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid)
    {
        super.onPostExecute(aVoid);
        dialog.dismiss();
        context.finish();
    }
}
