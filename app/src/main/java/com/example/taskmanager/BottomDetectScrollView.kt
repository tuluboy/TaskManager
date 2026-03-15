package com.example.taskmanager

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

class BottomDetectScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private var isAtBottom = false
    private var lastY = 0f
    private var pullDistance = 0f
    private var isPullingUp = false
    
    private var onPullUpAtBottomListener: (() -> Unit)? = null
    private var onScrollChangedListener: ((l: Int, t: Int, oldl: Int, oldt: Int) -> Unit)? = null
    
    private val PULL_THRESHOLD = 150
    
    fun setOnPullUpAtBottomListener(listener: () -> Unit) {
        onPullUpAtBottomListener = listener
    }
    
    fun setOnScrollChangedListener(listener: (l: Int, t: Int, oldl: Int, oldt: Int) -> Unit) {
        onScrollChangedListener = listener
    }
    
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollChangedListener?.invoke(l, t, oldl, oldt)
        
        val child = getChildAt(0)
        if (child != null) {
            val childHeight = child.height
            val scrollViewHeight = height
            val scrollY = t
            
            isAtBottom = scrollY + scrollViewHeight >= childHeight - 10
        }
    }
    
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = ev.rawY
                pullDistance = 0f
                isPullingUp = false
            }
            MotionEvent.ACTION_MOVE -> {
                val currentY = ev.rawY
                val deltaY = lastY - currentY
                
                if (isAtBottom && deltaY > 0) {
                    isPullingUp = true
                    pullDistance += deltaY
                    
                    if (pullDistance > PULL_THRESHOLD) {
                        onPullUpAtBottomListener?.invoke()
                        pullDistance = 0f
                    }
                } else {
                    pullDistance = 0f
                }
                
                lastY = currentY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pullDistance = 0f
                isPullingUp = false
            }
        }
        
        return super.dispatchTouchEvent(ev)
    }
}
