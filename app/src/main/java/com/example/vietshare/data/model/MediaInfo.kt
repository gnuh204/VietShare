package com.example.vietshare.data.model

// Data class to hold both the URL and the public ID for a media file from Cloudinary
data class MediaInfo(
    val url: String = "",
    val publicId: String = "" // The ID needed to delete the file from Cloudinary
)
