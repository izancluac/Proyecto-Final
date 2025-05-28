package com.example.proyecto.objectos
import com.google.firebase.Timestamp

data class ChatMensaje(
    val id: String = "", // Añadir campo ID
    val senderId: String,
    val text: String,
    val timestamp: Timestamp? = null
)
