package com.example.proyecto.objectos
import kotlinx.coroutines.tasks.await
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

data class User (val id : String, val nombre: String, val gmail: String, val contrasenya: String) {
    constructor(gmail: String, contrasenya: String) : this("Desconocido","", gmail, contrasenya) {
        // Llama al constructor primario con un nombre por defecto
    }

    suspend fun comprobarsiExiste(context: Context,db: FirebaseFirestore): Boolean { //paso un Context para que aparezca el toast en la Activity de la cual se castea este objeto
        var esta = false
        var result = db.collection("users").get().await()
        val usuarios = result.documents
        // Almacenas el resultado de la consulta (una lista de documentos)
        Log.d("Nombre", this.nombre)
        Log.d("Gmail", this.gmail)
        Log.d("contrasenya", this.contrasenya)
        for (document in usuarios) {
            // Accedo a todos los campos de la coleccion users de firebase
            val gmail = document.getString("gmail")
            val passw = document.getString("contrasenya")
            Log.d("Gmailvsgamil", "gmail insertado: "+this.gmail+" vs "+" gmail firabes: "+gmail)
            Log.d("contrasenyavscontrasenya", "Contraseña insertada: "+this.contrasenya+" vs "+" Contraseña firabes: "+passw)

            if(this.gmail == gmail &&
                this.contrasenya == passw){
                esta = true;
                break
            }

        }
        println("Esto es el objeto: "+this)
        return esta
    }




}