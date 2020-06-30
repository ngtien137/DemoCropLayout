package com.chim.croplayout

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout

class CropLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val DEF_COLOR_GRID = Color.WHITE
        val DEF_COLOR_CROP_BACKGROUND = Color.parseColor("#88000000")
    }

    private val rectView = RectF()
    private val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG) 
    private val paintGridBackground = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathGrid = Path()

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
        rectView.set(0f, 0f, w, h)
        pathGrid.moveTo(rectView.left,rectView.top)
        pathGrid.lineTo(rectView.right,rectView.top)
        pathGrid.lineTo(rectView.right,rectView.bottom)
        pathGrid.lineTo(rectView.left,rectView.bottom)
        pathGrid.lineTo(rectView.left,rectView.top)
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.let {
            drawCropLayout(it)
        }
    }

    private fun drawCropLayout(canvas:Canvas){
        canvas.drawPath(pathGrid,paintGrid)
    }
}