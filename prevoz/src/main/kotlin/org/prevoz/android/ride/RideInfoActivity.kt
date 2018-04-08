package org.prevoz.android.ride

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.*
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import com.hannesdorfmann.mosby.mvp.MvpActivity
import org.prevoz.android.PrevozApplication
import org.prevoz.android.R
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.util.LocaleUtil
import org.prevoz.android.util.ViewUtils
import org.threeten.bp.format.DateTimeFormatter;
import java.util.*
import javax.inject.Inject

class RideInfoActivity : MvpActivity<RideInfoActivity, RideInfoPresenter>() {

    companion object {
        @JvmField val TIMER_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

        const val ARG_RIDE = "ride"
        const val ARG_ACTION = "action"

        const val PARAM_ACTION_SHOW = "show"
        const val PARAM_ACTION_EDIT = "edit"
        const val PARAM_ACTION_SUBMIT = "submit"

        @JvmStatic
        fun show(parent: Activity, ride: RestRide) {
            val i = Intent(parent, RideInfoActivity::class.java)
            i.putExtra(ARG_RIDE, ride as Parcelable)
            parent.startActivity(i)
        }

        @JvmStatic
        fun show(parent: Activity, ride: RestRide, action: String, requestCode: Int) {
            val i = Intent(parent, RideInfoActivity::class.java)
            i.putExtra(ARG_RIDE, ride as Parcelable)
            i.putExtra(ARG_ACTION, action)
            parent.startActivityForResult(i, requestCode)
        }

        @JvmStatic
        fun show(parent: Activity, ride: RestRide, options: Bundle) {
            val i = Intent(parent, RideInfoActivity::class.java)
            i.putExtra(ARG_RIDE, ride as Parcelable)
            ActivityCompat.startActivity(parent, i, options)
        }
    }

    @BindView(R.id.rideinfo_container)
    lateinit var container: View

    @BindView(R.id.rideinfo_favorite)
    lateinit var imgFavorite: ImageView

    @BindView(R.id.rideinfo_from)
    lateinit var txtFrom: TextView

    @BindView(R.id.rideinfo_to)
    lateinit var txtTo: TextView

    @BindView(R.id.rideinfo_time)
    lateinit var txtTime: TextView

    @BindView(R.id.rideinfo_price)
    lateinit var txtPrice: TextView

    @BindView(R.id.rideinfo_date)
    lateinit var txtDate: TextView

    @BindView(R.id.rideinfo_details)
    lateinit var vDetails: View

    @BindView(R.id.rideinfo_phone)
    lateinit var txtPhone: TextView
    @BindView(R.id.rideinfo_people)
    lateinit var txtPeople: TextView
    @BindView(R.id.rideinfo_insurance)
    lateinit var txtInsurance: TextView
    @BindView(R.id.rideinfo_driver)
    lateinit var txtDriver: TextView
    @BindView(R.id.rideinfo_car)
    lateinit var txtCar: TextView
    @BindView(R.id.rideinfo_comment)
    lateinit var txtComment: TextView

    @BindView(R.id.rideinfo_full_box)
    lateinit var vFull: View
    @BindView(R.id.rideinfo_ridefull)
    lateinit var chkFull: CheckBox

    @BindView(R.id.rideinfo_button_call)
    lateinit var leftButton: Button
    @BindView(R.id.rideinfo_button_sms)
    lateinit var rightButton: Button

    @Inject lateinit var localeUtil: LocaleUtil

    lateinit var gestureDetector: GestureDetector

    var progressDialog: ProgressDialog? = null

