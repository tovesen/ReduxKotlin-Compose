package dk.mitberedskab.gettingstartedwithkoltinredux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dk.mitberedskab.gettingstartedwithkoltinredux.ui.store.*
import dk.mitberedskab.gettingstartedwithkoltinredux.ui.theme.GettingStartedWithKoltinReduxTheme
import kotlinx.coroutines.MainScope
import org.reduxkotlin.*

class MainActivity : ComponentActivity() {
    /**
     * Prepare subscription to store
     */
    private lateinit var store: Store<AppState>
    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = createStore(
            ::rootReducer,
            AppState(),
            applyMiddleware(
                createThunkMiddleware(),
                loggerMiddleware,
                asyncMiddlewares(
                    NetworkThunks(
                        MockRepo(),
                        mainScope.coroutineContext
                    )
                )
            ),
        )

        setContent {
            GettingStartedWithKoltinReduxTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val scope = rememberCoroutineScope()
                    withStore(store = store) {
                        val dispatcher = rememberDispatcher()
                        val todosSlice by selectMyState { todos }

                        DisplayTodos(
                            todosSlice,
                            onSyncClick = {
                                dispatcher(AddTodo("Some Todo", false))
                            }, onAsyncGlobalClick = {
                                dispatcher(AddTodoAsyncWithGlobalScope(
                                        "Async Global Todo",
                                        false,
                                        1000
                                    )
                                )
                            }, onAsyncSuppliedClick = {
                                dispatcher(AddTodoAsyncWithSuppliedScope(
                                    "Async Supplied Todo",
                                    false,
                                    1000,
                                    scope
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayTodos(
    todos: List<Todo>?,
    onSyncClick: ()->Unit,
    onAsyncGlobalClick: ()->Unit,
    onAsyncSuppliedClick: ()->Unit
) {
    Column {
        Button(onClick = {
            onSyncClick.invoke()
        }) {
            Text(text = "ADD SYNC TODO")
        }
        Button(onClick = {
            onAsyncGlobalClick.invoke()
        }) {
            Text(text = "ADD GLOBAL ASYNC TODO")
        }
        Button(onClick = {
            onAsyncSuppliedClick.invoke()
        }) {
            Text(text = "ADD SUPPLIED ASYNC TODO")
        }
        if (todos != null) {
            LazyColumn {
                items(todos.size) { index ->
                    Text(text = todos[index].text )
                }
            }
        } else {
            Text(text = "Empty Todo list")
        }
    }
}

@Composable
inline fun <TSlice> selectMyState(
    crossinline selector: @DisallowComposableCalls AppState.() -> TSlice
): State<TSlice> = selectState(selector)

@PublishedApi
internal val LocalStore: ProvidableCompositionLocal<Store<*>?> = compositionLocalOf { null }

@Composable
@PublishedApi
@Suppress("UNCHECKED_CAST")
internal inline fun <reified TState> getStore(): Store<TState> =
    LocalStore.current.runCatching { (this as Store<TState>) }.getOrElse {
        error("Store<${TState::class.simpleName}> not found in current composition scope")
    }

@Composable
inline fun <T : Any> withStore(store: Store<T>, crossinline content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStore provides store) { content() }
}

@Composable
fun rememberDispatcher(): Dispatcher = getStore<Any>().dispatch

@Composable
inline fun <reified TState, TSlice> selectState(
    crossinline selector: @DisallowComposableCalls TState.() -> TSlice
): State<TSlice> {
    return getStore<TState>().selectState(selector)
}

@Composable
inline fun <TState, TSlice> Store<TState>.selectState(
    crossinline selector: @DisallowComposableCalls TState.() -> TSlice
): State<TSlice> {
    val result = remember { mutableStateOf(state.selector()) }
    DisposableEffect(result) {
        val unsubscribe = subscribe { result.value = state.selector() }
        onDispose(unsubscribe)
    }
    return result
}

