package dk.mitberedskab.gettingstartedwithkoltinredux.ui.store

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.reduxkotlin.Dispatcher
import org.reduxkotlin.GetState
import org.reduxkotlin.Middleware
import org.reduxkotlin.middleware
import kotlin.coroutines.CoroutineContext

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
data class AddTodoAsyncWithGlobalScope(val text: String, val completed: Boolean, val delay: Long)
data class AddTodoAsyncWithSuppliedScope(val text: String, val completed: Boolean, val delay: Long, val scope: CoroutineScope)
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

/**
 * Very simple way one could implement async actions
 *
 * Thunk middleware for async action dispatches.
 * Usage:
 *    val store = createStore(myReducer, initialState,
 *          applyMiddleware(thunk, myMiddleware))
 *
 *    fun myNetworkThunk(query: String): Thunk<AppState> = { dispatch, getState, extraArgument ->
 *          launch {
 *              dispatch(LoadingAction())
 *              //do async stuff
 *              val result = api.fetch(query)
 *              dispatch(CompleteAction(result))
 *          }
 *      }
 *
 *    store.dispatch(myNetworkThunk("query"))
 */


fun asyncMiddlewares(
    networkThunks: NetworkThunks
) = middleware<AppState> { store, next, action ->
    val dispatch = store.dispatch

    when (action) {
        is AddTodoAsyncWithGlobalScope -> {
            dispatch(networkThunks.mockFetchGlobalScope(action.delay, action.text, action.completed))
        }
        is AddTodoAsyncWithSuppliedScope -> {
            dispatch(networkThunks.mockFetchSuppliedScope(action.scope, action.delay, action.text, action.completed))
        }
        else -> next(action)
    }
}

interface AppRepo {
    suspend fun getTodo(delay: Long): Boolean
}

class MockRepo(): AppRepo {
    override suspend fun getTodo(delay: Long): Boolean {
        delay(delay)
        return true
    }
}

class NetworkThunks(
    private val appRepo: AppRepo,
    networkContext: CoroutineContext
) {
    private val networkScope = CoroutineScope(networkContext)

    /**
     *
     */
    fun mockFetchGlobalScope(
        delay: Long,
        text: String,
        completed: Boolean
    ) = thunk { dispatch, getState, extraArgument ->
        networkScope.launch {

            appRepo.getTodo(delay)

            dispatch(AddTodo(text, completed))
        }
    }

    fun mockFetchSuppliedScope(
        scope: CoroutineScope,
        delay: Long,
        text: String,
        completed: Boolean
    ) = thunk { dispatch, getState, extraArgument ->
        scope.launch(context = Dispatchers.IO) {

            appRepo.getTodo(delay)

            dispatch(AddTodo(text, completed))
        }
    }
}


/**
 * Convenience function so state type does is not needed every time a thunk is created.
 */
fun thunk(thunkLambda: (dispatch: Dispatcher, getState: GetState<AppState>, extraArgument: Any?) -> Any) =
    createThunk(thunkLambda)

typealias Thunk<State> = (dispatch: Dispatcher, getState: GetState<State>, extraArg: Any?) -> Any
typealias ThunkMiddleware<State> = Middleware<State>

fun <State>createThunk(thunkLambda: (dispatch: Dispatcher, getState: GetState<State>, extraArgument: Any?) -> Any): Thunk<State> {
    return object : Thunk<State> {
        override fun invoke(dispatch: Dispatcher, getState: GetState<State>, extraArg: Any?) {
            thunkLambda(dispatch, getState, extraArg)
        }
    }
}

fun <State> ThunkMiddleware<State>.withExtraArgument(arg: Any?) = createThunkMiddleware<State>(arg)

fun <State> createThunkMiddleware(extraArgument: Any? = null): ThunkMiddleware<State> =
    { store ->
        { next: Dispatcher ->
            { action: Any ->
                if (action is Function<*>) {
                    @Suppress("UNCHECKED_CAST")
                    val thunk = try {
                        (action as Thunk<*>)
                    } catch (e: ClassCastException) {
                        throw IllegalArgumentException("Dispatching functions must use type Thunk:", e)
                    }
                    thunk(store.dispatch, store.getState, extraArgument)
                } else {
                    next(action)
                }
            }
        }
    }