package com.alica.instagramclonekotlin.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alica.instagramclonekotlin.databinding.RecyclerRowBinding
import com.alica.instagramclonekotlin.model.Post

class FeedRecyclerAdapter(private val postList : ArrayList<Post>) : RecyclerView.Adapter<FeedRecyclerAdapter.PostHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PostHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PostHolder,
        position: Int
    ) {
        val post = postList[position]
        holder.binding.textViewUserEmail.text = post.email
        holder.binding.textViewComment.text = post.comment
        // Base64 string'i Bitmap'e dönüştür
        try {
            val imageBitmap = base64ToBitmap(post.imageBitmap as String)
            holder.binding.imageViewPostImage.setImageBitmap(imageBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            // Hata durumunda placeholder göster
            holder.binding.imageViewPostImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    class PostHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root){

    }

}