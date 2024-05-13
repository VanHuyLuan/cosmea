
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.example.conversation.navigation.conversationScreen
import com.example.conversation.navigation.navigateToConversation
import com.example.cosmea.ui.AppState
import com.example.login.navigation.loginScreen
import com.example.messages.navigation.messagesScreen
import com.example.notifications.navigation.notificationsScreen
import com.example.profile.navigation.profileScreen
import com.example.register.navigation.REGISTER_ROUTE
import com.example.register.navigation.registerScreen
import com.example.servers.navigation.serversScreen

@Composable
fun AppNavHost(
    appState: AppState,
    modifier: Modifier = Modifier,
    startDestination: String = REGISTER_ROUTE,
) {
    val navController = appState.navController
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
    ) {
        serversScreen(onChannelClick = navController::navigateToConversation)
        conversationScreen(
            onNavIconPressed = navController::popBackStack
        )
        messagesScreen(onChannelClick = navController::navigateToConversation)
        notificationsScreen {}
        profileScreen (onTopicClick = navController::navigateToConversation)
        registerScreen {}
        loginScreen {}
    }
}
