package com.ultron.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Crimson = Color(0xFFE53A4F)
private val Background = Color(0xFF080A0F)
private val Panel = Color(0xFF151820)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { UltronApp() }
    }
}

@Composable
fun UltronApp() {
    var page by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    MaterialTheme(colorScheme = darkColorScheme(primary = Crimson, background = Background, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Background).padding(18.dp)) {
            Text("ULTRON", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
            Text("MOBILE  /  ALPHA 01", color = Crimson, fontSize = 11.sp)
            Spacer(Modifier.height(30.dp))
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (page) {
                    0 -> {
                        Text("◉", fontSize = 125.sp, color = Crimson)
                        Text("SYSTEM STANDBY", color = Color.White, letterSpacing = 3.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Neural interface initialized", color = Color.Gray)
                    }
                    1 -> {
                        Text("NEURAL LINK", color = Crimson, fontSize = 22.sp)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message Ultron") })
                        Spacer(Modifier.height(10.dp))
                        Text("AI connection arrives in the next milestone.", color = Color.Gray)
                    }
                    2 -> {
                        Text("CEREBRO MEMORY", color = Crimson, fontSize = 22.sp)
                        Text("Persistent memory is not enabled yet.", color = Color.Gray)
                    }
                    else -> {
                        Text("SYSTEM CONFIGURATION", color = Crimson, fontSize = 22.sp)
                        Text("Gemini API key setup is planned for alpha.2.", color = Color.Gray)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("HUD", "CHAT", "MEMORY", "SETTINGS").forEachIndexed { index, title ->
                    Button(
                        onClick = { page = index },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (page == index) Crimson else Panel),
                        shape = RoundedCornerShape(9.dp)
                    ) { Text(title, fontSize = 10.sp) }
                }
            }
        }
    }
}
