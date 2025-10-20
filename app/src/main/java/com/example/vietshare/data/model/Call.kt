package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class Call(
    val callId: String = "",
    val roomId: String? = null,
    val callerId: String = "",
    val recipientIds: List<String> = emptyList(),
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val duration: Long = 0,
    val status: String = ""
)
