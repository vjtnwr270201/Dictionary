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

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = DictDbHelper(this)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DictionaryApp(db = db, onSpeak = { text ->
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    })
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryApp(db: DictDbHelper, onSpeak: (String) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Dictionary", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0; selectedCat = "All" },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = { Text("Words") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1; selectedCat = "All" },
                    icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) },
                    label = { Text("Phrases") }
                )
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize()) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Search English or Hindi...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            val cats = if (tab == 0) {
                listOf(
                    "All",
                    "External Organs",
                    "Internal Organs",
                    "Reproductive Organs",
                    "Wild Animals",
                    "Domestic Animals",
                    "Birds",
                    "Insects",
                    "Reptiles & Amphibians",
                    "Sea Animals",
                    "Daily Veggies",
                    "Leafy Greens",
                    "Gourds",
                    "Roots & Tubers",
                    "Trees",
                    "Flowering Plants",
                    "Medicinal & Herbs",
                    "Crops & Climbers"
                )
            } else {
                listOf("All", "Home", "Hotel", "Market", "Travel")
            }

            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cats) { c ->
                    FilterChip(
                        selected = selectedCat == c,
                        onClick = { selectedCat = c },
                        label = { Text(c) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tab == 0) {
                val words = remember(search, selectedCat) { db.getWords(selectedCat, search) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(words) { w ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(w.english, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(w.hindi, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                    if (w.pron.isNotBlank()) {
                                        Text(w.pron, fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                                IconButton(onClick = { onSpeak(w.english) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak")
                                }
                            }
                        }
                    }
                }
            } else {
                val phrases = remember(search, selectedCat) { db.getPhrases(selectedCat, search) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(phrases) { ph ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ph.tag, fontSize = 12.sp, color = Color.Gray)
                                    IconButton(
                                        onClick = { onSpeak(ph.english) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Speak")
                                    }
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

class DictDbHelper(context: Context) : SQLiteOpenHelper(context, "dict.db", null, 4) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE words (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, english TEXT, hindi TEXT, pron TEXT)")
        db.execSQL("CREATE TABLE phrases (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, english TEXT, hindi TEXT, tag TEXT)")

        val words = listOf(
            // === HUMAN BODY: EXTERNAL ORGANS ===
            arrayOf("External Organs", "Head", "सिर", "हेड"),
            arrayOf("External Organs", "Hair", "बाल", "हेयर"),
            arrayOf("External Organs", "Face", "चेहरा", "फेस"),
            arrayOf("External Organs", "Forehead", "माथा", "फ़ोरहेड"),
            arrayOf("External Organs", "Eye", "आँख", "आई"),
            arrayOf("External Organs", "Eyebrow", "भौंह", "आईब्रो"),
            arrayOf("External Organs", "Eyelash", "पलक", "आईलैश"),
            arrayOf("External Organs", "Ear", "कान", "इयर"),
            arrayOf("External Organs", "Nose", "नाक", "नोज़"),
            arrayOf("External Organs", "Nostril", "नथुना", "नॉस्ट्रिल"),
            arrayOf("External Organs", "Cheek", "गाल", "चीक"),
            arrayOf("External Organs", "Lips", "होंठ", "लिप्स"),
            arrayOf("External Organs", "Mouth", "मुँह", "माउथ"),
            arrayOf("External Organs", "Teeth", "दाँत", "टीथ"),
            arrayOf("External Organs", "Tongue", "जीभ", "टंग"),
            arrayOf("External Organs", "Chin", "ठोड़ी", "चिन"),
            arrayOf("External Organs", "Neck", "गर्दन", "नेक"),
            arrayOf("External Organs", "Throat", "गला", "थ्रोट"),
            arrayOf("External Organs", "Shoulder", "कंधा", "शोल्डर"),
            arrayOf("External Organs", "Chest", "छाती", "चेस्ट"),
            arrayOf("External Organs", "Back", "पीठ", "बैक"),
            arrayOf("External Organs", "Waist", "कमर", "वेस्ट"),
            arrayOf("External Organs", "Stomach", "पेट", "स्टमक"),
            arrayOf("External Organs", "Navel", "नाभि", "नेवल"),
            arrayOf("External Organs", "Arm", "बाँह", "आर्म"),
            arrayOf("External Organs", "Upper Arm", "ऊपरी बाँह", "अपर आर्म"),
            arrayOf("External Organs", "Elbow", "कोहनी", "एल्बो"),
            arrayOf("External Organs", "Forearm", "अग्रभुजा", "फोरआर्म"),
            arrayOf("External Organs", "Wrist", "कलाई", "रिस्ट"),
            arrayOf("External Organs", "Hand", "हाथ", "हैंड"),
            arrayOf("External Organs", "Palm", "हथेली", "पाम"),
            arrayOf("External Organs", "Thumb", "अंगूठा", "थंब"),
            arrayOf("External Organs", "Index Finger", "तर्जनी", "इंडेक्स फिंगर"),
            arrayOf("External Organs", "Middle Finger", "मध्यमा", "मिडिल फिंगर"),
            arrayOf("External Organs", "Ring Finger", "अनामिका", "रिंग फिंगर"),
            arrayOf("External Organs", "Little Finger", "कनिष्ठिका", "लिटिल फिंगर"),
            arrayOf("External Organs", "Fingernail", "हाथ का नाखून", "फिंगरनेल"),
            arrayOf("External Organs", "Leg", "टाँग", "लेग"),
            arrayOf("External Organs", "Thigh", "जांघ", "थाई"),
            arrayOf("External Organs", "Knee", "घुटना", "नी"),
            arrayOf("External Organs", "Kneecap", "घुटने की चक्की", "नीकैप"),
            arrayOf("External Organs", "Calf", "पिंडली", "काफ"),
            arrayOf("External Organs", "Ankle", "टखना", "एंकर"),
            arrayOf("External Organs", "Foot", "पैर", "फुट"),
            arrayOf("External Organs", "Heel", "एड़ी", "हील"),
            arrayOf("External Organs", "Sole", "तलवा", "सोल"),
            arrayOf("External Organs", "Toenail", "पैर का नाखून", "टोनेल"),
            arrayOf("External Organs", "Big Toe", "पैर का अंगूठा", "बिग टो"),
            arrayOf("External Organs", "Skin", "त्वचा", "स्किन"),

            // === HUMAN BODY: INTERNAL ORGANS ===
            arrayOf("Internal Organs", "Brain", "दिमाग", "ब्रेन"),
            arrayOf("Internal Organs", "Heart", "दिल", "हार्ट"),
            arrayOf("Internal Organs", "Lungs", "फेफड़े", "लंग्स"),
            arrayOf("Internal Organs", "Liver", "जिगर / यकृत", "लिवर"),
            arrayOf("Internal Organs", "Kidney", "गुर्दा", "किडनी"),
            arrayOf("Internal Organs", "Small Intestine", "छोटी आंत", "स्मॉल इंटेस्टाइन"),
            arrayOf("Internal Organs", "Large Intestine", "बड़ी आंत", "लार्ज इंटेस्टाइन"),
            arrayOf("Internal Organs", "Pancreas", "अग्न्याशय", "पैंक्रियाज"),
            arrayOf("Internal Organs", "Spleen", "तिल्ली", "स्प्लीन"),
            arrayOf("Internal Organs", "Gallbladder", "पित्ताशय", "गॉलब्लैडर"),
            arrayOf("Internal Organs", "Urinary Bladder", "मूत्राशय", "यूरिनरी ब्लैडर"),
            arrayOf("Internal Organs", "Food Pipe", "ग्रासनली", "फूड पाइप"),
            arrayOf("Internal Organs", "Windpipe", "श्वासनली", "विंडपाइप"),
            arrayOf("Internal Organs", "Appendix", "अपेंडिक्स", "अपेंडिक्स"),
            arrayOf("Internal Organs", "Bone", "हड्डी", "बोन"),
            arrayOf("Internal Organs", "Skull", "खोपड़ी", "स्कल"),
            arrayOf("Internal Organs", "Backbone", "रीढ़ की हड्डी", "बैकबोन"),
            arrayOf("Internal Organs", "Rib", "पसली", "रिब"),
            arrayOf("Internal Organs", "Muscle", "मांसपेशी", "मसल"),
            arrayOf("Internal Organs", "Joint", "जोड़", "जॉइंट"),
            arrayOf("Internal Organs", "Cartilage", "उपास्थि", "कार्टिलेज"),
            arrayOf("Internal Organs", "Blood", "खून / रक्त", "ब्लड"),
            arrayOf("Internal Organs", "Nerve", "नस / तंत्रिका", "नर्व"),
            arrayOf("Internal Organs", "Vein", "नस / शिरा", "वेन"),
            arrayOf("Internal Organs", "Artery", "धमनी", "आर्टरी"),
            arrayOf("Internal Organs", "Blood Vessel", "रक्त वाहिका", "ब्लड वेसल"),

            // === HUMAN BODY: REPRODUCTIVE ORGANS ===
            arrayOf("Reproductive Organs", "Reproductive System", "प्रजनन तंत्र", "रिप्रोडक्टिव सिस्टम"),
            arrayOf("Reproductive Organs", "Penis", "शिश्न", "पेनिस"),
            arrayOf("Reproductive Organs", "Testicles", "अंडकोष", "टेस्टिकल्स"),
            arrayOf("Reproductive Organs", "Scrotum", "अंडकोश की थैली", "स्क्रोटम"),
            arrayOf("Reproductive Organs", "Sperm", "शुक्राणु", "स्पर्म"),
            arrayOf("Reproductive Organs", "Vagina", "योनि", "वजाइना"),
            arrayOf("Reproductive Organs", "Uterus", "गर्भाशय", "यूटेरस"),
            arrayOf("Reproductive Organs", "Cervix", "गर्भाशय ग्रीवा", "सर्विक्स"),
            arrayOf("Reproductive Organs", "Ovary", "अंडाशय", "ओवरी"),
            arrayOf("Reproductive Organs", "Ovum", "अंडाणु", "ओवम"),
            arrayOf("Reproductive Organs", "Fallopian Tube", "डिंबवाहिनी नलिका", "फैलोपियन ट्यूब"),
            arrayOf("Reproductive Organs", "Breast", "स्तन", "ब्रेस्ट"),
            arrayOf("Reproductive Organs", "Nipple", "निप्पल", "निप्पल"),

            // === ANIMALS: WILD ANIMALS ===
            arrayOf("Wild Animals", "Lion", "शेर", "लायन"),
            arrayOf("Wild Animals", "Tiger", "बाघ", "टाइगर"),
            arrayOf("Wild Animals", "Elephant", "हाथी", "एलिफ़ेंट"),
            arrayOf("Wild Animals", "Leopard", "तेंदुआ", "लेपर्ड"),
            arrayOf("Wild Animals", "Cheetah", "चीता", "चीता"),
            arrayOf("Wild Animals", "Jaguar", "जगुआर", "जगुआर"),
            arrayOf("Wild Animals", "Bear", "भालू", "बेयर"),
            arrayOf("Wild Animals", "Polar Bear", "ध्रुवीय भालू", "पोलर बेयर"),
            arrayOf("Wild Animals", "Panda", "पांडा", "पांडा"),
            arrayOf("Wild Animals", "Giant Panda", "विशाल पांडा", "जायंट पांडा"),
            arrayOf("Wild Animals", "Red Panda", "लाल पांडा", "रेड पांडा"),
            arrayOf("Wild Animals", "Wolf", "भेड़िया", "वुल्फ"),
            arrayOf("Wild Animals", "Fox", "लोमड़ी", "फॉक्स"),
            arrayOf("Wild Animals", "Jackal", "सियार", "सियार"),
            arrayOf("Wild Animals", "Hyena", "लकड़बग्घा", "हाइना"),
            arrayOf("Wild Animals", "Deer", "हिरण", "डियर"),
            arrayOf("Wild Animals", "Reindeer", "रेनडियर", "रेनडियर"),
            arrayOf("Wild Animals", "Moose", "सांभर / मूस", "मूस"),
            arrayOf("Wild Animals", "Giraffe", "जिराफ़", "जिराफ़"),
            arrayOf("Wild Animals", "Zebra", "ज़ेबरा", "ज़ेबरा"),
            arrayOf("Wild Animals", "Wild Boar", "जंगली सूअर", "वाइल्ड बोर"),
            arrayOf("Wild Animals", "Monkey", "बंदर", "मंकी"),
            arrayOf("Wild Animals", "Gorilla", "गोरिल्ला", "गोरिल्ला"),
            arrayOf("Wild Animals", "Chimpanzee", "चिंपैंजी", "चिंपैंजी"),
            arrayOf("Wild Animals", "Orangutan", "वनमानुष", "ओरंगुटान"),
            arrayOf("Wild Animals", "Kangaroo", "कंगारू", "कंगारू"),
            arrayOf("Wild Animals", "Koala", "कोआला", "कोआला"),
            arrayOf("Wild Animals", "Hedgehog", "कांटेदार चूहा", "हेजहॉग"),
            arrayOf("Wild Animals", "Porcupine", "साही", "पोर्क्युपाइन"),
            arrayOf("Wild Animals", "Rhinoceros", "गैंडा", "राइनोसेरस"),
            arrayOf("Wild Animals", "Hippopotamus", "दरियाई घोड़ा", "हिप्पोपोटेमस"),
            arrayOf("Wild Animals", "Sloth", "स्लॉथ", "स्लॉथ"),
            arrayOf("Wild Animals", "Otter", "ऊदबिलाव", "ऑटर"),
            arrayOf("Wild Animals", "Beaver", "बीवर", "बीवर"),
            arrayOf("Wild Animals", "Mongoose", "नेवला", "मंगूज़"),
            arrayOf("Wild Animals", "Meerkat", "मीरकैट", "मीरकैट"),
            arrayOf("Wild Animals", "Pangolin", "पैंगोलिन", "पैंगोलिन"),
            arrayOf("Wild Animals", "Armadillo", "आर्माडिलो", "आर्माडिलो"),
            arrayOf("Wild Animals", "Antelope", "मृग", "एंटीलोप"),
            arrayOf("Wild Animals", "Bison", "जंगली भैंसा", "बाइसन"),
            arrayOf("Wild Animals", "Raccoon", "रैकून", "रैकून"),
            arrayOf("Wild Animals", "Skunk", "स्कंक", "स्कंक"),
            arrayOf("Wild Animals", "Lemur", "लीमर", "लीमर"),

            // === ANIMALS: DOMESTIC ANIMALS ===
            arrayOf("Domestic Animals", "Cow", "गाय", "काऊ"),
            arrayOf("Domestic Animals", "Buffalo", "भैंस", "बफ़ेलो"),
            arrayOf("Domestic Animals", "Bull", "सांड", "बुल"),
            arrayOf("Domestic Animals", "Ox", "बैल", "ऑक्स"),
            arrayOf("Domestic Animals", "Goat", "बकरी", "गोट"),
            arrayOf("Domestic Animals", "Sheep", "भेड़", "शीप"),
            arrayOf("Domestic Animals", "Horse", "घोड़ा", "हॉर्स"),
            arrayOf("Domestic Animals", "Donkey", "गधा", "डंकी"),
            arrayOf("Domestic Animals", "Camel", "ऊँट", "कैमल"),
            arrayOf("Domestic Animals", "Pig", "सूअर", "पिग"),
            arrayOf("Domestic Animals", "Rabbit", "खरगोश", "रैबिट"),
            arrayOf("Domestic Animals", "Hare", "खरगोश", "हेयर"),
            arrayOf("Domestic Animals", "Dog", "कुत्ता", "डॉग"),
            arrayOf("Domestic Animals", "Puppy", "पिल्ला", "पपी"),
            arrayOf("Domestic Animals", "Cat", "बिल्ली", "कैट"),
            arrayOf("Domestic Animals", "Kitten", "बिल्ली का बच्चा", "किटन"),
            arrayOf("Domestic Animals", "Yak", "याक", "याक"),
            arrayOf("Domestic Animals", "Llama", "लामा", "लामा"),
            arrayOf("Domestic Animals", "Alpaca", "अल्पाका", "अल्पाका"),
            arrayOf("Domestic Animals", "Squirrel", "गिलहरी", "स्क्विरल"),
            arrayOf("Domestic Animals", "Rat", "चूहा", "रैट"),
            arrayOf("Domestic Animals", "Mouse", "छोटा चूहा", "माउस"),

            // === ANIMALS: BIRDS ===
            arrayOf("Birds", "Eagle", "चील / बाज़", "ईगल"),
            arrayOf("Birds", "Parrot", "तोता", "पैरट"),
            arrayOf("Birds", "Peacock", "मोर", "पीकॉक"),
            arrayOf("Birds", "Pigeon", "कबूतर", "पिजन"),
            arrayOf("Birds", "Dove", "फाख्ता", "डव"),
            arrayOf("Birds", "Crow", "कौआ", "क्रो"),
            arrayOf("Birds", "Sparrow", "गौरैया", "स्पैरो"),
            arrayOf("Birds", "Owl", "उल्लू", "आउल"),
            arrayOf("Birds", "Penguin", "पेंगुइन", "पेंगुइन"),
            arrayOf("Birds", "Flamingo", "राजहंस", "फ्लेमिंगो"),
            arrayOf("Birds", "Swan", "हंस", "स्वान"),
            arrayOf("Birds", "Duck", "बतख", "डक"),
            arrayOf("Birds", "Goose", "कलहंस", "गूज़"),
            arrayOf("Birds", "Hen", "मुर्गी", "हेन"),
            arrayOf("Birds", "Rooster", "मुर्गा", "रूस्टर"),
            arrayOf("Birds", "Turkey", "टर्की", "टर्की"),
            arrayOf("Birds", "Ostrich", "शुतुरमुर्ग", "ऑस्ट्रिच"),
            arrayOf("Birds", "Emu", "एमू", "एमू"),
            arrayOf("Birds", "Kingfisher", "रामचिरैया", "किंगफिशर"),
            arrayOf("Birds", "Woodpecker", "कठफोड़वा", "वुडपेकर"),
            arrayOf("Birds", "Hummingbird", "गुंजन पक्षी", "हमिंगबर्ड"),
            arrayOf("Birds", "Vulture", "गिद्ध", "वल्चर"),
            arrayOf("Birds", "Falcon", "बाज़", "फाल्कन"),
            arrayOf("Birds", "Hawk", "बाज़", "हॉक"),
            arrayOf("Birds", "Crane", "सारस", "क्रेन"),
            arrayOf("Birds", "Heron", "बगुला", "हेरॉन"),
            arrayOf("Birds", "Stork", "सारस / ढोक", "स्टॉर्क"),
            arrayOf("Birds", "Seagull", "गंगा-चिल्ली", "सीगल"),
            arrayOf("Birds", "Albatross", "अल्बाट्रॉस", "अल्बाट्रॉस"),
            arrayOf("Birds", "Cuckoo", "कोयल", "कुक्कू"),
            arrayOf("Birds", "Mynah", "मैना", "मैना"),
            arrayOf("Birds", "Hornbill", "धनेश", "हॉर्नबिल"),
            arrayOf("Birds", "Quail", "बटेर", "क्वेल"),
            arrayOf("Birds", "Partridge", "तीतर", "पार्ट्रिज"),
            arrayOf("Birds", "Pelican", "हवासील", "पेलिकन"),
            arrayOf("Birds", "Pheasant", "तीतर पक्षी", "फीज़ेंट"),

            // === ANIMALS: INSECTS & CREEPY CRAWLIES ===
            arrayOf("Insects", "Butterfly", "तितली", "बटरफ्लाई"),
            arrayOf("Insects", "Moth", "पतंगा", "मॉथ"),
            arrayOf("Insects", "Honeybee", "मधुमक्खी", "हनीबी"),
            arrayOf("Insects", "Ant", "चींटी", "आंट"),
            arrayOf("Insects", "Mosquito", "मच्छर", "मॉस्किटो"),
            arrayOf("Insects", "Housefly", "मक्खी", "हाउसफ्लाई"),
            arrayOf("Insects", "Dragonfly", "व्याध पतंग", "ड्रैगनफ्लाई"),
            arrayOf("Insects", "Grasshopper", "टिड्डा", "ग्रासहॉपर"),
            arrayOf("Insects", "Cricket", "झींगुर", "क्रिकेट"),
            arrayOf("Insects", "Cockroach", "तिलचट्टा", "कॉकरोच"),
            arrayOf("Insects", "Termite", "दीमक", "टरमाइट"),
            arrayOf("Insects", "Beetle", "भृंग / गुबरैला", "बीटल"),
            arrayOf("Insects", "Ladybug", "सोनपंखी", "लेडीबग"),
            arrayOf("Insects", "Spider", "मकड़ी", "स्पाइडर"),
            arrayOf("Insects", "Scorpion", "बिच्छू", "स्कॉर्पियन"),
            arrayOf("Insects", "Centipede", "कनखजूरा", "सेंटिपीड़"),
            arrayOf("Insects", "Millipede", "सहस्रपाद", "मिलीपीड़"),
            arrayOf("Insects", "Earthworm", "केंचुआ", "अर्थवर्म"),
            arrayOf("Insects", "Snail", "घोंघा", "स्नेल"),
            arrayOf("Insects", "Slug", "स्लग", "स्लग"),
            arrayOf("Insects", "Leech", "जोंक", "लीच"),

            // === ANIMALS: REPTILES & AMPHIBIANS ===
            arrayOf("Reptiles & Amphibians", "Crocodile", "मगरमच्छ", "क्रोकोडाइल"),
            arrayOf("Reptiles & Amphibians", "Alligator", "घड़ियाल", "एलिगेटर"),
            arrayOf("Reptiles & Amphibians", "Turtle", "कछुआ (जल)", "टर्टल"),
            arrayOf("Reptiles & Amphibians", "Tortoise", "कछुआ (स्थल)", "टॉर्टॉइज़"),
            arrayOf("Reptiles & Amphibians", "Snake", "साँप", "स्नेक"),
            arrayOf("Reptiles & Amphibians", "Lizard", "छिपकली", "लिज़र्ड"),
            arrayOf("Reptiles & Amphibians", "Chameleon", "गिरगिट", "कैमेलियन"),
            arrayOf("Reptiles & Amphibians", "Iguana", "इगुआना", "इगुआना"),
            arrayOf("Reptiles & Amphibians", "Frog", "मेंढक", "फ्रॉग"),
            arrayOf("Reptiles & Amphibians", "Toad", "दादुर / मेंढक", "टोड"),
            arrayOf("Reptiles & Amphibians", "Salamander", "सैलामैंडर", "सैलामैंडर"),
            arrayOf("Reptiles & Amphibians", "Newt", "न्यूट", "न्यूट"),
            arrayOf("Reptiles & Amphibians", "Bat", "चमगादड़", "बैट"),

            // === ANIMALS: SEA ANIMALS ===
            arrayOf("Sea Animals", "Dolphin", "डॉलफिन", "डॉलफिन"),
            arrayOf("Sea Animals", "Whale", "व्हेल", "व्हेल"),
            arrayOf("Sea Animals", "Shark", "शार्क", "शार्क"),
            arrayOf("Sea Animals", "Octopus", "अष्टबाहु", "ऑक्टोपस"),
            arrayOf("Sea Animals", "Squid", "स्क्वीड", "स्क्वीड"),
            arrayOf("Sea Animals", "Seal", "सील", "सील"),
            arrayOf("Sea Animals", "Sea Lion", "जल सिंह", "सी लायन"),
            arrayOf("Sea Animals", "Walrus", "वॉलरस", "वॉलरस"),
            arrayOf("Sea Animals", "Jellyfish", "जेलीफ़िश", "जेलीफ़िश"),
            arrayOf("Sea Animals", "Starfish", "तारा मछली", "स्टारफ़िश"),
            arrayOf("Sea Animals", "Seahorse", "समुद्री घोड़ा", "सीहॉर्स"),
            arrayOf("Sea Animals", "Crab", "केकड़ा", "क्रैब"),
            arrayOf("Sea Animals", "Lobster", "झींगा मछली", "लॉबस्टर"),
            arrayOf("Sea Animals", "Shrimp", "झींगा", "श्रिम्प"),
            arrayOf("Sea Animals", "Prawn", "झींगा", "प्रॉन"),
            arrayOf("Sea Animals", "Clam", "सीपी", "क्लैम"),
            arrayOf("Sea Animals", "Oyster", "सीप", "ऑयस्टर"),
            arrayOf("Sea Animals", "Mussel", "कस्तूरी सीपी", "मसल"),
            arrayOf("Sea Animals", "Coral", "मूंगा", "कोरल"),
            arrayOf("Sea Animals", "Sea Urchin", "जलसाही", "सी अर्चिन"),
            arrayOf("Sea Animals", "Sea Cucumber", "समुद्री खीरा", "सी कुकम्बर"),

            // === VEGETABLES: DAILY VEGGIES ===
            arrayOf("Daily Veggies", "Potato", "आलू", "पोटैटो"),
            arrayOf("Daily Veggies", "Tomato", "टमाटर", "टोमैटो"),
            arrayOf("Daily Veggies", "Onion", "प्याज़", "अनियन"),
            arrayOf("Daily Veggies", "Garlic", "लहसुन", "गार्लिक"),
            arrayOf("Daily Veggies", "Ginger", "अदरक", "जिंजर"),
            arrayOf("Daily Veggies", "Brinjal", "बैंगन", "ब्रिंजल"),
            arrayOf("Daily Veggies", "Cauliflower", "फूलगोभी", "कॉलीफ़्लॉवर"),
            arrayOf("Daily Veggies", "Cabbage", "पत्तागोभी", "कैबेज"),
            arrayOf("Daily Veggies", "Broccoli", "हरी गोभी", "ब्रोकोली"),
            arrayOf("Daily Veggies", "Ladyfinger", "भिंडी", "लेडीफिंगर"),
            arrayOf("Daily Veggies", "Green Peas", "मटर", "ग्रीन पीस"),
            arrayOf("Daily Veggies", "Green Beans", "हरी फलियाँ", "ग्रीन बीन्स"),
            arrayOf("Daily Veggies", "French Beans", "फ्रेंच बीन्स", "फ्रेंच बीन्स"),
            arrayOf("Daily Veggies", "Cluster Beans", "ग्वारफली", "क्लस्टर बीन्स"),
            arrayOf("Daily Veggies", "Broad Beans", "बाकला फलियाँ", "ब्रॉड बीन्स"),
            arrayOf("Daily Veggies", "Capsicum", "शिमला मिर्च", "कैप्सिकम"),
            arrayOf("Daily Veggies", "Green Chilli", "हरी मिर्च", "ग्रीन चिली"),
            arrayOf("Daily Veggies", "Red Chilli", "लाल मिर्च", "रेड चिली"),
            arrayOf("Daily Veggies", "Mushroom", "खुम्बी / मशरूम", "मशरूम"),
            arrayOf("Daily Veggies", "Sweet Corn", "भुट्टा / मक्का", "स्वीट कॉर्न"),
            arrayOf("Daily Veggies", "Drumstick", "सहजन", "ड्रमस्टिक"),
            arrayOf("Daily Veggies", "Raw Banana", "कच्चा केला", "रॉ बनाना"),
            arrayOf("Daily Veggies", "Raw Papaya", "कच्चा पपीता", "रॉ पपाया"),
            arrayOf("Daily Veggies", "Jackfruit", "कटहल", "कटहल"),
            arrayOf("Daily Veggies", "Brussels Sprouts", "छोटी बंदगोभी", "ब्रुसेल्स स्प्राउट्स"),
            arrayOf("Daily Veggies", "Zucchini", "तोरी प्रजाति", "ज़ुकिनी"),
            arrayOf("Daily Veggies", "Artichoke", "हाथी चक", "आर्टिचोक"),

            // === VEGETABLES: LEAFY GREENS ===
            arrayOf("Leafy Greens", "Spinach", "पालक", "स्पिनच"),
            arrayOf("Leafy Greens", "Fenugreek", "मेथी", "फेनुग्रीक"),
            arrayOf("Leafy Greens", "Mustard Greens", "सरसों का साग", "मस्टर्ड ग्रीन्स"),
            arrayOf("Leafy Greens", "Coriander", "धनिया", "कोरिएंडर"),
            arrayOf("Leafy Greens", "Mint", "पुदीना", "मिंट"),
            arrayOf("Leafy Greens", "Lettuce", "सलाद पत्ता", "लेट्यूस"),
            arrayOf("Leafy Greens", "Radish Leaves", "मूली के पत्ते", "रैडिश लीव्स"),
            arrayOf("Leafy Greens", "Spring Onion", "हरा प्याज़", "स्प्रिंग अनियन"),
            arrayOf("Leafy Greens", "Celery", "अजमोदा", "सेलरी"),
            arrayOf("Leafy Greens", "Kale", "केल पत्ता", "केल"),
            arrayOf("Leafy Greens", "Leek", "हरा लहसुन", "लीक"),
            arrayOf("Leafy Greens", "Turnip Greens", "शलजम के पत्ते", "शलगम ग्रीन्स"),

            // === VEGETABLES: GOURDS ===
            arrayOf("Gourds", "Cucumber", "खीरा", "कुकम्बर"),
            arrayOf("Gourds", "Bitter Gourd", "करेला", "बिटर गॉर्ड"),
            arrayOf("Gourds", "Bottle Gourd", "लौकी", "बॉटल गॉर्ड"),
            arrayOf("Gourds", "Ridge Gourd", "तोरई", "रिज गॉर्ड"),
            arrayOf("Gourds", "Sponge Gourd", "नेनुआ / गिलकी", "स्पंज गॉर्ड"),
            arrayOf("Gourds", "Snake Gourd", "चिचिंडा", "स्नेक गॉर्ड"),
            arrayOf("Gourds", "Ash Gourd", "पेठा", "ऐश गॉर्ड"),
            arrayOf("Gourds", "Pumpkin", "कद्दू", "पम्पकिन"),
            arrayOf("Gourds", "Round Gourd", "टिंडा", "राउंड गॉर्ड"),
            arrayOf("Gourds", "Pointed Gourd", "परवल", "पॉइंटेड गॉर्ड"),
            arrayOf("Gourds", "Ivy Gourd", "कुंदरू", "आइवी गॉर्ड"),

            // === VEGETABLES: ROOTS & TUBERS ===
            arrayOf("Roots & Tubers", "Carrot", "गाजर", "कैरट"),
            arrayOf("Roots & Tubers", "Radish", "मूली", "रैडिश"),
            arrayOf("Roots & Tubers", "Turnip", "शलजम", "टर्निश"),
            arrayOf("Roots & Tubers", "Beetroot", "चुकंदर", "बीटरूट"),
            arrayOf("Roots & Tubers", "Sweet Potato", "शकरकंद", "स्वीट पोटैटो"),
            arrayOf("Roots & Tubers", "Colocasia", "अरबी", "कोलोकेसिया"),
            arrayOf("Roots & Tubers", "Taro Root", "अरबी कंद", "टारो रूट"),
            arrayOf("Roots & Tubers", "Lotus Stem", "कमल ककड़ी", "लोटस स्टेम"),
            arrayOf("Roots & Tubers", "Water Chestnut", "सिंघाड़ा", "वाटर चेस्टनट"),
            arrayOf("Roots & Tubers", "Yam", "जिमीकंद / सूरन", "याम"),
            arrayOf("Roots & Tubers", "Parsnip", "सफेद गाजर", "पार्सनिप"),
            arrayOf("Roots & Tubers", "Asparagus", "शतावरी", "एस्परैगस"),
            arrayOf("Roots & Tubers", "Fennel", "सौंफ की जड़", "फेनल"),

            // === PLANTS: TREES ===
            arrayOf("Trees", "Tree", "पेड़ / वृक्ष", "ट्री"),
            arrayOf("Trees", "Banyan", "बरगद", "बरगद"),
            arrayOf("Trees", "Peepal", "पीपल", "पीपल"),
            arrayOf("Trees", "Neem", "नीम", "नीम"),
            arrayOf("Trees", "Mango Tree", "आम का पेड़", "मैंगो ट्री"),
            arrayOf("Trees", "Coconut Tree", "नारियल का पेड़", "कोकोनट ट्री"),
            arrayOf("Trees", "Palm Tree", "ताड़ का पेड़", "पाम ट्री"),
            arrayOf("Trees", "Ashoka Tree", "अशोक का पेड़", "अशोका ट्री"),
            arrayOf("Trees", "Eucalyptus", "नीलगिरी", "यूकेलिप्टस"),
            arrayOf("Trees", "Teak", "सागौन", "टीक"),
            arrayOf("Trees", "Sandalwood", "चंदन", "सैंडलवुड"),
            arrayOf("Trees", "Lemon Tree", "नींबू का पेड़", "लेमन ट्री"),
            arrayOf("Trees", "Guava Tree", "अमरूद का पेड़", "ग्वावा ट्री"),
            arrayOf("Trees", "Apple Tree", "सेब का पेड़", "एप्पल ट्री"),
            arrayOf("Trees", "Orange Tree", "संतरे का पेड़", "ऑरेंज ट्री"),
            arrayOf("Trees", "Papaya Tree", "पपीते का पेड़", "पपाया ट्री"),
            arrayOf("Trees", "Pomegranate Tree", "अनार का पेड़", "पोमेग्रेनेट ट्री"),
            arrayOf("Trees", "Pine Tree", "चीड़ का पेड़", "पाइन ट्री"),
            arrayOf("Trees", "Cedar Tree", "देवदार", "सीडर ट्री"),
            arrayOf("Trees", "Oak Tree", "बलूत का पेड़", "ओक ट्री"),
            arrayOf("Trees", "Maple Tree", "मैपल का पेड़", "मैपल ट्री"),
            arrayOf("Trees", "Willow Tree", "बेंत / विलो", "विलो ट्री"),
            arrayOf("Trees", "Birch Tree", "भोजपत्र", "बर्च ट्री"),
            arrayOf("Trees", "Cherry Blossom", "चेरी ब्लॉसम", "चेरी ब्लॉसम"),

            // === PLANTS: FLOWERING PLANTS ===
            arrayOf("Flowering Plants", "Rose Plant", "गुलाब का पौधा", "रोज़ प्लांट"),
            arrayOf("Flowering Plants", "Jasmine", "चमेली", "जैस्मीन"),
            arrayOf("Flowering Plants", "Hibiscus", "गुड़हल", "हिबिस्कस"),
            arrayOf("Flowering Plants", "Sunflower", "सूरजमुखी", "सनफ्लावर"),
            arrayOf("Flowering Plants", "Lotus", "कमल", "लोटस"),
            arrayOf("Flowering Plants", "Water Lily", "कुमुदिनी", "वाटर लिली"),
            arrayOf("Flowering Plants", "Marigold", "गेंदा", "मैरीगोल्ड"),
            arrayOf("Flowering Plants", "Lily", "कुमुद / लिली", "लिली"),
            arrayOf("Flowering Plants", "Daisy", "गुलबहार", "डेज़ी"),
            arrayOf("Flowering Plants", "Daffodil", "नरगिस", "डैफ़ोडिल"),
            arrayOf("Flowering Plants", "Tulip", "कंदपुष्प", "ट्यूलिप"),
            arrayOf("Flowering Plants", "Orchid", "ऑर्किड", "ऑर्किड"),
            arrayOf("Flowering Plants", "Lavender", "लैवेंडर", "लैवेंडर"),
            arrayOf("Flowering Plants", "Chrysanthemum", "गुलदाउदी", "गुलदाउदी"),
            arrayOf("Flowering Plants", "Bougainvillea", "बोगनवेलिया", "बोगनवेलिया"),
            arrayOf("Flowering Plants", "Periwinkle", "सदाबहार", "पेरिविंकल"),
            arrayOf("Flowering Plants", "Morning Glory", "महिमा फूल", "मॉर्निंग ग्लोरी"),
            arrayOf("Flowering Plants", "Night Jasmine", "पारिजात / हरसिंगार", "नाइट जैस्मीन"),

            // === PLANTS: MEDICINAL & HERBS ===
            arrayOf("Medicinal & Herbs", "Plant", "पौधा", "प्लांट"),
            arrayOf("Medicinal & Herbs", "Shrub", "झाड़ी", "शर्ब"),
            arrayOf("Medicinal & Herbs", "Herb", "जड़ी-बूटी", "हर्ब"),
            arrayOf("Medicinal & Herbs", "Tulsi", "तुलसी", "तुलसी"),
            arrayOf("Medicinal & Herbs", "Aloe Vera", "घृतकुमारी / एलोवेरा", "एलोवेरा"),
            arrayOf("Medicinal & Herbs", "Money Plant", "मनी प्लांट", "मनी प्लांट"),
            arrayOf("Medicinal & Herbs", "Snake Plant", "स्नेक प्लांट", "स्नेक प्लांट"),
            arrayOf("Medicinal & Herbs", "Curry Leaf Plant", "कढ़ी पत्ता", "करी लीफ़ प्लांट"),
            arrayOf("Medicinal & Herbs", "Basil", "तुलसी प्रजाति", "बेसिल"),
            arrayOf("Medicinal & Herbs", "Mint Plant", "पुदीने का पौधा", "मिंट प्लांट"),
            arrayOf("Medicinal & Herbs", "Coriander Plant", "धनिया का पौधा", "कोरिएंडर प्लांट"),
            arrayOf("Medicinal & Herbs", "Fern", "पर्णांग / फ़र्न", "फ़र्न"),
            arrayOf("Medicinal & Herbs", "Moss", "काई / मॉस", "मॉस"),

            // === PLANTS: CROPS & CLIMBERS ===
            arrayOf("Crops & Climbers", "Climber", "आरोही लता", "क्लाइंबर"),
            arrayOf("Crops & Climbers", "Creeper", "लता / बेल", "क्रीपर"),
            arrayOf("Crops & Climbers", "Cactus", "नागफनी", "कैक्टस"),
            arrayOf("Crops & Climbers", "Bamboo", "बांस", "बैम्बू"),
            arrayOf("Crops & Climbers", "Banana Plant", "केले का पौधा", "बनाना प्लांट"),
            arrayOf("Crops & Climbers", "Grapevine", "अंगूर की बेल", "ग्रेपवाइन"),
            arrayOf("Crops & Climbers", "Wheat Plant", "गेहूं का पौधा", "व्हीट प्लांट"),
            arrayOf("Crops & Climbers", "Rice Plant", "धान का पौधा", "राइस प्लांट"),
            arrayOf("Crops & Climbers", "Corn Plant", "मक्के का पौधा", "कॉर्न प्लांट"),
            arrayOf("Crops & Climbers", "Cotton Plant", "कपास का पौधा", "कॉटन प्लांट"),
            arrayOf("Crops & Climbers", "Sugarcane", "गन्ना", "शुगरकेन"),
            arrayOf("Crops & Climbers", "Tea Plant", "चाय का पौधा", "टी प्लांट"),
            arrayOf("Crops & Climbers", "Coffee Plant", "कॉफी का पौधा", "कॉफ़ी प्लांट"),
            arrayOf("Crops & Climbers", "Jute Plant", "पटसन", "जूट प्लांट"),
            arrayOf("Crops & Climbers", "Pea Plant", "मटर का पौधा", "पी प्लांट"),
            arrayOf("Crops & Climbers", "Mustard Plant", "सरसों का पौधा", "मस्टर्ड प्लांट"),
            arrayOf("Crops & Climbers", "Mustard Greens Plant", "राई का पौधा", "मस्टर्ड ग्रीन्स प्लांट")
        )

        for (w in words) {
            val cv = ContentValues().apply {
                put("category", w[0])
                put("english", w[1])
                put("hindi", w[2])
                put("pron", w[3])
            }
            db.insert("words", null, cv)
        }

        val phrases = listOf(
            arrayOf("Home", "Wake up, it's getting late.", "उठ जाओ, देर हो रही है।", "Morning"),
            arrayOf("Home", "Breakfast is ready.", "नाश्ता तैयार है।", "Meal"),
            arrayOf("Hotel", "A table for two, please.", "दो लोगों के लिए टेबल चाहिए।", "Entry"),
            arrayOf("Hotel", "Could we have the menu?", "क्या मेन्यू कार्ड मिल सकता है?", "Ordering"),
            arrayOf("Market", "How much is this for?", "यह कितने का है?", "Price"),
            arrayOf("Market", "Give some discount.", "कुछ कम करो भाई!", "Bargaining"),
            arrayOf("Travel", "Which platform will the train arrive?", "ट्रेन किस प्लेटफॉर्म पर आएगी?", "Station")
        )

        for (p in phrases) {
            val cv = ContentValues().apply {
                put("category", p[0])
                put("english", p[1])
                put("hindi", p[2])
                put("tag", p[3])
            }
            db.insert("phrases", null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS words")
        db.execSQL("DROP TABLE IF EXISTS phrases")
        onCreate(db)
    }

    fun getWords(category: String, query: String): List<WordItem> {
        val list = mutableListOf<WordItem>()
        val db = readableDatabase
        val cursor = if (query.isNotBlank()) {
            db.rawQuery("SELECT id, category, english, hindi, pron FROM words WHERE english LIKE ? OR hindi LIKE ?", arrayOf("%$query%", "%$query%"))
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
            db.rawQuery("SELECT id, category, english, hindi, tag FROM phrases WHERE english LIKE ? OR hindi LIKE ?", arrayOf("%$query%", "%$query%"))
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
