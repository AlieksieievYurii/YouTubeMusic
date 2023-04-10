package com.youtubemusic.feature.equalizer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.youtubemusic.feature.equalizer.R
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal class TwisterController(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    companion object {
        private const val POSITION_COUNT = 10
        private const val STAR_ANGLE_DIAL_POINT = 160
        private const val END_ANGLE_DIAL_POINT = 380

        private const val START_ANGLE_DIAL_POINT_R = STAR_ANGLE_DIAL_POINT * (Math.PI / 180)
        private const val END_ANGLE_DIAL_POINT_R = END_ANGLE_DIAL_POINT * (Math.PI / 180)
        private const val ANGLE_L = END_ANGLE_DIAL_POINT_R - START_ANGLE_DIAL_POINT_R
        private const val STEP = ANGLE_L / (POSITION_COUNT - 1)
    }

    private var enableDialColor = Color.LTGRAY
    private var enableMarkerPointColor = Color.DKGRAY

    private var disableDialColor = Color.GRAY
    private var disableMarkerPointColor = Color.WHITE

    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marginOfInnerCircle = 30f
    private val marginDialPointsFromInnerCircle = 10f
    private var angle = 0f

    var listener: ((value: Int) -> Unit)? = null

    var value: Int = 0
        set(v) {
            angle = if (v in 0..80) (v / 0.4).toFloat() else (v - 240).toFloat()
            field = v
            invalidate()
        }

    fun setEnable(enable: Boolean) {
        isEnabled = enable
        if (enable) {
            dialPaint.color = enableDialColor
            markerPointPaint.color = enableMarkerPointColor
        } else {
            dialPaint.color = disableDialColor
            markerPointPaint.color = disableMarkerPointColor
        }
        invalidate()
    }

    init {
        attributeSet.let {
            val typedArray = context.theme.obtainStyledAttributes(attributeSet, R.styleable.TwisterController, 0, 0)
            disableMarkerPointColor = typedArray.getColor(R.styleable.TwisterController_disableMarkerColor, Color.WHITE)
            disableDialColor = typedArray.getColor(R.styleable.TwisterController_disableColor, Color.GRAY)
            enableDialColor = typedArray.getColor(R.styleable.TwisterController_color, Color.LTGRAY)
            enableMarkerPointColor = typedArray.getColor(R.styleable.TwisterController_markerColor, Color.DKGRAY)
            isEnabled = typedArray.getBoolean(R.styleable.TwisterController_enabled, true)
            typedArray.recycle()
        }
        setEnable(isEnabled)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCircle(canvas)
        drawDialPoints(canvas)
        drawMarker(canvas)
    }

    private fun drawCircle(canvas: Canvas) {
        canvas.drawCircle(width / 2f, height / 2f, (width / 2f) - marginOfInnerCircle, dialPaint)
    }

    private fun drawDialPoints(canvas: Canvas) {
        (0 until POSITION_COUNT).forEach {
            val res = computeXYForPosition(it, (width / 2f) - marginDialPointsFromInnerCircle)
            canvas.drawCircle(res[0], res[1], 8f, dialPaint)
        }
    }

    private fun drawMarker(canvas: Canvas) {
        val r = (width / 2f) - marginOfInnerCircle - 40
        val angInR = START_ANGLE_DIAL_POINT_R + angle * (Math.PI / 180)
        val x = r * cos(angInR) + width / 2
        val y = r * sin(angInR) + height / 2
        canvas.drawCircle(x.toFloat(), y.toFloat(), 30f, markerPointPaint)
    }

    private fun computeXYForPosition(pos: Int, radius: Float): FloatArray {
        val result = FloatArray(2)
        val angle = START_ANGLE_DIAL_POINT_R + STEP * pos
        result[0] = (radius * cos(angle)).toFloat() + width / 2
        result[1] = (radius * sin(angle)).toFloat() + height / 2
        return result
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled)
            return false

        val a = -(getAngle(event.x, event.y) - 200).toFloat()
        if (a < 0 && a > -70 && angle != -140f)
            angle = 0f
        else if (a > -140 && -70 >= a && angle != 0f)
            angle = -140f
        else if (a >= 0 || a <= -140) {
            angle = a
        }
        invalidate()

        invokeCallBack()
        return true
    }

    private fun invokeCallBack() {
        if (angle in 0.0..200.0)
            listener?.invoke((angle * 0.4).toInt())
        else if (angle in -160.0..-140.0)
            listener?.invoke(angle.toInt() + 240)
    }

    /**
     * Calculates degree.
     * @return degree from 0 to 360
     */
    private fun getAngle(xT: Float, yT: Float): Double {
        val x = (xT - (width / 2)).toDouble()
        val y = (height - yT - (height / 2)).toDouble()

        return when (getQuadrant(x, y)) {
            1 -> asin(y / hypot(x, y)) * 180 / Math.PI
            2 -> 180 - asin(y / hypot(x, y)) * 180 / Math.PI
            3 -> 180 + (-1 * asin(y / hypot(x, y)) * 180 / Math.PI)
            4 -> 360 + asin(y / hypot(x, y)) * 180 / Math.PI
            else -> 0.0
        }

    }

    private fun getQuadrant(x: Double, y: Double): Int {
        return if (x >= 0)
            if (y >= 0) 1 else 4
        else
            if (y >= 0) 2 else 3
    }
}