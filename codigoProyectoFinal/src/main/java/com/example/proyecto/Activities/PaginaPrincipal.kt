package com.example.proyecto.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.objectos.Producto
import com.example.proyecto.objectos.ProductoAdaptador
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PaginaPrincipal : AppCompatActivity() {
    private lateinit var adapter: ProductoAdaptador
    private lateinit var iconoHome: ImageView
    private lateinit var iconoVenta: ImageView
    private lateinit var iconoPerfil: ImageView
    private lateinit var iconomensaje: ImageView
    lateinit var   userId : String
    private val listaProductos = mutableListOf<Producto>()
    private val db = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pagina_principal)

        // Recupera el ID del usuario pasado desde el Login
        userId = intent.getStringExtra("userId").toString()



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init_components()
        init_listeners()
        menu_repetido()
        cargarProductosDesdeFirebase()
    }

    private fun init_components() {
        iconoHome = findViewById(R.id.iconoHome)
        iconoVenta = findViewById(R.id.iconoVenta)
        iconoPerfil = findViewById(R.id.iconoPerfil)
        iconomensaje = findViewById(R.id.iconomensaje)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = ProductoAdaptador(listaProductos ,userId)
        recyclerView.adapter = adapter
    }

    private fun init_listeners() {
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { adapter.filtrar(it) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { adapter.filtrar(it) }
                return false
            }
        })
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

    private fun cargarProductosDesdeFirebase() {
        lifecycleScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("productos")
                    .get()
                    .await()

                val productos = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("idUsuario") ?: ""
                    val nombre = doc.getString("nombre") ?: return@mapNotNull null
                    val precio = doc.getDouble("precio") ?: 0.0
                    val descripcion = doc.getString("descripcion") ?: ""
                    val talla = doc.getString("talla") ?: ""
                    val marca = doc.getString("marca") ?: ""
                    val tipo = doc.getString("tipo") ?: ""
                    val imagenUrl = doc.getString("imagenUrl") ?: ""

                    Producto("",id ,nombre, precio, descripcion, talla, marca, tipo, imagenUrl)
                }


                adapter.actualizarLista(productos)
            } catch (e: Exception) {

            }
        }
    }
}