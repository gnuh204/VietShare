package com.example.vietshare.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimestampFormatter {
    fun formatTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) return ""

        val messageDate = timestamp.toDate()
        val now = Calendar.getInstance()
        val messageCal = Calendar.getInstance().apply { time = messageDate }

        return when {
            // Same day
            now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == messageCal.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
            }
            // Yesterday
            now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - 1 == messageCal.get(Calendar.DAY_OF_YEAR) -> {
                "Yesterday"
            }
            // Same year
            now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(messageDate)
            }
            // Different year
            else -> {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(messageDate)
            }
        }
    }
}
