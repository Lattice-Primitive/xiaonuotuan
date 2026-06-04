package com.nanackations2.papaya.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nanackations2.papaya.data.db.AppDatabase
import com.nanackations2.papaya.data.db.TodoEntity
import com.nanackations2.papaya.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar

class TodoRepository(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).todoDao()

    fun getAllTodos(): Flow<List<TodoEntity>> = dao.getAllTodos()

    fun getTodosByPerson(personId: String): Flow<List<TodoEntity>> = dao.getTodosByPerson(personId)

    suspend fun addTodo(personId: String, title: String, hour: Int, minute: Int): Long {
        val entity = TodoEntity(
            personId = personId,
            title = title,
            hour = hour,
            minute = minute,
            isDone = false,
            enabled = true,
        )
        val id = dao.insert(entity)
        if (id > 0) {
            scheduleAlarm(entity.copy(id = id))
        }
        return id
    }

    suspend fun updateTodo(todo: TodoEntity) {
        dao.update(todo)
        if (todo.enabled) {
            scheduleAlarm(todo)
        } else {
            cancelAlarm(todo.id)
        }
    }

    suspend fun deleteTodo(todo: TodoEntity) {
        dao.delete(todo)
        cancelAlarm(todo.id)
    }

    suspend fun toggleDone(id: Long, isDone: Boolean) {
        dao.updateDoneStatus(id, isDone)
    }

    suspend fun toggleEnabled(id: Long, enabled: Boolean) {
        dao.updateEnabled(id, enabled)
        val entity = dao.getById(id)
        if (entity != null) {
            if (enabled) {
                scheduleAlarm(entity)
            } else {
                cancelAlarm(id)
            }
        }
    }

    suspend fun resetAllDaily() {
        val list = dao.getAllTodosOnce()
        list.forEach { dao.updateDoneStatus(it.id, false) }
    }

    /**
     * 重新设置所有闹钟。使用协程替代 runBlocking，
     * 避免阻塞调用线程（尤其在 BroadcastReceiver 的主线程中）。
     */
    fun rescheduleAllAlarms() {
        val dao = AppDatabase.getDatabase(context).todoDao()
        CoroutineScope(Dispatchers.IO).launch {
            val todos = dao.getAllTodosOnce()
            todos.filter { it.enabled }.forEach { scheduleAlarm(it) }
        }
    }

    private fun scheduleAlarm(todo: TodoEntity) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Android 12+ 需要检查精确闹钟权限
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    // 没有权限，静默跳过（不崩溃）
                    return
                }
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("todo_id", todo.id)
                putExtra("todo_name", todo.title)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                todo.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, todo.hour)
                set(Calendar.MINUTE, todo.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                pendingIntent,
            )
        } catch (e: SecurityException) {
            // 权限不足，静默忽略
            android.util.Log.w("TodoRepository", "无法设置精确闹钟: ${e.message}")
        }
    }

    private fun cancelAlarm(todoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
