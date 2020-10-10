package com.parfoismeng.slidebacklib.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import java.time.Duration
import kotlin.math.max

/**
 * author : ParfoisMeng
 * time   : 2018/12/19
 * desc   : 边缘返回的图标View
 */
class SlideBackIconView constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    /**
     * 哪个方向
     */
    private var mBy = Gravity.LEFT

    // 路径对象
    private val bgPath: Path by lazy { Path() }
    private val realBgPath: Path by lazy { Path() }

    /**
     * 用于180度旋转 bgPath
     */
    private val bgPathMatrix: Matrix by lazy { Matrix() }
    private val arrowPath: Path by lazy { Path() }

    private var resetAnimator: ValueAnimator? = null

    // 画笔对象
    private val bgPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL_AND_STROKE // 填充内部和描边
            color = viewBackgroundColor
            strokeWidth = 0.01f // 画笔宽度
        }
    }
    private val arrowPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE // 描边
            color = Color.WHITE
            strokeWidth = 8f // 画笔宽度
            strokeJoin = Paint.Join.ROUND // * 结合处的样子 ROUND:圆弧
        }
    }

    var viewBackgroundColor = Color.BLACK // 控件背景色
    var viewHeight = 0f // 控件高度
    var viewArrowSize = 10f // 箭头图标大小
    var viewMaxLength = 0f // 最大拉动距离

    private var currentSlideLength = 0f // 当前拉动距离

    init {
        alpha = 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(viewMaxLength.toInt(), viewHeight.toInt())
    }

    /**
     * 因为过程中会多次绘制，所以要先重置路径再绘制。
     * 贝塞尔曲线没什么好说的，相关文章有很多。此曲线经测试比较类似“即刻App”。
     *
     *
     * 方便阅读再写一遍，此段代码中的变量定义：
     * iconViewHeight       控件高度
     * currentSlideLength   当前拉动距离
     * iconViewMaxLength    最大拉动距离
     * iconViewArrowSize    箭头图标大小
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 背景
        bgPathMatrix.reset()
        realBgPath.reset()
        bgPath.reset() // 会多次绘制，所以先重置
        bgPath.moveTo(0f, 0f)
        bgPath.cubicTo(0f, viewHeight * 2 / 9, currentSlideLength, viewHeight / 3, currentSlideLength, viewHeight / 2)
        bgPath.cubicTo(currentSlideLength, viewHeight * 2 / 3, 0f, viewHeight * 7 / 9, 0f, viewHeight)
        // 在右边时需要旋转 180 度
        bgPathMatrix.postRotate(if (mBy == Gravity.LEFT) 0f else 180f, viewMaxLength / 2, viewHeight / 2)
        realBgPath.addPath(bgPath, bgPathMatrix)
        canvas.drawPath(realBgPath, bgPaint) // 根据设置的贝塞尔曲线路径用画笔绘制

        // 箭头是先直线由短变长再折弯变成箭头状的
        // 依据当前拉动距离和最大拉动距离计算箭头大小值
        // 大小到一定值后开始折弯，计算箭头角度值
        val arrowZoom: Float = currentSlideLength / viewMaxLength // 箭头大小变化率
        val arrowAngle: Float = if (arrowZoom < 0.75f) 0f else (arrowZoom - 0.75f) * 2 // 箭头角度变化率
        // 箭头
        arrowPath.reset() // 先重置
        // 结合箭头大小值与箭头角度值设置折线路径
        arrowPath.moveTo(currentSlideLength / 2 + viewArrowSize * arrowAngle, viewHeight / 2 - arrowZoom * viewArrowSize)
        arrowPath.lineTo(currentSlideLength / 2 - viewArrowSize * arrowAngle, viewHeight / 2)
        arrowPath.lineTo(currentSlideLength / 2 + viewArrowSize * arrowAngle, viewHeight / 2 + arrowZoom * viewArrowSize)
        canvas.drawPath(arrowPath, arrowPaint)

        // 最多0.8透明度
        alpha = max(currentSlideLength / viewMaxLength - 0.2f, 0f)
    }

    /**
     * 更新当前拉动距离并重绘
     *
     * @param slideLength 当前拉动距离
     */
    fun updateSlideLength(slideLength: Float) {
        // 避免无意义重绘
        if (currentSlideLength == slideLength) return

        currentSlideLength = slideLength
        invalidate() // 会再次调用onDraw
    }

    /**
     * 给SlideBackIconView设置topMargin，起到定位效果
     *
     * @param position 触点位置
     */
    fun setSlideBackPosition(position: Float) {
        if (resetAnimator != null && resetAnimator!!.isRunning()) {
            return
        }
        // 触点位置减去SlideBackIconView一半高度即为topMargin
        val layoutParams = this.layoutParams as FrameLayout.LayoutParams
        layoutParams.topMargin = (position - viewHeight / 2).toInt()
        setLayoutParams(layoutParams)
    }

    /**
     * 恢复滑动
     */
    fun resetSlide(duration: Long) {
        if (resetAnimator != null && resetAnimator!!.isRunning) {
            resetAnimator?.cancel()
        }
        resetAnimator = ValueAnimator.ofFloat(currentSlideLength, 0f).setDuration(duration)
        resetAnimator?.addUpdateListener {
            val animatedValue = it.getAnimatedValue() as Float
            updateSlideLength(animatedValue)
        }
        resetAnimator?.start()
    }

    /**
     * 设置显示的位置：{@link Gravity#LEFT} 和 {@link Gravity#RIGHT}
     */
    fun setBy(by: Int) {
        if (resetAnimator != null && resetAnimator!!.isRunning()) {
            return
        }
        mBy = by
        val layoutParams = layoutParams as FrameLayout.LayoutParams
        when (mBy) {
            Gravity.LEFT -> {
                layoutParams.gravity = Gravity.LEFT
            }
            Gravity.RIGHT -> {
                layoutParams.gravity = Gravity.RIGHT
            }
        }
    }
}