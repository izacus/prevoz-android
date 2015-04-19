package org.prevoz.android.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class PrevozAccountAuthenticator extends AbstractAccountAuthenticator
{
    private static final String LOG_TAG = "Prevoz.Authenticator";

    public static final String PREF_KEY_EXPIRES = "oauth2.token_expires";
    public static final String PREF_OAUTH2 = "oauth2";

    private final Context ctx;

    public PrevozAccountAuthenticator(Context context)
    {
        super(context);
        this.ctx = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType)
    {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                                                                    String authTokenType,
                                                                    String[] requiredFeatures,
                                                                    Bundle options) throws NetworkErrorException
    {
        Log.d(LOG_TAG, "AddAccount.");

        final Bundle result = new Bundle();
        final Intent loginActivityIntent = new Intent(ctx, LoginActivity.class);
        loginActivityIntent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        result.putParcelable(AccountManager.KEY_INTENT, loginActivityIntent);
        return result;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                     Account account,
                                     Bundle options) throws NetworkErrorException
    {
        Log.d(LOG_TAG, "ConfirmCredentials.");
        return null;
    }

    @Override
    public Bundle getAuthToken(final AccountAuthenticatorResponse authenticatorResponse,
                               Account account,
                               String authTokenType,
                               Bundle options) throws NetworkErrorException
    {
        AccountManager am = AccountManager.get(ctx);
        return getLoginIntentBundle(authenticatorResponse);
    }

    @Override
    public String getAuthTokenLabel(String authTokenType)
    {
        Log.d(LOG_TAG, "GetAuthTokenLabel.");

        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account,
                                    String authTokenType,
                                    Bundle options) throws NetworkErrorException
    {
        Log.d(LOG_TAG, "UpdateCredentials.");

        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
                              Account account,
                              String[] features) throws NetworkErrorException
    {
        Log.d(LOG_TAG, "HasFeatures.");
        return null;
    }

    private Bundle getLoginIntentBundle(AccountAuthenticatorResponse response)
    {
        final Bundle result = new Bundle();
        final Intent loginActivityIntent = new Intent(ctx, LoginActivity.class);
        loginActivityIntent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        result.putParcelable(AccountManager.KEY_INTENT, loginActivityIntent);

        return result;
    }
}
