package com.example.agro.ui.screens.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val user = auth.currentUser

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var profileImage by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    LaunchedEffect(Unit) {
        user?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                name = doc.getString("name") ?: ""
                phone = doc.getString("phone") ?: ""
                role = doc.getString("role") ?: ""
                profileImage = doc.getString("profileImage") ?: ""
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image Section
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable { imagePickerLauncher.launch("image/*") }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                        if (selectedImageUri != null) {
                            Image(painter = rememberAsyncImagePainter(selectedImageUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else if (profileImage.isNotEmpty()) {
                            AsyncImage(model = profileImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(70.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(8.dp), tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = user?.email ?: "",
                    onValueChange = {},
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = false,
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.all { char -> char.isDigit() }) phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    label = { Text("Account Role") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = false,
                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) }
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        isUpdating = true
                        if (selectedImageUri != null) {
                            val ref = storage.reference.child("profiles/${user?.uid}.jpg")
                            ref.putFile(selectedImageUri!!)
                                .continueWithTask { task ->
                                    if (!task.isSuccessful) task.exception?.let { throw it }
                                    ref.downloadUrl
                                }
                                .addOnSuccessListener { downloadUrl ->
                                    updateProfile(db, user?.uid!!, name, phone, downloadUrl.toString()) {
                                        isUpdating = false
                                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                }
                                .addOnFailureListener {
                                    isUpdating = false
                                    Toast.makeText(context, "Image Upload Failed", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            updateProfile(db, user?.uid!!, name, phone, profileImage) {
                                isUpdating = false
                                Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isUpdating,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isUpdating) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    else Text("Update Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

private fun updateProfile(db: FirebaseFirestore, uid: String, name: String, phone: String, imageUrl: String, onDone: () -> Unit) {
    db.collection("users").document(uid).update(
        "name", name,
        "phone", phone,
        "profileImage", imageUrl
    ).addOnSuccessListener { onDone() }.addOnFailureListener { onDone() }
}
