package com.freellm.superpro.ui.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freellm.superpro.domain.engine.FileTextExtractor
import com.freellm.superpro.ui.chat.components.*
import com.freellm.superpro.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val conversations by viewModel.conversations.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val deepThinkEnabled by viewModel.deepThinkEnabled.collectAsState()
    val webSearchEnabled by viewModel.webSearchEnabled.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val selectedCitation by viewModel.selectedCitation.collectAsState()
    val canvasHtml by viewModel.canvasHtml.collectAsState()

    val fileExtractor = remember { FileTextExtractor(context) }

    // Auto-scroll on new message / streaming token
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length, messages.lastOrNull()?.reasoningContent?.length) {
        if (messages.isNotEmpty()) {
            runCatching {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Document File Picker Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val extracted = fileExtractor.extractTextFromUri(uri)
                if (extracted.isNotBlank()) {
                    val current = viewModel.inputText.value
                    viewModel.onInputChange("$current\n[Attached File Content]:\n$extracted")
                }
            }
        }
    }

    // Speech Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onInputChange(spokenText)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BackgroundDark,
                drawerShape = androidx.compose.ui.graphics.RectangleShape
            ) {
                SessionDrawer(
                    conversations = conversations,
                    activeConversationId = activeConversationId,
                    onSelectConversation = { id ->
                        viewModel.selectConversation(id)
                        scope.launch { drawerState.close() }
                    },
                    onNewChatClicked = {
                        viewModel.startNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    onPinToggle = { id, pinned -> viewModel.pinConversation(id, pinned) },
                    onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                text = "FreeLLM",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = PrimaryNeonCyan.copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SUPERPRO",
                                    color = PrimaryNeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimaryDark)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.startNewConversation() }) {
                            Icon(Icons.Default.Add, contentDescription = "New Chat", tint = PrimaryNeonCyan)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
                )
            },
            bottomBar = {
                ChatInputBar(
                    inputText = inputText,
                    onInputChange = { viewModel.onInputChange(it) },
                    onSendClicked = { viewModel.sendMessage() },
                    deepThinkEnabled = deepThinkEnabled,
                    onDeepThinkToggle = { viewModel.toggleDeepThink() },
                    webSearchEnabled = webSearchEnabled,
                    onWebSearchToggle = { viewModel.toggleWebSearch() },
                    currentModel = currentModel,
                    onModelSelect = { viewModel.selectModel(it) },
                    onAttachFileClicked = {
                        documentPickerLauncher.launch(arrayOf("*/*"))
                    },
                    onVoiceInputClicked = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to FreeLLM...")
                        }
                        speechLauncher.launch(intent)
                    },
                    isStreaming = isStreaming
                )
            },
            containerColor = BackgroundDark
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (messages.isEmpty()) {
                    // Empty State Screen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Empty",
                                tint = PrimaryNeonCyan.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "How can I help you today?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "DeepThink • Live Web Search • Multimodal Attachments",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(messages, key = { index, msg -> "${msg.id}_$index" }) { _, msg ->
                            MessageBubble(
                                message = msg,
                                onSourceClicked = { viewModel.selectCitation(it) },
                                onSpeakClicked = { viewModel.speak(it) },
                                onOpenCanvas = { viewModel.openCanvas(it) }
                            )
                        }
                    }
                }

                // In-Text Citation Bottom Sheet
                selectedCitation?.let { citation ->
                    CitationBottomSheet(
                        source = citation,
                        onDismiss = { viewModel.selectCitation(null) }
                    )
                }

                // Canvas Artifact Modal
                canvasHtml?.let { html ->
                    CanvasPreviewModal(
                        htmlContent = html,
                        onDismiss = { viewModel.openCanvas(null) }
                    )
                }
            }
        }
    }
}
