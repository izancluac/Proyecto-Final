package com.example.proyecto.Activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.proyecto.R
import com.example.proyecto.objectos.CarruselImagenAdaptador
import com.example.proyecto.objectos.PerfilUsario
import com.example.proyecto.objectos.User
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    lateinit var tvcreateaccount:TextView
    lateinit var buttonInicarSesion: Button
    lateinit var imageCarousel: ViewPager2

    val db = FirebaseFirestore.getInstance() //Instanciamos la bd

    //Creamos un objeto que queremos guardar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val version = Build.VERSION.SDK_INT
        Toast.makeText(this, "Versión de Android: $version", Toast.LENGTH_LONG).show()

        val imageList = listOf(
            R.drawable.imagenzapatillas,
            R.drawable.ropa,
            R.drawable.ropa2,
            R.drawable.ropa3
        )

        init_components()
        init_listeners()


        //Para que el carrusel vaya cada 3 secs
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val nextItem = (imageCarousel.currentItem + 1) % imageList.size
                imageCarousel.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable)

        //Llamada al adapatador del carrusel
        val adapter = CarruselImagenAdaptador(imageList)
        imageCarousel.adapter = adapter

    }

    private fun init_components(){
        tvcreateaccount = findViewById(R.id.tvcreateaccount)
        buttonInicarSesion = findViewById(R.id.buttonInicaSesion)
        imageCarousel = findViewById(R.id.imagenesCarrusel)
    }
    private fun init_listeners(){
        tvcreateaccount.setOnClickListener(){
          showRegisterDialogCrearCuenta()

        }

        buttonInicarSesion.setOnClickListener() {
            showRegisterDialogIniciarSesion()
        }
    }
    //La ventana emergente para crear una cuenta
    fun showRegisterDialogCrearCuenta() {
        // Cargar el layout personalizado para el diálogo
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_crea_una_cuenta, null)

        // Crear el AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setPositiveButton("Registrar") { dialog, _ ->
                // Obtener los valores ingresados por el usuario
                val etName:EditText = dialogView.findViewById(R.id.userName)
                val etEmail: EditText = dialogView.findViewById(R.id.gmail)
                val etPassword: EditText = dialogView.findViewById(R.id.password)

                val user = etName.text.toString()
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()

                // Validar los campos
                if (email.isEmpty() || password.isEmpty() || user.isEmpty()) {
                    Toast.makeText(this, "Por favor, ingrese todos los campos.", Toast.LENGTH_SHORT).show()
                    showRegisterDialogCrearCuenta()
                }else if (!email.endsWith("@gmail.com")) {  //El endsWith es algo que pensaba que no existia y parece bastante util la verdad
                    Toast.makeText(this, "El correo electrónico no es válido. Debe terminar en @gmail.com", Toast.LENGTH_SHORT).show()
                    showRegisterDialogCrearCuenta()
                }
                else {
                    // Guardar los datos en la base de datos
                    lifecycleScope.launch {
                        saveUserToDatabase(user, email, password)
                    }
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }


    //La ventana emergente para inicar sesion

    fun showRegisterDialogIniciarSesion() {
        // Cargar el layout personalizado para el diálogo
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_inicia_sesion, null)

        // Crear el AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setPositiveButton("Iniciar Sesión") { dialog, _ ->
                // Obtener los valores ingresados por el usuario
                val etgmail: EditText = dialogView.findViewById(R.id.gmail)
                val etPassword: EditText = dialogView.findViewById(R.id.password)

                val gmail = etgmail.text.toString()
                val password = etPassword.text.toString()
                // Validar los campos
                if (gmail.isEmpty() || password.isEmpty() ) {
                    Toast.makeText(this, "Por favor, ingrese todos los campos.", Toast.LENGTH_SHORT).show()
                    showRegisterDialogIniciarSesion()
                } else {
                    // Guardar los datos en la base de datos
                    val usuario = User(gmail , password)
                    var existe = false
                    //Esta es otra manera de hacer una corutina
                    lifecycleScope.launch {
                        // Llamar al método suspend y esperar el resultado
                        Log.d("Existe gmail?", gmail)
                        val existe = usuario.comprobarsiExiste(this@MainActivity, db)
                        Log.d("Existe ?", ""+existe)
                        if (existe) {
                            //Coger el id del usuario y pasarlo a la siguiente actividad
                            lifecycleScope.launch {
                                try {
                                    // Realizamos la consulta sobre la colección "users" filtrando por email y contraseña.
                                    val querySnapshot = db.collection("users")
                                        .whereEqualTo("gmail", gmail)
                                        .whereEqualTo("contrasenya", password)
                                        .get()
                                        .await()

                                    // Si la consulta no devuelve ningún resultado, las credenciales son incorrectas.
                                    if (querySnapshot.isEmpty) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Credenciales incorrectas. Inténtalo de nuevo.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        // Si encontramos un usuario, tomamos el primero (se asume que el usuario es único)
                                        val userDocument = querySnapshot.documents.first()
                                        // Recuperamos el ID asignado en el campo "id".
                                        // O, si prefieres, podrías usar: userDocument.id para el ID generado por Firestore.
                                        val userId = userDocument.getString("id") ?: userDocument.id

                                        // Pasamos el ID a la siguiente actividad mediante un Intent extra.
                                        val intent = Intent(this@MainActivity, PaginaPrincipal::class.java)
                                        intent.putExtra("userId", userId)
                                        startActivity(intent) //Nos vamos a la siguiente página
                                        finish()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Error al iniciar sesión. Inténtalo más tarde.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                        }
                        else{
                            Toast.makeText(this@MainActivity, "Esta cuenta no existe", Toast.LENGTH_SHORT).show()
                            showRegisterDialogIniciarSesion()
                        }
                    }

                }
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }


    //Es suspend porque dentro utiliza un metodo suspend para que el return sea asincrono al d
    private suspend fun saveUserToDatabase(user: String, email: String, password: String) {
        val usuario = User("",user ,email, password)
        var existe = usuario.comprobarsiExiste(this, db)

        if (!existe) {
            try {
                val nuevoId = generarNuevoIdUsuario(db)
                val perfilUsuario = PerfilUsario(nuevoId,"user","")
                val nuevoUsuario = User(nuevoId, user, email, password)
                //nuevo Usuario
                // Guardar el objeto en Firestore de manera suspendida
                val documentReference = db.collection("users")
                    .add(nuevoUsuario)
                    .await() // Usamos await() para esperar la respuesta de Firestore de forma asincrónica

                println("Documento agregado con ID: ${documentReference.id}")
                // Mostrar el Toast en el hilo principal
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Usuario registrado: $email", Toast.LENGTH_SHORT).show()
                }



                val perfil = PerfilUsario(
                    id = nuevoId,
                    nombre = user,  // Puedes dejarlo en blanco o pedirlo antes
                    imagenPerfilURl = ""       // Inicialmente vacío
                )

                val db = FirebaseFirestore.getInstance()
                db.collection("perfiles")
                    .add(perfil)
                    .addOnSuccessListener {
                        Log.d("MainActivity", "Perfil creado exitosamente")

                    }


            } catch (e: Exception) {
                // Manejo de excepciones si falla la operación
                println("Error al agregar documento: $e")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error al registrar el usuario", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "El correo que has indicado ya existe, inicia sesión", Toast.LENGTH_SHORT).show()
                showRegisterDialogCrearCuenta()
            }
        }
    }

    suspend fun generarNuevoIdUsuario(db: FirebaseFirestore): String {
        val snapshot = db.collection("users").orderBy("id").get().await()

        // Si no hay usuarios aún, empezamos desde 1
        if (snapshot.isEmpty) return "user001"

        // Tomamos el último documento (por orden alfabético del campo "id")
        val ultimoId = snapshot.documents.last().getString("id") ?: return "user001"

        // Extraemos el número del ID, asumiendo formato "user001"
        val numero = ultimoId.filter { it.isDigit() }.toIntOrNull() ?: 0
        val nuevoNumero = numero + 1


        // Formateamos a 3 cifras, puedes cambiar esto si necesitas otro formato
        return "user" + String.format("%03d", nuevoNumero)


    }
}