package com.yurii.youtubemusic.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.IntRange
import androidx.core.graphics.toRectF
import androidx.core.view.GestureDetectorCompat
import com.yurii.youtubemusic.R
import kotlin.math.min


class DownloadButton(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private var mSize: Int = toPx(50)
        set(value) {
            field = value
            mProgressBarRect.left = mProgressBarPadding
            mProgressBarRect.top = mProgressBarPadding
            mProgressBarRect.right = mSize - mProgressBarPadding
            mProgressBarRect.bottom = mSize - mProgressBarPadding

            mProgressBarStrokeWidth = mSize * 0.07f
            mIconStrokeWidth = mSize * 0.07f
            mProgressBarPadding = mSize * 0.07f

            mViewHolder.bottom = mSize
            mViewHolder.right = mSize
        }

    private var mBackgroundColor = DEFAULT_BACKGROUND_COLOR
    private var mProgressColor = DEFAULT_PROGRESS_COLOR
    private var mIconColor = DEFAULT_ICON_COLOR

    private var mProgressBarPadding = 0f
    private var mProgressBarStrokeWidth = 0f
    private var mIconStrokeWidth = 0f

    private var mIsHover = false

    var state: Int = STATE_DOWNLOAD
        set(value) {
            field = value
            invalidate()
        }

    @IntRange(from = 0, to = 100)
    var progress: Int = 0

    private var onClickListener: ((view: View) -> Unit)? = null
    private var onLongClickDownloadListener: ((view: View) -> Unit)? = null

    private var mViewHolder = Rect(0, 0, mSize, mSize)

    private var mStartProgressAngle = 0f

    private var mProgressBarRect = RectF(mProgressBarPadding, mProgressBarPadding, mSize - mProgressBarPadding, mSize - mProgressBarPadding)
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mPath = Path()

    private val mGestureClickListener = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?) = true
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            invokeClickListenerCallBack()
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
            super.onLongPress(e)
            if (state == STATE_DOWNLOAD)
                onLongClickDownloadListener?.invoke(this@DownloadButton).also {
                    vibrationEffect()
                }
        }
    })

    init {
        setUpAttributes(attributeSet)
    }

    private fun invokeClickListenerCallBack() {
        onClickListener?.invoke(this)
    }


    private val mProgressAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val value = it.animatedValue as Float
            mStartProgressAngle = value
            invalidate()
        }
    }

    fun setOnClickStateListener(onClickListener: (view: View) -> Unit) {
        this.onClickListener = onClickListener
    }

    fun setOnLongClickDownloadLister(onLongClickDownloadListener: (view: View) -> Unit) {
        this.onLongClickDownloadListener = onLongClickDownloadListener
    }

    private fun vibrationEffect() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26)
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        else
            vibrator.vibrate(50)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mProgressAnimator.start()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_CANCEL) {
            mIsHover = false
        }

        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> mIsHover = true
            MotionEvent.ACTION_UP -> mIsHover = false
            MotionEvent.ACTION_MOVE -> mIsHover = mViewHolder.contains(event.x.toInt(), event.y.toInt())
        }

        return mGestureClickListener.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun setUpAttributes(attributeSet: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(attributeSet, R.styleable.DownloadButton, 0, 0)

        mBackgroundColor = typedArray.getColor(R.styleable.DownloadButton_backgroundColor, DEFAULT_BACKGROUND_COLOR)
        mProgressColor = typedArray.getColor(R.styleable.DownloadButton_progressColor, DEFAULT_PROGRESS_COLOR)
        mIconColor = typedArray.getColor(R.styleable.DownloadButton_iconColor, DEFAULT_ICON_COLOR)

        progress = typedArray.getInt(R.styleable.DownloadButton_progress, 0)
        state = typedArray.getInt(R.styleable.DownloadButton_state, STATE_DOWNLOAD)

        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val exactSize = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY ||
                MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY

        if (exactSize) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)

            mSize = min(width, height)
        }

        setMeasuredDimension(mSize, mSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCircle(canvas)

        when (state) {
            STATE_DOWNLOAD -> drawDownloadIcon(canvas)
            STATE_DOWNLOADING -> drawCancelIconWithProgress(canvas)
            STATE_DOWNLOADED -> drawDeleteIcon(canvas)
            STATE_FAILED -> drawFailIcon(canvas)
        }

        if (mIsHover)
            drawHoverEffect(canvas)
    }

    private fun drawFailIcon(canvas: Canvas) {
        mPaint.color = Color.parseColor("#F44336")
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = mIconStrokeWidth * 0.75f

        mPaint.style = Paint.Style.FILL
        canvas.drawCircle(mSize * 0.5f, mSize * 0.62f, mIconStrokeWidth * 0.75f / 2, mPaint)

        mPaint.style = Paint.Style.STROKE

        mPath.reset()
        mPath.moveTo(mSize * 0.5f, mSize * 0.2f)
        mPath.lineTo(mSize * 0.75f, mSize * 0.7f)

        mPath.moveTo(mSize * 0.5f, mSize * 0.2f)
        mPath.lineTo(mSize * 0.25f, mSize * 0.7f)

        mPath.moveTo(mSize * 0.25f, mSize * 0.7f)
        mPath.lineTo(mSize * 0.75f, mSize * 0.7f)

        canvas.drawPath(mPath, mPaint)

        mPath.reset()
        mPaint.strokeWidth = mIconStrokeWidth * 0.5f

        mPath.moveTo(mSize * 0.5f, mSize * 0.4f)
        mPath.lineTo(mSize * 0.5f, mSize * 0.55f)


        canvas.drawPath(mPath, mPaint)
    }

    private fun drawCancelIconWithProgress(canvas: Canvas) {
        drawProgress(canvas)
        drawCancelIcon(canvas)
    }

    private fun drawCancelIcon(canvas: Canvas) {
        mPaint.color = mIconColor
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = mIconStrokeWidth

        mPath.reset()

        mPath.moveTo(mSize * 0.3f, mSize * 0.3f)
        mPath.lineTo(mSize * 0.7f, mSize * 0.7f)

        mPath.moveTo(mSize * 0.7f, mSize * 0.3f)
        mPath.lineTo(mSize * 0.3f, mSize * 0.7f)

        canvas.drawPath(mPath, mPaint)
    }

    private fun drawDeleteIcon(canvas: Canvas) {
        mPaint.color = mIconColor
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = mIconStrokeWidth * 0.75f

        mPath.reset()

        mPath.moveTo(mSize * 0.30f, mSize * 0.35f)
        mPath.lineTo(mSize * 0.70f, mSize * 0.35f)

        mPath.moveTo(mSize * 0.35f, mSize * 0.35f)
        mPath.lineTo(mSize * 0.35f, mSize * 0.75f)

        mPath.moveTo(mSize * 0.35f, mSize * 0.75f)
        mPath.lineTo(mSize * 0.65f, mSize * 0.75f)

        mPath.moveTo(mSize * 0.65f, mSize * 0.75f)
        mPath.lineTo(mSize * 0.65f, mSize * 0.35f)

        mPath.moveTo(mSize * 0.45f, mSize * 0.35f)
        mPath.lineTo(mSize * 0.45f, mSize * 0.30f)

        mPath.moveTo(mSize * 0.45f, mSize * 0.30f)
        mPath.lineTo(mSize * 0.55f, mSize * 0.30f)

        mPath.moveTo(mSize * 0.55f, mSize * 0.30f)
        mPath.lineTo(mSize * 0.55f, mSize * 0.35f)

        canvas.drawPath(mPath, mPaint)

        mPaint.strokeWidth = mIconStrokeWidth * 0.75f

        mPath.moveTo(mSize * 0.5f, mSize * 0.45f)
        mPath.lineTo(mSize * 0.5f, mSize * 0.65f)

        mPath.moveTo(mSize * 0.43f, mSize * 0.45f)
        mPath.lineTo(mSize * 0.43f, mSize * 0.65f)

        mPath.moveTo(mSize * 0.57f, mSize * 0.45f)
        mPath.lineTo(mSize * 0.57f, mSize * 0.65f)

        canvas.drawPath(mPath, mPaint)

    }

    private fun drawDownloadIcon(canvas: Canvas) {
        mPaint.color = mIconColor
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = mIconStrokeWidth

        mPath.reset()
        mPath.moveTo(mSize * 0.5f, mSize * 0.3f)
        mPath.lineTo(mSize * 0.5f, mSize * 0.7f)

        mPath.moveTo(mSize * 0.5f, mSize * 0.7f)
        mPath.lineTo(mSize * 0.3f, mSize * 0.5f)

        mPath.moveTo(mSize * 0.5f, mSize * 0.7f)
        mPath.lineTo(mSize * 0.7f, mSize * 0.5f)


        canvas.drawPath(mPath, mPaint)
    }

    private fun drawProgress(canvas: Canvas) {
        mPaint.color = mProgressColor
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = mProgressBarStrokeWidth
        mPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(mProgressBarRect, mStartProgressAngle, progress * 350f / 100f + 10f, false, mPaint)
    }

    private fun drawHoverEffect(canvas: Canvas) {
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.argb(30, 40, 23, 34)
        canvas.drawArc(mViewHolder.toRectF(), 0f, 360f, true, mPaint)
    }


    private fun drawCircle(canvas: Canvas) {
        mPaint.style = Paint.Style.FILL
        mPaint.color = mBackgroundColor
        canvas.drawArc(mViewHolder.toRectF(), 0f, 360f, true, mPaint)
    }

    companion object {
        const val DEFAULT_BACKGROUND_COLOR = Color.GRAY
        const val DEFAULT_PROGRESS_COLOR = Color.WHITE
        const val DEFAULT_ICON_COLOR = Color.WHITE

        const val STATE_DOWNLOAD: Int = 0
        const val STATE_DOWNLOADING: Int = 1
        const val STATE_DOWNLOADED: Int = 2
        const val STATE_FAILED: Int = 3
    }
}