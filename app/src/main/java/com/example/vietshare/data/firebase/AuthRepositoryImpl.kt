package com.example.vietshare.data.firebase

import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override suspend fun login(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun signup(email: String, password: String, username: String): Result<Unit> = try {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid
        if (userId != null) {
            val user = User(userId = userId, username = username, email = email)
            firestore.collection("Users").document(userId).set(user).await()
            Result.success(Unit)
        } else {
            Result.failure(Exception("User not created"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override fun getUserDetails(userId: String): Flow<User?> = callbackFlow {
        val listenerRegistration = firestore.collection("Users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user).isSuccess
            }
        awaitClose { listenerRegistration.remove() }
    }
}
