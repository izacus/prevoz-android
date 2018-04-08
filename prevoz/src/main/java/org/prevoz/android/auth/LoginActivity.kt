package org.prevoz.android.auth

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.LevelEndEvent
import com.crashlytics.android.answers.LevelStartEvent
import com.crashlytics.android.answers.LoginEvent
import de.greenrobot.event.EventBus
import org.prevoz.android.MainActivity
import org.prevoz.android.R
import org.prevoz.android.api.ApiClient
import org.prevoz.android.api.rest.RestAccountStatus
import org.prevoz.android.events.Events
import org.prevoz.android.util.PrevozActivity
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

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
            try {
                // Generate OAuth login URL
                val authenticationUrl = ApiClient.BASE_URL + String.format("/prevoz/application_approve/%s/?client_id=%s&response_type=code&redirect_uri=%s", CLIENT_ID, CLIENT_ID, URLEncoder.encode(REDIRECT_URL, "UTF-8"))
                Answers.getInstance().logLevelStart(LevelStartEvent().putLevelName("Login"))
                try {
                    startCustomTabsLoginFlow(authenticationUrl)
                } catch (e: ActivityNotFoundException) {
                    startExternalBrowserLoginFlow(authenticationUrl)
                }
            } catch (e: UnsupportedEncodingException) {
                Crashlytics.logException(e)
            }

            finish()
        }
    }

    private fun startCustomTabsLoginFlow(authenticationUrl: String) {
        val builder = CustomTabsIntent.Builder()
        builder.setInstantAppsEnabled(false)
        builder.setShowTitle(true)
        builder.setToolbarColor(ContextCompat.getColor(this, R.color.prevoztheme_color_dark))
        val intent = builder.build()
        intent.launchUrl(this, Uri.parse(authenticationUrl))
    }

    private fun startExternalBrowserLoginFlow(authenticationUrl: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authenticationUrl))
        startActivity(browserIntent)
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

    companion object {
        const val CLIENT_ID = "QTwUBJLA8ZngFS5iK2h2kcV68qAftyLIi2gjXkIy"
        const val CLIENT_SECRET = "qcPKCeIScAU8Ca009BwokX4xW86AhSaPZu1rqu2ZCygMPhsrG57sF1gMcryzCBqf2qwSuZpGFVkurl0ZtiVwLi62B3pLYoawTk2z0qX2PcSePZvVkrjGsntxSbxOroSc"
        private const val REDIRECT_URL = "prevoz://auth/done/"
    }
}
