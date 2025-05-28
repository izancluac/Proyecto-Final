package com.example.proyecto.objectos

import android.R.id.message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyecto.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.currentCoroutineContext
import kotlin.collections.addAll
import kotlin.text.clear

    class ChatAdaptadorIndividual(
        val messages: MutableList<ChatMensaje>,
        private val currentUserId: String,
        private val otherUserId: String
    ) : RecyclerView.Adapter<ChatAdaptadorIndividual.MessageViewHolder>() {
        private val db = FirebaseFirestore.getInstance()
        private val profileUrlCache = mutableMapOf<String, String?>()

        companion object {
            private const val VIEW_TYPE_LEFT  = 0  // Mensaje de otro
            private const val VIEW_TYPE_RIGHT = 1  // Mensaje propio
        }
        inner class MessageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.messageText)
            val profileImage: ImageView? = view.findViewById(R.id.ivReceptor) // Solo en left
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val layoutId = when (viewType) {
                VIEW_TYPE_RIGHT -> R.layout.item_message_right
                else            -> R.layout.item_message_left
            }
            val view = LayoutInflater.from(parent.context)
                .inflate(layoutId, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val currentMessage = messages[position]
            val layoutParams = holder.view.layoutParams as ViewGroup.MarginLayoutParams

            // Por defecto, sin margen
            layoutParams.bottomMargin = 0

            // Si hay un siguiente mensaje...
            if (position < messages.size - 1) {
                val nextMessage = messages[position + 1]

                // Si el siguiente mensaje es de otro usuario, añadimos margen abajo
                if (currentMessage.senderId != nextMessage.senderId) {
                    layoutParams.bottomMargin = 16 // dp en px, puedes usar `TypedValue` para convertir si quieres
                }
                else{
                    layoutParams.bottomMargin = 30
                }
            }

            holder.view.layoutParams = layoutParams

            // Setea el texto
            holder.textView.text = currentMessage.text

            val profileId = currentUserId

            holder.profileImage?.let { imgView ->
                    db.collection("perfiles")
                        .whereEqualTo("id", otherUserId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.isEmpty) {
                                Log.e("ChatAdapter", "No se encontró perfil con id campo = $profileId")
                            } else {
                                val doc = snapshot.documents[0]
                                val url = doc.getString("imagenPerfilURl")
                                profileUrlCache[profileId] = url
                                if(url!=""){
                                    Glide.with(imgView.context)
                                        .load(url)
                                        .placeholder(R.drawable.placeholder)
                                        .circleCrop()
                                        .into(imgView)
                                }else{
                                    Glide.with(imgView.context)
                                        .load(R.drawable.placeholder)
                                        .circleCrop()
                                        .into(imgView)
                                }


                            }
                        }
                        .addOnFailureListener {

                        }
                }

        }

        override fun getItemCount(): Int = messages.size

        override fun getItemViewType(position: Int): Int {
            val message = messages[position]
            return if (message.senderId == currentUserId) {
                VIEW_TYPE_RIGHT
            } else {
                VIEW_TYPE_LEFT
            }
        }


    }
