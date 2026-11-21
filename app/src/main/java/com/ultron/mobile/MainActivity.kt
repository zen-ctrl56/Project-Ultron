package com.ultron.mobile

import android.content.Context
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.concurrent.thread

private val Crimson = Color(0xFFE53A4F)
private val Background = Color(0xFF080A0F)
private val Panel = Color(0xFF151820)
private const val MODEL = "gemini-2.5-flash"
private const val KEY_ALIAS = "ultron_gemini_key"

private class SecureKeyStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("ultron_secure", Context.MODE_PRIVATE)
    private fun secret(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }
    fun save(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret())
        val bytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("key", Base64.encodeToString(cipher.iv + bytes, Base64.NO_WRAP)).apply()
    }
    fun load(): String? {
        val encoded = prefs.getString("key", null) ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) { null }
    }
    fun clear() { prefs.edit().remove("key").apply() }
}

private data class ChatLine(val role: String, val text: String)

private fun requestGemini(apiKey: String, history: List<ChatLine>): String {
    val contents = JSONArray()
    for (line in history) {
        contents.put(JSONObject()
            .put("role", if (line.role == "USER") "user" else "model")
            .put("parts", JSONArray().put(JSONObject().put("text", line.text))))
    }
    val payload = JSONObject()
        .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
            "You are ULTRON MOBILE, a concise, capable futuristic Android AI assistant. Be helpful and transparent about your capabilities. You cannot control device functions unless explicitly implemented."))))
        .put("contents", contents)
    val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 45000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-goog-api-key", apiKey)
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        val json = JSONObject(response)
        if (code !in 200..299) {
            val reason = json.optJSONObject("error")?.optString("message") ?: "HTTP $code"
            throw IllegalStateException("Gemini request failed ($code): $reason")
        }
        val candidates = json.optJSONArray("candidates")
        val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val answer = buildString {
            if (parts != null) for (i in 0 until parts.length()) {
                val text = parts.optJSONObject(i)?.optString("text").orEmpty()
                if (text.isNotBlank()) append(text)
            }
        }
        return answer.ifBlank { "No text response returned. The request may have been blocked or returned non-text output." }
    } finally { connection.disconnect() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = SecureKeyStore(applicationContext)
        setContent { UltronApp(secure, this) }
    }
}

@Composable
private fun UltronApp(secure: SecureKeyStore, activity: MainActivity) {
    var page by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var keyDraft by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(secure.load() != null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val chat = remember { mutableStateListOf<ChatLine>() }
    MaterialTheme(colorScheme = darkColorScheme(primary = Crimson, background = Background, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Background).padding(18.dp)) {
            Text("ULTRON", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
            Text("MOBILE  /  NEURAL CORE ALPHA 02", color = Crimson, fontSize = 11.sp)
            Spacer(Modifier.height(22.dp))
            Column(Modifier.weight(1f).fillMaxWidth()) {
                when (page) {
                    0 -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("◉", fontSize = 125.sp, color = Crimson)
                        Text(if (hasKey) "NEURAL CORE READY" else "NEURAL CORE OFFLINE", color = Color.White, letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(if (hasKey) "Open CHAT to communicate" else "Add your Gemini API key in SETTINGS", color = Color.Gray)
                    }
                    1 -> {
                        Text("NEURAL LINK", color = Crimson, fontSize = 22.sp)
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(chat.toList()) { line ->
                                Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(12.dp)) {
                                    Text(line.role, color = Crimson, fontSize = 10.sp)
                                    Text(line.text, color = Color.White)
                                }
                            }
                        }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (status.isNotBlank()) Text(status, color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(value = message, onValueChange = { message = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Message Ultron") }, maxLines = 4)
                        Spacer(Modifier.height(8.dp))
                        Button(enabled = !busy && message.isNotBlank() && hasKey, onClick = {
                            val key = secure.load()
                            if (key == null) {
                                hasKey = false
                                status = "Add your API key in SETTINGS."
                            } else {
                                val prompt = message.trim()
                                message = ""
                                chat.add(ChatLine("USER", prompt))
                                val snapshot = chat.toList()
                                busy = true
                                status = "Connecting to Gemini..."
                                thread {
                                    val result = runCatching { requestGemini(key, snapshot) }
                                    activity.runOnUiThread {
                                        result.onSuccess { chat.add(ChatLine("ULTRON", it)); status = "" }
                                            .onFailure { status = it.message ?: "Network request failed." }
                                        busy = false
                                    }
                                }
                            }
                        }) { Text("TRANSMIT") }
                    }
                    2 -> Column {
                        Text("CEREBRO MEMORY", color = Crimson, fontSize = 22.sp)
                        Text("Long-term encrypted memory is planned for a later release.", color = Color.Gray)
                        Text("Chat history currently lasts only until the app process is closed.", color = Color.Gray)
                    }
                    else -> Column {
                        Text("SYSTEM CONFIGURATION", color = Crimson, fontSize = 22.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(if (hasKey) "Gemini API key saved on this device" else "No Gemini API key configured", color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = keyDraft, onValueChange = { keyDraft = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Gemini API key") },
                            visualTransformation = PasswordVisualTransformation(), singleLine = true)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = keyDraft.isNotBlank(), onClick = {
                                secure.save(keyDraft.trim())
                                keyDraft = ""
                                hasKey = true
                                status = "Key saved."
                            }) { Text("SAVE KEY") }
                            OutlinedButton(onClick = { secure.clear(); hasKey = false; keyDraft = ""; status = "Key removed." }) {
                                Text("REMOVE KEY")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Stored with Android Keystore AES-GCM encryption. Requests are sent directly to Google's Gemini API. Never share your key or commit it to GitHub.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("HUD", "CHAT", "MEMORY", "SETTINGS").forEachIndexed { index, title ->
                    Button(onClick = { page = index }, modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (page == index) Crimson else Panel),
                        shape = RoundedCornerShape(9.dp)) { Text(title, fontSize = 10.sp) }
                }
            }
        }
    }
}
