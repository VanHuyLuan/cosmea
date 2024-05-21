package com.example.profile

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.example.model.ProfileData

@Composable
internal fun ProfileRoute(
    onLogoutClick: () -> Unit,
    onClickProfile: () -> Unit,
    onClickAccount: () -> Unit
) {
    ProfileScreen(onLogoutClick, onClickProfile, onClickAccount)
}

@Preview
@Composable
fun ProfileScreen(onLogoutClick: () -> Unit, onClickProfile: () -> Unit, onClickAccount: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        val bitmap = remember { mutableStateOf<Bitmap?>(null) }
        val avatarResource = remember { mutableStateOf(R.drawable.avatar_default) }

        val coroutineScope = rememberCoroutineScope()
        val sharedPref = context.getSharedPreferences("CosmeaApp", Context.MODE_PRIVATE)
        val idUser: String? = sharedPref.getString("currentUserId", null)
        var userData by remember { mutableStateOf<ProfileData?>(null) }

        LaunchedEffect(idUser) {
            if (idUser != null) {
                userData = getCurrentProfileData(idUser,coroutineScope)
            }
        }
        bitmap.value?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
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
            if(userData?.avatar == "null"){
                Image(
                    painter = painterResource(id = avatarResource.value),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(150.dp)
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
                        .size(150.dp)
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
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = userData?.displayName.toString(), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { /* Handle Edit Profile */
            onClickProfile()
        }) {
            Text("Edit Profile")
        }
        Button(onClick = { /* Handle Edit Profile */
            onClickAccount()
        }) {
            Text("Account Management")
        }
        Button(onClick = { /* Handle Add Status */
            onLogoutClick()

        }) {
            Text("Logout")
        }
        userData?.let { AboutMeCard(it) }
    }
}


@Composable
fun AboutMeCard(userData : ProfileData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "About Me", style = MaterialTheme.typography.titleMedium)
            // Replace with actual About Me text
            Text(text = if(userData.bio == "null"){""} else {userData.bio.toString()}, style = MaterialTheme.typography.bodyLarge)
            Friends()
        }
    }
}

@Composable
fun Friends() {
    // Replace with actual implementation
}