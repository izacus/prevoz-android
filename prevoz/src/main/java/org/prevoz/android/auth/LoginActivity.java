package org.prevoz.android.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crashlytics.android.Crashlytics;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestAccountStatus;
import org.prevoz.android.api.rest.RestAuthTokenResponse;
import org.prevoz.android.events.Events;
import org.prevoz.android.util.PrevozActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class LoginActivity extends PrevozActivity
{
    private static final String LOG_TAG = "Prevoz.Login";
    private static final String CLIENT_ID = "b89d13d3b102d84963bb";
    private static final String CLIENT_SECRET = "d94e76ff9086e1fe428519b6aed6dbe65adde616";
    private static final String REDIRECT_URL = "http://app.local/login_done/";

    private AccountAuthenticatorResponse authenticatorResponse;
    private Bundle authenticatorResult;

    @InjectView(R.id.toolbar)
    protected Toolbar toolbar;

    @InjectView(R.id.login_webview)
    protected WebView webview;

    private boolean tokenRequestInProgress = false; // Workaround for Android 2.3

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getApplicationComponent().inject(this);
        ButterKnife.inject(this);

        CookieSyncManager.createInstance(this);
        authenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (authenticatorResponse != null)
            authenticatorResponse.onRequestContinued();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        webview.setWebViewClient(new WebViewController());
        webview.setVisibility(View.VISIBLE);
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAppCachePath(getCacheDir().getAbsolutePath());
        settings.setAppCacheEnabled(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            settings.setSavePassword(false);
            settings.setPluginState(WebSettings.PluginState.OFF);
        }

        settings.setDefaultTextEncodingName("UTF-8");
        settings.setGeolocationEnabled(false);
        settings.setAllowFileAccess(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
            settings.setAllowContentAccess(false);
        }

        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        // Generate OAuth login URL
        String authenticationUrl = null;

        try
        {
            authenticationUrl = ApiClient.BASE_URL + String.format("/oauth2/authorize/%s/code/?client_id=%s&response_type=code&redirect_uri=%s", CLIENT_ID, CLIENT_ID, URLEncoder.encode(REDIRECT_URL, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            Crashlytics.logException(e);
        }


        CookieManager.getInstance().removeAllCookie();
        Log.d(LOG_TAG, "Opening login at " + authenticationUrl);
        webview.loadUrl(authenticationUrl);
        setSupportProgressBarIndeterminate(true);
    }

    private void getAccountUsernameAndApiKey(final String code)
    {
        final ProgressDialog dialog = ProgressDialog.show(this, "Prijava", "Prijavljam....", true, false);
        webview.setVisibility(View.INVISIBLE);

        Observable<RestAuthTokenResponse> accessToken = ApiClient.getAdapter().getAccessToken("authorization_code", CLIENT_ID, CLIENT_SECRET, code, REDIRECT_URL)
                                                        .cache();

        Observable<RestAccountStatus> accountStatus = accessToken.flatMap(restAuthTokenResponse -> {
            ApiClient.setBearer(restAuthTokenResponse.accessToken);
            return ApiClient.getAdapter().getAccountStatus();
        });

        Observable.zip(accessToken, accountStatus, (token, status) -> new Pair<>(token, status))
                  .doOnNext(authInfoPair -> {
                      updateAuthenticatorResult(authInfoPair.second, authInfoPair.first.accessToken, authInfoPair.first.refreshToken);
                      AccountManager am = AccountManager.get(LoginActivity.this);
                      authUtils.removeExistingAccounts();
                      Account acc = new Account(authInfoPair.second.username, getString(R.string.account_type));
                      am.addAccountExplicitly(acc, authInfoPair.first.refreshToken, null);
                      am.setAuthToken(acc, "default", authInfoPair.first.accessToken);

                      SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                      sp.edit().putBoolean(PrevozAccountAuthenticator.PREF_OAUTH2, true)
                              .putLong(PrevozAccountAuthenticator.PREF_KEY_EXPIRES, System.currentTimeMillis() + (authInfoPair.first.expiresIn * 1000))
                              .apply();
                  })
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(authInfoPair -> {},
                          throwable -> {
                              Crashlytics.logException(throwable.getCause());
                              ApiClient.setBearer(null);
                              final Bundle result = new Bundle();
                              result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_AUTHENTICATION);
                              result.putString(AccountManager.KEY_ERROR_MESSAGE, "Failed to confirm authentication.");
                              authenticatorResult = result;
                          },
                          () -> {
                              EventBus.getDefault().postSticky(new Events.LoginStateChanged());
                              dialog.dismiss();
                              finish();
                          });

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

    private class WebViewController extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            Log.d(LOG_TAG, "Loading " + url);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1 && url.startsWith(REDIRECT_URL))
            {
                Uri uri = Uri.parse(url);
                String code = uri.getQueryParameter("code");
                // TODO: Error handling.
                getAccountUsernameAndApiKey(code);
                return true;
            }

            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1 && url.startsWith(REDIRECT_URL))
            {
                if (tokenRequestInProgress)
                    return;

                tokenRequestInProgress = true;
                Uri uri = Uri.parse(url);
                String code = uri.getQueryParameter("code");
                // TODO: Error handling.
                getAccountUsernameAndApiKey(code);
            }
            else
            {
                super.onPageStarted(view, url, favicon);
                setSupportProgressBarIndeterminateVisibility(true);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            setSupportProgressBarIndeterminateVisibility(false);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        @Override
        public void onReceivedLoginRequest(WebView view, String realm, String account, String args)
        {
            super.onReceivedLoginRequest(view, realm, account, args);
            //autologin = new DeviceAccountLogin(LoginActivity.this, view);
            //autologin.handleLogin(realm, account, args);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webview.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webview.restoreState(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            {
                authenticatorResponse.onResult(authenticatorResult);
                setResult(RESULT_OK);
            }
            else
            {
                authenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "Cancelled.");
            }
        }


        CookieManager.getInstance().removeAllCookie();
        super.finish();
    }
}
