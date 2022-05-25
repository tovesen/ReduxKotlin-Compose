package dk.mitberedskab.gettingstartedwithkoltinredux.ui.store

import android.util.Log
import org.reduxkotlin.middleware

/**
 * to-do model
 */
data class Todo(
    val text: String,
    val completed: Boolean = false,
    val id: Int
)

/**
 * Visibility filter
 */
enum class VisibilityFilter {
    SHOW_ALL,
    SHOW_COMPLETED,
    SHOW_ACTIVE
}

/**
 * Actions
 */
data class AddTodo(val text: String, val completed: Boolean = false)
data class AddTodoAsync(val text: String, val completed: Boolean)
data class ToggleTodo(val index: Int)
data class SetVisibilityFilter(val visibilityFilter: VisibilityFilter)

/**
 * AppState
 */
data class AppState(
    val todos: List<Todo> = listOf(),
    val visibilityFilter: VisibilityFilter = VisibilityFilter.SHOW_ALL
) {
    val visibleTodos: List<Todo>
        get() = getVisibleTodos(visibilityFilter)

    private fun getVisibleTodos(visibilityFilter: VisibilityFilter) = when (visibilityFilter) {
        VisibilityFilter.SHOW_ALL -> todos
        VisibilityFilter.SHOW_ACTIVE -> todos.filter { !it.completed }
        VisibilityFilter.SHOW_COMPLETED -> todos.filter { it.completed }
    }
}

fun todosReducer(state: List<Todo>, action: Any) =
    when (action) {
        is AddTodo -> state + Todo(action.text, id = state.size)
        is ToggleTodo -> state.mapIndexed { index, todo ->
            if (index == action.index) {
                todo.copy(completed = !todo.completed)
            } else {
                todo
            }
        }
        else -> state
    }

fun visibilityFilterReducer(state: VisibilityFilter, action: Any) =
    when (action) {
        is SetVisibilityFilter -> action.visibilityFilter
        else -> state
    }

/**
 * The root reducer of the app.  This is the reducer passed to `createStore()`.
 * Notice that sub-states are delegated to other reducers.
 */
fun rootReducer(state: AppState, action: Any) = AppState(
    todos = todosReducer(state.todos, action),
    visibilityFilter = visibilityFilterReducer(state.visibilityFilter, action)
)

val loggerMiddleware = middleware<AppState> { store, next, action ->
    val result = next(action)
    Log.d("DISPATCH action:" ,"${action::class.simpleName}: $action")
    Log.d("next state:", "${store.state}")
    result
}