    override fun createPresenter(): RideInfoPresenter {
        val ride = intent.getParcelableExtra<RestRide>(ARG_RIDE)

        val action = when(intent.getStringExtra(ARG_ACTION)) {
          PARAM_ACTION_SHOW -> if (ride.isAuthor) RideInfoPresenter.Action.ACTION_EDIT else RideInfoPresenter.Action.ACTION_SHOW
          PARAM_ACTION_SUBMIT -> RideInfoPresenter.Action.ACTION_SUBMIT
          PARAM_ACTION_EDIT -> RideInfoPresenter.Action.ACTION_EDIT
          else -> RideInfoPresenter.Action.ACTION_SHOW
        }

        return RideInfoPresenter((application as PrevozApplication).component(), ride, action)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleUtil.checkSetLocale(this, resources.configuration)
        (application as PrevozApplication).component().inject(this)
        setContentView(R.layout.activity_rideinfo)
        ButterKnife.bind(this)
        super.onCreate(savedInstanceState)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                finish()
                return true
            }
        })
    }

    override fun onDestroy() {
        hideProgressDialog()
        super.onDestroy()
    }

    fun showRide(ride: RestRide) {
        txtFrom.text = localeUtil.getLocalizedCityName(ride.fromCity, ride.fromCountry)
        txtTo.text = localeUtil.getLocalizedCityName(ride.toCity, ride.toCountry)
        txtTime.text = ride.date.format(TIMER_FORMATTER)

        if (ride.price == null || ride.price == 0f) {
            txtPrice.visibility = View.INVISIBLE
        } else {
            txtPrice.text = String.format(LocaleUtil.getLocale(), "%1.1f €", ride.price)
        }

        txtDate.text = localeUtil.localizeDate(ride.date)
        txtPhone.text = getPhoneNumberString(ride.phoneNumber, ride.phoneNumberConfirmed)
        txtComment.text = ride.comment
        setPeopleText(ride.numPeople ?: 0, ride.isFull)

        if (TextUtils.isEmpty(ride.carInfo)) {
            txtCar.visibility = View.GONE
        } else {
            txtCar.visibility = View.VISIBLE
            txtCar.text = ride.carInfo
        }

        vDetails.visibility = View.VISIBLE

        if ((ride.author == null || ride.author!!.isEmpty()) && ride.published == null) {
            txtDriver.visibility = View.GONE
        } else if (ride.published == null) {
            txtDriver.text = ride.author!! + "\u00A0"
        } else {
            val timeAgo = DateUtils.getRelativeTimeSpanString(ride.published!!.toInstant().toEpochMilli(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_ABBREV_WEEKDAY).toString().toLowerCase(Locale.getDefault())

            if (ride.author == null || ride.author!!.length == 0) {
                txtDriver.text = timeAgo + "\u00A0"   // Add non-breaking space at the end to prevent italic letter clipping
            } else {
                val ssb = SpannableStringBuilder()
                ssb.append(ride.author)
                ssb.append(", ")
                ssb.append(timeAgo)
                ssb.setSpan(StyleSpan(Typeface.BOLD), 0, ride.author!!.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.append("\u00A0")
                txtDriver.text = ssb
            }
        }

        txtInsurance.text = if (ride.insured) "\u2713 Ima zavarovanje." else "\u2717 Nima zavarovanja."
    }

    fun showFavoritesIcon(show: Boolean) {
        imgFavorite.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    fun setFavoritesIconState(full: Boolean, animateChange: Boolean) {
        val drawable = ContextCompat.getDrawable(this, if (full) R.drawable.ic_favorite else R.drawable.ic_favorite_outline)
        drawable!!.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        imgFavorite.setImageDrawable(drawable)

        if (animateChange) {
            imgFavorite.animate().scaleX(2.0f).scaleY(2.0f).alpha(0.2f).setDuration(200).setListener(object : AnimatorListenerAdapter() {
                @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    imgFavorite.alpha = 1.0f
                    imgFavorite.scaleX = 1f
                    imgFavorite.scaleY = 1f
                }
            })
        }
    }

    fun setPeopleText(numPeople: Int, full: Boolean) {
        txtPeople.setText(numPeople.toString() + if (full) " (Polno)" else "")
    }

    fun hideButtons() {
        leftButton.visibility = View.GONE
        rightButton.visibility = View.GONE
    }

    private fun getPhoneNumberString(phoneNumber: String?, confirmed: Boolean): SpannableString {
        val phoneNumberString = SpannableString(phoneNumber!! + if (confirmed) "" else "\nNi potrjena")
        if (confirmed)
            return phoneNumberString

        val sizeSpan = RelativeSizeSpan(0.55f)
        phoneNumberString.setSpan(sizeSpan, phoneNumber.length, phoneNumberString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(this, android.R.color.darker_gray))
        phoneNumberString.setSpan(colorSpan, phoneNumber.length, phoneNumberString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val typefaceSpan = TypefaceSpan("sans-serif-thin")
        phoneNumberString.setSpan(typefaceSpan, phoneNumber.length, phoneNumberString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val styleSpan = StyleSpan(Typeface.BOLD_ITALIC)
        phoneNumberString.setSpan(styleSpan, phoneNumber.length, phoneNumberString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return phoneNumberString
    }

    fun showEditingControls(rideFull: Boolean) {
        vFull.visibility = View.VISIBLE
        chkFull.isChecked = rideFull
        leftButton.setText(R.string.rideinfo_edit)
        rightButton.setText(R.string.rideinfo_delete)
    }

    fun showSubmitControls() {
        leftButton.setText(R.string.rideinfo_cancel)
        rightButton.setText(R.string.rideinfo_submit)
    }

    fun showViewControls() {
        leftButton.setText(R.string.rideinfo_call)
        rightButton.setText(R.string.rideinfo_send_sms)
    }

    fun showDeleteDialog(ride: RestRide) {
        AlertDialog.Builder(this, R.style.Prevoz_Theme_Dialog)
                .setTitle("${ride.fromCity} - ${ride.toCity}")
                .setMessage(getString(R.string.ride_delete_message, localeUtil.getDayName(ride.date).toLowerCase(LocaleUtil.getLocale()), LocaleUtil.getFormattedTime(ride.date)))
                .setNegativeButton(R.string.ride_delete_cancel, null)
                .setPositiveButton(R.string.ride_delete_ok, { _, _ ->  presenter.onDeleteRideClicked() })
                .show()
    }

    fun showDeleteProgressDialog() {
        val deleteDialog = ProgressDialog(this)
        deleteDialog.setMessage(getString(R.string.ride_delete_progress))
        deleteDialog.show()
        progressDialog = deleteDialog
    }

    fun showSubmitProgressDialog() {
        val submitDialog = ProgressDialog(this)
        submitDialog.setMessage(getString(R.string.newride_submit_progress))
        submitDialog.show()
        progressDialog = submitDialog
    }

    fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    fun showMissingSmsAppError() {
        Toast.makeText(this, "Nimate nameščene nobene aplikacije za pošiljanje SMS sporočil.", Toast.LENGTH_SHORT).show()
    }

    fun showUnableToUpdateFullStatusError() {
        ViewUtils.showMessage(this@RideInfoActivity, "Stanja prevoza ni bilo mogoče spremeniti :(", true)
    }

    fun setFullCheckEnabled(enabled: Boolean) {
        chkFull.isEnabled = enabled
    }

    fun updateRideFullStatus(ride: RestRide) {
        chkFull.isChecked = ride.isFull
        setPeopleText(ride.numPeople ?: 0, ride.isFull)
    }

    @OnClick(R.id.rideinfo_button_call)
    protected fun onLeftButtonClicked() {
        presenter.onLeftButtonClicked()
    }

    @OnClick(R.id.rideinfo_button_sms)
    protected fun onRightButtonClicked() {
        presenter.onRightButtonClicked()
    }

    @OnClick(R.id.rideinfo_favorite)
    protected fun onFavoriteButtonClicked() {
        presenter.onFavoriteButtonClicked()
    }

    @OnCheckedChanged(R.id.rideinfo_ridefull)
    protected fun onFullButtonClicked() {
        presenter.onFullButtonClicked(chkFull.isChecked)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}