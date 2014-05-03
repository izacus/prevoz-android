package org.prevoz.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.api.Scope;
import org.prevoz.android.R;

@EBean(scope = Scope.Singleton)
public class AuthenticationUtils
{
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
}
