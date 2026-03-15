package com.example.taskmanager

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.taskmanager.database.TaskDatabaseHelper

class MainActivity : AppCompatActivity() {
    
    private lateinit var dbHelper: TaskDatabaseHelper
    private lateinit var tasksContainer: LinearLayout
    private lateinit var scrollView: BottomDetectScrollView
    
    private var currentCellCount = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        dbHelper = TaskDatabaseHelper(this)
        tasksContainer = findViewById(R.id.tasksContainer)
        scrollView = findViewById(R.id.scrollView)
        
        setupUI()
        setupScrollViewListener()
    }
    
    private fun setupUI() {
        for (i in 0 until 10) {
            addTaskCell(i + 1)
        }
        currentCellCount = 10
    }
    
    private fun setupScrollViewListener() {
        scrollView.setOnPullUpAtBottomListener {
            addNewCell()
        }
    }
    
    private fun addNewCell() {
        currentCellCount++
        addTaskCell(currentCellCount)
    }
    
    private fun addTaskCell(position: Int) {
        val inflater = LayoutInflater.from(this)
        val cellView = inflater.inflate(R.layout.task_cell, tasksContainer, false)
        
        val taskCellView = cellView.findViewById<TaskCellView>(R.id.slidingCellView)
        
        val taskId = position.toString()
        val task = dbHelper.getTaskByDate(taskId)
        val content = task?.remark ?: ""
        
        val creationTime = if (!content.isEmpty() && task?.createTime != null) {
            task.createTime.substring(0, 10)
        } else {
            ""
        }
        val weekday = if (!content.isEmpty()) {
            task?.weekday ?: ""
        } else {
            ""
        }
        val completeTime = task?.completeTime ?: ""
        
        taskCellView.setContent(content, creationTime, weekday, completeTime)
        taskCellView.setProgress(task?.progress ?: 0)
        
        taskCellView.setOnProgressChangeListener { newProgress ->
            dbHelper.updateProgress(position.toString(), newProgress)
            if (newProgress == 100) {
                val updatedTask = dbHelper.getTaskByDate(taskId)
                val completeTime = updatedTask?.completeTime ?: ""
                taskCellView.setContent(
                    taskCellView.getContent(),
                    creationTime,
                    weekday,
                    completeTime
                )
            }
        }
        
        var clickCount = 0
        var lastClickTime = 0L
        val DOUBLE_CLICK_TIME_DELTA = 300
        
        taskCellView.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                clickCount++
            } else {
                clickCount = 1
            }
            lastClickTime = currentTime
            
            if (clickCount == 2) {
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
        
        inputField.setText(currentContent)
        inputField.setSelection(currentContent.length)
        
        if (currentContent.isNotEmpty()) {
            inputField.isFocusable = false
            inputField.isCursorVisible = false
            inputField.keyListener = null
        } else {
            inputField.isFocusable = true
            inputField.isFocusableInTouchMode = true
            inputField.isCursorVisible = true
            inputField.hint = "请输入任务内容..."
        }
        
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        alertDialog.setOnCancelListener {
            if (currentContent.isEmpty()) {
                val newContent = inputField.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    dbHelper.insertOrUpdateTask(position.toString(), newContent)
                    
                    val updatedTask = dbHelper.getTaskByDate(position.toString())
                    val creationTime = if (updatedTask?.createTime != null) {
                        updatedTask.createTime.substring(0, 10)
                    } else {
                        ""
                    }
                    val weekday = updatedTask?.weekday ?: ""
                    val completeTime = updatedTask?.completeTime ?: ""
                    
                    taskCellView.setContent(newContent, creationTime, weekday, completeTime)
                    taskCellView.setProgress(updatedTask?.progress ?: 0)
                }
            }
        }
            
        alertDialog.show()
        
        if (currentContent.isEmpty()) {
            inputField.requestFocus()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
