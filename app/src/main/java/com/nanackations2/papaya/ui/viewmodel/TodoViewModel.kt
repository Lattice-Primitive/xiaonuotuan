package com.nanackations2.papaya.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanackations2.papaya.data.db.TodoEntity
import com.nanackations2.papaya.data.repository.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TodoRepository(application)

    val todos: StateFlow<List<TodoEntity>> = repository.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = todos
        .map { list -> list.count { !it.isDone && it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addTodo(personId: String, title: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.addTodo(personId, title, hour, minute)
        }
    }

    fun toggleDone(todo: TodoEntity) {
        viewModelScope.launch {
            repository.toggleDone(todo.id, !todo.isDone)
        }
    }

    fun toggleEnabled(todo: TodoEntity) {
        viewModelScope.launch {
            repository.toggleEnabled(todo.id, !todo.enabled)
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }
}
