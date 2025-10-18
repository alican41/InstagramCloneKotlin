package com.alica.instagramclonekotlin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.alica.instagramclonekotlin.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    lateinit var email : String
    lateinit var pass : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        val currentUser = auth.currentUser
        if (currentUser != null){
            intentToFeed()
        }

    }

    private fun intentToFeed(){
        val intent = Intent(this@MainActivity, FeedActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun loginclicked(view : View){

        email = binding.editTextEmail.text.toString()
        pass = binding.editTextPassword.text.toString()

        if (email.equals("") || pass.equals("")){
            Toast.makeText(this@MainActivity,"Please fill in all fields!", Toast.LENGTH_LONG).show()
        }else {
            auth.signInWithEmailAndPassword(email,pass)
                .addOnSuccessListener {
                    intentToFeed()
                }.addOnFailureListener {
                    Toast.makeText(this@MainActivity,it.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }



    }
    fun signupclicked(view : View){

        email = binding.editTextEmail.text.toString()
        pass = binding.editTextPassword.text.toString()

        if (email.equals("") || pass.equals("")){
            Toast.makeText(this@MainActivity,"Please fill in all fields!", Toast.LENGTH_LONG).show()
        }else {
            auth.createUserWithEmailAndPassword(email,pass)
                .addOnSuccessListener {
                    intentToFeed()
            }.addOnFailureListener {
                    Toast.makeText(this@MainActivity,it.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }


    }
    fun forgotpassclicked(view : View){

        // Dialog için özel layout dosyasını yüklüyoruz
        // R.layout.dialog_forgot_password dosyasının daha önce oluşturulduğunu varsayıyoruz
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)

        // Dialog içindeki elemanları bulma
        val emailEditText = dialogView.findViewById<EditText>(R.id.editTextResetEmailDialog)
        val sendButton = dialogView.findViewById<Button>(R.id.buttonResetSend)

        // AlertDialog oluşturucu
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        // AlertDialog'u oluştur ve göster
        val dialog = builder.create()

        // Dialogun arka planını şeffaf yaparak özel drawable'ımızın görünmesini sağlıyoruz
        // Dialogun köşeleri için res/drawable/dialog_background.xml dosyasını kullanıyoruz
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        sendButton.setOnClickListener {
            val resetEmail = emailEditText.text.toString().trim()

            if (resetEmail.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta adresinizi giriniz.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Firebase ile şifre sıfırlama e-postası gönderme
            auth.sendPasswordResetEmail(resetEmail)
                .addOnSuccessListener {
                    Toast.makeText(this, "Şifre sıfırlama linki e-posta adresinize gönderildi.", Toast.LENGTH_LONG).show()
                    dialog.dismiss() // İşlem başarılı, dialogu kapat
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }

        dialog.show()
    }




}