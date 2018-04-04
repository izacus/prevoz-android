package org.prevoz.android.auth

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.customtabs.CustomTabsCallback
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.customtabs.CustomTabsServiceConnection
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.LoginEvent

import org.prevoz.android.R
import org.prevoz.android.api.ApiClient
import org.prevoz.android.api.rest.RestAccountStatus
import org.prevoz.android.api.rest.RestAuthTokenResponse
import org.prevoz.android.events.Events
import org.prevoz.android.util.PrevozActivity

import java.io.UnsupportedEncodingException
import java.net.URLEncoder

import butterknife.BindView
import butterknife.ButterKnife
import com.crashlytics.android.answers.LevelEndEvent
import com.crashlytics.android.answers.LevelStartEvent
import de.greenrobot.event.EventBus
import org.prevoz.android.MainActivity
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class LoginActivity : PrevozActivity() {

    private val authenticatorResponse: AccountAuthenticatorResponse? = null
    private var authenticatorResult: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent

        if (Intent.ACTION_VIEW == intent.action && intent.data != null) {
            val code = intent.data.getQueryParameter("code")
            // TODO: Error handling.
            getAccountUsernameAndApiKey(code)
        } else {
            // Generate OAuth login URL
            var authenticationUrl: String? = null
            try {
                authenticationUrl = ApiClient.BASE_URL + String.format("/prevoz/application_approve/%s/?client_id=%s&response_type=code&redirect_uri=%s", CLIENT_ID, CLIENT_ID, URLEncoder.encode(REDIRECT_URL, "UTF-8"))
            } catch (e: UnsupportedEncodingException) {
                Crashlytics.logException(e)
            }

            Answers.getInstance().logLevelStart(LevelStartEvent().putLevelName("Login"))
            startExternalBrowserLoginFlow(authenticationUrl!!)
        }
    }

    private fun startExternalBrowserLoginFlow(authenticationUrl: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authenticationUrl))
        startActivity(browserIntent)
        finish()
    }

    private fun getAccountUsernameAndApiKey(code: String) {
        val dialog = ProgressDialog.show(this, "Prijava", "Prijavljam....", true, false)
        val accessToken = ApiClient.getAdapter().getAccessToken("authorization_code", CLIENT_ID, CLIENT_SECRET, code, REDIRECT_URL)
                                   .cache()

        val accountStatus = accessToken.flatMap { restAuthTokenResponse ->
            ApiClient.setBearer(restAuthTokenResponse.accessToken)
            ApiClient.getAdapter().accountStatus
        }

        Observable.zip(accessToken, accountStatus) { token, status -> Pair(token, status) }
                .doOnNext { authInfoPair ->
                    updateAuthenticatorResult(authInfoPair.second, authInfoPair.first.accessToken, authInfoPair.first.refreshToken)
                    val am = AccountManager.get(this@LoginActivity)
                    authUtils.removeExistingAccounts()
                    val acc = Account(authInfoPair.second.username, getString(R.string.account_type))
                    am.addAccountExplicitly(acc, authInfoPair.first.refreshToken, null)
                    am.setAuthToken(acc, "default", authInfoPair.first.accessToken)

                    val sp = PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                    sp.edit().putBoolean(PrevozAccountAuthenticator.PREF_OAUTH2, true)
                            .putLong(PrevozAccountAuthenticator.PREF_KEY_EXPIRES, System.currentTimeMillis() + authInfoPair.first.expiresIn * 1000)
                            .apply()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ authInfoPair ->
                            Answers.getInstance().logLogin(LoginEvent().putSuccess(true))
                            Answers.getInstance().logLevelEnd(LevelEndEvent().putLevelName("Login").putSuccess(true))
                        },
                        { throwable ->
                            Answers.getInstance().logLogin(LoginEvent().putSuccess(false))
                            Answers.getInstance().logLevelEnd(LevelEndEvent().putLevelName("Login").putSuccess(false))
                            Crashlytics.logException(throwable.cause)
                            ApiClient.setBearer(null)
                            val result = Bundle()
                            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_AUTHENTICATION)
                            result.putString(AccountManager.KEY_ERROR_MESSAGE, "Failed to confirm authentication.")
                            authenticatorResult = result
                        }
                ) {
                    EventBus.getDefault().postSticky(Events.LoginStateChanged())
                    dialog.dismiss()

                    val mainActivityIntent = Intent(this, MainActivity::class.java)
                    mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(mainActivityIntent)
                }

    }

    private fun updateAuthenticatorResult(restAccountStatus: RestAccountStatus, accessToken: String, refreshToken: String) {
        if (authenticatorResponse != null) {
            val result = Bundle()
            if (restAccountStatus.isAuthenticated) {
                result.putString(AccountManager.KEY_ACCOUNT_NAME, restAccountStatus.username)
                result.putString(AccountManager.KEY_PASSWORD, refreshToken)
                result.putString(AccountManager.KEY_AUTHTOKEN, accessToken)
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type))
            } else {
                result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_AUTHENTICATION)
                result.putString(AccountManager.KEY_ERROR_MESSAGE, "Failed to confirm authentication.")
            }

            authenticatorResult = result
        }
    }

    private inner class WebViewController : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Log.d(LOG_TAG, "Loading " + url)

            if (url.startsWith(REDIRECT_URL)) {
                val uri = Uri.parse(url)
                val code = uri.getQueryParameter("code")
                // TODO: Error handling.
                getAccountUsernameAndApiKey(code)
                return true
            }

            view.loadUrl(url)
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
            super.onPageStarted(view, url, favicon)
            setSupportProgressBarIndeterminateVisibility(true)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            setSupportProgressBarIndeterminateVisibility(false)
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        override fun onReceivedLoginRequest(view: WebView, realm: String, account: String, args: String) {
            super.onReceivedLoginRequest(view, realm, account, args)
            //autologin = new DeviceAccountLogin(LoginActivity.this, view);
            //autologin.handleLogin(realm, account, args);
        }
    }

    companion object {
        private val LOG_TAG = "Prevoz.Login"
        val CLIENT_ID = "QTwUBJLA8ZngFS5iK2h2kcV68qAftyLIi2gjXkIy"
        val CLIENT_SECRET = "qcPKCeIScAU8Ca009BwokX4xW86AhSaPZu1rqu2ZCygMPhsrG57sF1gMcryzCBqf2qwSuZpGFVkurl0ZtiVwLi62B3pLYoawTk2z0qX2PcSePZvVkrjGsntxSbxOroSc"
        private val REDIRECT_URL = "prevoz://auth/done/"
    }
}
