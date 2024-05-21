package com.example.profile

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.example.data.service.UserService
import com.example.designsystem.component.Background
import com.example.model.ProfileData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Locale

@Composable
internal fun ProfileViewRoute(
    onBackClick: () -> Unit
) {
    ProfileViewScreen(onBackClick)
}

suspend fun getCurrentProfileData(id: String, coroutineScope: CoroutineScope): ProfileData? {
    var profileData: ProfileData? = null
    coroutineScope.launch {
        val userService = UserService(FirebaseFirestore.getInstance())
        profileData = userService.getUserProfile(id)
    }.join()
    return profileData
}



@SuppressLint("CoroutineCreationDuringComposition")
@Preview
@Composable
fun ProfileViewScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val isUploading = remember { mutableStateOf(false) }

    val launcherCamera = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) {
        if(it != null){
            bitmap.value = it
        }

    }
    // Kiểm tra xem thiết bị có ứng dụng camera không
    val packageManager: PackageManager = context.packageManager
    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    val cameraAvailable = cameraIntent.resolveActivity(packageManager) != null

    val launcherImage = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
        if(Build.VERSION.SDK_INT < 28){
            bitmap.value = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
        }
        else {
            val source = it?.let { it1 ->
                ImageDecoder.createSource(context.contentResolver, it1)
            }
            bitmap.value = source?.let { it1 ->
                ImageDecoder.decodeBitmap(it1)
            }!!
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val sharedPref = context.getSharedPreferences("CosmeaApp", Context.MODE_PRIVATE)
    val idUser: String? = sharedPref.getString("currentUserId", null)
    var userData by remember { mutableStateOf<ProfileData?>(null) }

    LaunchedEffect(idUser) {
        if (idUser != null) {
            userData = getCurrentProfileData(idUser,coroutineScope)
        }
    }
    var userNameState: MutableState<String>? = null
    var dobState : MutableState<String>? = null
    var bioState : MutableState<String>? = null
    userData?.let {
        println(it.displayName)
        userNameState = remember { mutableStateOf(userData?.displayName.toString()) }
        dobState = remember { mutableStateOf(userData?.dob.toString()) }
        bioState = remember { mutableStateOf(userData?.bio.toString()) }
    }


    Background {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Nút Back
                Button(
                    onClick = { onBackClick() },
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text("Back")
                }
            }
            bitmap.value?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                )
            }
            
            if(bitmap.value == null){
                Log.e("USERDATA", userData?.avatar.toString())
                if(userData?.avatar == "null" || userData?.avatar == ""){
                    Image(
                        painter = painterResource(id = R.drawable.avatar_default),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = CircleShape
                            ),
                        colorFilter = ColorFilter.tint(Color.Gray)
                    )
                }
                else {
                    Image(
                        painter = rememberImagePainter(userData?.avatar),
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
            }

            if(userNameState == null) {
                Text(text = "User Name", style = MaterialTheme.typography.titleLarge)
            }
            else {
                Text(text = userNameState!!.value, style = MaterialTheme.typography.titleLarge)
            }

            // Button to change avatar (you can implement your own logic)
            Button(onClick = { /* Handle changing avatar */
//                launcher.launch("image/*")
                showDialog = true
            }) {
                Text("Change Avatar")
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Text field for user name
                userNameState?.let {
                    OutlinedTextField(
                        value = userNameState!!.value,
                        onValueChange = { userNameState!!.value = it },
                        label = { Text("Display Name", style = MaterialTheme.typography.bodyLarge) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { /* Handle user name change */ }
                        )
                    )
                }

                val calendar = Calendar.getInstance()

                // DatePickerDialog
                val datePickerDialog = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        // Update the dobState with the selected date
                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                        calendar.set(year, month, dayOfMonth)
                        dobState!!.value = sdf.format(calendar.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                // Text field for email
                Box {
                    dobState?.let {
                        OutlinedTextField(
                            value = if(dobState?.value == "null"){""}else{dobState!!.value},
                            onValueChange = { dobState!!.value = it },
                            label = { Text("Date Of Birth", style = MaterialTheme.typography.bodyLarge) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                        )

                        IconButton(onClick = { datePickerDialog.show() },
                            modifier = Modifier.padding(start = 230.dp, top = 10.dp),
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    }
                }

                bioState?.let {
                    OutlinedTextField(
                        value = if(bioState?.value == "null"){""}else{bioState!!.value},
                        onValueChange = { bioState!!.value = it },
                        label = { Text("Story", style = MaterialTheme.typography.bodyLarge) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { /* Handle password change */ }
                        )
                    )
                }

                // Button to save changes
                Button(
                    onClick = {
                        // Handle saving changes
                        isUploading.value = true
                        coroutineScope.launch {
                            if (bitmap.value != null && idUser != null) {
                                uploadImageToFirebase(bitmap.value!!, idUser, context as ComponentActivity) { success, imageUrl ->
                                    if (success && imageUrl != null) {
                                        coroutineScope.launch {
                                            val userService = UserService(FirebaseFirestore.getInstance())
                                            userService.updateProfileImageUrl(idUser, imageUrl)
                                            val updatedProfile = ProfileData(
                                                displayName = userNameState?.value,
                                                dob = dobState?.value,
                                                bio = bioState?.value,
                                                avatar = imageUrl,
                                                id = idUser
                                            )
                                            println(imageUrl)
                                            userService.updateUserProfile(idUser, updatedProfile)
                                            Toast.makeText(context, "Uploaded Successfully", Toast.LENGTH_SHORT).show()
                                            isUploading.value = false
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                                        isUploading.value = false
                                    }
                                }
                            } else {
                                val updatedProfile = ProfileData(
                                    displayName = userNameState?.value,
                                    dob = dobState?.value,
                                    bio = bioState?.value,
                                    avatar = userData?.avatar,
                                    id = idUser ?: ""
                                )
                                coroutineScope.launch {
                                    if (idUser != null) {
                                        val userService = UserService(FirebaseFirestore.getInstance())
                                        userService.updateUserProfile(idUser, updatedProfile)
                                        isUploading.value = false
                                    }
                                }
                            }
                            Toast.makeText(context, "Changed Profile Successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isUploading.value
                ) {
                    Text("Save Changes")
                }
            }

        }

        Column(
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 10.dp)
        ) {
            if (showDialog) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(300.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(start = 60.dp)) {
                        Image(painter = painterResource(id = R.drawable.camera),
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clickable {
                                    if (cameraAvailable) {
                                        launcherCamera.launch()
                                    } else {
                                        Toast
                                            .makeText(
                                                context,
                                                "Không có ứng dụng camera khả dụng",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                }
                        )
                        Text(
                            text = "Camera",
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.padding(30.dp))
                    Column {
                        Image(painter = painterResource(id = R.drawable.picture),
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clickable {
                                    launcherImage.launch("image/*")
                                    showDialog = false
                                }
                        )
                        Text(
                            text = "Gallery",
                            color = Color.White
                        )
                    }
                    Column(modifier = Modifier.padding(start = 50.dp, bottom = 80.dp)) {
                        Text(text = "X",
                            color = Color.White,
                            modifier = Modifier
                                .clickable { showDialog = false }
                        )
                    }
                }
            }
        }
        Column (horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .height(450.dp)
                .fillMaxWidth()
        ){
            if(isUploading.value){
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(16.dp),
                    color = Color.White
                )
            }
        }
    }
}


fun uploadImageToFirebase(bitmap: Bitmap, userId: String?, context: ComponentActivity, callback: (Boolean, String?) -> Unit) {
    val storageRef = Firebase.storage.reference
    val imageRef = storageRef.child("avatar/${userId}.jpg")

    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    val imageData = baos.toByteArray()

    imageRef.putBytes(imageData).addOnSuccessListener {
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            callback(true, uri.toString())
        }.addOnFailureListener {
            callback(false, null)
        }
    }.addOnFailureListener {
        callback(false, null)
    }
}


