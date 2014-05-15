package org.prevoz.android.auth;

import android.accounts.*;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.google.api.client.auth.oauth2.*;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.store.DataStoreFactory;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestAccountStatus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@EActivity
public class LoginActivity extends SherlockFragmentActivity
{
    private static final String LOG_TAG = "Prevoz.Login";
    private static final String CLIENT_ID = "b89d13d3b102d84963bb";
    private static final String CLIENT_SECRET = "d94e76ff9086e1fe428519b6aed6dbe65adde616";
    private static final String REDIRECT_URL = "http://app.local/login_done/";

    private AccountAuthenticatorResponse authenticatorResponse;
    private Bundle authenticatorResult;
    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        authenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (authenticatorResponse != null)
            authenticatorResponse.onRequestContinued();

        CookieManager.getInstance().removeAllCookie();

        webview = new WebView(this);
        webview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(webview);

        webview.setWebViewClient(new WebViewController());
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);

        // Generate OAuth login URL
        List<String> responseTypes = new ArrayList<String>();
        responseTypes.add("code");
        String authenticationUrl = new AuthorizationRequestUrl("https://prevoz.org/oauth2/authorize/",
                                                                CLIENT_ID,
                                                                responseTypes)
                                                                .setRedirectUri(REDIRECT_URL)
                                                                .build();


        webview.loadUrl(authenticationUrl);
        setSupportProgressBarIndeterminate(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    private void getAccountUsernameAndApiKey(final String code)
    {
        final ProgressDialog dialog = ProgressDialog.show(this, "Prijava", "Prijavljam....", true, false);
        requestAccessToken(dialog, code);
    }

    @Background
    protected void requestAccessToken(final ProgressDialog dialog, String code)
    {
        final NetHttpTransport transport = new NetHttpTransport();
        final AndroidJsonFactory jsonFactory = new AndroidJsonFactory();
        final ClientParametersAuthentication authentication = new ClientParametersAuthentication(CLIENT_ID, CLIENT_SECRET);

        AuthorizationCodeTokenRequest tokenRequest = new AuthorizationCodeTokenRequest(transport,
                                                                                       jsonFactory,
                                                                                       new GenericUrl("https://prevoz.org/oauth2/access_token/"),
                                                                                       code);

        tokenRequest.setClientAuthentication(authentication);

        try
        {
            final TokenResponse retrievedToken = tokenRequest.execute();
            ApiClient.setBearer(retrievedToken.getAccessToken());

            ApiClient.getAdapter().getAccountStatus(new Callback<RestAccountStatus>()
            {
                @Override
                public void success(RestAccountStatus restAccountStatus, Response response)
                {
                    updateAuthenticatorResult(restAccountStatus, retrievedToken.getAccessToken(), retrievedToken.getRefreshToken());
                    UpdateAccountInformationTask updateInfoTask = new UpdateAccountInformationTask(dialog, restAccountStatus, retrievedToken.getAccessToken(), retrievedToken.getRefreshToken());
                    updateInfoTask.execute();
                }

                @Override
                public void failure(RetrofitError retrofitError)
                {
                    Log.e(LOG_TAG, "Failed to login: " + retrofitError);
                    dialog.dismiss();
                }
            });
        }
        catch (IOException e)
        {
            final Bundle result = new Bundle();
            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_AUTHENTICATION);
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "Failed to confirm authentication.");
            authenticatorResult = result;
            finish();
        }
    }

    private void updateAuthenticatorResult(RestAccountStatus restAccountStatus, String accessToken, String refreshToken)
    {
        if (authenticatorResponse != null)
        {
            final Bundle result = new Bundle();
            if (restAccountStatus.isAuthenticated)
            {
                result.putString(AccountManager.KEY_ACCOUNT_NAME, restAccountStatus.username);
                result.putString(AccountManager.KEY_PASSWORD, refreshToken);
                result.putString(AccountManager.KEY_AUTHTOKEN, accessToken);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
            }
            else
            {
                result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_AUTHENTICATION);
                result.putString(AccountManager.KEY_ERROR_MESSAGE, "Failed to confirm authentication.");
            }

            authenticatorResult = result;
        }
    }

    private class UpdateAccountInformationTask extends AsyncTask<Void, Void, Void>
    {
        private final ProgressDialog dialog;
        private final RestAccountStatus status;
        private final String accessToken;
        private final String refreshToken;

        public UpdateAccountInformationTask(ProgressDialog dialog, RestAccountStatus status, String accessToken, String refreshToken)
        {
            this.dialog = dialog;
            this.status = status;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }


        @Override
        protected Void doInBackground(Void... params)
        {
            // Try to find existing account
            AccountManager am = AccountManager.get(LoginActivity.this);

            // Remove possible existing accounts
            Account[] accounts = am.getAccountsByType(getString(R.string.account_type));
            for (Account acc : accounts)
            {
                AccountManagerFuture<Boolean> future = am.removeAccount(acc, null, null);
                try
                {
                    future.getResult();
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


            Account acc = new Account(status.username, getString(R.string.account_type));
            am.addAccountExplicitly(acc, refreshToken, null);
            am.setAuthToken(acc, "default", accessToken);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            dialog.dismiss();
            finish();
        }
    }


    private class WebViewController extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            Log.d(LOG_TAG, "Loading " + url);

            if (url.startsWith(REDIRECT_URL))
            {
                AuthorizationCodeResponseUrl authorizationCodeResponseUrl = new AuthorizationCodeResponseUrl(url);
                // TODO: Error handling.
                getAccountUsernameAndApiKey(authorizationCodeResponseUrl.getCode());
                return true;
            }

            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            super.onPageStarted(view, url, favicon);
            setSupportProgressBarIndeterminateVisibility(true);
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            setSupportProgressBarIndeterminateVisibility(false);
        }
    }

    @Override
    public void onBackPressed()
    {
        if (webview.canGoBack())
            webview.goBack();
        else
            super.onBackPressed();
    }

    @Override
    public void finish()
    {
        if (authenticatorResponse != null)
        {
            if (authenticatorResult != null)
                authenticatorResponse.onResult(authenticatorResult);
            else
                authenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "Cancelled.");
        }


        CookieManager.getInstance().removeAllCookie();
        super.finish();
    }
}
