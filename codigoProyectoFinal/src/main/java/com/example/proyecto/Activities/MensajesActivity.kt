package com.example.proyecto.Activities

import android.content.Intent
import android.os.Build
import android.os.Bundle

import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.objectos.ChatAdaptadorLista
import com.example.proyecto.objectos.ChatPreviewItem
import com.google.firebase.firestore.FirebaseFirestore

class MensajesActivity : AppCompatActivity() {
    private lateinit var iconoHome: ImageView
    private lateinit var iconoVenta: ImageView
    private lateinit var iconoPerfil: ImageView
    private lateinit var iconomensaje: ImageView
    private lateinit var recyclerViewChats: RecyclerView
    lateinit var   userId : String
    private lateinit var db: FirebaseFirestore
    private lateinit var chatAdapter: ChatAdaptadorLista
    override fun onCreate(savedInstanceState: Bundle?) {
        // Recupera el ID del usuario pasado desde el Login
        userId = intent.getStringExtra("userId").toString()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mensajes)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init_components()
        init_listeners()
        setupRecyclerView()
        loadChats()
        menu_repetido()
    }

    private fun init_components(){
        iconoHome = findViewById(R.id.iconoHome)
        iconoVenta = findViewById(R.id.iconoVenta)
        iconoPerfil = findViewById(R.id.iconoPerfil)
        iconomensaje = findViewById(R.id.iconomensaje)
        recyclerViewChats = findViewById(R.id.recyclerViewChats) // <-- Nuevo RecyclerView
    }
    private fun init_listeners(){

    }


    private fun setupRecyclerView() {
        chatAdapter = ChatAdaptadorLista(mutableListOf(), userId)
        recyclerViewChats.layoutManager = LinearLayoutManager(this)
        recyclerViewChats.adapter = chatAdapter
    }

    private fun loadChats() {
        db = FirebaseFirestore.getInstance()
        val chatList = mutableListOf<ChatPreviewItem>()
        db.collection("privateChats")
            .whereArrayContains("users", userId)
            .get()
            .addOnSuccessListener { result ->

                for (document in result) {
                    val user1 = document.getString("user1") ?: ""
                    val user2 = document.getString("user2") ?: ""
                    val chatId = document.id

                    val otherUserId = if (user1 == userId) user2 else user1

                    val chatPreview = ChatPreviewItem(chatId, otherUserId)
                    chatList.add(chatPreview)
                }
                chatAdapter.updateChats(chatList)
            }
            .addOnFailureListener { e ->

            }
    }

    private fun menu_repetido() {
        iconomensaje.setOnClickListener {
            val intent = Intent(this, MensajesActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }
        iconoVenta.setOnClickListener {
            val intent = Intent(this, VentaActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
            Toast.makeText(this, "Hola", Toast.LENGTH_SHORT).show()
        }
        iconoHome.setOnClickListener {
            val intent = Intent(this, PaginaPrincipal::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }
        iconoPerfil.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }
    }

}