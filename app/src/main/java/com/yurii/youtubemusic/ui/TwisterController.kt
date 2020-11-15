package com.yurii.youtubemusic.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.yurii.youtubemusic.R
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class TwisterController(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    companion object {
        private const val POSITION_COUNT = 10
        private const val STAR_ANGLE_DIAL_POINT = 160
        private const val END_ANGLE_DIAL_POINT = 380

        private const val START_ANGLE_DIAL_POINT_R = STAR_ANGLE_DIAL_POINT * (Math.PI / 180)
        private const val END_ANGLE_DIAL_POINT_R = END_ANGLE_DIAL_POINT * (Math.PI / 180)
        private const val ANGLE_L = END_ANGLE_DIAL_POINT_R - START_ANGLE_DIAL_POINT_R
        private const val STEP = ANGLE_L / (POSITION_COUNT - 1)
    }

    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
    }

    private val markerPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }


    init {
        attributeSet.let {
            val typedArray = context.theme.obtainStyledAttributes(attributeSet, R.styleable.TwisterController, 0, 0)
            dialPaint.color = typedArray.getColor(R.styleable.TwisterController_color, Color.GRAY)
            markerPointPaint.color = typedArray.getColor(R.styleable.TwisterController_markerColor, Color.RED)
            typedArray.recycle()
        }
    }

    private var angle = 0f

    private val marginOfInnerCircle = 30f
    private val marginDialPointsFromInnerCircle = 10f

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
        val a = -(getAngle(event.x, event.y) - 200).toFloat()
        Log.i("TEST", a.toString())
        if (a < 0 && a > -70 && angle != -140f)
            angle = 0f
        else if (a > -140 && -70 >= a && angle != 0f)
            angle = -140f
        else if (a >= 0 || a <= -140) {
            angle = a
        }
        invalidate()
        return true
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