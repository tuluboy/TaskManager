package com.example.taskmanager

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.taskmanager.database.TaskDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var dbHelper: TaskDatabaseHelper
    private lateinit var tasksContainer: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        dbHelper = TaskDatabaseHelper(this)
        tasksContainer = findViewById(R.id.tasksContainer)
        
        setupUI()
    }
    
    private fun setupUI() {
        // 生成固定数量的任务格子（例如10个任务项）
        for (i in 0 until 10) {
            addTaskCell(i + 1)
        }
    }
    
    private fun addTaskCell(position: Int) {
        val inflater = LayoutInflater.from(this)
        val cellView = inflater.inflate(R.layout.task_cell, tasksContainer, false)
        
        val taskCellView = cellView.findViewById<TaskCellView>(R.id.slidingCellView) // 注意ID仍然是slidingCellView，因为布局文件没改
        
        // 从数据库加载对应位置的任务内容
        val taskId = position.toString()
        val task = dbHelper.getTaskByDate(taskId) // 使用position作为标识符
        val content = task?.remark ?: ""
        
        // 提取日期和星期几信息用于显示
        val creationTime = if (!content.isEmpty() && task?.createTime != null) {
            // 提取日期部分（去掉时间）
            task.createTime.substring(0, 10) // "YYYY-MM-DD"
        } else {
            ""
        }
        val weekday = if (!content.isEmpty()) {
            task?.weekday ?: ""
        } else {
            ""
        }
        
        taskCellView.setContent(content, creationTime, weekday)
        taskCellView.setProgress(task?.progress ?: 0)  // 设置进度值，默认为0
        
        // 设置进度变化监听器，用于保存进度到数据库
        taskCellView.setOnProgressChangeListener { newProgress ->
            dbHelper.updateProgress(position.toString(), newProgress)
        }
        
        // 设置双击事件处理
        var clickCount = 0
        var lastClickTime = 0L
        val DOUBLE_CLICK_TIME_DELTA = 300 // 双击间隔时间（毫秒）
        
        taskCellView.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                clickCount++
            } else {
                clickCount = 1
            }
            lastClickTime = currentTime
            
            if (clickCount == 2) { // 双击触发
                // 每次双击时都从数据库获取最新内容
                val currentTask = dbHelper.getTaskByDate(taskId)
                val currentContent = currentTask?.remark ?: ""
                showTaskDialog(position, taskCellView, currentContent)
                clickCount = 0
            }
        }
        
        tasksContainer.addView(cellView)
    }
    
    private fun showTaskDialog(position: Int, taskCellView: TaskCellView, currentContent: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.input_dialog, null)
        val inputField = dialogView.findViewById<EditText>(R.id.taskInput)
        
        // 设置输入框的内容和属性
        inputField.setText(currentContent)
        inputField.setSelection(currentContent.length) // 将光标移到文本末尾
        
        if (currentContent.isNotEmpty()) {
            // 只读模式：设置为不可编辑，但仍可滚动查看
            inputField.isFocusable = false
            inputField.isCursorVisible = false
            inputField.keyListener = null // 移除键盘输入能力
        } else {
            // 编辑模式：确保可以编辑
            inputField.isFocusable = true
            inputField.isFocusableInTouchMode = true
            inputField.isCursorVisible = true
            inputField.hint = "请输入任务内容..."
        }
        
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(if (currentContent.isNotEmpty()) "关闭" else "确定") { _, _ ->
                if (currentContent.isEmpty()) {
                    // 仅在编辑模式下保存内容
                    val newContent = inputField.text.toString().trim()
                    if (newContent.isNotEmpty()) {
                        // 保存到数据库
                        dbHelper.insertOrUpdateTask(position.toString(), newContent)
                        
                        // 更新界面显示
                        val updatedTask = dbHelper.getTaskByDate(position.toString())
                        val creationTime = if (updatedTask?.createTime != null) {
                            updatedTask.createTime.substring(0, 10) // "YYYY-MM-DD"
                        } else {
                            ""
                        }
                        val weekday = updatedTask?.weekday ?: ""
                        
                        taskCellView.setContent(newContent, creationTime, weekday)
                        taskCellView.setProgress(updatedTask?.progress ?: 0)  // 确保进度值正确设置
                    }
                }
            }
            .setNegativeButton(if (currentContent.isNotEmpty()) null else "取消", null)
            .create()
            
        alertDialog.show()
        
        // 如果是编辑模式，自动弹出软键盘并聚焦到输入框
        if (currentContent.isEmpty()) {
            inputField.requestFocus()
            // 这里无法直接调用键盘，将在dialog显示后再处理
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
