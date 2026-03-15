package com.example.taskmanager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

class TaskCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var hasContent = false // 是否有内容
    private var contentText = "" // 格子中的文字内容
    private var creationTime = "" // 创建时间
    private var weekday = "" // 星期几
    private var progressValue = 0 // 进度值
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cellWidth = 0
    private var cellHeight = 0
    
    // 滑动控制进度的相关变量
    private var isDragging = false
    private var showVerticalLine = false
    private var verticalLineX = 0f
    
    // 进度回调接口
    private var onProgressChangeListener: ((Int) -> Unit)? = null
    
    fun setOnProgressChangeListener(listener: (Int) -> Unit) {
        onProgressChangeListener = listener
    }
    
    fun setContent(content: String, time: String = "", dayOfWeek: String = "") {
        contentText = content
        hasContent = content.isNotEmpty()
        creationTime = time
        weekday = dayOfWeek
        invalidate()
    }
    
    fun setProgress(progress: Int) {
        progressValue = progress
        invalidate()
    }
    
    fun getProgress(): Int = progressValue
    
    fun getContent(): String = contentText
    
    fun hasContent(): Boolean = hasContent

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellWidth = w
        cellHeight = h
    }

    // 添加长按相关变量
    private var longPressHandler: android.os.Handler? = null
    private var isLongPressed = false
    private var touchDownTime = 0L
    
    init {
        // 设置长按时间为2秒
        setOnLongClickListener {
            // 长按时不需要特殊处理，只需允许后续移动事件更新进度
            true
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!hasContent) {
            // 如果格子没有内容，则不响应滑动手势，交给父类处理
            return super.onTouchEvent(event)
        }
        
        event?.let { ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownTime = System.currentTimeMillis()
                    // 启动长按检测计时器（2秒）
                    longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    val runnable = object : Runnable {
                        override fun run() {
                            // 2秒后进入长按状态
                            if (System.currentTimeMillis() - touchDownTime >= 2000) { // 确认确实过了2秒
                                isLongPressed = true
                                showVerticalLine = true
                                verticalLineX = ev.x
                                invalidate()
                                
                                // 立即根据当前位置更新进度
                                updateProgressFromPosition(ev.x)
                            }
                        }
                    }
                    longPressHandler?.postDelayed(runnable, 2000) // 2秒长按
                    
                    return true // 必须消费DOWN事件以接收后续的UP/MOVE事件
                }
                
                MotionEvent.ACTION_MOVE -> {
                    if (isLongPressed) {
                        // 只有在长按激活状态下才更新进度和竖线
                        verticalLineX = ev.x.coerceIn(40f, (cellWidth - 40).toFloat())
                        
                        // 更新进度值
                        updateProgressFromPosition(verticalLineX)
                        invalidate()
                        return true
                    }
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 移除长按计时器
                    longPressHandler?.removeCallbacksAndMessages(null)
                    longPressHandler = null
                    
                    if (isLongPressed) {
                        // 退出长按状态
                        isLongPressed = false
                        showVerticalLine = false
                        invalidate()
                        return true
                    } else {
                        // 如果不是长按状态，而是短时间内的点击，则让父级处理点击事件
                        // 检查是否是快速点击（小于2秒）
                        if (System.currentTimeMillis() - touchDownTime < 2000) {
                            // 这是一个短点击，应该触发onClick事件
                            performClick() // 触发click listener
                        }
                    }
                    
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event)
    }

    // 重写performClick以确保能正确处理点击事件
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    // 计算进度值的辅助函数
    private fun updateProgressFromPosition(xPos: Float) {
        val clampedX = xPos.coerceIn(40f, (cellWidth - 40).toFloat()) // 限制在左右各40像素范围内
        
        // 根据手指位置计算进度值
        val progressRange = (cellWidth - 80).toFloat() // 总可用宽度（减去两边的40px）
        val positionInSlider = (clampedX - 40f).coerceIn(0f, progressRange)
        val newProgress = ((positionInSlider / progressRange) * 100).toInt().coerceIn(0, 100)
        
        progressValue = newProgress
        onProgressChangeListener?.invoke(progressValue)
    }

    // 添加变量记录触摸状态
    private var downX = 0f
    private var downY = 0f
    private var isPotentialSwipe = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制背景色 - 默认白色，如果有内容则显示不同的颜色
        val backgroundColor = if (hasContent) {
            ContextCompat.getColor(context, R.color.cell_uncompleted_background) // 亮黄色
        } else {
            ContextCompat.getColor(context, R.color.cell_normal_background) // 白色或深色主题对应的背景色
        }
        
        paint.style = Paint.Style.FILL
        paint.color = backgroundColor
        canvas.drawRect(0f, 0f, cellWidth.toFloat(), cellHeight.toFloat(), paint)
        
        // 绘制边框
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = ContextCompat.getColor(context, R.color.cell_border_color)
        canvas.drawRect(0f, 0f, cellWidth.toFloat(), cellHeight.toFloat(), paint)
        
        // 绘制创建时间在左上角（如果有的话），日期和星期几在同一行
        if (creationTime.isNotEmpty() && weekday.isNotEmpty()) {
            paint.style = Paint.Style.FILL
            paint.textSize = 24f
            paint.color = ContextCompat.getColor(context, R.color.cell_uncompleted_text) // 红色字体
            paint.textAlign = Paint.Align.LEFT
            
            // 左上角显示日期和星期几在同一行
            val margin = 5f
            val timeText = "$creationTime $weekday"  // 同一行显示
            canvas.drawText(timeText, margin, margin + 20f, paint)
        }
        
        // 绘制内容文本（如果有的话）
        if (hasContent && contentText.isNotEmpty()) {
            paint.style = Paint.Style.FILL
            paint.textSize = 36f
            paint.color = ContextCompat.getColor(context, R.color.cell_uncompleted_text) // 红色字体
            paint.textAlign = Paint.Align.LEFT
            
            // 计算文本位置，避开左上角的时间区域
            val topMargin = if (creationTime.isNotEmpty() && weekday.isNotEmpty()) 60f else 10f
            val sideMargin = 10f
            val fm = paint.fontMetrics
            val textHeight = fm.descent - fm.ascent
            val availableHeight = cellHeight.toFloat() - topMargin - sideMargin
            val lineHeight = textHeight
            val maxLines = (availableHeight / lineHeight).toInt().coerceAtLeast(1)
            
            // 简单截断或换行文本以适应单元格
            var displayText = contentText
            if (paint.measureText(displayText) > cellWidth - 2 * sideMargin) {
                // 截断文本并添加省略号
                var endIndex = displayText.length
                while (endIndex > 0 && paint.measureText(displayText + "...") > cellWidth - 2 * sideMargin) {
                    endIndex--
                    displayText = contentText.substring(0, endIndex)
                }
                displayText += "..."
            }
            
            val y = topMargin + (availableHeight - textHeight) / 2 - fm.ascent
            canvas.drawText(displayText, sideMargin, y, paint)
        }
        
        // 绘制进度值%，位置与左上角的创建日期时间串同高，靠右显示，字体大小和颜色与日期串一致
        if (hasContent && progressValue >= 0) {  // 当格子有内容时显示进度值（包括0%）
            paint.style = Paint.Style.FILL
            paint.textSize = 24f  // 和日期时间一样的字体大小
            paint.color = ContextCompat.getColor(context, R.color.cell_uncompleted_text) // 红色字体
            paint.textAlign = Paint.Align.RIGHT // 右对齐
            
            val progressText = "${progressValue}%"
            val x = cellWidth - 5f  // 靠右对齐，留出边距
            val y = 5f + 20f  // 与左上角创建时间同一水平线
            canvas.drawText(progressText, x, y, paint)
        }
        
        // 如果正在拖动，则绘制橙色竖线
        if (showVerticalLine) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f  // 宽度为4
            paint.color = android.graphics.Color.parseColor("#FFA500") // 橙色
            canvas.drawLine(verticalLineX, 0f, verticalLineX, cellHeight.toFloat(), paint)
        }
    }
}