package com.qcafr.gatherapp.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.data.preferences.AuthPreferenceManager
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel

@Composable
fun SignInScreen(
    viewModel: QuestionnaireViewModel,
    authManager: AuthPreferenceManager,
    isDarkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onSignInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Theme-aware colors – onSurface is dark in light mode, light in dark mode
    val textColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fieldContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    fun doSignIn(u: String, p: String) {
        if (u.isBlank() || p.isBlank()) {
            errorMessage = "Please enter both username and password (እባክዎ የተጠቃሚ ስም እና የይለፍ ቃል ያስገቡ)"
            return
        }
        isLoading = true
        errorMessage = null
        viewModel.signIn(u.trim(), p, authManager) { success, err ->
            isLoading = false
            if (success) {
                Toast.makeText(context, "እንኳን ደህና መጡ! Signed in successfully.", Toast.LENGTH_SHORT).show()
                onSignInSuccess()
            } else {
                errorMessage = err ?: "Sign in failed. Check credentials or server connection."
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            // imePadding() pushes the content above the soft keyboard so the
            // password field is never hidden when the keyboard slides up.
            .imePadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top bar: Theme Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { onToggleTheme(!isDarkMode) },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDarkMode) PrimaryIndigo.copy(alpha = 0.15f)
                        else AccentAmber.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (isDarkMode) PrimaryIndigo.copy(alpha = 0.4f)
                        else AccentAmber.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = if (isDarkMode) AccentAmber else PrimaryIndigo
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Logo Icon (Official ICS / CRVS Logo)
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.qcafr.gatherapp.R.drawable.app_logo),
            contentDescription = "ICS CRVS Logo",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, PrimaryIndigo.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "CRVS Assessment",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            textAlign = TextAlign.Center
        )

        Text(
            text = "የሲአርቪኤስ መመዘኛ እና መጠይቅ ሥርዓት",
            fontSize = 13.sp,
            color = mutedColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in to take exam or manage system\nፈተናውን ለመውሰድ እባክዎ ይግቡ",
            fontSize = 13.sp,
            color = mutedColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Error message banner
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentDanger.copy(alpha = 0.12f))
                    .border(1.dp, AccentDanger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = AccentDanger,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = AccentDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Username Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; errorMessage = null },
            label = { Text("Username / የተጠቃሚ ስም") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = mutedColor)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            textStyle = LocalTextStyle.current.copy(color = textColor, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = BorderSubtle,
                focusedLabelColor = PrimaryIndigo,
                unfocusedLabelColor = mutedColor,
                // theme-aware: dark text on light bg, light text on dark bg
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = PrimaryIndigo,
                focusedContainerColor = fieldContainerColor,
                unfocusedContainerColor = fieldContainerColor
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password / የይለፍ ቃል") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = mutedColor)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = mutedColor
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            textStyle = LocalTextStyle.current.copy(color = textColor, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    doSignIn(username, password)
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = BorderSubtle,
                focusedLabelColor = PrimaryIndigo,
                unfocusedLabelColor = mutedColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = PrimaryIndigo,
                focusedContainerColor = fieldContainerColor,
                unfocusedContainerColor = fieldContainerColor
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign In Button
        Button(
            onClick = {
                focusManager.clearFocus()
                doSignIn(username, password)
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("ይግቡ በመገናኘት ላይ… (Signing In…)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("ይግቡ / Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Navigate to Sign Up
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSignUp() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                fontSize = 14.sp,
                color = mutedColor
            )
            Text(
                text = "Sign Up / አዲስ መለያ ይፍጠሩ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
