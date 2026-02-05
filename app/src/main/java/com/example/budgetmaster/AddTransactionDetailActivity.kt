package com.example.budgetmaster

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.budgetmaster.data.FirestoreTransactionRepository
import com.example.budgetmaster.data.Transaction
import com.example.budgetmaster.viewmodel.TransactionViewModel
import com.example.budgetmaster.viewmodel.TransactionViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionDetailActivity : AppCompatActivity() {

    private lateinit var dateTextView: TextView
    private lateinit var categorySpinner: Spinner
    private lateinit var amountEditText: EditText
    private lateinit var titleEditText: EditText
    private lateinit var messageEditText: EditText
    private lateinit var addPhotoLayout: LinearLayout

    private lateinit var receiptImageContainer: FrameLayout
    private lateinit var receiptImageView: ImageView
    private lateinit var removeImageButton: ImageView

    private lateinit var saveButton: Button


    private val calendar = Calendar.getInstance()
    private var selectedImageUri: Uri? = null
    private var latestTmpUri: Uri? = null

    private val PICK_IMAGE_FROM_GALLERY_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private val PERMISSIONS_REQUEST_CODE = 3

    private lateinit var transactionViewModel: TransactionViewModel
    private val firebaseStorage = FirebaseStorage.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestoreDb = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction_detail)

        dateTextView = findViewById(R.id.date_text_view)
        categorySpinner = findViewById(R.id.category_spinner)
        amountEditText = findViewById(R.id.amount_edit_text)
        titleEditText = findViewById(R.id.title_edit_text)
        messageEditText = findViewById(R.id.message_edit_text)
        addPhotoLayout = findViewById(R.id.add_photo_layout)


        receiptImageContainer = findViewById(R.id.receiptImageContainer)
        receiptImageView = findViewById(R.id.receiptImageView)
        removeImageButton = findViewById(R.id.removeImageButton)
        saveButton = findViewById(R.id.save_button)




        removeImageButton.setOnClickListener {
            removeSelectedImage()
        }



        updateDateInView()
        dateTextView.setOnClickListener {
            showDatePickerDialog()
        }


        val firestoreTransactionRepository = FirestoreTransactionRepository()
        val viewModelFactory = TransactionViewModelFactory(firestoreTransactionRepository)
        transactionViewModel = ViewModelProvider(this, viewModelFactory)[TransactionViewModel::class.java] // ViewModel initialisation: [https://developer.android.com/topic/libraries/architecture/viewmodel]


        setupCategorySpinner()


        val selectedCategoryFromIntent = intent.getStringExtra("SELECTED_CATEGORY")
        if (selectedCategoryFromIntent != null && selectedCategoryFromIntent.isNotEmpty()) {
            transactionViewModel.allDistinctCategories.observe(this) { categories ->
                val adapter = categorySpinner.adapter as? ArrayAdapter<String>
                if (adapter != null) {
                    val position = adapter.getPosition(selectedCategoryFromIntent)
                    if (position != -1) {
                        categorySpinner.setSelection(position)
                    } else {

                        val currentSpinnerItems = (0 until adapter.count).map { adapter.getItem(it) as String }.toMutableList()
                        currentSpinnerItems.add(0, selectedCategoryFromIntent)

                        val combinedAndDistinctCategories = currentSpinnerItems.distinct().toMutableList()
                        val newAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, combinedAndDistinctCategories)
                        newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        categorySpinner.adapter = newAdapter
                        categorySpinner.setSelection(combinedAndDistinctCategories.indexOf(selectedCategoryFromIntent))
                    }
                }
            }
        }


        addPhotoLayout.setOnClickListener {
            showImageSourceDialog()
        }


        saveButton.setOnClickListener {
            saveTransaction()
        }
    }


    private fun removeSelectedImage() {
        selectedImageUri = null
        latestTmpUri = null
        receiptImageView.setImageDrawable(null)
        receiptImageContainer.visibility = View.GONE
        Toast.makeText(this, "Image removed.", Toast.LENGTH_SHORT).show()
        Log.d("AddTransactionDetail", "Selected image cleared.")
    }

    private fun updateDateInView() {
        val dateFormat = "MMMM dd,yyyy"
        val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
        dateTextView.text = sdf.format(calendar.time)
    }

    private fun showDatePickerDialog() {
        DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupCategorySpinner() {

        val defaultCategories = mutableListOf(
            "Food", "Transport", "Medicine", "Groceries",
            "Rent", "Gifts", "Entertainment", "Hobbies"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, defaultCategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        transactionViewModel.allDistinctCategories.observe(this) { firestoreCategories ->
            val combinedCategories = mutableListOf<String>()

            firestoreCategories.forEach { category ->
                if (!defaultCategories.contains(category) && category != "Savings") {
                    combinedCategories.add(category)
                }
            }

            combinedCategories.addAll(defaultCategories)


            val newAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, combinedCategories.distinct())
            newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = newAdapter
        }
    }


    private fun showImageSourceDialog() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Library", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Photo")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Take Photo" -> {
                    checkCameraPermissionAndTakePhoto()
                }
                options[item] == "Choose from Library" -> {
                    openGallery()
                }
                options[item] == "Cancel" -> {
                    dialog.dismiss()
                }
            }
        }
        builder.show()
    }


    private fun checkCameraPermissionAndTakePhoto() {
        when {

            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Camera Permission Needed")
                    .setMessage("This app needs the Camera permission to take photos of receipts.")
                    .setPositiveButton("OK") { _, _ ->
                        requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CODE)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            else -> {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CODE)
            }
        }
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, PICK_IMAGE_FROM_GALLERY_REQUEST)
    }


    private fun takePhoto() {

        val photoFile: File? = try {
            createImageFile()
        } catch (ex: Exception) {
            Toast.makeText(this, "Error creating image file.", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            latestTmpUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                it
            )
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, latestTmpUri)
            startActivityForResult(takePictureIntent, TAKE_PHOTO_REQUEST)
        }
    }


    private fun createImageFile(): File {

        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                takePhoto()
            } else {

                Toast.makeText(this, "Camera permission denied. Cannot take photo.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_FROM_GALLERY_REQUEST -> {
                    data?.data?.let { uri ->
                        val contentResolver = applicationContext.contentResolver
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        contentResolver.takePersistableUriPermission(uri, takeFlags)
                        selectedImageUri = uri
                        receiptImageView.setImageURI(selectedImageUri)
                        receiptImageContainer.visibility = View.VISIBLE
                        Log.d("AddTransactionDetail", "Selected image URI from gallery: $selectedImageUri")
                    }
                }
                TAKE_PHOTO_REQUEST -> {
                    latestTmpUri?.let { uri ->
                        selectedImageUri = uri
                        receiptImageView.setImageURI(selectedImageUri)
                        receiptImageContainer.visibility = View.VISIBLE
                        Log.d("AddTransactionDetail", "Captured image URI from camera: $selectedImageUri")
                    }
                }
            }
        } else {
            Log.d("AddTransactionDetail", "Image picker/camera operation cancelled or failed (Result Code: $resultCode)")
            selectedImageUri = null
            receiptImageContainer.visibility = View.GONE
        }
    }

    private fun saveTransaction() {
        val category = categorySpinner.selectedItem.toString()
        val type = "Expense"

        val amountString = amountEditText.text.toString().trim()
        val title = titleEditText.text.toString().trim()
        val message = messageEditText.text.toString().trim()

        if (amountString.isEmpty()) {
            amountEditText.error = "Amount is required"
            return
        }

        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            amountEditText.error = "Please enter a valid positive amount"
            return
        }

        if (title.isEmpty()) {
            titleEditText.error = "Expense title is required"
            return
        }

        lifecycleScope.launch {
            var receiptImageUrl: String? = null
            if (selectedImageUri != null) {
                try {
                    val userId = firebaseAuth.currentUser?.uid
                    if (userId == null) {
                        Toast.makeText(this@AddTransactionDetailActivity, "User not logged in. Cannot upload image.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val imageFileName = "${System.currentTimeMillis()}_${selectedImageUri!!.lastPathSegment ?: "image_receipt"}"

                    val storageRef = firebaseStorage.reference.child("users/$userId/receipts/$imageFileName")

                    val uploadTask = storageRef.putFile(selectedImageUri!!).await()
                    receiptImageUrl = uploadTask.storage.downloadUrl.await().toString()
                    Log.d("AddTransactionDetail", "Image uploaded successfully. Download URL: $receiptImageUrl")

                    Glide.with(this@AddTransactionDetailActivity)
                        .load(receiptImageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_dialog_alert)
                        .into(receiptImageView)
                    receiptImageContainer.visibility = View.VISIBLE

                } catch (e: Exception) {
                    Toast.makeText(this@AddTransactionDetailActivity, "Error uploading receipt image: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("AddTransactionDetail", "Error uploading image", e)
                    return@launch
                }
            }

            val transaction = Transaction(
                date = calendar.timeInMillis,
                category = category,
                type = type,
                amount = amount,
                title = title,
                message = if (message.isEmpty()) null else message,
                receiptImageUri = receiptImageUrl
            )

            try {
                transactionViewModel.insertTransaction(transaction)
                Toast.makeText(this@AddTransactionDetailActivity, "Transaction Saved Successfully!", Toast.LENGTH_SHORT).show()

                val userId = firebaseAuth.currentUser?.uid
                if (userId != null) {
                    val userDocRef = firestoreDb.collection("users").document(userId)
                    userDocRef.update("transactionsMadeCount", FieldValue.increment(1))
                        .addOnSuccessListener {
                            Log.d("AddTransactionDetail", "Cumulative transaction count incremented successfully in Firestore.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("AddTransactionDetail", "Error incrementing cumulative transaction count: ${e.message}", e)
                        }
                }

                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddTransactionDetailActivity, "Error saving transaction: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AddTransactionDetail", "Error saving transaction", e)
            }
        }
    }
}