package cu.todus.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cu.todus.app.ui.screens.*
import cu.todus.app.ui.viewmodel.MainViewModel

object Routes {
    const val TERMS = "terms"
    const val LOGIN = "login"
    const val HOME = "home"
    const val CHAT = "chat/{jid}"
    const val CONTACTS = "contacts"
    const val MY_PROFILE = "my_profile"
    const val EDIT_PROFILE = "edit_profile"
    const val USER_PROFILE = "user_profile/{jid}"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val startDestination = if (isLoggedIn) Routes.HOME else Routes.TERMS

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.TERMS) { TermsScreen(onAccept = { navController.navigate(Routes.LOGIN) }) }
        composable(Routes.LOGIN) {
            LoginScreen(onContinue = { phone, uuid ->
                viewModel.login(phone, uuid)
                navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            val connectionState by viewModel.connectionState.collectAsState()
            val alias by viewModel.alias.collectAsState()
            val chats by viewModel.chats.collectAsState()
            HomeScreen(connectionState = connectionState, alias = alias, chats = chats,
                onChatClick = { viewModel.openChat(it); navController.navigate("chat/$it") },
                onMyProfile = { navController.navigate(Routes.MY_PROFILE) },
                onContacts = { navController.navigate(Routes.CONTACTS) })
        }
        composable(Routes.CHAT) { backStackEntry ->
            val jid = backStackEntry.arguments?.getString("jid") ?: ""
            val messages by viewModel.currentMessages.collectAsState()
            val isTyping by viewModel.activeChatTyping.collectAsState()
            ChatScreen(jid = jid, messages = messages, isTyping = isTyping,
                onBack = { viewModel.closeChat(); navController.popBackStack() },
                onUserProfile = { navController.navigate("user_profile/$jid") },
                onSendMessage = { body, replyTo -> viewModel.sendMessage(body, replyTo) },
                onSendTyping = { viewModel.sendTyping() },
                onEditMessage = { id, body -> viewModel.editMessage(id, body) },
                onDeleteMessage = { id, forAll -> viewModel.deleteMessage(id, forAll) })
        }
        composable(Routes.CONTACTS) {
            val contacts by viewModel.contacts.collectAsState()
            val searchResult by viewModel.searchResult.collectAsState()
            val searchNotFound by viewModel.searchNotFound.collectAsState()
            ContactsScreen(contacts = contacts, searchResult = searchResult, searchNotFound = searchNotFound,
                onBack = { navController.popBackStack() },
                onContactClick = { viewModel.openChat(it); navController.navigate("chat/$it") },
                onSearch = { viewModel.searchByTodusId(it) },
                onClearSearch = { viewModel.clearSearch() })
        }
        composable(Routes.MY_PROFILE) {
            val alias by viewModel.alias.collectAsState()
            val todusId by viewModel.todusId.collectAsState()
            val phone by viewModel.phone.collectAsState()
            val bio by viewModel.bio.collectAsState()
            ProfileScreen(alias = alias, todusId = todusId, phone = phone, bio = bio,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.EDIT_PROFILE) },
                onLogout = { viewModel.logout(); navController.navigate(Routes.TERMS) { popUpTo(0) { inclusive = true } } })
        }
        composable(Routes.EDIT_PROFILE) {
            val alias by viewModel.alias.collectAsState()
            val bio by viewModel.bio.collectAsState()
            EditProfileScreen(alias = alias, bio = bio,
                onBack = { navController.popBackStack() },
                onSaved = { newAlias, newBio -> navController.popBackStack() })
        }
        composable(Routes.USER_PROFILE) { backStackEntry ->
            UserProfileScreen(jid = backStackEntry.arguments?.getString("jid") ?: "",
                onBack = { navController.popBackStack() },
                onSendMessage = { viewModel.openChat(it); navController.navigate("chat/$it") })
        }
    }
}
