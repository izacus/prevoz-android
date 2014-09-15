package org.prevoz.android.auth;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.crashlytics.android.Crashlytics;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestAccountStatus;
import org.prevoz.android.api.rest.RestAuthTokenResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import retrofit.RetrofitError;


@EActivity(R.layout.activity_login)
@SuppressLint("Registered")     // AndroidAnnotated activity is registered.
public class LoginActivity extends SherlockFragmentActivity
{
    private static final String LOG_TAG = "Prevoz.Login";
    private static final String CLIENT_ID = "b89d13d3b102d84963bb";
    private static final String CLIENT_SECRET = "d94e76ff9086e1fe428519b6aed6dbe65adde616";
    private static final String REDIRECT_URL = "http://app.local/login_done/";

    private AccountAuthenticatorResponse authenticatorResponse;
    private Bundle authenticatorResult;

    @ViewById(R.id.login_webview)
    protected WebView webview;

    @ViewById(R.id.login_autologin_stub)
    protected ViewStub autologinStub;

    @Bean
    protected AuthenticationUtils authUtils;

    private DeviceAccountLogin autologin;
    private AutologinBar autologinBar;

    private boolean tokenRequestInProgress = false; // Workaround for Android 2.3

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        CookieSyncManager.createInstance(this);
        authenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (authenticatorResponse != null)
            authenticatorResponse.onRequestContinued();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @AfterViews
    @SuppressLint("SetJavaScriptEnabled")
    protected void initActivity()
    {
        webview.setWebViewClient(new WebViewController());
        webview.setVisibility(View.VISIBLE);
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);

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
        requestAccessToken(dialog, code);
    }

    @Background
    protected void requestAccessToken(final ProgressDialog dialog, String code)
    {
        RestAuthTokenResponse retrievedToken;
        try
        {
             retrievedToken = ApiClient.getAdapter().getAccessToken("authorization_code", CLIENT_ID, CLIENT_SECRET, code);
        }
        catch (RetrofitError e)
        {
            final Bundle result = new Bundle();
            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_AUTHENTICATION);
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "Failed to confirm authentication.");
            authenticatorResult = result;
            finish();
            return;
        }

        ApiClient.setBearer(retrievedToken.accessToken);

        try
        {
            RestAccountStatus restAccountStatus = ApiClient.getAdapter().getAccountStatus();
            updateAuthenticatorResult(restAccountStatus, retrievedToken.accessToken, retrievedToken.refreshToken);
            UpdateAccountInformationTask updateInfoTask = new UpdateAccountInformationTask(LoginActivity.this,
                                                                                            authUtils,
                                                                                            dialog,
                                                                                            restAccountStatus,
                                                                                            retrievedToken.accessToken,
                                                                                            retrievedToken.refreshToken,
                                                                                            System.currentTimeMillis() + (retrievedToken.expiresIn * 1000));
            updateInfoTask.execute();
        }
        catch (RetrofitError e)
        {
            Log.e(LOG_TAG, "Failed to login: " + e.getBody());
            dialog.dismiss();
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

    private void inflateAutoLoginBar()
    {
        if (autologinBar != null)
        {
            return;
        }

        autologinBar = (AutologinBar) autologinStub.inflate();
        autologinBar.setActivity(this);
    }

    public void hideAutoLogin()
    {
        updateAutoLogin(false);
    }

    public void showAutoLogin()
    {
        updateAutoLogin(false);
    }

    public void hideAutoLogin(boolean animate)
    {
        autologinBar.setVisibility(View.GONE);
        webview.invalidate();
    }

    public void showAutoLogin(boolean animate)
    {
        if (autologinBar == null)
        {
            inflateAutoLoginBar();
        }

        autologinBar.setVisibility(View.VISIBLE);
    }

    private void updateAutoLogin(boolean animate)
    {
        if(autologinBar == null)
        {
            if(getDeviceAccountLogin() == null)
            {
                return;
            }

            inflateAutoLoginBar();
        }

        autologinBar.updateAutoLogin(this, animate);
    }

    // Used for Google authentication
    public void setDeviceAccountLogin(DeviceAccountLogin login)
    {
        this.autologin = login;
    }

    public DeviceAccountLogin getDeviceAccountLogin()
    {
        return autologin;
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

        @Override
        public void onReceivedLoginRequest(WebView view, String realm, String account, String args)
        {
            super.onReceivedLoginRequest(view, realm, account, args);

            // TODO TODO TODO TODO : Enable when the site is ready for that

            //autologin = new DeviceAccountLogin(LoginActivity.this, view);
            //autologin.handleLogin(realm, account, args);
        }
    }

    @OptionsItem(android.R.id.home)
    public void clickHome()
    {
        finish();
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
