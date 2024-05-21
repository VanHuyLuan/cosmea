package com.example.conversation

//import coil.transform.BlurTransformation
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.common.sightengine.SightEngineClient
import com.example.data.mockChannel
import com.example.data.mockMessages
import com.example.data.service.ChannelService
import com.example.data.service.MessageService
import com.example.data.service.UserService
import com.example.designsystem.theme.CosmeaTheme
import com.example.model.ChannelData
import com.example.model.MessageData
import com.example.ui.AppBar
import com.example.ui.UserHead
import com.example.ui.UserInput
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ConversationRoute(
    conversationId: String?,
    onBackPressed: () -> Unit,
) {
    println("Conversation ID: $conversationId")
    val channelService = ChannelService(FirebaseFirestore.getInstance())
    val messageService = MessageService(FirebaseDatabase.getInstance())
    val userService = UserService(FirebaseFirestore.getInstance())
    val channelViewModel: ConversationViewModel = viewModel(factory = ConversationViewModelFactory(channelService, messageService, userService, conversationId!!))

    ConversationScreen(
        conversation = channelViewModel.channelData.collectAsState().value,
        messages = channelViewModel.messageData.collectAsState().value,
        onBackPressed = onBackPressed,
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversation: ChannelData,
    messages: List<MessageData>,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
) {
    println("Conversation: ${conversation.toString()}")
    println("Messages: ${messages.toString()}")

    val scrollState = rememberLazyListState()
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("CosmeaApp", Context.MODE_PRIVATE)
    val userId = sharedPref.getString("currentUserId", null)

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ChannelNameBar(
                channelName = conversation.name,
                channelMembers = conversation.members.size,
                onBackPressed = onBackPressed,
                scrollBehavior = scrollBehavior,
            )
        },
        // Exclude ime and navigation bar padding so this can be added by the UserInput composable
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.ime),
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
            Messages(
                messageData = messages, // temporary
                navigateToProfile = { },
                modifier = Modifier.weight(1f),
                scrollState = scrollState
            )
            UserInput(
                onMessageSent = { messageText, imageUri ->
                    println("Message sent: $messageText with image: $imageUri")
                    if (imageUri != null) {
                        SightEngineClient.checkImageForNSFW(imageUri, context) {
                            coroutineScope.launch {
                                userId?.let {it1 ->
                                    uploadImageAndSaveMessage(
                                        imageUri = imageUri,
                                        messageContent = messageText,
                                        userId = it1,
                                        conversationId = conversation.id
                                    ) { success, url ->
                                        if (success) {
                                            println("Image uploaded and message saved: $url")
                                            val message1 = MessageData(author = it1,
                                                receiver = conversation.id,
                                                content = messageText,
                                                image = url,
                                                nsfw = it,
                                                timestamp = System.currentTimeMillis().toString())
                                            println(message1.image)
                                            coroutineScope.launch {
                                                addMessageToChannel(message1, conversation.id)
                                            }
                                        } else {
                                            println("Failed to upload image and save message")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        coroutineScope.launch {
                            val newMessage = userId?.let { it1 ->
                                MessageData(
                                    author = it1,
                                    receiver = conversation.id,
                                    content = messageText,
                                    image = null,
                                    timestamp = System.currentTimeMillis().toString()
                                )
                            }
                            newMessage?.let { it1 -> addMessageToChannel(it1, conversation.id) }
                        }
                    }
                },
                resetScroll = {

                },
                // let this element handle the padding so that the elevation is shown behind the
                // navigation bar
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            )
        }
    }
}

suspend fun addMessageToChannel(message: MessageData, channelId: String) {
    val messageService = MessageService(FirebaseDatabase.getInstance())
    messageService.addMessageData(channelId, message)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelNameBar(
    channelName: String,
    channelMembers: Int,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBackPressed: () -> Unit = { }
) {
    var functionalityNotAvailablePopupShown by remember { mutableStateOf(false) }
//    if (functionalityNotAvailablePopupShown) {
//        FunctionalityNotAvailablePopup { functionalityNotAvailablePopupShown = false }
//    }
    AppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        onBackPressed = onBackPressed,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Channel name
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleMedium
                )
                // Number of members
                Text(
                    text = "$channelMembers members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            // Search icon
            Icon(
                imageVector = Icons.Outlined.Search,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = { functionalityNotAvailablePopupShown = true })
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .height(24.dp),
                contentDescription = "Search"
            )
            // Info icon
            Icon(
                imageVector = Icons.Outlined.Info,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = { functionalityNotAvailablePopupShown = true })
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .height(24.dp),
                contentDescription = "Info"
            )
        }
    )
}

const val ConversationTestTag = "ConversationTestTag"

@Composable
fun Messages(
    messageData: List<MessageData>,
    navigateToProfile: (String) -> Unit,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {

        val authorMe = "Me"
        LazyColumn(
            reverseLayout = true,
            state = scrollState,
            modifier = Modifier
                .testTag(ConversationTestTag)
                .fillMaxSize()
        ) {
            for (index in messageData.indices) {
                val prevAuthor = messageData.getOrNull(index - 1)?.author
                val nextAuthor = messageData.getOrNull(index + 1)?.author
                val content = messageData[index]
                val isFirstMessageByAuthor = prevAuthor != content.author
                val isLastMessageByAuthor = nextAuthor != content.author

                // Hardcode day dividers for simplicity
//                if (index == messageData.size - 1) {
//                    item {
//                        DayHeader("20 Aug")
//                    }
//                } else if (index == 2) {
//                    item {
//                        DayHeader("Today")
//                    }
//                }

                item {
                    Message(
                        onAuthorClick = { name -> navigateToProfile(name) },
                        msg = content,
                        isUserMe = content.author == authorMe,
                        isFirstMessageByAuthor = isFirstMessageByAuthor,
                        isLastMessageByAuthor = isLastMessageByAuthor
                    )
                }
            }
        }
    }
}

