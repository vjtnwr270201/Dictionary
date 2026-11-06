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
            arrayOf("Human Body", "Head", "सिर", "हेड"),
            arrayOf("Human Body", "Hair", "बाल", "हेयर"),
            arrayOf("Human Body", "Face", "चेहरा", "फेस"),
            arrayOf("Human Body", "Forehead", "माथा", "फ़ोरहेड"),
            arrayOf("Human Body", "Eye", "आँख", "आई"),
            arrayOf("Human Body", "Eyebrow", "भौंह", "आईब्रो"),
            arrayOf("Human Body", "Eyelash", "पलक", "आईलैश"),
            arrayOf("Human Body", "Ear", "कान", "इयर"),
            arrayOf("Human Body", "Nose", "नाक", "नोज़"),
            arrayOf("Human Body", "Nostril", "नथुना", "नॉस्ट्रिल"),
            arrayOf("Human Body", "Cheek", "गाल", "चीक"),
            arrayOf("Human Body", "Lips", "होंठ", "लिप्स"),
            arrayOf("Human Body", "Mouth", "मुँह", "माउथ"),
            arrayOf("Human Body", "Teeth", "दाँत", "टीथ"),
            arrayOf("Human Body", "Tongue", "जीभ", "टंग"),
            arrayOf("Human Body", "Chin", "ठोड़ी", "चिन"),
            arrayOf("Human Body", "Neck", "गर्दन", "नेक"),
            arrayOf("Human Body", "Throat", "गला", "थ्रोट"),
            arrayOf("Human Body", "Shoulder", "कंधा", "शोल्डर"),
            arrayOf("Human Body", "Chest", "छाती", "चेस्ट"),
            arrayOf("Human Body", "Back", "पीठ", "बैक"),
            arrayOf("Human Body", "Waist", "कमर", "वेस्ट"),
            arrayOf("Human Body", "Stomach", "पेट", "स्टमक"),
            arrayOf("Human Body", "Navel", "नाभि", "नेवल"),
            arrayOf("Human Body", "Arm", "बाँह", "आर्म"),
            arrayOf("Human Body", "Upper Arm", "ऊपरी बाँह", "अपर आर्म"),
            arrayOf("Human Body", "Elbow", "कोहनी", "एल्बो"),
            arrayOf("Human Body", "Forearm", "अग्रभुजा", "फोरआर्म"),
            arrayOf("Human Body", "Wrist", "कलाई", "रिस्ट"),
            arrayOf("Human Body", "Hand", "हाथ", "हैंड"),
            arrayOf("Human Body", "Palm", "हथेली", "पाम"),
            arrayOf("Human Body", "Thumb", "अंगूठा", "थंब"),
            arrayOf("Human Body", "Index Finger", "तर्जनी", "इंडेक्स फिंगर"),
            arrayOf("Human Body", "Middle Finger", "मध्यमा", "मिडिल फिंगर"),
            arrayOf("Human Body", "Ring Finger", "अनामिका", "रिंग फिंगर"),
            arrayOf("Human Body", "Little Finger", "कनिष्ठिका", "लिटिल फिंगर"),
            arrayOf("Human Body", "Fingernail", "हाथ का नाखून", "फिंगरनेल"),
            arrayOf("Human Body", "Leg", "टाँग", "लेग"),
            arrayOf("Human Body", "Thigh", "जांघ", "थाई"),
            arrayOf("Human Body", "Knee", "घुटना", "नी"),
            arrayOf("Human Body", "Kneecap", "घुटने की चक्की", "नीकैप"),
            arrayOf("Human Body", "Calf", "पिंडली", "काफ"),
            arrayOf("Human Body", "Ankle", "टखना", "एंकर"),
            arrayOf("Human Body", "Foot", "पैर", "फुट"),
            arrayOf("Human Body", "Heel", "एड़ी", "हील"),
            arrayOf("Human Body", "Sole", "तलवा", "सोल"),
            arrayOf("Human Body", "Toenail", "पैर का नाखून", "टोनेल"),
            arrayOf("Human Body", "Big Toe", "पैर का अंगूठा", "बिग टो"),
            arrayOf("Human Body", "Brain", "दिमाग", "ब्रेन"),
            arrayOf("Human Body", "Heart", "दिल", "हार्ट"),
            arrayOf("Human Body", "Lungs", "फेफड़े", "लंग्स"),
            arrayOf("Human Body", "Liver", "जिगर / यकृत", "लिवर"),
            arrayOf("Human Body", "Kidney", "गुर्दा", "किडनी"),
            arrayOf("Human Body", "Small Intestine", "छोटी आंत", "स्मॉल इंटेस्टाइन"),
            arrayOf("Human Body", "Large Intestine", "बड़ी आंत", "लार्ज इंटेस्टाइन"),
            arrayOf("Human Body", "Pancreas", "अग्न्याशय", "पैंक्रियाज"),
            arrayOf("Human Body", "Spleen", "तिल्ली", "स्प्लीन"),
            arrayOf("Human Body", "Gallbladder", "पित्ताशय", "गॉलब्लैडर"),
            arrayOf("Human Body", "Urinary Bladder", "मूत्राशय", "यूरिनरी ब्लैडर"),
            arrayOf("Human Body", "Food Pipe", "ग्रासनली", "फूड पाइप"),
            arrayOf("Human Body", "Windpipe", "श्वासनली", "विंडपाइप"),
            arrayOf("Human Body", "Appendix", "अपेंडिक्स", "अपेंडिक्स"),
            arrayOf("Human Body", "Bone", "हड्डी", "बोन"),
            arrayOf("Human Body", "Skull", "खोपड़ी", "स्कल"),
            arrayOf("Human Body", "Backbone", "रीढ़ की हड्डी", "बैकबोन"),
            arrayOf("Human Body", "Rib", "पसली", "रिब"),
            arrayOf("Human Body", "Muscle", "मांसपेशी", "मसल"),
            arrayOf("Human Body", "Joint", "जोड़", "जॉइंट"),
            arrayOf("Human Body", "Cartilage", "उपास्थि", "कार्टिलेज"),
            arrayOf("Human Body", "Blood", "खून / रक्त", "ब्लड"),
            arrayOf("Human Body", "Skin", "त्वचा", "स्किन"),
            arrayOf("Human Body", "Nerve", "नस / तंत्रिका", "नर्व"),
            arrayOf("Human Body", "Vein", "नस / शिरा", "वेन"),
            arrayOf("Human Body", "Artery", "धमनी", "आर्टरी"),
            arrayOf("Human Body", "Blood Vessel", "रक्त वाहिका", "ब्लड वेसल"),
            arrayOf("Human Body", "Reproductive Organ", "प्रजनन अंग", "रिप्रोडक्टिव ऑर्गन"),
            arrayOf("Human Body", "Penis", "शिश्न", "पेनिस"),
            arrayOf("Human Body", "Testicles", "अंडकोष", "टेस्टिकल्स"),
            arrayOf("Human Body", "Vagina", "योनि", "वजाइना"),
            arrayOf("Human Body", "Uterus", "गर्भाशय", "यूटेरस"),
            arrayOf("Human Body", "Ovary", "अंडाशय", "ओवरी"),
            arrayOf("Human Body", "Breast", "स्तन", "ब्रेस्ट"),
            arrayOf("Human Body", "Nipple", "निप्पल", "निप्पल"),
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
