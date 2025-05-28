package com.example.proyecto.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.proyecto.Activities.VentaActivity
import com.example.proyecto.R
import com.example.proyecto.objectos.PerfilUsario
import com.example.proyecto.objectos.Producto
import com.example.proyecto.objectos.RetrofitInstance
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class PerfilActivity : AppCompatActivity() {
    lateinit var   userId : String
    private lateinit var iconoHome: ImageView
    private lateinit var iconoVenta: ImageView
    private lateinit var iconoPerfil: ImageView
    private lateinit var ivImagenView : ImageView
    private lateinit var iconomensaje: ImageView
    private lateinit var tvNombre : TextView
    private lateinit var ivlapizEditar2 : ImageView
    private lateinit var btCerrarSession: Button
    
    var imagenFile : File? = null // Archivo de imagen seleccionado
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var imgurUrl: String? = null // Almacenará la URL de
    private val IMAGE_PICK_CODE = 1000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)

        // Recupera el ID del usuario pasado desde el Login
        userId = intent.getStringExtra("userId").toString()


        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                uri?.let {
                    val filePath = getRealPathFromURI(uri)
                    if (filePath != null) {
                        imagenFile = File(filePath)
                        Glide.with(this@PerfilActivity)
                            .load(imagenFile)
                            .override(200, 200) // No más grande que esto
                            .thumbnail(0.1f)
                            .circleCrop()
                            .into(ivImagenView)

                        // Subir la imagen justo después de seleccionarla
                        lifecycleScope.launch {
                            uploadImageToImgur(imagenFile!!)
                        }

                    } else {
                        Toast.makeText(this@PerfilActivity, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init_components()
        menu_repetido()
        init_values()
        init_listeners()

    }

    private fun init_components(){
        iconoPerfil = findViewById(R.id.iconoPerfil)
        iconoHome = findViewById<ImageView>(R.id.iconoHome)
        iconoVenta = findViewById<ImageView>(R.id.iconoVenta)
        iconomensaje = findViewById(R.id.iconomensaje)
        tvNombre = findViewById(R.id.tvNombre)
        ivImagenView = findViewById(R.id.ivImagenPerfil)
        ivlapizEditar2 = findViewById(R.id.ivlapiEditar2)
        btCerrarSession = findViewById(R.id.btcerrarSesion)

    }
    private fun init_listeners(){
        ivImagenView.setOnClickListener {
            checkPermissionsAndPickImage()
        }


        tvNombre.setOnClickListener   {
            showRegisterNombre()
        }
        ivlapizEditar2.setOnClickListener   {
            showRegisterNombre()
        }
        btCerrarSession.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


    }
    fun menu_repetido(){
        iconoVenta.setOnClickListener {
            val intent = Intent(this, VentaActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
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

    // TODO ESTO PARA QUE LA IMAGEN SE GUARDE EN LA BD
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
                            Glide.with(this@PerfilActivity)
                                .load(link)
                                .override(200, 200) // No más grande que esto
                                .thumbnail(0.1f)
                                .circleCrop()
                                .into(ivImagenView)
                            Toast.makeText(this@PerfilActivity, "Imagen subida exitosamente", Toast.LENGTH_SHORT).show()
                            actualizarPerfil(imgurUrl)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PerfilActivity, "Error al subir imagen: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {

            withContext(Dispatchers.Main) {
                Toast.makeText(this@PerfilActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun showRegisterNombre() {
        // Cargar el layout personalizado para el diálogo
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_perfil_editarnombre, null)
        val userName:EditText = dialogView.findViewById(R.id.etNombrePerfil)
        // Crear el AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                tvNombre.text = userName.text
                actualizarPerfil("no")
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
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

    //TODO ESTO PARA PONER LA IMAGEN EN EL IMAGE VIEW


    private fun actualizarPerfil(imgurUrl: String?) {
        val id = userId
        val nombre = tvNombre.text.toString()
        val imagen = imgurUrl

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val perfilesRef = db.collection("perfiles")
                val querySnapshot = perfilesRef
                    .whereEqualTo("id", id)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    // El perfil ya existe, actualizamos la imagen
                    val documentId = querySnapshot.documents[0].id
                    if(imagen != "no"){
                        perfilesRef.document(documentId)
                            .update("imagenPerfilURl", imagen)
                            .await()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@PerfilActivity, "Perfil Actualizado", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else{
                        perfilesRef.document(documentId)
                            .update("nombre", tvNombre.text.toString())
                            .await()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@PerfilActivity, "Perfil Actualizado", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    // El perfil no existe, lo creamos
                    val perfil = PerfilUsario(id, nombre, imagen.toString())
                    perfilesRef.add(perfil).await()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PerfilActivity, "Perfil creado exitosamente", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PerfilActivity, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()

                }
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

    private fun  init_values(){
        val db = FirebaseFirestore.getInstance()
        val perfilesRef = db.collection("perfiles")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val querySnapshot = perfilesRef
                    .whereEqualTo("id", userId)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val documento = querySnapshot.documents[0]
                    val nombre = documento.getString("nombre")
                    val imagenUrl = documento.getString("imagenPerfilURl")

                    withContext(Dispatchers.Main) {
                        nombre?.let { tvNombre.text = it }
                        imagenUrl?.let {
                            Glide.with(this@PerfilActivity)
                                .load(it)
                                .override(200, 200) // No más grande que esto
                                .thumbnail(0.1f)
                                .circleCrop()
                                .into(ivImagenView)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PerfilActivity, "Perfil no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PerfilActivity, "Error al obtener el perfil: ${e.message}", Toast.LENGTH_LONG).show()

                }
            }
        }

    }

}