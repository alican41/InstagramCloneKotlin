package com.alica.instagramclonekotlin.view

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.util.Base64
import com.alica.instagramclonekotlin.R
import com.alica.instagramclonekotlin.databinding.ActivityUploadBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraResultLauncher : ActivityResultLauncher<Uri>
    private var selectedPicture : Uri? = null
    private var currentPhotoUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        registerLauncher()

    }

    fun addimageclicked(view : View){
        showImageSourceDialog(view)
    }

    private fun showImageSourceDialog(view: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_source, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.layoutGallery).setOnClickListener {
            dialog.dismiss()
            checkGalleryPermission(view)
        }

        dialogView.findViewById<View>(R.id.layoutCamera).setOnClickListener {
            dialog.dismiss()
            checkCameraPermission(view)
        }

        dialog.show()
    }

    private fun checkGalleryPermission(view: View) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give Permission") {
                        permissionLauncher.launch(permission)
                    }.show()
            } else {
                permissionLauncher.launch(permission)
            }
        } else {
            openGallery()
        }
    }

    private fun checkCameraPermission(view: View) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Snackbar.make(view, "Camera permission required", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give permission") {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }.show()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            openCamera()
        }
    }
    private fun openGallery() {
        val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(intentToGallery)
    }

    private fun openCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "Photo file could not be created", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                it
            )
            currentPhotoUri = photoURI
            cameraResultLauncher.launch(photoURI)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    fun sharebuttonclicked(view : View){
        if (selectedPicture == null) {
            Toast.makeText(this, "Please choose a picture", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = binding.editTextCaption.text.toString()
        if (comment.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val userEmail = auth.currentUser?.email
        if (userEmail == null) {
            Toast.makeText(this, "User login not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Resmi bitmap'e çevir
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, selectedPicture!!)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, selectedPicture)
            }

            // Bitmap'i küçült (Firestore limitleri için)
            val scaledBitmap = scaleBitmap(bitmap, 800, 800)

            // Bitmap'i Base64 string'e çevir
            val imageBitmap = bitmapToBase64(scaledBitmap)

            // Firestore'a kaydet
            saveToFirestore(imageBitmap, comment, userEmail)

        } catch (e: Exception) {
            Toast.makeText(this, "Error while processing image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveToFirestore(imageBitmap: String, comment: String, email: String) {
        val postMap = hashMapOf(
            "imageBitmap" to imageBitmap,
            "comment" to comment,
            "email" to email,
            "timestamp" to Timestamp.now()
        )

        firestore.collection("Posts")
            .add(postMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
    }




    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    selectedPicture = intentFromResult.data
                    selectedPicture?.let {
                        binding.imageViewPostImage.setImageURI(it)
                    }
                }
            }
        }

        cameraResultLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri?.let {
                    selectedPicture = it
                    binding.imageViewPostImage.setImageURI(it)
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                // İzin verildi - son hangi işlem istenmişse tekrar kontrol et
                Toast.makeText(this@UploadActivity, "Permission granted, please select again", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@UploadActivity, "Permission needed!", Toast.LENGTH_LONG).show()
            }
        }
    }



}