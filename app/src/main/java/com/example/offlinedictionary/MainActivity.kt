package com.example.offlinedictionary

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class WordItem(val id: Int, val category: String, val english: String, val hindi: String, val pron: String)
data class PhraseItem(val id: Int, val category: String, val english: String, val hindi: String, val tag: String)

class DictDbHelper(context: Context) : SQLiteOpenHelper(context, "dict.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE words (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, english TEXT, hindi TEXT, pron TEXT)")
        db.execSQL("CREATE TABLE phrases (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, english TEXT, hindi TEXT, tag TEXT)")
        
        val words = listOf(
            arrayOf("Animals", "Lion", "शेर", "लायन"),
            arrayOf("Animals", "Tiger", "बाघ", "टाइगर"),
            arrayOf("Animals", "Elephant", "हाथी", "एलिफ़ेंट"),
            arrayOf("Animals", "Cheetah", "चीता", "चीता"),
            arrayOf("Animals", "Camel", "ऊँट", "कैमल"),
            arrayOf("Human Body", "Forehead", "माथा", "फ़ोरहेड"),
            arrayOf("Human Body", "Shoulder", "कंधा", "शोल्डर"),
            arrayOf("Human Body", "Wrist", "कलाई", "रिस्ट"),
            arrayOf("Plants", "Root", "जड़", "रूट"),
            arrayOf("Plants", "Leaf", "पत्ती", "लीफ़"),
            arrayOf("Vegetables", "Potato", "आलू", "पोटैटो"),
            arrayOf("Vegetables", "Tomato", "टमाटर", "टोमैटो"),
            arrayOf("Vegetables", "Garlic", "लहसुन", "गार्लिक")
        )
        for (w in words) {
            val cv = ContentValues().apply {
                put("category", w[0]); put("english", w[1]); put("hindi", w[2]); put("pron", w[3])
            }
            db.insert("words", null, cv)
        }

        val phrases = listOf(
            arrayOf("Home", "Wake up, it's getting late.", "उठ जाओ, देर हो रही है।", "Morning"),
            arrayOf("Home", "Breakfast is ready.", "नाश्ता तैयार है।", "Meal"),
            arrayOf("Hotel", "A table for two, please.", "दो लोगों के लिए टेबल चाहिए।", "Entry"),
            arrayOf("Hotel", "Could we have the menu?", "क्या मेन्यू कार्ड मिल सकता है?", "Ordering"),
            arrayOf("Market", "How much is this for?", "यह कितने का है?", "Price"),
            arrayOf("Market", "Give some discount.", "कुछ कम करो भाई।", "Bargaining"),
            arrayOf("Travel", "Which platform will the train arrive?", "ट्रेन किस प्लेटफ़ॉर्म पर आएगी?", "Station")
        )
        for (p in phrases) {
            val cv = ContentValues().apply {
                put("category", p[0]); put("english", p[1]); put("hindi", p[2]); put("tag", p[3])
            }
            db.insert("phrases", null, cv)
        }
    }
    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {}

    fun getWords(category: String, query: String): List<WordItem> {
        val list = mutableListOf<WordItem>()
        val db = readableDatabase
        val cursor = if (query.isNotBlank()) {
            db.rawQuery("SELECT id, category, english, hindi, pron FROM words WHERE english LIKE ? OR hindi LIKE ? ORDER BY english ASC", arrayOf("%" + query + "%", "%" + query + "%"))
        } else if (category != "All") {
            db.rawQuery("SELECT id, category, english, hindi, pron FROM words WHERE category = ? ORDER BY english ASC", arrayOf(category))
        } else {
            db.rawQuery("SELECT id, category, english, hindi, pron FROM words ORDER BY english ASC", null)
        }
        while (cursor.moveToNext()) {
            list.add(WordItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)))
        }
        cursor.close()
        return list
    }

    fun getPhrases(category: String, query: String): List<PhraseItem> {
        val list = mutableListOf<PhraseItem>()
        val db = readableDatabase
        val cursor = if (query.isNotBlank()) {
            db.rawQuery("SELECT id, category, english, hindi, tag FROM phrases WHERE english LIKE ? OR hindi LIKE ? ORDER BY id ASC", arrayOf("%" + query + "%", "%" + query + "%"))
        } else if (category != "All") {
            db.rawQuery("SELECT id, category, english, hindi, tag FROM phrases WHERE category = ? ORDER BY id ASC", arrayOf(category))
        } else {
            db.rawQuery("SELECT id, category, english, hindi, tag FROM phrases ORDER BY id ASC", null)
        }
        while (cursor.moveToNext()) {
            list.add(PhraseItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)))
        }
        cursor.close()
        return list
    }
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var dbHelper: DictDbHelper
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DictDbHelper(this)
        tts = TextToSpeech(this, this)

        setContent {
            MaterialTheme {
                MainScreen(dbHelper) { text ->
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(db: DictDbHelper, onSpeak: (String) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("All") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Offline Dictionary", fontWeight = FontWeight.Bold) }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0; selectedCat = "All" }, icon = { Icon(Icons.Default.MenuBook, null) }, label = { Text("Words") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1; selectedCat = "All" }, icon = { Icon(Icons.Default.ChatBubbleOutline, null) }, label = { Text("Phrases") })
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize()) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Search English or Hindi...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            val cats = if (tab == 0) listOf("All", "Animals", "Human Body", "Plants", "Vegetables") else listOf("All", "Home", "Hotel", "Market", "Travel")

            LazyRow(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats) { c ->
                    FilterChip(selected = selectedCat == c, onClick = { selectedCat = c }, label = { Text(c) })
                }
            }

            if (tab == 0) {
                val words = remember(search, selectedCat) { db.getWords(selectedCat, search) }
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(words) { w ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(w.english, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(w.hindi, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onSpeak(w.english) }) { Icon(Icons.Default.VolumeUp, null) }
                            }
                        }
                    }
                }
            } else {
                val phrases = remember(search, selectedCat) { db.getPhrases(selectedCat, search) }
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(phrases) { ph ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(ph.tag, fontSize = 12.sp, color = Color.Gray)
                                    IconButton(onClick = { onSpeak(ph.english) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.VolumeUp, null) }
                                }
                                Text(ph.english, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(ph.hindi, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
