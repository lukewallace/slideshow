package com.usbscreensaver

private val imageExtensions = setOf("jpg", "jpeg", "png", "bmp", "webp", "gif")
internal fun isPhoto(name: String) = name.substringAfterLast('.', "").lowercase() in imageExtensions
