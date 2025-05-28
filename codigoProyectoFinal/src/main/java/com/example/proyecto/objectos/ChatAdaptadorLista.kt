package com.example.proyecto.objectos

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyecto.Activities.ChatActivity
import com.example.proyecto.R
import com.google.firebase.firestore.FirebaseFirestore

data class ChatPreviewItem(val chatId: String, val otherUserId: String)

class ChatAdaptadorLista(
    private var chats: MutableList<ChatPreviewItem>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatAdaptadorLista.ChatViewHolder>() {
    private val db = FirebaseFirestore.getInstance()
    private val profileCache = mutableMapOf<String, Pair<String, String>>() // userId -> (name, imageUrl)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_preview, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    fun updateChats(newChats: List<ChatPreviewItem>) {
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged()

    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewOtherUser: TextView = itemView.findViewById(R.id.textViewOtherUser)
        private val imageViewUser: ImageView = itemView.findViewById(R.id.ivImagenPerfil)
        private val cajaUser: CardView = itemView.findViewById(R.id.cvPerfil)
        fun bind(chat: ChatPreviewItem) {
            val context = itemView.context
            val userId = chat.otherUserId

            // Verifica si ya está cacheado
            val cached = profileCache[userId]
            if (cached != null) {
                textViewOtherUser.text = cached.first
                Glide.with(context)
                    .load(cached.second)
                    .placeholder(R.drawable.placeholder)
                    .circleCrop()
                    .into(imageViewUser)
            } else {
                db.collection("perfiles")
                    .whereEqualTo("id", userId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            val doc = snapshot.documents[0]
                            val name = doc.getString("nombre") ?: "Usuario"
                            val imageUrl = doc.getString("imagenPerfilURl") ?: ""

                            profileCache[userId] = Pair(name, imageUrl)

                            textViewOtherUser.text = name
                            Glide.with(context)
                                .load(imageUrl)
                                .placeholder(R.drawable.placeholder)
                                .circleCrop()
                                .into(imageViewUser)
                        } else {
                            textViewOtherUser.text = "Usuario"
                        }
                    }
                    .addOnFailureListener {
                        Log.e("ChatAdapter", "Error cargando perfil: ${it.message}")
                        textViewOtherUser.text = "Usuario"
                    }

            }
            cajaUser.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("currentUserUid", currentUserId)      // tu UID
                    putExtra("otherUserUid", chat.otherUserId)     // el UID del otro
                    putExtra("userId", currentUserId)              // si ChatActivity lo usa, pásalo también
                }
                context.startActivity(intent)
            }
        }

    }
}