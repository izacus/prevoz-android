package org.prevoz.android.auth;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestAccountStatus;
import org.prevoz.android.api.rest.RestApiKey;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;

public class PrevozAccountAuthenticator extends AbstractAccountAuthenticator
{
    private static final String LOG_TAG = "Prevoz.Authenticator";
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
        final Intent loginActivityIntent = new Intent(ctx, LoginActivity_.class);
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
        String apiKey = am.getPassword(account);

        if (apiKey != null)
        {
            Log.d(LOG_TAG, "Attempting authentication with apikey " + apiKey);
            ApiClient.getAdapter().loginWithApiKey(new RestApiKey(apiKey), new Callback<RestAccountStatus>()
            {
                @Override
                public void success(RestAccountStatus restAccountStatus, Response response)
                {
                    String cookies = null;
                    for (Header h : response.getHeaders())
                    {
                        if ("Set-Cookie".equals(h.getName()))
                        {
                            cookies = h.getValue();
                            break;
                        }
                    }

                    if (cookies == null)
                    {
                        authenticatorResponse.onResult(getLoginIntentBundle(authenticatorResponse));
                        return;
                    }

                    Bundle b = new Bundle();
                    b.putString(AccountManager.KEY_ACCOUNT_NAME, restAccountStatus.username);
                    b.putString(AccountManager.KEY_ACCOUNT_TYPE, ctx.getString(R.string.account_type));
                    b.putString(AccountManager.KEY_AUTHTOKEN, cookies);
                    authenticatorResponse.onResult(b);
                }

                @Override
                public void failure(RetrofitError retrofitError)
                {
                    Bundle b = new Bundle();
                    b.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR);
                    b.putString(AccountManager.KEY_ERROR_MESSAGE, retrofitError.getMessage());
                    authenticatorResponse.onResult(b);
                }

            });

            return null;
        }

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
        final Intent loginActivityIntent = new Intent(ctx, LoginActivity_.class);
        loginActivityIntent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        result.putParcelable(AccountManager.KEY_INTENT, loginActivityIntent);

        return result;
    }
}
