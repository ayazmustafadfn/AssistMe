package com.artsistem.assistme.mail.auth

import android.app.Activity
import android.content.Context
import com.artsistem.assistme.R
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * MSAL tek-hesap sarmalayıcı. Microsoft Graph için token sağlar.
 * Tüm geri-çağrımlı (callback) API'ler coroutine'e uyarlanır.
 */
class MsalAuth(private val appContext: Context) {

    private val scopes = listOf("Mail.Read", "User.Read")
    private val authority = "https://login.microsoftonline.com/929de741-ac9a-4802-b2e0-422475057664"

    @Volatile
    private var client: ISingleAccountPublicClientApplication? = null

    private suspend fun client(): ISingleAccountPublicClientApplication {
        client?.let { return it }
        return suspendCancellableCoroutine { cont ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                appContext,
                R.raw.auth_config_single_account,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        client = application
                        cont.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }
    }

    /** Etkileşimli giriş (bir Activity gerekir). */
    suspend fun signIn(activity: Activity): IAuthenticationResult {
        val app = client()
        return suspendCancellableCoroutine { cont ->
            val params = SignInParameters.builder()
                .withActivity(activity)
                .withScopes(scopes)
                .withCallback(object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        cont.resume(authenticationResult)
                    }

                    override fun onError(exception: MsalException) {
                        cont.resumeWithException(exception)
                    }

                    override fun onCancel() {
                        cont.resumeWithException(MsalUserCancel())
                    }
                })
                .build()
            app.signIn(params)
        }
    }

    /** Sessiz token (giriş yapılmışsa). Hesap yoksa null. */
    suspend fun acquireTokenSilent(): IAuthenticationResult? {
        val app = client()
        val account = currentAccount(app) ?: return null
        return suspendCancellableCoroutine { cont ->
            val params = AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.authority ?: authority)
                .withScopes(scopes)
                .withCallback(object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        cont.resume(authenticationResult)
                    }

                    override fun onError(exception: MsalException) {
                        cont.resumeWithException(exception)
                    }
                })
                .build()
            app.acquireTokenSilentAsync(params)
        }
    }

    suspend fun isSignedIn(): Boolean = currentAccount(client()) != null

    suspend fun signOut() {
        val app = client()
        suspendCancellableCoroutine<Unit> { cont ->
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() { cont.resume(Unit) }
                override fun onError(exception: MsalException) { cont.resumeWithException(exception) }
            })
        }
    }

    private suspend fun currentAccount(app: ISingleAccountPublicClientApplication): IAccount? =
        suspendCancellableCoroutine { cont ->
            app.getCurrentAccountAsync(object :
                ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    cont.resume(activeAccount)
                }

                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                    cont.resume(currentAccount)
                }

                override fun onError(exception: MsalException) {
                    cont.resumeWithException(exception)
                }
            })
        }
}

/** Kullanıcı giriş ekranını iptal etti. */
class MsalUserCancel : Exception("Giriş iptal edildi")
