package com.yurii.youtubemusic.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.yurii.youtubemusic.R
import kotlin.collections.ArrayList
import kotlin.math.abs


typealias EventListener = (bandId: Int, level: Int, fromUser: Boolean) -> Unit

class EqualizerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0, defStyleRes: Int = 0) :
    ViewGroup(context, attrs, defStyle, defStyleRes), SeekBar.OnSeekBarChangeListener {

    companion object {
        private const val DEFAULT_BAND_SIZE = 3
        private const val BAND_NAME_HEIGHT = 30
        private const val BAND_PADDING = 20
    }

    var minValue = -1500
    var maxValue = 1500
    private var bandSize = 0
    private var progressDrawable = 0
    private var connectorColor = 0
    private var thumb = 0
    private var maxBand = 50
    private var bandNames: ArrayList<Int>? = null

    var listener: EventListener? = null

    private val bandList: ArrayList<BandView> = ArrayList(0)
    private var bandNameLayout: BandNameLayout? = null
    private var bandConnectorLayout: BandConnectorLayout? = null
    private var bandConnectorShadowView: BandConnectorShadowView? = null

    init {
        attrs?.let {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.EqualizerView, 0, 0)

            bandSize = typedArray.getInteger(R.styleable.EqualizerView_bands, DEFAULT_BAND_SIZE)
            connectorColor = typedArray.getColor(R.styleable.EqualizerView_connectorColor, Color.GREEN)
            progressDrawable = typedArray.getResourceId(R.styleable.EqualizerView_progressDrawable, R.drawable.seekbar_style)
            thumb = typedArray.getResourceId(R.styleable.EqualizerView_thumb, R.drawable.seekbar_thumb)
            typedArray.recycle()

            setup()
        }
    }

    fun setBands(bands: ArrayList<Int>) {
        if (bands.size > DEFAULT_BAND_SIZE) {
            bandSize = bands.size
            bandNames = bands
        }
    }

    fun setBandSettings(levels: Map<Int, Int>) = bandList.forEach {
        it.progress = (levels.getValue(it.id) - minValue) * maxBand / (maxValue - minValue)
    }

    fun setBandLevel(band: Int, level: Int) {
        val bv = bandList.find { band == it.id }
        bv?.progress = level
    }

    fun setBandListener(bandListener: EventListener) {
        listener = bandListener
    }

    fun draw() {
        setup()
    }

    private fun setup() {
        if (bandSize == 0)
            return

        maxBand = abs(minValue) + abs(maxValue)

        setWillNotDraw(false)

        removeAllViews()
        bandList.clear()

        for (index in 0 until bandSize) {
            val bv = BandView(context)
            bv.progressDrawable = resources.getDrawable(progressDrawable, null)
            bv.thumb = resources.getDrawable(thumb, null)
            bv.max = maxBand
            bv.progress = maxBand / 2
            bv.id = index
            bv.setPadding(toPx(BAND_PADDING), 0, toPx(BAND_PADDING), 0)
            bv.setOnSeekBarChangeListener(this)
            bandList.add(bv)
            addView(bv)
        }

        bandConnectorLayout = BandConnectorLayout(context)
        addView(bandConnectorLayout)

        bandConnectorShadowView = BandConnectorShadowView(context)
        addView(bandConnectorShadowView)

        bandNameLayout = BandNameLayout(context)
        bandNameLayout?.hertz = bandNames

        addView(bandNameLayout)
    }

    private fun setBandsVertically(width: Int, height: Int) {
        val distW = width / bandSize
        var left = (-height / 2 + (distW / 2)) + toPx(BAND_NAME_HEIGHT)
        var right = (height / 2 + (distW / 2)) - toPx(BAND_NAME_HEIGHT)

        for (band in bandList) {
            val forceBandHeight = toPx(20)
            val top = (height / 2 - forceBandHeight) - toPx(BAND_NAME_HEIGHT)
            val bottom = (height / 2 + forceBandHeight) - toPx(BAND_NAME_HEIGHT)
            band.bringToFront()
            band.layout(left, top, right, bottom)
            left += distW
            right += distW
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (bandSize == 0)
            return

        setBandsVertically(width, height)

        bandConnectorLayout?.layout(0, 0, width, height)
        bandConnectorLayout?.connect(bandList)

        bandConnectorShadowView?.layout(0, 0, width, height)
        bandConnectorShadowView?.draw(bandList)

        bandNameLayout?.layout(0, (height - toPx(BAND_NAME_HEIGHT * 2)), width, height)
    }

    private fun setGridLines(canvas: Canvas?) {
        val gridLinePaint = Paint()
        gridLinePaint.color = Color.GRAY
        gridLinePaint.alpha = 100
        gridLinePaint.strokeWidth = toPx(1).toFloat()
        gridLinePaint.style = Paint.Style.STROKE

        val distW = width.toFloat() / bandSize
        var currentX = distW - (distW / 2)
        for (i in 0 until bandSize) {
            val verticalGridPath = Path()
            verticalGridPath.moveTo(currentX, height.toFloat() - toPx(BAND_NAME_HEIGHT))
            verticalGridPath.lineTo(currentX, 0f)
            canvas?.drawPath(verticalGridPath, gridLinePaint)

            currentX += distW
        }

        val horizontalGridPath = Path()
        horizontalGridPath.moveTo(0f, (height.toFloat() / 2) - toPx(BAND_NAME_HEIGHT))
        horizontalGridPath.lineTo(width.toFloat(), (height.toFloat() / 2) - toPx(BAND_NAME_HEIGHT))
        canvas?.drawPath(horizontalGridPath, gridLinePaint)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (bandSize == 0)
            return

        setGridLines(canvas)
    }

    override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
        bandConnectorLayout?.connect(bandList)
        bandConnectorShadowView?.draw(bandList)
        listener?.invoke(seekbar?.id!!, convertProgressToValue(progress), fromUser)
    }

    private fun convertProgressToValue(progress: Int): Int = progress * (maxValue - minValue) / maxBand + minValue

    override fun onStartTrackingTouch(seekbar: SeekBar?) {
        //Not used
    }

    override fun onStopTrackingTouch(seekbar: SeekBar?) {
        //Not used
    }

    inner class BandView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
        androidx.appcompat.widget.AppCompatSeekBar(context, attrs, defStyle) {

        private val vertical = 270f // default: 270f

        init {
            rotation = vertical
        }

    }

    inner class BandConnectorLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
    ) : View(context, attrs, defStyle, defStyleRes) {

        private var pathPaint: Paint = Paint()
        private var path: Path = Path()

        init {
            pathPaint.color = connectorColor
            pathPaint.strokeWidth = toPx(5).toFloat()
            pathPaint.style = Paint.Style.STROKE
        }

        fun connect(bandList: ArrayList<BandView>) {
            path.reset()
            for ((index, band) in bandList.withIndex()) {
                val bounds = band.thumb.bounds
                val offset = (bounds.width() / 2) - toPx(BAND_NAME_HEIGHT * 2) - toPx(BAND_PADDING)

                val distW = width.toFloat() / bandList.size
                val x = bounds.centerX().toFloat()
                var y: Float
                if (index == 0) {
                    y = (distW / 2) * (index + 1)
                    path.moveTo(y, (height.toFloat() - x) + offset)
                } else {
                    y = ((distW) * (index + 1)) - (distW / 2)
                    path.lineTo(y, (height.toFloat() - x) + offset)
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            canvas?.drawPath(path, pathPaint)
        }
    }

    inner class BandNameLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
    ) : View(context, attrs, defStyle, defStyleRes) {

        var hertz: ArrayList<Int>? = null
        private val textPaint = Paint()

        init {
            setBackgroundColor(Color.WHITE)

            textPaint.color = Color.BLACK
            textPaint.alpha = 100
            textPaint.textSize = toPx(15).toFloat()
            textPaint.textAlign = Paint.Align.CENTER
            draw()
        }

        private fun draw() {
            invalidate()
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            hertz?.let {
                val distW = width / (hertz?.size ?: 3)
                var centerX = (distW / 2)
                for (hert in hertz!!) {
                    val name = String.format("%sHz", hert)
                    canvas?.drawText(name, centerX.toFloat(), (height / 2).toFloat(), textPaint)
                    centerX += distW
                }
            }

        }

    }

    inner class BandConnectorShadowView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
    ) : View(context, attrs, defStyle, defStyleRes) {

        private var pathPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var path: Path = Path()

        fun draw(bandList: ArrayList<BandView>) {
            path.reset()

            val startX = (width.toFloat() / bandList.size) / 2
            val startY = height.toFloat()
            path.moveTo(startX, startY)

            for ((index, band) in bandList.withIndex()) {
                val bounds = band.thumb.bounds
                val offset =
                    (bounds.width() / 2) - toPx(BAND_NAME_HEIGHT * 2) - toPx(BAND_PADDING)
                val distW = width.toFloat() / bandList.size
                val x = bounds.centerX().toFloat()
                var y: Float
                if (index == 0) {
                    y = (distW / 2) * (index + 1)
                    path.lineTo(y, (height.toFloat() - x) + offset)
                } else {
                    y = ((distW) * (index + 1)) - (distW / 2)
                    path.lineTo(y, (height.toFloat() - x) + offset)
                }
            }

            val endX = width.toFloat() - startX
            path.lineTo(endX, startY)

            path.close()
            pathPaint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), connectorColor, Color.TRANSPARENT, Shader.TileMode.MIRROR)

            invalidate()
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            canvas?.drawPath(path, pathPaint)
        }
    }
}