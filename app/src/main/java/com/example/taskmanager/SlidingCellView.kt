package com.example.taskmanager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class TaskCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var hasContent = false
    private var contentText = ""
    private var creationTime = ""
    private var weekday = ""
    private var progressValue = 0
    private var completeTime = ""
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cellWidth = 0
    private var cellHeight = 0
    
    private var isDragging = false
    private var showVerticalLine = false
    private var verticalLineX = 0f
    
    private var onProgressChangeListener: ((Int) -> Unit)? = null
    
    fun setOnProgressChangeListener(listener: (Int) -> Unit) {
        onProgressChangeListener = listener
    }
    
    fun setContent(content: String, time: String = "", dayOfWeek: String = "", completeTimeStr: String = "") {
        contentText = content
        hasContent = content.isNotEmpty()
        creationTime = time
        weekday = dayOfWeek
        completeTime = completeTimeStr
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

    private var longPressHandler: android.os.Handler? = null
    private var isLongPressed = false
    private var touchDownTime = 0L
    
    init {
        setOnLongClickListener {
            true
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!hasContent) {
            return super.onTouchEvent(event)
        }
        
        event?.let { ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownTime = System.currentTimeMillis()
                    longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    val runnable = object : Runnable {
                        override fun run() {
                            if (System.currentTimeMillis() - touchDownTime >= 1000) {
                                isLongPressed = true
                                showVerticalLine = true
                                verticalLineX = ev.x
                                invalidate()
                                updateProgressFromPosition(ev.x)
                            }
                        }
                    }
                    longPressHandler?.postDelayed(runnable, 1000)
                    return true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    if (isLongPressed) {
                        verticalLineX = ev.x.coerceIn(40f, (cellWidth - 40).toFloat())
                        updateProgressFromPosition(verticalLineX)
                        invalidate()
                        return true
                    }
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressHandler?.removeCallbacksAndMessages(null)
                    longPressHandler = null
                    
                    if (isLongPressed) {
                        isLongPressed = false
                        showVerticalLine = false
                        invalidate()
                        return true
                    } else {
                        if (System.currentTimeMillis() - touchDownTime < 1000) {
                            performClick()
                        }
                    }
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateProgressFromPosition(xPos: Float) {
        val clampedX = xPos.coerceIn(40f, (cellWidth - 40).toFloat())
        val progressRange = (cellWidth - 80).toFloat()
        val positionInSlider = (clampedX - 40f).coerceIn(0f, progressRange)
        val newProgress = ((positionInSlider / progressRange) * 100).toInt().coerceIn(0, 100)
        
        progressValue = newProgress
        onProgressChangeListener?.invoke(progressValue)
    }

    private var downX = 0f
    private var downY = 0f
    private var isPotentialSwipe = false

    private fun interpolateColor(startColor: Int, endColor: Int, progress: Int): Int {
        val ratio = progress / 100f
        val r = (Color.red(startColor) + (Color.red(endColor) - Color.red(startColor)) * ratio).toInt()
        val g = (Color.green(startColor) + (Color.green(endColor) - Color.green(startColor)) * ratio).toInt()
        val b = (Color.blue(startColor) + (Color.blue(endColor) - Color.blue(startColor)) * ratio).toInt()
        return Color.rgb(r, g, b)
    }
    
    private fun getWeekdayFromDateString(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(dateStr)
            val weekdayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            weekdayFormat.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val backgroundColor = if (hasContent) {
            val startBgColor = Color.parseColor("#FFFF00")
            val endBgColor = Color.parseColor("#000000")
            interpolateColor(startBgColor, endBgColor, progressValue)
        } else {
            ContextCompat.getColor(context, R.color.cell_normal_background)
        }
        
        paint.style = Paint.Style.FILL
        paint.color = backgroundColor
        canvas.drawRect(0f, 0f, cellWidth.toFloat(), cellHeight.toFloat(), paint)
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = ContextCompat.getColor(context, R.color.cell_border_color)
        canvas.drawRect(0f, 0f, cellWidth.toFloat(), cellHeight.toFloat(), paint)
        
        val textColor = if (hasContent) {
            val startTextColor = Color.parseColor("#000000")
            val endTextColor = Color.parseColor("#666666")
            interpolateColor(startTextColor, endTextColor, progressValue)
        } else {
            Color.parseColor("#000000")
        }
        
        if (creationTime.isNotEmpty() && weekday.isNotEmpty()) {
            paint.style = Paint.Style.FILL
            paint.textSize = 24f
            paint.color = textColor
            paint.textAlign = Paint.Align.LEFT
            
            val margin = 5f
            val timeText = "$creationTime $weekday"
            canvas.drawText(timeText, margin, margin + 20f, paint)
        }
        
        if (hasContent && contentText.isNotEmpty()) {
            paint.style = Paint.Style.FILL
            paint.textSize = 36f
            paint.color = textColor
            paint.textAlign = Paint.Align.LEFT
            
            val topMargin = if (creationTime.isNotEmpty() && weekday.isNotEmpty()) 60f else 10f
            val sideMargin = 10f
            val fm = paint.fontMetrics
            val textHeight = fm.descent - fm.ascent
            val availableHeight = cellHeight.toFloat() - topMargin - sideMargin
            val lineHeight = textHeight
            val maxLines = (availableHeight / lineHeight).toInt().coerceAtLeast(1)
            
            var displayText = contentText
            if (paint.measureText(displayText) > cellWidth - 2 * sideMargin) {
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
        
        if (hasContent && progressValue >= 0) {
            paint.style = Paint.Style.FILL
            paint.textSize = 24f
            paint.color = textColor
            paint.textAlign = Paint.Align.RIGHT
            
            val progressText = "${progressValue}%"
            val x = cellWidth - 5f
            val y = 5f + 20f
            canvas.drawText(progressText, x, y, paint)
        }
        
        if (hasContent && progressValue == 100 && completeTime.isNotEmpty()) {
            val completeDate = try {
                completeTime.substring(0, 10)
            } catch (e: Exception) {
                ""
            }
            val completeWeekday = getWeekdayFromDateString(completeTime)
            
            if (completeDate.isNotEmpty()) {
                paint.style = Paint.Style.FILL
                paint.textSize = 24f
                paint.color = textColor
                paint.textAlign = Paint.Align.RIGHT
                
                val completeText = "$completeDate $completeWeekday"
                val x = cellWidth - 5f
                val y = cellHeight - 5f
                canvas.drawText(completeText, x, y, paint)
            }
        }
        
        if (showVerticalLine) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.parseColor("#FFA500")
            canvas.drawLine(verticalLineX, 0f, verticalLineX, cellHeight.toFloat(), paint)
        }
    }
}
