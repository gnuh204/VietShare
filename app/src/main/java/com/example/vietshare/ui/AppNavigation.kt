package com.example.vietshare.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vietshare.ui.chat.ChatScreen
import com.example.vietshare.ui.chatlist.ChatListScreen
import com.example.vietshare.ui.createpost.CreatePostScreen
import com.example.vietshare.ui.editprofile.EditProfileScreen
import com.example.vietshare.ui.feed.FeedScreen
import com.example.vietshare.ui.findfriends.FindFriendsScreen
import com.example.vietshare.ui.login.LoginScreen
import com.example.vietshare.ui.notification.NotificationScreen
import com.example.vietshare.ui.postdetail.PostDetailScreen
import com.example.vietshare.ui.profile.ProfileScreen
import com.example.vietshare.ui.settings.SettingsScreen
import com.example.vietshare.ui.signup.SignupScreen

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val FEED = "feed"
    const val CREATE_POST = "create_post"
    const val PROFILE = "profile/{userId}"
    const val POST_DETAIL = "post/{postId}"
    const val CHAT_LIST = "chat_list"
    const val CHAT_DETAIL = "chat/{roomId}"
    const val NOTIFICATION = "notification"
    const val FIND_FRIENDS = "find_friends"
    const val EDIT_PROFILE = "edit_profile"
    const val SETTINGS = "settings"

    fun profile(userId: String) = "profile/$userId"
    fun postDetail(postId: String) = "post/$postId"
    fun chatDetail(roomId: String) = "chat/$roomId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Routes.FEED) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onNavigateToSignup = { navController.navigate(Routes.SIGNUP) }
            )
        }
        composable(Routes.SIGNUP) {
            SignupScreen(
                onSignupSuccess = { navController.navigate(Routes.FEED) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FEED) {
            FeedScreen(
                onNavigateToCreatePost = { navController.navigate(Routes.CREATE_POST) },
                onNavigateToProfile = { userId -> navController.navigate(Routes.profile(userId)) },
                onNavigateToPostDetail = { postId -> navController.navigate(Routes.postDetail(postId)) },
                onNavigateToChatList = { navController.navigate(Routes.CHAT_LIST) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATION) },
                onNavigateToFindFriends = { navController.navigate(Routes.FIND_FRIENDS) }
            )
        }
        composable(Routes.CREATE_POST) {
            CreatePostScreen(
                onPostCreated = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            ProfileScreen(
                onNavigateToPostDetail = { postId -> navController.navigate(Routes.postDetail(postId)) },
                onNavigateToChat = { roomId -> navController.navigate(Routes.chatDetail(roomId)) },
                onNavigateToEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.POST_DETAIL,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) {
            PostDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId -> navController.navigate(Routes.profile(userId)) }
            )
        }
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onNavigateToChat = { roomId -> navController.navigate(Routes.chatDetail(roomId)) }
            )
        }
        composable(
            route = Routes.CHAT_DETAIL,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) {
            ChatScreen()
        }
        composable(Routes.NOTIFICATION) {
            NotificationScreen(
                onNavigateToProfile = { userId -> navController.navigate(Routes.profile(userId)) },
                onNavigateToPost = { postId -> navController.navigate(Routes.postDetail(postId)) }
            )
        }
        composable(Routes.FIND_FRIENDS) {
            FindFriendsScreen(
                onNavigateToProfile = { userId -> navController.navigate(Routes.profile(userId)) }
            )
        }
        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
