package com.example.dubaothoitiet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.dubaothoitiet.viewmodel.AuthViewModel
import com.example.dubaothoitiet.viewmodel.UserViewModel
import com.example.dubaothoitiet.viewmodel.AuthState


@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    onAuthSuccess: () -> Unit,
) {
    val authState by authViewModel.authState.collectAsState()
    var isLoginScreen by remember { mutableStateOf(true) }

    // Reset state mỗi khi màn hình được compose
    LaunchedEffect(Unit) {
        authViewModel.resetState()
    }

    // Khi xác thực thành công, cập nhật user state và đóng dialog
    LaunchedEffect(authState) {
        val state = authState
        if (state is AuthState.Authenticated) {
            userViewModel.onLoginSuccess(state.userId, state.username)
            onAuthSuccess()
        }
    }

    // [THAY ĐỔI] Bọc toàn bộ nội dung trong một Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(), // Co lại theo nội dung
        shape = MaterialTheme.shapes.large, // Bo góc lớn
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Dùng màu nền chuẩn của Theme
        ),
        elevation = CardDefaults.cardElevation(8.dp) // Thêm đổ bóng
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp), // Thêm padding bên trong Card
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // [THÊM] Icon để trông đẹp hơn
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Authentication",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isLoginScreen) "Đăng Nhập" else "Đăng Ký",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoginScreen) {
                LoginContent(authViewModel)
            } else {
                RegisterContent(authViewModel)
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = authState) {
                is AuthState.Loading -> CircularProgressIndicator()
                is AuthState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            AuthToggle(isLoginScreen = isLoginScreen, onToggle = {
                isLoginScreen = !isLoginScreen
                authViewModel.resetState() // Reset state khi chuyển tab
            })
        }
    }
}

@Composable
fun LoginContent(viewModel: AuthViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Tên đăng nhập") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Mật khẩu") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { viewModel.login(username, password) },
        enabled = username.isNotBlank() && password.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Đăng Nhập")
    }
}

@Composable
fun RegisterContent(viewModel: AuthViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Tên đăng nhập") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Mật khẩu") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = { Text("Nhập lại mật khẩu") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { viewModel.register(username, password, confirmPassword) },
        enabled = username.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Đăng Ký")
    }
}

@Composable
fun AuthToggle(isLoginScreen: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (isLoginScreen) "Chưa có tài khoản?" else "Đã có tài khoản?")
        Text(
            text = if (isLoginScreen) " Đăng ký ngay" else " Đăng nhập",
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(4.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}