@Composable
fun Message(
    onAuthorClick: (String) -> Unit,
    msg: MessageData,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean
) {
//    val borderColor = if (isUserMe) {
//        MaterialTheme.colorScheme.primary
//    } else {
//        MaterialTheme.colorScheme.tertiary
//    }

    val spaceBetweenAuthors = if (isLastMessageByAuthor) Modifier.padding(top = 8.dp) else Modifier
    Row(modifier = spaceBetweenAuthors) {
        if (isLastMessageByAuthor) {
            // Avatar
            Box(
                modifier = Modifier
                    .padding(8.dp)
            ) {
                UserHead(id = "4", name = "Test")
            }
        } else {
            // Space under avatar
            Spacer(modifier = Modifier.width(57.dp))
        }
        AuthorAndTextMessage(
            msg = msg,
            isUserMe = isUserMe,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            authorClicked = onAuthorClick,
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f)
        )
    }
}

@Composable
fun AuthorAndTextMessage(
    msg: MessageData,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    authorClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isLastMessageByAuthor) {
            AuthorNameTimestamp(msg)
        }
        ChatItemBubble(msg, isUserMe, authorClicked = authorClicked)
        if (isFirstMessageByAuthor) {
            // Last bubble before next author
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            // Between bubbles
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AuthorNameTimestamp(msg: MessageData) {
    // Combine author and timestamp for a11y.
    Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = msg.author,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .alignBy(LastBaseline)
                .paddingFrom(LastBaseline, after = 8.dp) // Space to 1st bubble
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = msg.timestamp,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alignBy(LastBaseline),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val ChatBubbleShape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

@Composable
fun DayHeader(dayString: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .height(16.dp)
    ) {
        DayHeaderLine()
        Text(
            text = dayString,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DayHeaderLine()
    }
}

@Composable
private fun RowScope.DayHeaderLine() {
    HorizontalDivider(
        modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}

@SuppressLint("ResourceType")
@Composable
fun ChatItemBubble(
    messageData: MessageData,
    isUserMe: Boolean,
    authorClicked: (String) -> Unit
) {
    val context = LocalContext.current

    val backgroundBubbleColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column {
        Surface(
            color = backgroundBubbleColor,
            shape = ChatBubbleShape
        ) {
            if(messageData.content != ""){
                ClickableMessage(
                    messageData = messageData,
                    isUserMe = isUserMe,
                    authorClicked = authorClicked
                )
            }
        }

        messageData.image?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = backgroundBubbleColor,
                shape = ChatBubbleShape
            ) {
                val isNSFW = remember {mutableStateOf(messageData.nsfw)}
               Box(modifier = Modifier, contentAlignment = Alignment.Center) {
                   Image(
                       painter = rememberAsyncImagePainter(
                            if(isNSFW.value){
                               R.drawable.blackk} else {it}
                       ),
                       contentScale = ContentScale.Fit,
                       modifier = Modifier.size(160.dp),
                       contentDescription = null
                   )
                   if(isNSFW.value){
                       Button(onClick = { isNSFW.value = false }) {
                           Text(text = "Click")
                       }
                   }
               }
//                AsyncImage(
//                    model = imageUrl,
//                    contentDescription = null,
//                    modifier = Modifier.size(160.dp),
//                    contentScale = ContentScale.Crop
//                )
            }
        }
    }
}

@Composable
fun ClickableMessage(
    messageData: MessageData,
    isUserMe: Boolean,
    authorClicked: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    val styledMessage = messageFormatter(
        text = messageData.content,
        primary = isUserMe
    )

    ClickableText(
        text = styledMessage,
        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
        modifier = Modifier.padding(16.dp),
        onClick = {
            styledMessage
                .getStringAnnotations(start = it, end = it)
                .firstOrNull()
                ?.let { annotation ->
                    when (annotation.tag) {
                        SymbolAnnotationType.LINK.name -> uriHandler.openUri(annotation.item)
                        SymbolAnnotationType.PERSON.name -> authorClicked(annotation.item)
                        else -> Unit
                    }
                }
        }
    )
}

suspend fun uploadImageAndSaveMessage(
    imageUri: Uri,
    messageContent: String,
    userId: String,
    conversationId: String,
    onComplete: (Boolean, String?) -> Unit
) {
    val storageRef = Firebase.storage.reference
    val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

    val uploadTask = imageRef.putFile(imageUri)
    uploadTask.addOnSuccessListener {
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            // Image uploaded successfully, now save the message data
            val message = MessageData(
                author = userId,
                receiver = conversationId,
                content = messageContent,
                image = uri.toString(),
                timestamp = System.currentTimeMillis().toString()
            )
            onComplete(true, uri.toString())
        }.addOnFailureListener {
            onComplete(false, null)
        }
    }.addOnFailureListener {
        onComplete(false, null)
    }
}

fun saveMessageToFirestore(message: MessageData, conversationId: String, onComplete: (Boolean) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val messagesRef = firestore.collection("conversations").document(conversationId).collection("messages")

    messagesRef.add(message).addOnSuccessListener {
        onComplete(true)
    }.addOnFailureListener {
        onComplete(false)
    }
}


@Preview
@Composable
fun ConversationScreenPreview() {
    CosmeaTheme {
        ConversationScreen(
            conversation = mockChannel,
            messages = mockMessages
        ) { }
    }
}

@Preview
@Composable
fun ConversationScreenDarkPreview() {
    CosmeaTheme(darkTheme = true) {
        ConversationScreen(
            conversation = mockChannel,
            messages = mockMessages
        ) { }
    }
}
