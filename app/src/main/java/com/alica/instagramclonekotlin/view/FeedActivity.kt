package com.alica.instagramclonekotlin.view

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alica.instagramclonekotlin.R
import com.alica.instagramclonekotlin.adapter.FeedRecyclerAdapter
import com.alica.instagramclonekotlin.databinding.ActivityFeedBinding
import com.alica.instagramclonekotlin.model.Post
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postArrayList : ArrayList<Post>
    private lateinit var feedAdapter : FeedRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        firestore = Firebase.firestore

        postArrayList = ArrayList<Post>()

        getData()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        feedAdapter = FeedRecyclerAdapter(postArrayList)
        binding.recyclerView.adapter = feedAdapter

    }

    private fun getData(){
        firestore.collection("Posts").orderBy("timestamp",
            Query.Direction.DESCENDING).addSnapshotListener { value, error ->
            if (error != null){
                Toast.makeText(this,error.localizedMessage, Toast.LENGTH_LONG).show()
            } else {
                if (value != null){
                    if (!value.isEmpty){

                        val documents = value.documents

                        postArrayList.clear()

                        for (document in documents){
                            val comment = document.get("comment") as String
                            val userEmail = document.get("email") as String
                            val imageBitmap = document.get("imageBitmap") as String

                            val post = Post(userEmail,comment,imageBitmap)
                            postArrayList.add(post)

                        }
                        feedAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.options_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.addpost){
            val intent = Intent(this@FeedActivity, UploadActivity::class.java)
            startActivity(intent)
        }else if (item.itemId == R.id.logout){
            auth.signOut()
            val intentToLogin = Intent(this@FeedActivity, MainActivity::class.java)
            startActivity(intentToLogin)
            finish()
        }

        return super.onOptionsItemSelected(item)
    }



}