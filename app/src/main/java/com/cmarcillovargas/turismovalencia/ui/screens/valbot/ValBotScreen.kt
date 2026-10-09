package com.cmarcillovargas.turismovalencia.ui.screens.valbot

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.model.ChatMessage
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.ui.components.OfflineSnackbar
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.components.StatusChip
import com.cmarcillovargas.turismovalencia.ui.components.StatusTone
import com.cmarcillovargas.turismovalencia.ui.theme.PillShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.ui.theme.screenTitle
import com.cmarcillovargas.turismovalencia.util.formatDistance
import com.cmarcillovargas.turismovalencia.util.walkMinutes

private val BubbleRadius = 20.dp
private val BubbleTail = 4.dp
private val BotBubbleShape = RoundedCornerShape(BubbleRadius, BubbleRadius, BubbleRadius, BubbleTail)
private val UserBubbleShape = RoundedCornerShape(BubbleRadius, BubbleRadius, BubbleTail, BubbleRadius)
private val MiniAvatarSize = 28.dp
private val InputHeight = 48.dp

@Composable
fun ValBotRoute(
    question: String?,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel: ValBotViewModel = viewModel(factory = containerFactory { ValBotViewModel(it) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(question) { if (!question.isNullOrBlank()) viewModel.send(question) }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(viewModel::send)
        }
    }
    ValBotScreen(
        uiState = uiState,
        onBack = onBack,
        onInputChange = viewModel::onInputChange,
        onSend = { viewModel.send() },
        onSuggestion = viewModel::send,
        onOpenItem = onOpenItem,
        onOpenFavorites = onOpenFavorites,
        onOpenMap = onOpenMap,
        onNewConversation = viewModel::onNewConversation,
        onRetry = viewModel::onRetry,
        onMic = {
            try {
                speech.launch(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    ),
                )
            } catch (_: ActivityNotFoundException) {
                // Sin reconocimiento de voz en el dispositivo: el micrófono no hace nada.
            }
        },
    )
}

@Composable
fun ValBotScreen(
    uiState: ValBotUiState,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenMap: () -> Unit,
    onNewConversation: () -> Unit,
    onRetry: () -> Unit,
    onMic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val count = uiState.messages.size + (if (uiState.typing) 1 else 0) + (if (!uiState.online) 2 else 0)
    LaunchedEffect(count) { if (count > 0) listState.animateScrollToItem(count) }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Header(uiState.online, onBack, onNewConversation)
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                item {
                    Surface(shape = PillShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        Text(
                            stringResource(R.string.valbot_today),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xxs),
                        )
                    }
                }
                items(uiState.messages, key = { it.id }) { message ->
                    if (message.fromUser) UserBubble(message) else BotBubble(message, uiState.items, onOpenItem)
                }
                if (uiState.typing) item { TypingBubble() }
                if (!uiState.online) {
                    item {
                        BotRow {
                            Surface(shape = BotBubbleShape, color = MaterialTheme.colorScheme.errorContainer) {
                                Text(
                                    stringResource(R.string.valbot_unavailable),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(Spacing.m),
                                )
                            }
                        }
                    }
                    item { OfflineLinks(onOpenFavorites, onOpenMap) }
                }
            }
            InputArea(uiState, onInputChange, onSend, onSuggestion, onMic)
        }
        if (!uiState.online) {
            OfflineSnackbar(onRetry = onRetry, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.xxl + Spacing.xxl + Spacing.s))
        }
    }
}

@Composable
private fun Header(online: Boolean, onBack: () -> Unit, onNewConversation: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.appColors.bot, shadowElevation = Spacing.xxs) {
        Row(
            modifier = Modifier.statusBarsPadding().height(Spacing.xxl + Spacing.l).padding(horizontal = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back), tint = Color.White)
            }
            Box(Modifier.size(Spacing.xxl - Spacing.xs).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(Spacing.l))
            }
            Spacer(Modifier.width(Spacing.s))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.nav_valbot), style = MaterialTheme.typography.screenTitle, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(Spacing.s).clip(CircleShape).background(if (online) MaterialTheme.appColors.ecoLight else Color.White.copy(alpha = 0.6f)),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        stringResource(if (online) R.string.valbot_online else R.string.valbot_offline_status),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.valbot_menu), tint = Color.White)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.valbot_new_chat)) },
                        onClick = {
                            menu = false
                            onNewConversation()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BotRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Box(
            Modifier.size(MiniAvatarSize).clip(CircleShape).background(MaterialTheme.appColors.bot),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(Spacing.l - Spacing.xxs))
        }
        Spacer(Modifier.width(Spacing.xs))
        Box(Modifier.weight(1f, fill = false)) { content() }
        Spacer(Modifier.width(Spacing.m))
    }
}

