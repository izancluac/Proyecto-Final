package com.example.proyecto.Activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyecto.R
import com.example.proyecto.objectos.ChatAdaptadorIndividual
import com.example.proyecto.objectos.ChatMensaje
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.firestore.MetadataChanges
import java.util.UUID
import kotlin.collections.set

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var chatAdapter: ChatAdaptadorIndividual
    private lateinit var flecha : ImageView

    private lateinit var db: FirebaseFirestore
    private lateinit var chatRef: DocumentReference
    private lateinit var chatId: String
    private lateinit var userId: String
    private lateinit var currentUserId : String
    private lateinit var otherUserId : String

    private lateinit var imagenUsuario: ImageView
    private lateinit var tvUsuario: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        init_components()
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
            reverseLayout = false // <--- Añadir esta línea
        }
        init_listeners()
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = chatAdapter
        createChatIfNotExists(currentUserId, otherUserId)
        cargarPerfilReceptor()
        listenToMessages()
    }
    private fun init_components(){
        recyclerView = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        currentUserId = intent.getStringExtra("currentUserUid").toString()
        otherUserId = intent.getStringExtra("otherUserUid").toString()
        userId = intent.getStringExtra("userId").toString()
        chatId = createChatId(currentUserId, otherUserId)
        db = FirebaseFirestore.getInstance()
        chatRef = db.collection("privateChats").document(chatId)
        chatAdapter = ChatAdaptadorIndividual(mutableListOf(), currentUserId, otherUserId)
        flecha = findViewById(R.id.ivFlechaAtras)

    }

    private fun init_listeners(){
        flecha.setOnClickListener {
            finish()//Salimos de la actividad
        }
        buttonSend.setOnClickListener {
            val text = editTextMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(currentUserId, text)
                editTextMessage.text.clear()
            }
        }
    }

    private fun createChatId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

    private fun createChatIfNotExists(uid1: String, uid2: String) {
        chatRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val chatData = hashMapOf(
                    "user1" to uid1,
                    "user2" to uid2,
                    "users" to listOf(uid1, uid2), // AÑADE ESTE CAMPO
                    "createdAt" to FieldValue.serverTimestamp()
                )
                chatRef.set(chatData)
            }
        }.addOnFailureListener {
        }
    }

    private fun sendMessage(senderId: String, text: String) {
        val messageData = hashMapOf(
            "senderId" to senderId,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        chatRef.collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                // El mensaje será recibido por el listener automáticamente
                editTextMessage.text.clear()
            }
            .addOnFailureListener { e ->

            }
    }
    //Esta parte es algo muy interesante y nuevo que he aprendido explicar en memoria
    private fun listenToMessages() {
        chatRef.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val toAdd   = mutableListOf<ChatMensaje>()
                val toModif = mutableListOf<ChatMensaje>()

                for (change in snapshots.documentChanges) {
                    val doc = change.document
                    val msg = ChatMensaje(
                        id       = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        text     = doc.getString("text")     ?: "",
                        timestamp= doc.getTimestamp("timestamp")
                    )

                    when (change.type) {
                        DocumentChange.Type.ADDED    -> toAdd.add(msg)
                        DocumentChange.Type.MODIFIED -> toModif.add(msg)
                        else                        -> { /** ignore **/ }
                    }
                }

                // 1) Añadimos los nuevos
                if (toAdd.isNotEmpty()) {
                    chatAdapter.messages.addAll(toAdd)
                }
                // 2) Actualizamos los modificados (timestamp recién llegado)
                for (m in toModif) {
                    val idx = chatAdapter.messages.indexOfFirst { it.id == m.id }
                    if (idx != -1) chatAdapter.messages[idx] = m
                }
                // 3) Re‑ordenamos toda la lista y refrescamos
                chatAdapter.messages.sortBy { it.timestamp?.toDate()?.time ?: Long.MAX_VALUE }
                chatAdapter.notifyDataSetChanged()

                // 4) Scroll al final
                if (chatAdapter.itemCount > 0) {
                    recyclerView.post {
                        recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
    }
    private fun cargarPerfilReceptor(){
        imagenUsuario = findViewById(R.id.ivImagenUsuario)
        tvUsuario = findViewById(R.id.tvNombreUsuario)

        val db = FirebaseFirestore.getInstance()
        db.collection("perfiles")
            .whereEqualTo("id", otherUserId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {

                    return@addOnSuccessListener
                }
                val doc = snapshot.documents[0]

                // 3) Carga el nombre (campo ejemplo: "nombre")
                val nombre = doc.getString("nombre") ?: "Usuario"
                tvUsuario.text = nombre

                // 4) Carga la URL de la imagen (campo exacto: "imagenPerfilURL")
                val url = doc.getString("imagenPerfilURl")

                if (!url.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(url)
                        .override(200, 200) // No más grande que esto
                        .thumbnail(0.1f)
                        .placeholder(R.drawable.placeholder)
                        .circleCrop()
                        .into(imagenUsuario)
                }
                else{
                    Glide.with(this)
                        .load(R.drawable.placeholder)
                        .override(200, 200) // No más grande que esto
                        .circleCrop()
                        .into(imagenUsuario)
                }
            }
            .addOnFailureListener { e ->

            }

    }

}
