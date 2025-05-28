package com.example.proyecto.objectos

import android.widget.ImageView
import java.io.Serializable

data class Producto(val id : String = "",
                    val idUsuario : String = " ",
                    val nombre: String = "",
                    val precio: Double = 0.0,
                    val descripcion: String = "",
                    val talla: String = "",
                    val marca: String = "",
                    val tipo: String = "",
                    val imagenUrl: String = "",
                     ) : Serializable {

}