@Composable
private fun BotBubble(message: ChatMessage, items: Map<String, ContentItem>, onOpenItem: (String) -> Unit) {
    BotRow {
        Surface(shape = BotBubbleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
            Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(message.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                message.cardIds.mapNotNull(items::get).forEach { MiniCard(it) { onOpenItem(it.id) } }
                Text(message.time, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun UserBubble(message: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Spacer(Modifier.width(Spacing.xxl))
        Surface(shape = UserBubbleShape, color = MaterialTheme.appColors.bot) {
            Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s), horizontalAlignment = Alignment.End) {
                Text(message.text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(message.time, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun MiniCard(item: ContentItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Spacing.m)).clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(Spacing.m),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            PlaceImage(item.thumb, item.category, Modifier.size(Spacing.xxl).clip(RoundedCornerShape(Spacing.s)), item.title)
            Spacer(Modifier.width(Spacing.s))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                if (item.distanceMeters > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.m + Spacing.xxs))
                        Spacer(Modifier.width(Spacing.xxs))
                        Text(
                            "${formatDistance(item.distanceMeters)} · ${walkMinutes(item.distanceMeters)} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (item.isFree) StatusChip(stringResource(R.string.valbot_free), StatusTone.Success, height = Spacing.l, small = true)
        }
    }
}

@Composable
private fun TypingBubble() {
    BotRow {
        Surface(shape = BotBubbleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
            val transition = rememberInfiniteTransition(label = "typing")
            Row(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                repeat(3) { index ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(600, delayMillis = index * 150), RepeatMode.Reverse),
                        label = "dot$index",
                    )
                    Box(Modifier.size(Spacing.s).alpha(alpha).clip(CircleShape).background(MaterialTheme.appColors.controlInactive))
                }
            }
        }
    }
}

@Composable
private fun OfflineLinks(onOpenFavorites: () -> Unit, onOpenMap: () -> Unit) {
    BotRow {
        Surface(shape = BotBubbleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
            Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(stringResource(R.string.valbot_offline_meanwhile), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    LinkChip(stringResource(R.string.valbot_my_favorites), Icons.Outlined.AccountBalance, MaterialTheme.colorScheme.secondary, onOpenFavorites)
                    LinkChip(stringResource(R.string.valbot_saved_map), Icons.Outlined.Eco, MaterialTheme.colorScheme.tertiary, onOpenMap)
                }
            }
        }
    }
}

@Composable
private fun LinkChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(Spacing.xl + Spacing.xs).clip(PillShape).clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(Spacing.l - Spacing.xxs))
            Spacer(Modifier.width(Spacing.xs))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun InputArea(
    uiState: ValBotUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit,
    onMic: () -> Unit,
) {
    val bot = MaterialTheme.appColors.bot
    val enabled = uiState.online
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs) {
        Column(Modifier.padding(vertical = Spacing.s)) {
            LazyRow(contentPadding = PaddingValues(horizontal = Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                items(uiState.suggestions) { suggestion ->
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, bot),
                        modifier = Modifier.alpha(if (enabled) 1f else 0.6f).clip(PillShape).clickable(enabled = enabled, role = Role.Button) { onSuggestion(suggestion) },
                    ) {
                        Row(Modifier.padding(horizontal = Spacing.m).height(Spacing.xxl - Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = bot, modifier = Modifier.size(Spacing.l - Spacing.xxs))
                            Spacer(Modifier.width(Spacing.xs))
                            Text(suggestion, style = MaterialTheme.typography.labelLarge, color = bot)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.s))
            Row(Modifier.padding(horizontal = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(InputHeight)
                        .clip(PillShape)
                        .background(if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHighest)
                        .then(if (enabled) Modifier.background(Color.Transparent) else Modifier)
                        .padding(start = Spacing.m, end = Spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = uiState.input,
                        onValueChange = onInputChange,
                        enabled = enabled,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(bot),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box {
                                if (uiState.input.isEmpty()) {
                                    Text(
                                        stringResource(if (enabled) R.string.valbot_hint else R.string.valbot_hint_offline),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    IconButton(onClick = onMic, enabled = enabled) {
                        Icon(Icons.Outlined.Mic, stringResource(R.string.valbot_mic), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Spacer(Modifier.width(Spacing.s))
                Box(
                    modifier = Modifier
                        .size(Spacing.xxl + Spacing.xxs)
                        .clip(CircleShape)
                        .background(if (enabled) bot else MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(enabled = enabled, role = Role.Button, onClick = onSend),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        stringResource(R.string.valbot_send),
                        tint = if (enabled) Color.White else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

private val PreviewMessages = listOf(
    ChatMessage(1, false, "¡Hola! Soy ValBot, tu asistente de turismo en Valencia. ¿En qué puedo ayudarte?", "10:24"),
    ChatMessage(2, true, "¿Qué ver hoy gratis en Valencia?", "10:25"),
    ChatMessage(3, false, "Hoy tienes buenas opciones sin gastar nada. Las más cercanas a ti:", "10:25", listOf("jardin-turia", "barrio-carmen")),
    ChatMessage(4, true, "¿Cómo llego al Jardín del Turia?", "10:26"),
)

private val PreviewItems: Map<String, ContentItem> = FakeQueVer.associateBy { it.id }

@Preview(name = "D1 · ValBot", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ValBotPreview() {
    TurismoValenciaTheme {
        ValBotScreen(
            ValBotUiState(messages = PreviewMessages, typing = true, suggestions = listOf("¿Qué ver hoy gratis en Valencia?", "Restaurantes de paella cerca"), items = PreviewItems),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "D2 · ValBot sin conexión", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ValBotOfflinePreview() {
    TurismoValenciaTheme {
        ValBotScreen(
            ValBotUiState(
                messages = listOf(PreviewMessages[0], ChatMessage(2, true, "¿Dónde puedo cenar una paella?", "21:02")),
                online = false,
                suggestions = listOf("¿Qué ver hoy gratis en Valencia?", "Restaurantes de paella cerca"),
                items = PreviewItems,
            ),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
