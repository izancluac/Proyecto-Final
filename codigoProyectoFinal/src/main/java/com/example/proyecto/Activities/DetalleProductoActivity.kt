package com.example.proyecto.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.proyecto.Activities.PerfilActivity
import com.example.proyecto.R
import com.example.proyecto.objectos.Producto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DetalleProductoActivity : AppCompatActivity() {
    lateinit var producto : Producto
    lateinit var nombre : TextView
    lateinit var precio : TextView
    lateinit var descripcion : TextView
    lateinit var talla : TextView
    lateinit var marca : TextView
    lateinit var tipo : TextView
    lateinit var imagen : ImageView
    lateinit var flecha : ImageView
    var id : String = ""
    //Vendedor
    lateinit var tvNombreVendedor : TextView
    lateinit var ivImagenVendedor : ImageView
    lateinit var btChat : Button
    private  lateinit var  userIDvendedor : String
    var userId : String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_producto)

        init_components()
        init_listeners()
        init_values()
    }
    private fun init_components(){
        producto = intent.getSerializableExtra("producto") as Producto
        nombre = findViewById<TextView>(R.id.tvNombreDetalle)
        precio = findViewById<TextView>(R.id.tvPrecioDetalle)
        descripcion = findViewById<TextView>(R.id.tvDescripcionDetalle)
        talla = findViewById<TextView>(R.id.tvTallaDetalle)
        marca = findViewById<TextView>(R.id.tvMarcaDetalle)
        tipo = findViewById<TextView>(R.id.tvTipoDetalle)
        imagen = findViewById<ImageView>(R.id.ivImagenDetalle)
        flecha = findViewById<ImageView>(R.id.ivFlechaAtras)

        tvNombreVendedor = findViewById<TextView>(R.id.tvNombreVendedor)
        ivImagenVendedor = findViewById<ImageView>(R.id.ivImagenVendedor)
        btChat = findViewById(R.id.btChat)
        userId = intent.getStringExtra("userId").toString()
    }
    private fun init_values(){
        id = producto.id
        nombre.text = producto.nombre
        precio.text = "Precio: ${producto.precio}€"
        descripcion.text = producto.descripcion
        talla.text = "Talla: ${producto.talla}"
        marca.text = "Marca: ${producto.marca}"
        tipo.text = "Tipo: ${producto.tipo}"
        // Carga la imagen con Glide
        Glide.with(this)
            .load(producto.imagenUrl)
            .override(200, 200) // No más grande que esto
            .thumbnail(0.1f)
            .placeholder(R.drawable.placeholder) // imagen mientras carga
            .error(R.drawable.placeholder)       // imagen si falla la carga
            .into(imagen)

        vendedor_values()
    }
    private  fun vendedor_values(){
        val db = FirebaseFirestore.getInstance()
        val perfilesRef = db.collection("perfiles")
        userIDvendedor = producto.idUsuario
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val querySnapshot = perfilesRef
                    .whereEqualTo("id", userIDvendedor)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val documento = querySnapshot.documents[0]
                    val nombre = documento.getString("nombre")
                    val imagenUrl = documento.getString("imagenPerfilURl")

                    withContext(Dispatchers.Main) {
                        nombre?.let { tvNombreVendedor.text = it }
                        imagenUrl?.let {
                            Glide.with(this@DetalleProductoActivity)
                                .load(it)
                                .override(200, 200) // No más grande que esto
                                .circleCrop()
                                .thumbnail(0.1f)
                                .into(ivImagenVendedor)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DetalleProductoActivity, "Perfil no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetalleProductoActivity, "Error al obtener el perfil: ${e.message}", Toast.LENGTH_LONG).show()

                }
            }
        }

    }
    private fun init_listeners(){
        flecha.setOnClickListener {
            finish()//Salimos de la actividad
        }
        btChat.setOnClickListener{
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("currentUserUid", userId) // tu propio UID
            intent.putExtra("otherUserUid", userIDvendedor)     // el destinatario
            if(userId != userIDvendedor){
                startActivity(intent)
            }


        }
    }
}