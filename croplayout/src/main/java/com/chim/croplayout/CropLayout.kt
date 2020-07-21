package com.chim.croplayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.core.graphics.contains
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class CropLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val DEF_COLOR_GRID = Color.WHITE
        val DEF_COLOR_CROP_BACKGROUND = Color.parseColor("#88000000")
    }

    private val rectView = RectF()
    private val rectBorder = RectF()
    private val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val paintGridInside = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val paintDecoration = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintGridBackground = Paint(Paint.ANTI_ALIAS_FLAG)

    private val pathGrid = Path()
    private val pathGridInside = Path()
    private val pathBackground = Path()
    private val pathDecoration = Path()

    private var gridType = GridType.NONE
    private var decorationType = DecorationType.NONE
    private var decorationSize = 0f

    private var touchSensitivitySize = 0f
    private var isTouchMoving = false
    private var isMovingCropArea = false
    private var currentScaleMode = ScaleMode.CENTER
    private val currentPointAction = PointF()
    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }
    private var minCropPercent = 0.1f

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.CropLayout)
            gridType =
                when (ta.getInt(R.styleable.CropLayout_cl_grid_size, GridType.Size3x3.size)) {
                    GridType.Size3x3.size -> GridType.Size3x3
                    GridType.Size4x4.size -> GridType.Size4x4
                    GridType.Size9x9.size -> GridType.Size9x9
                    else -> GridType.NONE
                }
            paintGrid.color = ta.getColor(R.styleable.CropLayout_cl_grid_color, DEF_COLOR_GRID)
            paintGridInside.color =
                ta.getColor(R.styleable.CropLayout_cl_grid_color, paintGrid.color)
            paintGridBackground.color =
                ta.getColor(R.styleable.CropLayout_cl_grid_background, DEF_COLOR_CROP_BACKGROUND)
            paintGrid.strokeWidth = ta.getDimension(
                R.styleable.CropLayout_cl_grid_border_outside_size,
                getDimen(R.dimen.default_border_size)
            )
            paintGridInside.strokeWidth = ta.getDimension(
                R.styleable.CropLayout_cl_grid_border_inside_size,
                paintGrid.strokeWidth
            )

            var defaultDecorationSize = 0f
            decorationType =
                when (ta.getInt(R.styleable.CropLayout_cl_decoration, DecorationType.NONE.type)) {
                    DecorationType.CORNERS_LINE.type -> {
                        paintDecoration.style = Paint.Style.STROKE
                        defaultDecorationSize = paintGrid.strokeWidth
                        DecorationType.CORNERS_LINE
                    }
                    DecorationType.CORNERS_OVAL.type -> {
                        paintDecoration.style = Paint.Style.FILL
                        defaultDecorationSize = paintGrid.strokeWidth * 3
                        DecorationType.CORNERS_OVAL
                    }
                    else -> DecorationType.NONE
                }
            paintDecoration.color =
                ta.getColor(R.styleable.CropLayout_cl_decoration_color, paintGrid.color)
            decorationSize =
                ta.getDimension(R.styleable.CropLayout_cl_decoration_size, defaultDecorationSize)
            when (decorationType) {
                DecorationType.CORNERS_LINE -> {
                    paintDecoration.strokeWidth = decorationSize
                }
                DecorationType.CORNERS_OVAL -> {
                    paintDecoration.strokeWidth = 0f
                }
                else -> {
                    paintDecoration.strokeWidth = 0f
                }
            }

            minCropPercent = ta.getFloat(R.styleable.CropLayout_cl_min_crop_percent, 10f) / 100f

            touchSensitivitySize = ta.getDimension(
                R.styleable.CropLayout_cl_touch_sensitivity_size,
                getDimen(R.dimen.default_sensitivity_size)
            )
            ta.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val borderGridSizeHalf = paintGrid.strokeWidth / 2f
        rectView.set(0f, 0f, w, h)
        rectBorder.set(
            rectView.left + borderGridSizeHalf,
            rectView.top + borderGridSizeHalf,
            rectView.right - borderGridSizeHalf,
            rectView.bottom - borderGridSizeHalf
        )
        validateCropViewWithBorder()
    }

    private fun validateCropViewWithBorder() {
        validateGridWithBorder()
        validatePathBackgroundWithBorder()
        validateDecorationWithBorder()
    }

    private fun validateDecorationWithBorder() {
        pathDecoration.reset()
        when (decorationType) {
            DecorationType.CORNERS_LINE -> {
                val space = paintGrid.strokeWidth
                val size = min(rectBorder.width(), rectBorder.height())
                val lineSize = size * 10f / 100

                val leftDecor = rectBorder.left + space + paintDecoration.strokeWidth / 2f
                val rightDecor = rectBorder.right - space - paintDecoration.strokeWidth / 2f
                val topDecor = rectBorder.top + space + paintDecoration.strokeWidth / 2f
                val bottomDecor = rectBorder.bottom - space - paintDecoration.strokeWidth / 2f

                pathDecoration.moveTo(leftDecor + lineSize, topDecor)
                pathDecoration.lineTo(leftDecor, topDecor)
                pathDecoration.lineTo(leftDecor, topDecor + lineSize)

                pathDecoration.moveTo(rightDecor - lineSize, topDecor)
                pathDecoration.lineTo(rightDecor, topDecor)
                pathDecoration.lineTo(rightDecor, topDecor + lineSize)

                pathDecoration.moveTo(rightDecor, bottomDecor - lineSize)
                pathDecoration.lineTo(rightDecor, bottomDecor)
                pathDecoration.lineTo(rightDecor - lineSize, bottomDecor)

                pathDecoration.moveTo(leftDecor, bottomDecor - lineSize)
                pathDecoration.lineTo(leftDecor, bottomDecor)
                pathDecoration.lineTo(leftDecor + lineSize, bottomDecor)

            }
            DecorationType.CORNERS_OVAL -> {
                val ovalTopLeft = RectF().setCenter(
                    rectBorder.left,
                    rectBorder.top,
                    decorationSize,
                    decorationSize
                )
                val ovalTopRight = RectF().setCenter(
                    rectBorder.right,
                    rectBorder.top,
                    decorationSize,
                    decorationSize
                )
                val ovalBottomRight = RectF().setCenter(
                    rectBorder.right,
                    rectBorder.bottom,
                    decorationSize,
                    decorationSize
                )
                val ovalBottomLeft = RectF().setCenter(
                    rectBorder.left,
                    rectBorder.bottom,
                    decorationSize,
                    decorationSize
                )

                pathDecoration.addOval(ovalTopLeft, Path.Direction.CCW)
                pathDecoration.addOval(ovalTopRight, Path.Direction.CCW)
                pathDecoration.addOval(ovalBottomRight, Path.Direction.CCW)
                pathDecoration.addOval(ovalBottomLeft, Path.Direction.CCW)
            }
            else -> {
            }
        }
    }

    private fun validatePathBackgroundWithBorder() {
        val borderGridSizeHalf = paintGrid.strokeWidth / 2f
        pathBackground.reset()
        pathBackground.addRect(
            rectView.left,
            rectView.top,
            rectView.right,
            rectBorder.top - borderGridSizeHalf,
            Path.Direction.CCW
        )
        pathBackground.addRect(
            rectView.left,
            rectBorder.bottom + borderGridSizeHalf,
            rectView.right,
            rectView.bottom,
            Path.Direction.CCW
        )
        pathBackground.addRect(
            rectView.left,
            rectBorder.top - borderGridSizeHalf,
            rectBorder.left - borderGridSizeHalf,
            rectBorder.bottom + borderGridSizeHalf,
            Path.Direction.CCW
        )
        pathBackground.addRect(
            rectBorder.right + borderGridSizeHalf,
            rectBorder.top - borderGridSizeHalf,
            rectView.right,
            rectBorder.bottom + borderGridSizeHalf,
            Path.Direction.CCW
        )
    }

    private fun validateGridWithBorder() {
        initGridOutsize()
        initGridInside()
    }

    private fun initGridOutsize() {
        pathGrid.reset()
        pathGrid.moveTo(rectBorder.left, rectBorder.top)
        pathGrid.lineTo(rectBorder.right, rectBorder.top)
        pathGrid.lineTo(rectBorder.right, rectBorder.bottom)
        pathGrid.lineTo(rectBorder.left, rectBorder.bottom)
        pathGrid.lineTo(rectBorder.left, rectBorder.top)
        pathGrid.close()
    }

    private fun initGridInside() {
        pathGridInside.reset()
        if (gridType == GridType.NONE)
            return
        val widthGridItem = rectBorder.width() / gridType.size
        val heightGridItem = rectBorder.height() / gridType.size
        var offsetHorizontal = 0f
        var offsetVertical = 0f
        for (i in 1 until gridType.size) {
            offsetHorizontal = i * widthGridItem
            pathGridInside.moveTo(rectBorder.left + offsetHorizontal, rectBorder.top)
            pathGridInside.lineTo(rectBorder.left + offsetHorizontal, rectBorder.bottom)
            offsetVertical = i * heightGridItem
            pathGridInside.moveTo(rectBorder.left, rectBorder.top + offsetVertical)
            pathGridInside.lineTo(rectBorder.right, rectBorder.top + offsetVertical)
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.let {
            drawCropLayout(it)
        }
    }

    private fun drawCropLayout(canvas: Canvas) {
        canvas.drawPath(pathBackground, paintGridBackground)
        canvas.drawPath(pathGridInside, paintGridInside)
        canvas.drawPath(pathGrid, paintGrid)
        canvas.drawPath(pathDecoration, paintDecoration)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ev?.let {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    currentPointAction.set(ev.x, ev.y)
                    //eLog("Down")
                    currentScaleMode = checkTouchToScale(currentPointAction)
                    if (rectBorder.contains(currentPointAction))
                        isMovingCropArea = true
                    return if (currentScaleMode != ScaleMode.NONE || isMovingCropArea)
                        true
                    else super.dispatchTouchEvent(ev)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isTouchMoving) {
                        //eLog("Moving")
                        val disX = ev.x - currentPointAction.x
                        val disY = ev.y - currentPointAction.y
                        when {
                            currentScaleMode != ScaleMode.NONE -> {
                                scaleCropArea(disX.toInt(), disY.toInt())
                                currentPointAction.set(ev.x, ev.y)
                            }
                            isMovingCropArea -> {
                                moveCropArea(disX.toInt(), disY.toInt())
                                currentPointAction.set(ev.x, ev.y)
                            }
                            else -> {
                            }
                        }
                        return true
                    } else {
                        if (!isTouchMoving) {
                            if (abs(ev.x - currentPointAction.x) > touchSlop || abs(ev.y - currentPointAction.y) > touchSlop) {
                                isTouchMoving = true
                                return true
                            }
                        }
                        return super.dispatchTouchEvent(ev)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    //eLog("Up")
                    val isHandleChild = !(isTouchMoving || isMovingCropArea)
                    isTouchMoving = false
                    isMovingCropArea = false
                    return if (isHandleChild) {
                        super.dispatchTouchEvent(ev)
                    } else
                        true
                }
                else -> {
                    return super.dispatchTouchEvent(ev)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun moveCropArea(disX: Int, disY: Int) {
        val currentWidth = rectBorder.width()
        val currentHeight = rectBorder.height()
        val minLeft = 0f + paintGrid.strokeWidth / 2f
        val maxLeft = width - currentWidth - paintGrid.strokeWidth / 2f
        val minTop = 0f + paintGrid.strokeWidth / 2f
        val maxTop = height - currentHeight - paintGrid.strokeWidth / 2f
        val newLeft = when {
            rectBorder.left + disX < minLeft -> minLeft
            rectBorder.left + disX > maxLeft -> maxLeft
            else -> rectBorder.left + disX
        }
        val newTop = when {
            rectBorder.top + disY < minTop -> minTop
            rectBorder.top + disY > maxTop -> maxTop
            else -> rectBorder.top + disY
        }
        rectBorder.set(newLeft, newTop, newLeft + currentWidth, newTop + currentHeight)
        validateCropViewWithBorder()
        invalidate()
    }

    private fun scaleCropArea(disX: Int, disY: Int) {
        val minLeft = 0f + paintGrid.strokeWidth / 2f
        val minTop = 0f + paintGrid.strokeWidth / 2f
        val maxRight = width - paintGrid.strokeWidth / 2f
        val maxBottom = height - paintGrid.strokeWidth / 2f

        var addLeft = 0f
        var addTop = 0f
        var addRight = 0f
        var addBottom = 0f
        when (currentScaleMode) {
            ScaleMode.TOP_LEFT -> {
                addLeft += disX
                addTop += disY
            }
            ScaleMode.TOP_RIGHT -> {
                addTop += disY
                addRight += disX
            }
            ScaleMode.BOTTOM_LEFT -> {
                addLeft += disX
                addBottom += disY
            }
            ScaleMode.BOTTOM_RIGHT -> {
                addRight += disX
                addBottom += disY
            }
            ScaleMode.CENTER -> {

            }
            else -> {

            }
        }
        val left =
            if (rectBorder.left + addLeft < minLeft) minLeft else if (rectBorder.left + addLeft > rectBorder.right - minCropPercent * width) rectBorder.right - minCropPercent * width else rectBorder.left + addLeft
        val right =
            if (rectBorder.right + addRight > maxRight) maxRight else if (rectBorder.right + addRight < rectBorder.left + minCropPercent * width) rectBorder.left + minCropPercent * width else rectBorder.right + addRight
        val top =
            if (rectBorder.top + addTop < minTop) minTop else if (rectBorder.top + addTop > rectBorder.bottom - minCropPercent * height) rectBorder.bottom - minCropPercent * height else rectBorder.top + addTop
        val bottom =
            if (rectBorder.bottom + addBottom > maxBottom) maxBottom else if (rectBorder.bottom + addBottom < rectBorder.top + minCropPercent * height) rectBorder.top + minCropPercent * height else rectBorder.bottom + addBottom
        rectBorder.set(left, top, right, bottom)
        validateCropViewWithBorder()
        invalidate()
    }

    fun applyCropToImageView(@IdRes imageViewId: Int): Bitmap {
        return applyCropToImageView(findViewById<ImageView>(imageViewId))
    }

    fun applyCropToImageView(imageView: ImageView): Bitmap {
        val bitmap = imageView.drawable.toBitmap()
        val ratioW = width.toFloat() / bitmap.width
        val ratioH = height.toFloat() / bitmap.height
        val transX = (rectBorder.left - paintGrid.strokeWidth / 2f) / ratioW
        val transY = (rectBorder.top - paintGrid.strokeWidth / 2f) / ratioH
        val drawBitmap = Bitmap.createBitmap(
            (rectBorder.width() / ratioW).roundToInt(),
            (rectBorder.height() / ratioH).roundToInt(), Bitmap.Config.ARGB_8888
        )
        val matrix = Matrix()
        matrix.postTranslate(
            -transX,
            -transY
        )
        val c = Canvas(drawBitmap)
        c.drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
        imageView.setImageBitmap(drawBitmap)
        return bitmap
    }

    fun applyCropToTextureView(textureView: TextureView, videoWidth: Int, videoHeight: Int) {
        val ratioW = width.toFloat() / videoWidth
        val ratioH = height.toFloat() / videoHeight
        val transX = (rectBorder.left - paintGrid.strokeWidth / 2f) / ratioW
        val transY = (rectBorder.top - paintGrid.strokeWidth / 2f) / ratioH
        val matrix = Matrix()
        textureView.getTransform(matrix)
        matrix.postTranslate(
            -transX,
            -transY
        )
        textureView.setTransform(matrix)
    }

    fun convertToRealSize(oldW: Int, oldH: Int): Size {
        val ratioW = oldW.toFloat() / width
        val ratioH = oldH.toFloat() / height
        return Size(rectBorder.width() * ratioW, rectBorder.height() * ratioH)
    }

    private fun checkTouchToScale(pointF: PointF): ScaleMode {
        val rectScaleTopLeft = RectF().setCenter(
            rectBorder.left,
            rectBorder.top,
            touchSensitivitySize,
            touchSensitivitySize
        )
        val rectScaleTopRight = RectF().setCenter(
            rectBorder.right,
            rectBorder.top,
            touchSensitivitySize,
            touchSensitivitySize
        )
        val rectScaleBottomLeft = RectF().setCenter(
            rectBorder.left,
            rectBorder.bottom,
            touchSensitivitySize,
            touchSensitivitySize
        )
        val rectScaleBottomRight = RectF().setCenter(
            rectBorder.right,
            rectBorder.bottom,
            touchSensitivitySize,
            touchSensitivitySize
        )
        return when {
            rectScaleTopLeft.contains(pointF) -> ScaleMode.TOP_LEFT
            rectScaleTopRight.contains(pointF) -> ScaleMode.TOP_RIGHT
            rectScaleBottomLeft.contains(pointF) -> ScaleMode.BOTTOM_LEFT
            rectScaleBottomRight.contains(pointF) -> ScaleMode.BOTTOM_RIGHT
            else -> ScaleMode.NONE
        }
    }

    enum class ScaleMode {
        CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, NONE
    }

    enum class GridType(val size: Int) {
        NONE(0), Size3x3(3), Size4x4(4), Size9x9(9)
    }

    enum class DecorationType(val type: Int) {
        NONE(0), CORNERS_LINE(1), CORNERS_OVAL(2)
    }

    class Size(var width: Int = 0, var height: Int = 0) {
        constructor(w: Float, h: Float) : this(w.toInt(), h.toInt())

        override fun toString(): String {
            return "Size(width=$width, height=$height)"
        }


    }
}