package dk.mitberedskab.gettingstartedwithkoltinredux

import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dk.mitberedskab.gettingstartedwithkoltinredux.ui.store.*
import dk.mitberedskab.gettingstartedwithkoltinredux.ui.theme.GettingStartedWithKoltinReduxTheme
import kotlinx.coroutines.delay
import org.reduxkotlin.Store
import org.reduxkotlin.StoreSubscription
import org.reduxkotlin.createThreadSafeStore

class MainActivity : ComponentActivity() {
    /**
     * Prepare subscription to store
     */
    lateinit var store: Store<AppState>
    lateinit var storeSubscription: StoreSubscription
    private var storeLiveData: MutableLiveData<AppState> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = createThreadSafeStore(::rootReducer, AppState())
        storeSubscription = store.subscribe { storeLiveData.value = store.state }

        setContent {
            GettingStartedWithKoltinReduxTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    DisplayTodos( storeLiveData.observeAsState().value?.todos ) {
                        store.dispatch(AddTodo("Some Todo", false))
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayTodos(todos: List<Todo>?, onClick: ()->Unit) {
    Column {
        Button(onClick = {
            onClick.invoke()
        }) {
            Text(text = "ADD TODO")
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