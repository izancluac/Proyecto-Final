package com.example.proyecto.objectos

import androidx.recyclerview.widget.DiffUtil

class ProductoDiffCallback(
    private val oldList: List<Producto>,
    private val newList: List<Producto>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Compara por ID (ajusta si tu modelo usa otro identificador único)
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Compara todo el contenido
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}