package com.example.taskmanager.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class TaskDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "TaskManager.db"
        private const val DATABASE_VERSION = 1
        
        // 表名和字段
        const val TABLE_TASKS = "tasks"
        const val COLUMN_ID = "_id"
        const val COLUMN_DATE = "date"
        const val COLUMN_WEEKDAY = "weekday"
        const val COLUMN_CREATE_TIME = "create_time"
        const val COLUMN_PROGRESS = "progress"
        const val COLUMN_COMPLETE_TIME = "complete_time"
        const val COLUMN_REMARK = "remark"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTasksTable = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT NOT NULL UNIQUE,
                $COLUMN_WEEKDAY TEXT,
                $COLUMN_CREATE_TIME TEXT,
                $COLUMN_PROGRESS INTEGER DEFAULT 0,
                $COLUMN_COMPLETE_TIME TEXT,
                $COLUMN_REMARK TEXT
            )
        """.trimIndent()
        
        db?.execSQL(createTasksTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }

    // 插入或更新任务
    fun insertOrUpdateTask(date: String, remark: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, date)
            put(COLUMN_REMARK, remark)
            
            // 只有在插入新记录时才设置创建时间
            val existingTask = getTaskByDate(date)
            if (existingTask == null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                put(COLUMN_CREATE_TIME, sdf.format(Date()))
                put(COLUMN_WEEKDAY, getCurrentWeekday())
            }
        }
        
        return db.insertWithOnConflict(TABLE_TASKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // 获取特定日期的任务
    fun getTaskByDate(date: String): Task? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TASKS,
            arrayOf(COLUMN_ID, COLUMN_DATE, COLUMN_WEEKDAY, COLUMN_CREATE_TIME, COLUMN_PROGRESS, COLUMN_COMPLETE_TIME, COLUMN_REMARK),
            "$COLUMN_DATE = ?",
            arrayOf(date),
            null, null, null
        )

        var task: Task? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val taskDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            val weekday = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WEEKDAY))
            val createTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATE_TIME))
            val progress = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS))
            val completeTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPLETE_TIME))
            val remark = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMARK))

            task = Task(id, taskDate, weekday, createTime, progress, completeTime, remark)
        }
        cursor.close()

        return task
    }

    // 更新任务进度
    fun updateProgress(date: String, progress: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_PROGRESS, progress)

        // 如果进度达到100%，则记录完成时间
        if (progress >= 100) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            values.put(COLUMN_COMPLETE_TIME, sdf.format(Date()))
        } else {
            values.putNull(COLUMN_COMPLETE_TIME)
        }

        val rowsAffected = db.update(
            TABLE_TASKS,
            values,
            "$COLUMN_DATE = ?",
            arrayOf(date)
        )

        return rowsAffected > 0
    }

    // 删除任务（清空任务）
    fun deleteTask(date: String): Boolean {
        val db = writableDatabase
        val rowsAffected = db.delete(
            TABLE_TASKS,
            "$COLUMN_DATE = ?",
            arrayOf(date)
        )

        return rowsAffected > 0
    }

    // 获取所有任务
    fun getAllTasks(): List<Task> {
        val db = readableDatabase
        val tasks = mutableListOf<Task>()
        
        val cursor = db.query(
            TABLE_TASKS,
            arrayOf(COLUMN_ID, COLUMN_DATE, COLUMN_WEEKDAY, COLUMN_CREATE_TIME, COLUMN_PROGRESS, COLUMN_COMPLETE_TIME, COLUMN_REMARK),
            null, null, null, null, null
        )

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            val weekday = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WEEKDAY))
            val createTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATE_TIME))
            val progress = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS))
            val completeTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPLETE_TIME))
            val remark = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMARK))

            tasks.add(Task(id, date, weekday, createTime, progress, completeTime, remark))
        }
        cursor.close()

        return tasks
    }

    private fun getCurrentWeekday(): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
    }
}

data class Task(
    val id: Int,
    val date: String,
    val weekday: String?,
    val createTime: String?,
    val progress: Int,
    val completeTime: String?,
    val remark: String
)