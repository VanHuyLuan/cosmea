package com.example.profile

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.service.UserService
import com.example.designsystem.component.Background
import com.example.designsystem.icon.Icons
import com.example.model.UserData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun AccountManagementRoute(
    onPasswordChangeClick: () -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    AccountManagementScreen(onPasswordChangeClick,onBackClick, onLogoutClick)
}


@Preview
@Composable
fun AccountManagementScreen(
    onPasswordChangeClick: () -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = context.getSharedPreferences("CosmeaApp", Context.MODE_PRIVATE)
    val idUser: String? = sharedPref.getString("currentUserId", null)
    var userData by remember { mutableStateOf<UserData?>(null) }
    var userNameState: MutableState<String>? = null
    var emailState : MutableState<String>? = null

    LaunchedEffect(idUser) {
        if (idUser != null) {
            userData = getCurrentAccountData(idUser,coroutineScope)
        }
    }

    userData?.let {
        userNameState = remember { mutableStateOf(userData?.username.toString()) }
        emailState = remember { mutableStateOf(userData?.email.toString()) }
    }
    Background {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Your Information!!", style = MaterialTheme.typography.titleLarge )
                Spacer(modifier = Modifier.height(16.dp))
                userNameState?.let {
                    OutlinedTextField(
                        value = userNameState!!.value,
                        onValueChange = { userNameState!!.value = it },
                        label = { Text("User Name", style = MaterialTheme.typography.titleMedium) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { /* Handle user name change */ }
                        )
                    )
                }
                emailState?.let {
                    OutlinedTextField(
                        value = emailState!!.value,
                        onValueChange = { emailState!!.value = it },
                        label = { Text("Email", style = MaterialTheme.typography.titleMedium) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { /* Handle user name change */ }
                        )
                    )
                }
                userData?.password?.let {
                    OutlinedTextField(
                        value = it,
                        onValueChange = { },
                        label = { Text("Password", style = MaterialTheme.typography.titleMedium) },
                        visualTransformation = if (passwordVisible) {
                            // display password if passwordVisible is true
                            VisualTransformation.None
                        } else {
                            // hide password if passwordVisible is false
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            // display an icon to toggle password visibility
                            val icon = if (passwordVisible) Icons.Eye else Icons.EyeOff
                            Icon(
                                imageVector = icon,
                                contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                modifier = Modifier.clickable {
                                    passwordVisible = !passwordVisible
                                }
                            )
                        }
                    )
                }
                Button(onClick = onPasswordChangeClick) {
                    Text("Change Password")
                }
                Button(onClick = {
                    if (idUser != null) {
                        userData?.let { changeInformation(userData = it, userName = userNameState?.value.toString(), email = emailState?.value.toString(), idUser = idUser, coroutineScope = coroutineScope) }
                        Toast.makeText(context, "Change Information Successfully", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "Failed to change information", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Save")
                }
                Button(onClick = onLogoutClick) {
                    Text("Logout")
                }
            }
        }
    }
}
fun changeInformation(userData: UserData, userName :String, email: String, idUser: String, coroutineScope: CoroutineScope){
    userData.username = userName
    userData.email = email
    coroutineScope.launch {
        val userService = UserService(FirebaseFirestore.getInstance())
        userService.updateUserData(idUser, userData)
    }
}
fun changePassword(userData: UserData, newPassword :String, idUser: String, coroutineScope: CoroutineScope){
    userData.password = newPassword
    coroutineScope.launch {
        val userService = UserService(FirebaseFirestore.getInstance())
        userService.updateUserData(idUser, userData)
    }
}
suspend fun getCurrentAccountData(id: String, coroutineScope: CoroutineScope): UserData? {
    var userData: UserData? = null
    coroutineScope.launch {
        val userService = UserService(FirebaseFirestore.getInstance())
        userData = userService.getUserDataById(id)
    }.join()
    return userData
}

@Composable
internal fun ChangePasswordRoute(
    onBackClick: () -> Unit,
) {
    ChangePasswordScreen(onBackClick)
}

@Preview
@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit,
) {
    var checkChangePassword = true
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = context.getSharedPreferences("CosmeaApp", Context.MODE_PRIVATE)
    val idUser: String? = sharedPref.getString("currentUserId", null)
    var userData by remember { mutableStateOf<UserData?>(null) }

    LaunchedEffect(idUser) {
        if (idUser != null) {
            userData = getCurrentAccountData(idUser,coroutineScope)
        }
    }

    var newPassword by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Old Password") },
                    visualTransformation = if (passwordVisible) {
                        // display password if passwordVisible is true
                        VisualTransformation.None
                    } else {
                        // hide password if passwordVisible is false
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        // display an icon to toggle password visibility
                        val icon = if (passwordVisible) Icons.Eye else Icons.EyeOff
                        Icon(
                            imageVector = icon,
                            contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                            modifier = Modifier.clickable {
                                passwordVisible = !passwordVisible
                            }
                        )
                    }
                )
                if(userData?.password != oldPassword.toString() && oldPassword.toString() != "") {
                    checkChangePassword = false
                    Text(text = "Password is incorrect!",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else {
                    checkChangePassword = true
                    Spacer(modifier = Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = if (passwordVisible) {
                        // display password if passwordVisible is true
                        VisualTransformation.None
                    } else {
                        // hide password if passwordVisible is false
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        // display an icon to toggle password visibility
                        val icon = if (passwordVisible) Icons.Eye else Icons.EyeOff
                        Icon(
                            imageVector = icon,
                            contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                            modifier = Modifier.clickable {
                                passwordVisible = !passwordVisible
                            }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = if (passwordVisible) {
                        // display password if passwordVisible is true
                        VisualTransformation.None
                    } else {
                        // hide password if passwordVisible is false
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        // display an icon to toggle password visibility
                        val icon = if (passwordVisible) Icons.Eye else Icons.EyeOff
                        Icon(
                            imageVector = icon,
                            contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                            modifier = Modifier.clickable {
                                passwordVisible = !passwordVisible
                            }
                        )
                    }

                )
                if(newPassword != confirmPassword && confirmPassword.toString() != "")
                {
                    checkChangePassword = false
                    Text(text = "Confirm Password is incorrect!",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else {
                    checkChangePassword = true
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { if(checkChangePassword){
                    userData?.let { changePassword(userData = it, newPassword = newPassword, idUser = userData?.id.toString() ,coroutineScope ) }
                    Toast.makeText(context, "Change Password Successfully", Toast.LENGTH_SHORT).show()
                }
                    else {
                    Toast.makeText(context, "Information is incorrect", Toast.LENGTH_SHORT).show()
                }
                }) {
                    Text("Change Password")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
