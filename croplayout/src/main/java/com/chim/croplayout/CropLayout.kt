package com.chim.croplayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

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
    private val paintGridBackground = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathGrid = Path()

    private var touchAction = Touch.NONE
    private val pointDown = PointF()
    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.CropLayout)
            paintGrid.color = ta.getColor(R.styleable.CropLayout_cl_grid_color, DEF_COLOR_GRID)
            paintGridBackground.color =
                ta.getColor(R.styleable.CropLayout_cl_grid_background, DEF_COLOR_CROP_BACKGROUND)
            paintGrid.strokeWidth = ta.getDimension(
                R.styleable.CropLayout_cl_grid_border_outside_size,
                getDimen(R.dimen.default_border_size)
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
        pathGrid.moveTo(rectBorder.left, rectBorder.top)
        pathGrid.lineTo(rectBorder.right, rectBorder.top)
        pathGrid.lineTo(rectBorder.right, rectBorder.bottom)
        pathGrid.lineTo(rectBorder.left, rectBorder.bottom)
        pathGrid.lineTo(rectBorder.left, rectBorder.top)
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.let {
            drawCropLayout(it)
        }
    }

    private fun drawCropLayout(canvas: Canvas) {
        canvas.drawPath(pathGrid, paintGrid)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        ev?.let {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pointDown.set(ev.x, ev.y)
                    //eLog("Down")
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (touchAction == Touch.MOVING) {
                        //eLog("Moving")
                        return false
                    } else {
                        if (touchAction != Touch.SCALING && touchAction != Touch.MOVING) {
                            if (abs(ev.x - pointDown.x) > touchSlop || abs(ev.y - pointDown.y) > touchSlop) {
                                touchAction = Touch.MOVING
                            }
                        }
                        return false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    //eLog("Up")
                    touchAction = Touch.NONE
                    return true
                }
                else -> {
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    enum class Touch {
        NONE, MOVING, SCALING
    }
}