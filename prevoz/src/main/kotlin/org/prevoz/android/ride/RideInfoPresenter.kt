package org.prevoz.android.ride

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Parcelable
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.hannesdorfmann.mosby.mvp.MvpPresenter
import de.greenrobot.event.EventBus
import okhttp3.ResponseBody
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.R
import org.prevoz.android.api.ApiClient
import org.prevoz.android.api.PrevozApi
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.api.rest.RestStatus
import org.prevoz.android.auth.AuthenticationUtils
import org.prevoz.android.events.Events
import org.prevoz.android.model.Bookmark
import org.prevoz.android.myrides.NewRideActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rx.schedulers.Schedulers
import javax.inject.Inject

class RideInfoPresenter(component: ApplicationComponent, val ride: RestRide, val action: Action) : MvpPresenter<RideInfoActivity> {

    init {
        component.inject(this)
    }

    enum class Action {
        ACTION_SHOW,
        ACTION_EDIT,
        ACTION_SUBMIT
    }

    @Inject lateinit var authUtils: AuthenticationUtils
    @Inject lateinit var packageManager: PackageManager
    @Inject lateinit var connectivityService: ConnectivityManager

    var view: RideInfoActivity? = null

    override fun attachView(view: RideInfoActivity?) {
        this.view = view
        view?.let {
            view.showRide(ride)
            view.showFavoritesIcon(authUtils.isAuthenticated && !ride.isAuthor)
            view.setFavoritesIconState(Bookmark.shouldShow(ride.bookmark), false)

            when (action) {
                Action.ACTION_SHOW -> {
                    if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                        view.hideButtons()
                    } else {
                        view.showViewControls()
                    }
                }
                Action.ACTION_EDIT -> {
                    view.showEditingControls(ride.isFull)
                }
                Action.ACTION_SUBMIT -> {
                    view.showSubmitControls()
                }
            }
        }
    }

    override fun detachView(retainInstance: Boolean) {
        this.view = null
    }

    fun onLeftButtonClicked() {
        view?.let {
            when (action) {
                Action.ACTION_SHOW -> call()
                Action.ACTION_EDIT -> editRide()
                else -> {
                }
            }

            it.finish()
        }
    }

    fun onRightButtonClicked() {
        when(action) {
            Action.ACTION_SHOW -> sendSms()
            Action.ACTION_EDIT -> deleteRide()
            Action.ACTION_SUBMIT -> submitRide()
        }
    }

    fun onFavoriteButtonClicked() {
        if (Bookmark.shouldShow(ride.bookmark)) {
            Answers.getInstance().logCustom(CustomEvent("Bookmark - clear"))
            ride.bookmark = null
        } else {
            ride.bookmark = Bookmark.BOOKMARK
            Answers.getInstance().logCustom(CustomEvent("Bookmark - set"))
        }

        view?.setFavoritesIconState(Bookmark.shouldShow(ride.bookmark), true)

        val response = ApiClient.getAdapter().setRideBookmark(ride.id.toString(), if (Bookmark.shouldShow(ride.bookmark)) "bookmark" else "erase")
        response.enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.i("Prevoz", "Bookmark set OK.")
                EventBus.getDefault().postSticky(Events.MyRideStatusUpdated(ride, false))
            }

            override fun onFailure(call: Call<ResponseBody>, throwable: Throwable) {
                Log.e("Prevoz", "Failed to set bookmark status.", throwable)
                Crashlytics.logException(throwable)
            }
        })
    }

    private fun submitRide() {
        if (connectivityService.activeNetworkInfo?.isConnected == false) {
            EventBus.getDefault().postSticky(Events.ShowMessage("Ni mogoče oddati prevoza, internet ni na voljo!", true))
            view?.finish()
            return
        }

        view?.showSubmitProgressDialog()
        val call = ApiClient.getAdapter().postRide(ride)
        call.enqueue(object : Callback<RestStatus> {
            override fun onResponse(call: Call<RestStatus>?, response: Response<RestStatus>?) {
                view?.hideProgressDialog()
                response?.let {
                    if (!response.isSuccessful && response.code() == 403) {
                        EventBus.getDefault().postSticky(Events.ShowMessage("Vaša prijava ni več veljavna, prosimo ponovno se prijavite.", true))
                        authUtils.logout().subscribeOn(Schedulers.io()).subscribe()
                    } else {
                        response.body()?.let {
                            if (it.status == "created" || it.status == "updated") {
                                EventBus.getDefault().postSticky(Events.ShowMessage(R.string.newride_publish_success, false))
                                EventBus.getDefault().post(Events.MyRideStatusUpdated(ride, false))
                                Answers.getInstance().logCustom(CustomEvent("New ride submitted"))
                                view?.setResult(Activity.RESULT_OK)
                            } else {
                                if (it.error != null && it.error.isNotEmpty()) {
                                    val firstKey = it.error.keys.iterator().next()
                                    EventBus.getDefault().postSticky(Events.ShowMessage(it.error[firstKey]?.get(0), true))
                                } else {}
                            }
                        }
                    }
                }

                view?.finish()
            }

            override fun onFailure(call: Call<RestStatus>?, t: Throwable?) {
                view?.hideProgressDialog()
                Crashlytics.logException(t)
                EventBus.getDefault().postSticky(Events.ShowMessage(R.string.newride_publish_failure, true))
                view?.setResult(Activity.RESULT_CANCELED)
                view?.finish()
            }
        })
    }

    private fun deleteRide() {
        view?.showDeleteDialog(ride)
    }

    private fun editRide() {
        view?.let {
            val intent = Intent(it, NewRideActivity::class.java)
            intent.putExtra(NewRideActivity.PARAM_EDIT_RIDE, ride as Parcelable)
            ActivityCompat.startActivity(it, intent, ActivityOptionsCompat.makeCustomAnimation(it, R.anim.slide_up, 0).toBundle())
        }
    }

    private fun call() {
        ride.phoneNumber?.let {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$it")
            view?.startActivity(intent)
        }
    }

    private fun sendSms() {
        ride.phoneNumber?.let {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("smsto:$it")
            try {
                view?.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                view?.showMissingSmsAppError()
            }
        }
    }

    fun onDeleteRideClicked() {
        view?.showDeleteProgressDialog()
        val call = ApiClient.getAdapter().deleteRide(ride.id.toString())
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                view?.hideProgressDialog()
                view?.finish()
                EventBus.getDefault().post(Events.MyRideStatusUpdated(ride, true))
                EventBus.getDefault().postSticky(Events.ShowMessage(R.string.ride_delete_success, false))
            }

            override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                view?.hideProgressDialog()
                view?.finish()
                EventBus.getDefault().postSticky(Events.ShowMessage(R.string.ride_delete_failure, true))
            }
        })
    }

    fun onFullButtonClicked(rideFull: Boolean) {
        if (rideFull == ride.isFull) return
        view?.setFullCheckEnabled(false)
        val call = ApiClient.getAdapter().setFull(ride.id.toString(), if (rideFull) PrevozApi.FULL_STATE_FULL else PrevozApi.FULL_STATE_AVAILABLE)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                ride.isFull = rideFull
                view?.updateRideFullStatus(ride)
                view?.setFullCheckEnabled(true)
                EventBus.getDefault().post(Events.MyRideStatusUpdated(ride, false))
            }

            override fun onFailure(call: Call<ResponseBody>, throwable: Throwable) {
                view?.updateRideFullStatus(ride)
                view?.showUnableToUpdateFullStatusError()
                view?.setFullCheckEnabled(true)
            }
        })
    }
}