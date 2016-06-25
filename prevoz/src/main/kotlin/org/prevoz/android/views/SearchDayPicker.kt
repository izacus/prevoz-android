package org.prevoz.android.views

import android.content.Context
import android.graphics.*
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import org.prevoz.android.R
import org.prevoz.android.util.LocaleUtil
import org.threeten.bp.ZonedDateTime

class SearchDayPicker(context: Context?, attrs: AttributeSet?, defStyleAttr: Int): View(context, attrs, defStyleAttr) {

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : this(context, attrs, defStyleAttr)

    val r: Rect = Rect()
    val size : Int
    val sizeWithPadding : Int
    val paint : Paint;
    val textColorNormal: Int
    val textColorDark: Int

    var viewCount: Int = 0
    var totalLeftPadding: Int = 0

    val nameTextSize: Float
    val numberTextSize: Float
    val numberTextPadding: Int

    var selected: Int = 1

    // Lateinit so it works in UI editor
    var startDate: ZonedDateTime = ZonedDateTime.now()

    val detector: GestureDetectorCompat

    init {
        paint = Paint()
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.LEFT
        paint.isAntiAlias = true

        nameTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14.0f, context!!.resources.displayMetrics)
        numberTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16.0f, context.resources.displayMetrics)
        numberTextPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4.0f, context.resources.displayMetrics).toInt()

        size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48.0f, context.resources.displayMetrics).toInt()
        sizeWithPadding = size +  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8.0f, context.resources.displayMetrics).toInt()

        val typedValue = TypedValue()
        val typedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
        textColorDark = typedArray.getColor(0, 0)
        typedArray.recycle()

        textColorNormal = Color.WHITE

        detector = GestureDetectorCompat(context, SearchDayPickerGestureDetector())
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        setMeasuredDimension(widthMeasureSpec, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        viewCount = (width - paddingLeft - paddingRight) / sizeWithPadding
        // Calculate additional padding to center
        totalLeftPadding = paddingLeft + (width - (viewCount * sizeWithPadding)) / 2
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas!!)

        canvas.getClipBounds(r)
        val canvasHeight = r.height()

        for (i in 0..viewCount - 2) {
            val x = totalLeftPadding + (i * sizeWithPadding)

            if (i == selected) {
                paint.color = textColorNormal
                paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawCircle((x + size / 2).toFloat(), (canvasHeight / 2).toFloat(), (size / 2).toFloat(), paint)
                paint.color = textColorDark
            } else {
                paint.color = textColorNormal
                paint.typeface = Typeface.DEFAULT
            }

            drawDay(canvas, canvasHeight, x, i)
        }

        paint.color = textColorNormal
        drawDayName(canvas, canvasHeight, paint, "• • •", totalLeftPadding + (viewCount - 1) * sizeWithPadding)
    }

    fun drawDay(canvas: Canvas, canvasHeight:Int, x: Int, index: Int) {
        val drawnDay = startDate.plusDays(index.toLong())
        drawDayName(canvas, canvasHeight, paint, LocaleUtil.getShortDayName(resources, drawnDay), x)
    }

    fun drawDayName(canvas: Canvas, canvasHeight: Int, p: Paint, text: String, x: Int) {
        p.textSize = nameTextSize
        p.getTextBounds(text, 0, text.length, r);
        val tx = x + (size / 2f - r.width() / 2f - r.left).toFloat()
        val ty = canvasHeight / 2f + r.height() / 2f - r.bottom;

        canvas.drawText(text, tx, ty, paint);
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return detector.onTouchEvent(event)
    }

    fun getSelectedDate() : ZonedDateTime {
        return startDate.plusDays(selected.toLong())
    }

    inner class SearchDayPickerGestureDetector : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if (e == null) return false
            // Determine tapped views
            selected = ((e.x - totalLeftPadding) / sizeWithPadding).toInt()
            invalidate()
            return true
        }
    }
}