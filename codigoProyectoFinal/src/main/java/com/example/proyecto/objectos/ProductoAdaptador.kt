package com.example.proyecto.objectos

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.proyecto.Activities.DetalleProductoActivity
import com.example.proyecto.R
import com.google.firebase.firestore.Query

class ProductoAdaptador(private var productos: List<Producto> = emptyList(), private val userId: String
):

    RecyclerView.Adapter<ProductoAdaptador.ProductoViewHolder>() {

    private var productosFiltrados: List<Producto> = productos
    private var lastQuery: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false) // ✅ Layout personalizado

        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productosFiltrados[position]
        with(holder) {
            tvNombre.text = producto.nombre
            tvPrecio.text = "${"%.2f".format(producto.precio)}€"
            val requestOptions = RequestOptions()
                .transform(com.bumptech.glide.load.resource.bitmap.CenterCrop(), RoundedCorners(40))//Las configuaricones que le quiero meter al glide osea que la imagen tenga los bordes redondos

            // Carga la imagen con Glide
            Glide.with(itemView.context)
                .load(producto.imagenUrl)
                .thumbnail(0.1f)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .apply(requestOptions)
                .into(ivProducto)
        }
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetalleProductoActivity::class.java)
            intent.putExtra("userId", userId) // 👈 Añades el userId aquí
            intent.putExtra("producto", productos[position])
            context.startActivity(intent)

        }
    }

    override fun getItemCount() = productosFiltrados.size

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val ivProducto: ImageView = itemView.findViewById(R.id.imagenProducto) // ← AÑADIR ESTO
        // Todos los elementos del layout

    }

    fun filtrar(query: String) {
        lastQuery = query.takeIf { it.isNotEmpty() }
        aplicarFiltroActual()
        notifyDataSetChanged()
    }

    fun actualizarLista(nuevaLista: List<Producto>) {
        val oldList = productosFiltrados // Guarda la lista actual antes de cambiarla
        productos = nuevaLista
        productosFiltrados = nuevaLista

        val diffCallback = ProductoDiffCallback(oldList = oldList, newList = nuevaLista)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        diffResult.dispatchUpdatesTo(this)
    }


    private fun aplicarFiltroActual() {
        productosFiltrados = lastQuery?.let { query ->
            val palabras = query.split("\\s+".toRegex()) // Divide el query en palabras
            productos.filter { producto ->
                palabras.all { palabra ->  //Que todas las palabras esten en el producto
                    producto.nombre.contains(palabra, ignoreCase = true)
                }
            }
        } ?: productos
    }
}