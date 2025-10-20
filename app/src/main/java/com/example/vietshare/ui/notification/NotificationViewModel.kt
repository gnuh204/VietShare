package com.example.vietshare.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.Notification
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.NotificationRepository
import com.example.vietshare.domain.repository.UserRepository
import com.example.vietshare.domain.usecase.FollowUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data class to combine a Notification, sender info, and follow status
data class NotificationItemState(
    val notification: Notification,
    val sender: User,
    val isFollowingBack: Boolean
)

sealed class NotificationState {
    object Loading : NotificationState()
    data class Success(val notifications: List<NotificationItemState>) : NotificationState()
    data class Error(val message: String) : NotificationState()
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val followUserUseCase: FollowUserUseCase
) : ViewModel() {

    private val _notificationState = MutableStateFlow<NotificationState>(NotificationState.Loading)
    val notificationState: StateFlow<NotificationState> = _notificationState

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch

            // Mark all notifications as read when the user enters the screen
            notificationRepository.markAllAsRead(currentUserId)

            // This flow is now fully reactive to all data changes
            val reactiveFlow = notificationRepository.getNotifications(currentUserId).flatMapLatest { notifications ->
                if (notifications.isEmpty()) {
                    return@flatMapLatest flowOf(emptyList<NotificationItemState>())
                }

                val senderIds = notifications.map { it.senderId }.distinct()
                val sendersFlow = userRepository.getUsers(senderIds)
                val currentUserFlow = userRepository.getUser(currentUserId)

                sendersFlow.combine(currentUserFlow) { senders, currentUser ->
                    if (currentUser == null) return@combine emptyList<NotificationItemState>()

                    val senderMap = senders.associateBy { it.userId }
                    notifications.mapNotNull { notification ->
                        senderMap[notification.senderId]?.let { sender ->
                            NotificationItemState(
                                notification = notification,
                                sender = sender,
                                isFollowingBack = currentUser.following.contains(sender.userId)
                            )
                        }
                    }
                }
            }

            reactiveFlow
                .onStart { _notificationState.value = NotificationState.Loading }
                .catch { e -> _notificationState.value = NotificationState.Error(e.message ?: "An error occurred") }
                .collect { resultList ->
                    _notificationState.value = NotificationState.Success(resultList)
                }
        }
    }

    fun followBack(userId: String) {
        viewModelScope.launch {
            followUserUseCase(userId)
        }
    }
}
