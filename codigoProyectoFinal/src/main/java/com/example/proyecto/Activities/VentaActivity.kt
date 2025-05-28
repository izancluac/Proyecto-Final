package com.example.proyecto.Activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyecto.objectos.Producto
import com.example.proyecto.objectos.RetrofitInstance
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream
import android.Manifest
import android.app.AlertDialog
import android.net.ConnectivityManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.proyecto.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.asRequestBody


class VentaActivity : AppCompatActivity() {
    private lateinit var iconoHome: ImageView
    private lateinit var iconoVenta: ImageView
    private lateinit var iconoPerfil: ImageView
    private lateinit var iconomensaje: ImageView
    private lateinit var ivImagen: ImageView
    private lateinit var etNombre: EditText
    private lateinit var etPrecio: EditText
    private lateinit var etDescripcion : EditText
    private lateinit var etTalla : EditText
    private lateinit var etMarca : EditText
    private lateinit var etTipo : EditText
    private var imgurUrl: String? = null // Almacenará la URL de Imgur
    private var imageFile: File? = null // Archivo de imagen seleccionado
    lateinit var   userId : String

    //Para subir imagen
    private val IMAGE_PICK_CODE = 1000
    private var imageUri: Uri? = null // Si realmente necesitas el Uri
    private lateinit var imageView: ImageView
    private lateinit var uploadButton: Button

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    val opciones = arrayOf("Camiseta/top", "Pantalón", "Suéter", "Vestido", "Abrigo", "Sandalia", "Camisa", "Zapatilla deportiva", "Bolso", "Bota")



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_venta)
        // Recupera el ID del usuario pasado desde el Login
        userId = intent.getStringExtra("userId").toString()


        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                uri?.let {
                    val filePath = getRealPathFromURI(uri)
                    if (filePath != null) {
                        this.imageFile = File(filePath)
                        Glide.with(this@VentaActivity).load(this.imageFile).override(200, 200) // No más grande que esto
                            .thumbnail(0.1f).into(imageView) // Cambia this por this@VentaActivity

                    } else {
                        Toast.makeText(this@VentaActivity, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        init_components()
        menu_repetido()
        init_listeners()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }


    }
    fun init_components(){
        imageView = findViewById(R.id.imageView)
        uploadButton = findViewById(R.id.uploadButton)
        iconoPerfil = findViewById(R.id.iconoPerfil)
        iconoHome = findViewById<ImageView>(R.id.iconoHome)
        iconoVenta = findViewById<ImageView>(R.id.iconoVenta)
        iconomensaje = findViewById(R.id.iconomensaje)
        etNombre = findViewById(R.id.etNombre)
        etPrecio = findViewById(R.id.etPrecio)
        etDescripcion = findViewById(R.id.etDescripcion)
        etTalla = findViewById(R.id.etTalla)
        etMarca = findViewById(R.id.etMarca)
        etTipo = findViewById(R.id.etTipo)
        etTipo.setFocusable(false) // Deshabilitar el enfoque del EditText (evitar el cursor)
        etTipo.setClickable(true)  // Asegurarse de que el EditText aún sea clickeable
        // Asegúrate de tener en tus recursos:
        imageView.setImageResource(R.drawable.placeholder) // Debe existir en res/drawable
    }



    fun menu_repetido(){
        iconoVenta.setOnClickListener {
            val intent = Intent(this, VentaActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            Toast.makeText(this, "Hola", Toast.LENGTH_SHORT).show()
        }
        iconoHome.setOnClickListener {
            val intent = Intent(this, PaginaPrincipal::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        iconoPerfil.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }
        iconomensaje.setOnClickListener {
            val intent = Intent(this, MensajesActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }
    }

    fun init_listeners(){
        imageView.setOnClickListener {
            checkPermissionsAndPickImage()
        }
        etTipo.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Selecciona una opción")
            builder.setItems(opciones) { dialog, which ->
                // Aquí se captura la opción seleccionada
                etTipo.setText(opciones[which])
            }
            builder.show()
        }

        uploadButton.setOnClickListener {
            if (!isInternetAvailable()) {
                Toast.makeText(this, "Sin conexión a Internet", Toast.LENGTH_SHORT).show()
            } else {
                imageFile?.let { file ->
                    lifecycleScope.launch {
                        uploadImageToImgur(file)
                    }
                } ?: Toast.makeText(this, "Selecciona una imagen primero", Toast.LENGTH_SHORT).show()

            }
        }


    }
    private fun checkPermissionsAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), IMAGE_PICK_CODE)
        } else {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        pickImageLauncher.launch(intent)
    }



    // Subir imagen a Imgur de forma asincrónica
    private suspend fun uploadImageToImgur(imageFile: File) {
        try {
            val token = refreshAccessToken()
            token?.let { accessToken ->
                val requestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("image", imageFile.name, requestBody)


                // Realiza la llamada en el hilo de E/S para evitar el NetworkOnMainThreadException
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.uploadImage("Bearer $accessToken", body).execute()
                }

                if (response.isSuccessful) {
                    response.body()?.data?.link?.let { link ->
                        imgurUrl = link
                        withContext(Dispatchers.Main) {
                            Glide.with(this@VentaActivity).load(link).override(200, 200) // No más grande que esto
                                .thumbnail(0.1f).into(imageView)
                            Toast.makeText(this@VentaActivity, "Imagen subida exitosamente", Toast.LENGTH_SHORT).show()
                            guardarProducto(imgurUrl)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@VentaActivity, "Error al subir imagen: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {

            withContext(Dispatchers.Main) {
                Toast.makeText(this@VentaActivity, "No permite la subida de archivos de archivos ." +
                        "" +
                        "" +
                        "" +
                        "" +
                        "" +
                        "webp", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun refreshAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val clientId = "f3769d8a6174224"
            val clientSecret = "c951183267aea80a49bf1b6e4b964de52de657c1"
            val refreshToken = "2b2832fc426d744425d2567d7e80182db35c8306"

            val response = RetrofitInstance.api.refreshAccessToken(
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken
            ).execute()

            if (response.isSuccessful) {
                response.body()?.access_token
            } else {

                null
            }
        } catch (e: Exception) {

            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == IMAGE_PICK_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarProducto(imgurUrl: String?) {
        // Validación de campos
        val idUsuario = userId
        val nombre = etNombre.text.toString().trim()
        val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
        val descripcion = etDescripcion.text.toString().trim()
        val talla = etTalla.text.toString().trim()
        val marca = etMarca.text.toString().trim()
        val tipo = etTipo.text.toString().trim()

        if (nombre.isEmpty() || precio <= 0 || descripcion.isEmpty() || talla.isEmpty() || marca.isEmpty() || tipo.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val newDocRef = firestore.collection("productos").document() // genera ID

                val producto = Producto(
                    id = newDocRef.id, // asigna el ID generado automáticamente
                    idUsuario = idUsuario,
                    nombre = nombre,
                    precio = precio,
                    descripcion = descripcion,
                    talla = talla,
                    marca = marca,
                    tipo = tipo,
                    imagenUrl = imgurUrl.toString()
                )

                newDocRef.set(producto).await() // guarda el producto con su ID adentro

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VentaActivity, "Producto guardado con ID!", Toast.LENGTH_SHORT).show()
                    resetForm()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VentaActivity, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resetForm() {
        etNombre.text.clear()
        etPrecio.text.clear()
        etDescripcion.text.clear()
        etTalla.text.clear()
        etMarca.text.clear()
        etTipo.text.clear()
        imageView.setImageResource(R.drawable.placeholder)
        imgurUrl = null
        imageFile = null
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output ->
                inputStream?.copyTo(output)
            }
            file.absolutePath.apply {
                // Opcional: programar borrado después de 1 hora
                file.deleteOnExit()
            }
        } catch (e: Exception) {
            null
        }
    }
}