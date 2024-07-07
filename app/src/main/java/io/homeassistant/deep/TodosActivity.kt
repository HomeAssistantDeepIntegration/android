package io.homeassistant.deep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daksh.mdparserkit.core.parseMarkdown
import io.homeassistant.deep.ui.theme.AppTheme
import kotlinx.coroutines.launch

class TodosActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

            val url = sharedPreferences.getString(
                "url", ""
            )
            val token = sharedPreferences.getString(
                "auth_token", ""
            )

            if (url.isNullOrEmpty() || token.isNullOrEmpty()) {
                // Navigate to the configuration activity
                startActivity(
                    Intent(
                        this@TodosActivity, ConfigActivity::class.java
                    )
                )
            } else {
                val repository = HomeAssistantRepository(url, token)
                val viewModel: HomeAssistantViewModel = viewModel(
                    factory = HomeAssistantViewModelFactory(repository)
                )
                val todoViewModel: TodoViewModel = viewModel(
                    factory = TodoViewModelFactory(repository)
                )

                AppTheme {
                    val states by viewModel.statesLiveData.observeAsState()
                    val todos by todoViewModel.itemsLiveData.observeAsState()
                    LaunchedEffect(Unit) {
                        viewModel.loadStates()
                    }

                    Scaffold(topBar = {
                        TopAppBar(title = {
                            Text("Todos")
                        }, navigationIcon = {
                            IconButton(onClick = {
                                finish()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        })
                    }) { padding ->
                        val todoEntities by remember {
                            derivedStateOf {
                                states?.filter { state ->
                                    state.entityId.startsWith("todo.")
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize()
                        ) {
                            if (todoEntities.isNullOrEmpty()) {
                                Loading("")
                            } else {
//                                val selectedTabIndex by remember { derivedStateOf { if (selectedEntity.isNullOrEmpty()) 0 else (todoEntities!!.indexOfFirst { it.entityId == selectedEntity } + 1) } }

                                val pagerState = rememberPagerState(pageCount = {
                                    todoEntities!!.size + 1
                                })
                                val animationScope = rememberCoroutineScope()

                                // Load all items initially
                                // Run once when todoEntities gets its first data
                                LaunchedEffect(todoEntities) {
                                    todoEntities?.forEach { entity ->
                                        todoViewModel.loadItems(entity.entityId)
                                    }
                                }
                                // Load items when selectedEntity changes
                                LaunchedEffect(pagerState.settledPage) {
                                    if (pagerState.settledPage != 0) {
                                        todoViewModel.loadItems(todoEntities!![pagerState.settledPage - 1].entityId)
                                    }
                                }

                                PrimaryScrollableTabRow(
                                    selectedTabIndex = if (pagerState.currentPage != -1) pagerState.currentPage else 0,
                                ) {
                                    Tab(selected = pagerState.currentPage <= 0,
                                        onClick = {
                                            animationScope.launch {
                                                pagerState.animateScrollToPage(
                                                    0
                                                )
                                            }
                                        },
                                        text = { Text(text = "All") },
                                        icon = {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null
                                            )
                                        })
                                    todoEntities?.forEachIndexed { i, entity ->
                                        val name = entity.attributes["friendly_name"] as String?
                                        if (name is String) {
                                            Tab(selected = i + 1 == pagerState.currentPage,
                                                onClick = {
                                                    animationScope.launch {
                                                        pagerState.animateScrollToPage(
                                                            i + 1
                                                        )
                                                    }
                                                },
                                                text = {
                                                    Text(text = name)
                                                },
                                                icon = {
                                                    Icon(
                                                        rememberIconicsPainter(
                                                            getStateIcon(entity)
                                                        ), name
                                                    )
                                                })
                                        }
                                    }
                                }

                                val state =
                                    rememberPullRefreshState(todoViewModel.loading, onRefresh = {
                                        todoEntities?.forEach { entity ->
                                            todoViewModel.loadItems(entity.entityId)
                                        }
                                    })

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pullRefresh(state)
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                        when (page) {
                                            0 -> {
                                                LazyColumn {
                                                    todoEntities?.filter { todos?.containsKey(it.entityId) == true }
                                                        ?.forEach { entity ->
                                                            val listItems =
                                                                todos?.get(entity.entityId)
                                                                    ?: emptyList()
                                                            val name =
                                                                entity.attributes["friendly_name"] as String?
                                                            item {
                                                                ListItem(headlineContent = {
                                                                    Text(
                                                                        text = name
                                                                            ?: "Unknown",
                                                                        style = MaterialTheme.typography.headlineLarge
                                                                    )
                                                                })
                                                            }
                                                            items(listItems.filter { it.status != "completed" }) {
                                                                ListItem(
                                                                    headlineContent = { Text(it.summary) },
                                                                    overlineContent = {
                                                                        it.due?.let { it1 ->
                                                                            Text(
                                                                                DateFormat.format(
                                                                                    "dd/MM/yyyy",
                                                                                    it1
                                                                                ).toString()
                                                                            )
                                                                        }
                                                                    },
                                                                    supportingContent = {
                                                                        it.description?.let { it1 ->
                                                                            Text(
                                                                                parseMarkdown(
                                                                                    it1
                                                                                )
                                                                            )
                                                                        }
                                                                    },
                                                                    leadingContent = {
                                                                        Checkbox(
                                                                            checked = it.status == "completed",
                                                                            onCheckedChange = { checked ->
                                                                                todoViewModel.updateItem(
                                                                                    entity.entityId,
                                                                                    it.summary,
                                                                                    status = if (checked) "completed" else "needs_action",
                                                                                )
                                                                            }
                                                                        )
                                                                    },
                                                                )
                                                            }
                                                        }
                                                }
                                            }

                                            else -> {
                                                val entity by
                                                remember { derivedStateOf { todoEntities!![page - 1] } }
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.Top,
                                                ) {
                                                    val listItems =
                                                        todos?.get(entity.entityId)
                                                            ?: emptyList()
                                                    items(listItems.sortedBy { it.status == "completed" }) {
                                                        ListItem(
                                                            headlineContent = { Text(it.summary) },
                                                            overlineContent = {
                                                                it.due?.let { it1 ->
                                                                    Text(
                                                                        DateFormat.format(
                                                                            "dd/MM/yyyy",
                                                                            it1
                                                                        ).toString()
                                                                    )
                                                                }
                                                            },
                                                            supportingContent = {
                                                                it.description?.let { it1 ->
                                                                    Text(
                                                                        parseMarkdown(
                                                                            it1
                                                                        )
                                                                    )
                                                                }
                                                            },
                                                            leadingContent = {
                                                                Checkbox(
                                                                    checked = it.status == "completed",
                                                                    onCheckedChange = { checked ->
                                                                        todoViewModel.updateItem(
                                                                            entity.entityId,
                                                                            it.summary,
                                                                            status = if (checked) "completed" else "needs_action",
                                                                        )
                                                                    }
                                                                )
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    PullRefreshIndicator(
                                        todoViewModel.loading, state, Modifier.align(
                                            Alignment.TopCenter
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
