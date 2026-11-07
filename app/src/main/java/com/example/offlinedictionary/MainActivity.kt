package com.example.offlinedictionary

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import kotlinx.coroutines.launch
import java.util.Locale

data class WordItem(val id: Int, val mainCat: String, val subCat: String, val english: String, val hindi: String, val pron: String)
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
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemDark) }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DictionaryApp(
                        db = db,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { isDarkMode = !isDarkMode },
                        onSpeak = { text ->
                            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    )
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
fun DictionaryApp(
    db: DictDbHelper,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentView by remember { mutableStateOf("Words") } // "Words", "Phrases", "About"
    var search by remember { mutableStateOf("") }
    var selectedWordCat by remember { mutableStateOf("All") }
    var selectedPhraseCat by remember { mutableStateOf("All") }

    // Drawer state for expanding categories
    var isVocabExpanded by remember { mutableStateOf(true) }

    val mainCategories = listOf("All", "Human Body", "Animals", "Plants", "Vegetables")
    val subCategoriesMap = mapOf(
        "Human Body" to listOf("External Organs", "Internal Organs", "Reproductive Organs"),
        "Animals" to listOf("Wild Animals", "Domestic Animals", "Birds", "Insects", "Reptiles & Amphibians", "Sea Animals"),
        "Plants" to listOf("Trees", "Flowering Plants", "Medicinal & Herbs", "Crops & Climbers"),
        "Vegetables" to listOf("Daily Veggies", "Leafy Greens", "Gourds", "Roots & Tubers")
    )
    val phraseCategories = listOf("All", "Home", "Hotel", "Market", "Travel")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Offline Dictionary",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        HorizontalDivider()

                        // Collapsible Header for Words & Vocab
                        ListItem(
                            headlineContent = { Text("Words & Vocab", fontWeight = FontWeight.Bold) },
                            leadingContent = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                            trailingContent = {
                                Icon(
                                    imageVector = if (isVocabExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { isVocabExpanded = !isVocabExpanded }
                        )
                    }

                    // Expandable Sub-Categories List
                    if (isVocabExpanded) {
                        item {
                            NavigationDrawerItem(
                                label = { Text("All Words") },
                                selected = currentView == "Words" && selectedWordCat == "All",
                                onClick = {
                                    currentView = "Words"
                                    selectedWordCat = "All"
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(start = 28.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
                            )
                        }

                        subCategoriesMap.forEach { (mainCategory, subList) ->
                            item {
                                Text(
                                    text = mainCategory.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 32.dp, top = 10.dp, bottom = 4.dp)
                                )
                            }
                            items(subList) { subCat ->
                                NavigationDrawerItem(
                                    label = { Text(subCat, fontSize = 14.sp) },
                                    selected = currentView == "Words" && selectedWordCat == subCat,
                                    onClick = {
                                        currentView = "Words"
                                        selectedWordCat = subCat
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(start = 36.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        NavigationDrawerItem(
                            label = { Text("Phrases & Sentences") },
                            icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) },
                            selected = currentView == "Phrases",
                            onClick = {
                                currentView = "Phrases"
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("About Developer") },
                            icon = { Icon(Icons.Default.Info, contentDescription = null) },
                            selected = currentView == "About",
                            onClick = {
                                currentView = "About"
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Dark Mode", fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentView) {
                                "Words" -> if (selectedWordCat == "All") "Vocabulary" else selectedWordCat
                                "Phrases" -> "Phrases"
                                else -> "About"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { p ->
            Column(modifier = Modifier.padding(p).fillMaxSize()) {
                when (currentView) {
                    "Words" -> {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            placeholder = { Text("Search English or Hindi...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Main Category Filter Chips on Main Screen
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mainCategories) { cat ->
                                FilterChip(
                                    selected = selectedWordCat == cat,
                                    onClick = { selectedWordCat = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val words = remember(search, selectedWordCat) {
                            db.getWords(selectedWordCat, search)
                        }

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
                                            Text(
                                                text = w.english,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (w.pron.isNotBlank()) {
                                                Text(
                                                    text = w.pron,
                                                    fontSize = 13.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        // Hindi meaning directly in front
                                        Text(
                                            text = w.hindi,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        IconButton(onClick = { onSpeak(w.english) }) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Phrases" -> {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            placeholder = { Text("Search phrases...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        LazyRow(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(phraseCategories) { cat ->
                                FilterChip(
                                    selected = selectedPhraseCat == cat,
                                    onClick = { selectedPhraseCat = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val phrases = remember(search, selectedPhraseCat) {
                            db.getPhrases(selectedPhraseCat, search)
                        }

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
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(ph.hindi, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    "About" -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Offline Dictionary App",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Developer: Vijay Tanwar",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "A complete offline English to Hindi learning dictionary featuring structured vocabulary categories, instant sub-category navigation drawer, real-time search, voice pronunciation, conversation phrases, and dark mode support.",
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class DictDbHelper(context: Context) : SQLiteOpenHelper(context, "dict.db", null, 6) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE words (id INTEGER PRIMARY KEY AUTOINCREMENT, main_cat TEXT, sub_cat TEXT, english TEXT, hindi TEXT, pron TEXT)")
        db.execSQL("CREATE TABLE phrases (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, english TEXT, hindi TEXT, tag TEXT)")

        val words = listOf(
            // === HUMAN BODY ===
            arrayOf("Human Body", "External Organs", "Head", "सिर", "हेड"),
            arrayOf("Human Body", "External Organs", "Hair", "बाल", "हेयर"),
            arrayOf("Human Body", "External Organs", "Face", "चेहरा", "फेस"),
            arrayOf("Human Body", "External Organs", "Forehead", "माथा", "फ़ोरहेड"),
            arrayOf("Human Body", "External Organs", "Eye", "आँख", "आई"),
            arrayOf("Human Body", "External Organs", "Eyebrow", "भौंह", "आईब्रो"),
            arrayOf("Human Body", "External Organs", "Eyelash", "पलक", "आईलैश"),
            arrayOf("Human Body", "External Organs", "Ear", "कान", "इयर"),
            arrayOf("Human Body", "External Organs", "Nose", "नाक", "नोज़"),
            arrayOf("Human Body", "External Organs", "Nostril", "नथुना", "नॉस्ट्रिल"),
            arrayOf("Human Body", "External Organs", "Cheek", "गाल", "चीक"),
            arrayOf("Human Body", "External Organs", "Lips", "होंठ", "लिप्स"),
            arrayOf("Human Body", "External Organs", "Mouth", "मुँह", "माउथ"),
            arrayOf("Human Body", "External Organs", "Teeth", "दाँत", "टीथ"),
            arrayOf("Human Body", "External Organs", "Tongue", "जीभ", "टंग"),
            arrayOf("Human Body", "External Organs", "Chin", "ठोड़ी", "चिन"),
            arrayOf("Human Body", "External Organs", "Neck", "गर्दन", "नेक"),
            arrayOf("Human Body", "External Organs", "Throat", "गला", "थ्रोट"),
            arrayOf("Human Body", "External Organs", "Shoulder", "कंधा", "शोल्डर"),
            arrayOf("Human Body", "External Organs", "Chest", "छाती", "चेस्ट"),
            arrayOf("Human Body", "External Organs", "Back", "पीठ", "बैक"),
            arrayOf("Human Body", "External Organs", "Waist", "कमर", "वेस्ट"),
            arrayOf("Human Body", "External Organs", "Stomach", "पेट", "स्टमक"),
            arrayOf("Human Body", "External Organs", "Navel", "नाभि", "नेवल"),
            arrayOf("Human Body", "External Organs", "Arm", "बाँह", "आर्म"),
            arrayOf("Human Body", "External Organs", "Upper Arm", "ऊपरी बाँह", "अपर आर्म"),
            arrayOf("Human Body", "External Organs", "Elbow", "कोहनी", "एल्बो"),
            arrayOf("Human Body", "External Organs", "Forearm", "अग्रभुजा", "फोरआर्म"),
            arrayOf("Human Body", "External Organs", "Wrist", "कलाई", "रिस्ट"),
            arrayOf("Human Body", "External Organs", "Hand", "हाथ", "हैंड"),
            arrayOf("Human Body", "External Organs", "Palm", "हथेली", "पाम"),
            arrayOf("Human Body", "External Organs", "Thumb", "अंगूठा", "थंब"),
            arrayOf("Human Body", "External Organs", "Index Finger", "तर्जनी", "इंडेक्स फिंगर"),
            arrayOf("Human Body", "External Organs", "Middle Finger", "मध्यमा", "मिडिल फिंगर"),
            arrayOf("Human Body", "External Organs", "Ring Finger", "अनामिका", "रिंग फिंगर"),
            arrayOf("Human Body", "External Organs", "Little Finger", "कनिष्ठिका", "लिटिल फिंगर"),
            arrayOf("Human Body", "External Organs", "Fingernail", "हाथ का नाखून", "फिंगरनेल"),
            arrayOf("Human Body", "External Organs", "Leg", "टाँग", "लेग"),
            arrayOf("Human Body", "External Organs", "Thigh", "जांघ", "थाई"),
            arrayOf("Human Body", "External Organs", "Knee", "घुटना", "नी"),
            arrayOf("Human Body", "External Organs", "Kneecap", "घुटने की चक्की", "नीकैप"),
            arrayOf("Human Body", "External Organs", "Calf", "पिंडली", "काफ"),
            arrayOf("Human Body", "External Organs", "Ankle", "टखना", "एंकर"),
            arrayOf("Human Body", "External Organs", "Foot", "पैर", "फुट"),
            arrayOf("Human Body", "External Organs", "Heel", "एड़ी", "हील"),
            arrayOf("Human Body", "External Organs", "Sole", "तलवा", "सोल"),
            arrayOf("Human Body", "External Organs", "Toenail", "पैर का नाखून", "टोनेल"),
            arrayOf("Human Body", "External Organs", "Big Toe", "पैर का अंगूठा", "बिग टो"),
            arrayOf("Human Body", "External Organs", "Skin", "त्वचा", "स्किन"),

            arrayOf("Human Body", "Internal Organs", "Brain", "दिमाग", "ब्रेन"),
            arrayOf("Human Body", "Internal Organs", "Heart", "दिल", "हार्ट"),
            arrayOf("Human Body", "Internal Organs", "Lungs", "फेफड़े", "लंग्स"),
            arrayOf("Human Body", "Internal Organs", "Liver", "जिगर / यकृत", "लिवर"),
            arrayOf("Human Body", "Internal Organs", "Kidney", "गुर्दा", "किडनी"),
            arrayOf("Human Body", "Internal Organs", "Small Intestine", "छोटी आंत", "स्मॉल इंटेस्टाइन"),
            arrayOf("Human Body", "Internal Organs", "Large Intestine", "बड़ी आंत", "लार्ज इंटेस्टाइन"),
            arrayOf("Human Body", "Internal Organs", "Pancreas", "अग्न्याशय", "पैंक्रियाज"),
            arrayOf("Human Body", "Internal Organs", "Spleen", "तिल्ली", "स्प्लीन"),
            arrayOf("Human Body", "Internal Organs", "Gallbladder", "पित्ताशय", "गॉलब्लैडर"),
            arrayOf("Human Body", "Internal Organs", "Urinary Bladder", "मूत्राशय", "यूरिनरी ब्लैडर"),
            arrayOf("Human Body", "Internal Organs", "Food Pipe", "ग्रासनली", "फूड पाइप"),
            arrayOf("Human Body", "Internal Organs", "Windpipe", "श्वासनली", "विंडपाइप"),
            arrayOf("Human Body", "Internal Organs", "Appendix", "अपेंडिक्स", "अपेंडिक्स"),
            arrayOf("Human Body", "Internal Organs", "Bone", "हड्डी", "बोन"),
            arrayOf("Human Body", "Internal Organs", "Skull", "खोपड़ी", "स्कल"),
            arrayOf("Human Body", "Internal Organs", "Backbone", "रीढ़ की हड्डी", "बैकबोन"),
            arrayOf("Human Body", "Internal Organs", "Rib", "पसली", "रिब"),
            arrayOf("Human Body", "Internal Organs", "Muscle", "मांसपेशी", "मसल"),
            arrayOf("Human Body", "Internal Organs", "Joint", "जोड़", "जॉइंट"),
            arrayOf("Human Body", "Internal Organs", "Cartilage", "उपास्थि", "कार्टिलेज"),
            arrayOf("Human Body", "Internal Organs", "Blood", "खून / रक्त", "ब्लड"),
            arrayOf("Human Body", "Internal Organs", "Nerve", "नस / तंत्रिका", "नर्व"),
            arrayOf("Human Body", "Internal Organs", "Vein", "नस / शिरा", "वेन"),
            arrayOf("Human Body", "Internal Organs", "Artery", "धमनी", "आर्टरी"),
            arrayOf("Human Body", "Internal Organs", "Blood Vessel", "रक्त वाहिका", "ब्लड वेसल"),

            arrayOf("Human Body", "Reproductive Organs", "Reproductive System", "प्रजनन तंत्र", "रिप्रोडक्टिव सिस्टम"),
            arrayOf("Human Body", "Reproductive Organs", "Penis", "शिश्न", "पेनिस"),
            arrayOf("Human Body", "Reproductive Organs", "Testicles", "अंडकोष", "टेस्टिकल्स"),
            arrayOf("Human Body", "Reproductive Organs", "Scrotum", "अंडकोश की थैली", "स्क्रोटम"),
            arrayOf("Human Body", "Reproductive Organs", "Sperm", "शुक्राणु", "स्पर्म"),
            arrayOf("Human Body", "Reproductive Organs", "Vagina", "योनि", "वजाइना"),
            arrayOf("Human Body", "Reproductive Organs", "Uterus", "गर्भाशय", "यूटेरस"),
            arrayOf("Human Body", "Reproductive Organs", "Cervix", "गर्भाशय ग्रीवा", "सर्विक्स"),
            arrayOf("Human Body", "Reproductive Organs", "Ovary", "अंडाशय", "ओवरी"),
            arrayOf("Human Body", "Reproductive Organs", "Ovum", "अंडाणु", "ओवम"),
            arrayOf("Human Body", "Reproductive Organs", "Fallopian Tube", "डिंबवाहिनी नलिका", "फैलोपियन ट्यूब"),
            arrayOf("Human Body", "Reproductive Organs", "Breast", "स्तन", "ब्रेस्ट"),
            arrayOf("Human Body", "Reproductive Organs", "Nipple", "निप्पल", "निप्पल"),

            // === ANIMALS ===
            arrayOf("Animals", "Wild Animals", "Lion", "शेर", "लायन"),
            arrayOf("Animals", "Wild Animals", "Tiger", "बाघ", "टाइगर"),
            arrayOf("Animals", "Wild Animals", "Elephant", "हाथी", "एलिफ़ेंट"),
            arrayOf("Animals", "Wild Animals", "Leopard", "तेंदुआ", "लेपर्ड"),
            arrayOf("Animals", "Wild Animals", "Cheetah", "चीता", "चीता"),
            arrayOf("Animals", "Wild Animals", "Jaguar", "जगुआर", "जगुआर"),
            arrayOf("Animals", "Wild Animals", "Bear", "भालू", "बेयर"),
            arrayOf("Animals", "Wild Animals", "Polar Bear", "ध्रुवीय भालू", "पोलर बेयर"),
            arrayOf("Animals", "Wild Animals", "Panda", "पांडा", "पांडा"),
            arrayOf("Animals", "Wild Animals", "Giant Panda", "विशाल पांडा", "जायंट पांडा"),
            arrayOf("Animals", "Wild Animals", "Red Panda", "लाल पांडा", "रेड पांडा"),
            arrayOf("Animals", "Wild Animals", "Wolf", "भेड़िया", "वुल्फ"),
            arrayOf("Animals", "Wild Animals", "Fox", "लोमड़ी", "फॉक्स"),
            arrayOf("Animals", "Wild Animals", "Jackal", "सियार", "सियार"),
            arrayOf("Animals", "Wild Animals", "Hyena", "लकड़बग्घा", "हाइना"),
            arrayOf("Animals", "Wild Animals", "Deer", "हिरण", "डियर"),
            arrayOf("Animals", "Wild Animals", "Reindeer", "रेनडियर", "रेनडियर"),
            arrayOf("Animals", "Wild Animals", "Moose", "सांभर / मूस", "मूस"),
            arrayOf("Animals", "Wild Animals", "Giraffe", "जिराफ़", "जिराफ़"),
            arrayOf("Animals", "Wild Animals", "Zebra", "ज़ेबरा", "ज़ेबरा"),
            arrayOf("Animals", "Wild Animals", "Wild Boar", "जंगली सूअर", "वाइल्ड बोर"),
            arrayOf("Animals", "Wild Animals", "Monkey", "बंदर", "मंकी"),
            arrayOf("Animals", "Wild Animals", "Gorilla", "गोरिल्ला", "गोरिल्ला"),
            arrayOf("Animals", "Wild Animals", "Chimpanzee", "चिंपैंजी", "चिंपैंजी"),
            arrayOf("Animals", "Wild Animals", "Orangutan", "वनमानुष", "ओरंगुटान"),
            arrayOf("Animals", "Wild Animals", "Kangaroo", "कंगारू", "कंगारू"),
            arrayOf("Animals", "Wild Animals", "Koala", "कोआला", "कोआला"),
            arrayOf("Animals", "Wild Animals", "Hedgehog", "कांटेदार चूहा", "हेजहॉग"),
            arrayOf("Animals", "Wild Animals", "Porcupine", "साही", "पोर्क्युपाइन"),
            arrayOf("Animals", "Wild Animals", "Rhinoceros", "गैंडा", "राइनोसेरस"),
            arrayOf("Animals", "Wild Animals", "Hippopotamus", "दरियाई घोड़ा", "हिप्पोपोटेमस"),
            arrayOf("Animals", "Wild Animals", "Sloth", "स्लॉथ", "स्लॉथ"),
            arrayOf("Animals", "Wild Animals", "Otter", "ऊदबिलाव", "ऑटर"),
            arrayOf("Animals", "Wild Animals", "Beaver", "बीवर", "बीवर"),
            arrayOf("Animals", "Wild Animals", "Mongoose", "नेवला", "मंगूज़"),
            arrayOf("Animals", "Wild Animals", "Meerkat", "मीरकैट", "मीरकैट"),
            arrayOf("Animals", "Wild Animals", "Pangolin", "पैंगोलिन", "पैंगोलिन"),
            arrayOf("Animals", "Wild Animals", "Armadillo", "आर्माडिलो", "आर्माडिलो"),
            arrayOf("Animals", "Wild Animals", "Antelope", "मृग", "एंटीलोप"),
            arrayOf("Animals", "Wild Animals", "Bison", "जंगली भैंसा", "बाइसन"),
            arrayOf("Animals", "Wild Animals", "Raccoon", "रैकून", "रैकून"),
            arrayOf("Animals", "Wild Animals", "Skunk", "स्कंक", "स्कंक"),
            arrayOf("Animals", "Wild Animals", "Lemur", "लीमर", "लीमर"),

            arrayOf("Animals", "Domestic Animals", "Cow", "गाय", "काऊ"),
            arrayOf("Animals", "Domestic Animals", "Buffalo", "भैंस", "बफ़ेलो"),
            arrayOf("Animals", "Domestic Animals", "Bull", "सांड", "बुल"),
            arrayOf("Animals", "Domestic Animals", "Ox", "बैल", "ऑक्स"),
            arrayOf("Animals", "Domestic Animals", "Goat", "बकरी", "गोट"),
            arrayOf("Animals", "Domestic Animals", "Sheep", "भेड़", "शीप"),
            arrayOf("Animals", "Domestic Animals", "Horse", "घोड़ा", "हॉर्स"),
            arrayOf("Animals", "Domestic Animals", "Donkey", "गधा", "डंकी"),
            arrayOf("Animals", "Domestic Animals", "Camel", "ऊँट", "कैमल"),
            arrayOf("Animals", "Domestic Animals", "Pig", "सूअर", "पिग"),
            arrayOf("Animals", "Domestic Animals", "Rabbit", "खरगोश", "रैबिट"),
            arrayOf("Animals", "Domestic Animals", "Hare", "खरगोश", "हेयर"),
            arrayOf("Animals", "Domestic Animals", "Dog", "कुत्ता", "डॉग"),
            arrayOf("Animals", "Domestic Animals", "Puppy", "पिल्ला", "पपी"),
            arrayOf("Animals", "Domestic Animals", "Cat", "बिल्ली", "कैट"),
            arrayOf("Animals", "Domestic Animals", "Kitten", "बिल्ली का बच्चा", "किटन"),
            arrayOf("Animals", "Domestic Animals", "Yak", "याक", "याक"),
            arrayOf("Animals", "Domestic Animals", "Llama", "लामा", "लामा"),
            arrayOf("Animals", "Domestic Animals", "Alpaca", "अल्पाका", "अल्पाका"),
            arrayOf("Animals", "Domestic Animals", "Squirrel", "गिलहरी", "स्क्विरल"),
            arrayOf("Animals", "Domestic Animals", "Rat", "चूहा", "रैट"),
            arrayOf("Animals", "Domestic Animals", "Mouse", "छोटा चूहा", "माउस"),

            arrayOf("Animals", "Birds", "Eagle", "चील / बाज़", "ईगल"),
            arrayOf("Animals", "Birds", "Parrot", "तोता", "पैरट"),
            arrayOf("Animals", "Birds", "Peacock", "मोर", "पीकॉक"),
            arrayOf("Animals", "Birds", "Pigeon", "कबूतर", "पिजन"),
            arrayOf("Animals", "Birds", "Dove", "फाख्ता", "डव"),
            arrayOf("Animals", "Birds", "Crow", "कौआ", "क्रो"),
            arrayOf("Animals", "Birds", "Sparrow", "गौरैया", "स्पैरो"),
            arrayOf("Animals", "Birds", "Owl", "उल्लू", "आउल"),
            arrayOf("Animals", "Birds", "Penguin", "पेंगुइन", "पेंगुइन"),
            arrayOf("Animals", "Birds", "Flamingo", "राजहंस", "फ्लेमिंगो"),
            arrayOf("Animals", "Birds", "Swan", "हंस", "स्वान"),
            arrayOf("Animals", "Birds", "Duck", "बतख", "डक"),
            arrayOf("Animals", "Birds", "Goose", "कलहंस", "गूज़"),
            arrayOf("Animals", "Birds", "Hen", "मुर्गी", "हेन"),
            arrayOf("Animals", "Birds", "Rooster", "मुर्गा", "रूस्टर"),
            arrayOf("Animals", "Birds", "Turkey", "टर्की", "टर्की"),
            arrayOf("Animals", "Birds", "Ostrich", "शुतुरमुर्ग", "ऑस्ट्रिच"),
            arrayOf("Animals", "Birds", "Emu", "एमू", "एमू"),
            arrayOf("Animals", "Birds", "Kingfisher", "रामचिरैया", "किंगफिशर"),
            arrayOf("Animals", "Birds", "Woodpecker", "कठफोड़वा", "वुडपेकर"),
            arrayOf("Animals", "Birds", "Hummingbird", "गुंजन पक्षी", "हमिंगबर्ड"),
            arrayOf("Animals", "Birds", "Vulture", "गिद्ध", "वल्चर"),
            arrayOf("Animals", "Birds", "Falcon", "बाज़", "फाल्कन"),
            arrayOf("Animals", "Birds", "Hawk", "बाज़", "हॉक"),
            arrayOf("Animals", "Birds", "Crane", "सारस", "क्रेन"),
            arrayOf("Animals", "Birds", "Heron", "बगुला", "हेरॉन"),
            arrayOf("Animals", "Birds", "Stork", "सारस / ढोक", "स्टॉर्क"),
            arrayOf("Animals", "Birds", "Seagull", "गंगा-चिल्ली", "सीगल"),
            arrayOf("Animals", "Birds", "Albatross", "अल्बाट्रॉस", "अल्बाट्रॉस"),
            arrayOf("Animals", "Birds", "Cuckoo", "कोयल", "कुक्कू"),
            arrayOf("Animals", "Birds", "Mynah", "मैना", "मैना"),
            arrayOf("Animals", "Birds", "Hornbill", "धनेश", "हॉर्नबिल"),
            arrayOf("Animals", "Birds", "Quail", "बटेर", "क्वेल"),
            arrayOf("Animals", "Birds", "Partridge", "तीतर", "पार्ट्रिज"),
            arrayOf("Animals", "Birds", "Pelican", "हवासील", "पेलिकन"),
            arrayOf("Animals", "Birds", "Pheasant", "तीतर पक्षी", "फीज़ेंट"),

            arrayOf("Animals", "Insects", "Butterfly", "तितली", "बटरफ्लाई"),
            arrayOf("Animals", "Insects", "Moth", "पतंगा", "मॉथ"),
            arrayOf("Animals", "Insects", "Honeybee", "मधुमक्खी", "हनीबी"),
            arrayOf("Animals", "Insects", "Ant", "चींटी", "आंट"),
            arrayOf("Animals", "Insects", "Mosquito", "मच्छर", "मॉस्किटो"),
            arrayOf("Animals", "Insects", "Housefly", "मक्खी", "हाउसफ्लाई"),
            arrayOf("Animals", "Insects", "Dragonfly", "व्याध पतंग", "ड्रैगनफ्लाई"),
            arrayOf("Animals", "Insects", "Grasshopper", "टिड्डा", "ग्रासहॉपर"),
            arrayOf("Animals", "Insects", "Cricket", "झींगुर", "क्रिकेट"),
            arrayOf("Animals", "Insects", "Cockroach", "तिलचट्टा", "कॉकरोच"),
            arrayOf("Animals", "Insects", "Termite", "दीमक", "टरमाइट"),
            arrayOf("Animals", "Insects", "Beetle", "भृंग / गुबरैला", "बीटल"),
            arrayOf("Animals", "Insects", "Ladybug", "सोनपंखी", "लेडीबग"),
            arrayOf("Animals", "Insects", "Spider", "मकड़ी", "स्पाइडर"),
            arrayOf("Animals", "Insects", "Scorpion", "बिच्छू", "स्कॉर्पियन"),
            arrayOf("Animals", "Insects", "Centipede", "कनखजूरा", "सेंटिपीड़"),
            arrayOf("Animals", "Insects", "Millipede", "सहस्रपाद", "मिलीपीड़"),
            arrayOf("Animals", "Insects", "Earthworm", "केंचुआ", "अर्थवर्म"),
            arrayOf("Animals", "Insects", "Snail", "घोंघा", "स्नेल"),
            arrayOf("Animals", "Insects", "Slug", "स्लग", "स्लग"),
            arrayOf("Animals", "Insects", "Leech", "जोंक", "लीच"),

            arrayOf("Animals", "Reptiles & Amphibians", "Crocodile", "मगरमच्छ", "क्रोकोडाइल"),
            arrayOf("Animals", "Reptiles & Amphibians", "Alligator", "घड़ियाल", "एलिगेटर"),
            arrayOf("Animals", "Reptiles & Amphibians", "Turtle", "कछुआ (जल)", "टर्टल"),
            arrayOf("Animals", "Reptiles & Amphibians", "Tortoise", "कछुआ (स्थल)", "टॉर्टॉइज़"),
            arrayOf("Animals", "Reptiles & Amphibians", "Snake", "साँप", "स्नेक"),
            arrayOf("Animals", "Reptiles & Amphibians", "Lizard", "छिपकली", "लिज़र्ड"),
            arrayOf("Animals", "Reptiles & Amphibians", "Chameleon", "गिरगिट", "कैमेलियन"),
            arrayOf("Animals", "Reptiles & Amphibians", "Iguana", "इगुआना", "इगुआना"),
            arrayOf("Animals", "Reptiles & Amphibians", "Frog", "मेंढक", "फ्रॉग"),
            arrayOf("Animals", "Reptiles & Amphibians", "Toad", "दादुर / मेंढक", "टोड"),
            arrayOf("Animals", "Reptiles & Amphibians", "Salamander", "सैलामैंडर", "सैलामैंडर"),
            arrayOf("Animals", "Reptiles & Amphibians", "Newt", "न्यूट", "न्यूट"),
            arrayOf("Animals", "Reptiles & Amphibians", "Bat", "चमगादड़", "बैट"),

            arrayOf("Animals", "Sea Animals", "Dolphin", "डॉलफिन", "डॉलफिन"),
            arrayOf("Animals", "Sea Animals", "Whale", "व्हेल", "व्हेल"),
            arrayOf("Animals", "Sea Animals", "Shark", "शार्क", "शार्क"),
            arrayOf("Animals", "Sea Animals", "Octopus", "अष्टबाहु", "ऑक्टोपस"),
            arrayOf("Animals", "Sea Animals", "Squid", "स्क्वीड", "स्क्वीड"),
            arrayOf("Animals", "Sea Animals", "Seal", "सील", "सील"),
            arrayOf("Animals", "Sea Animals", "Sea Lion", "जल सिंह", "सी लायन"),
            arrayOf("Animals", "Sea Animals", "Walrus", "वॉलरस", "वॉलरस"),
            arrayOf("Animals", "Sea Animals", "Jellyfish", "जेलीफ़िश", "जेलीफ़िश"),
            arrayOf("Animals", "Sea Animals", "Starfish", "तारा मछली", "स्टारफ़िश"),
            arrayOf("Animals", "Sea Animals", "Seahorse", "समुद्री घोड़ा", "सीहॉर्स"),
            arrayOf("Animals", "Sea Animals", "Crab", "केकड़ा", "क्रैब"),
            arrayOf("Animals", "Sea Animals", "Lobster", "झींगा मछली", "लॉबस्टर"),
            arrayOf("Animals", "Sea Animals", "Shrimp", "झींगा", "श्रिम्प"),
            arrayOf("Animals", "Sea Animals", "Prawn", "झींगा", "प्रॉन"),
            arrayOf("Animals", "Sea Animals", "Clam", "सीपी", "क्लैम"),
            arrayOf("Animals", "Sea Animals", "Oyster", "सीप", "ऑयस्टर"),
            arrayOf("Animals", "Sea Animals", "Mussel", "कस्तूरी सीपी", "मसल"),
            arrayOf("Animals", "Sea Animals", "Coral", "मूंगा", "कोरल"),
            arrayOf("Animals", "Sea Animals", "Sea Urchin", "जलसाही", "सी अर्चिन"),
            arrayOf("Animals", "Sea Animals", "Sea Cucumber", "समुद्री खीरा", "सी कुकम्बर"),

            // === PLANTS ===
            arrayOf("Plants", "Trees", "Tree", "पेड़ / वृक्ष", "ट्री"),
            arrayOf("Plants", "Trees", "Banyan", "बरगद", "बरगद"),
            arrayOf("Plants", "Trees", "Peepal", "पीपल", "पीपल"),
            arrayOf("Plants", "Trees", "Neem", "नीम", "नीम"),
            arrayOf("Plants", "Trees", "Mango Tree", "आम का पेड़", "मैंगो ट्री"),
            arrayOf("Plants", "Trees", "Coconut Tree", "नारियल का पेड़", "कोकोनट ट्री"),
            arrayOf("Plants", "Trees", "Palm Tree", "ताड़ का पेड़", "पाम ट्री"),
            arrayOf("Plants", "Trees", "Ashoka Tree", "अशोक का पेड़", "अशोका ट्री"),
            arrayOf("Plants", "Trees", "Eucalyptus", "नीलगिरी", "यूकेलिप्टस"),
            arrayOf("Plants", "Trees", "Teak", "सागौन", "टीक"),
            arrayOf("Plants", "Trees", "Sandalwood", "चंदन", "सैंडलवुड"),
            arrayOf("Plants", "Trees", "Lemon Tree", "नींबू का पेड़", "लेमन ट्री"),
            arrayOf("Plants", "Trees", "Guava Tree", "अमरूद का पेड़", "ग्वावा ट्री"),
            arrayOf("Plants", "Trees", "Apple Tree", "सेब का पेड़", "एप्पल ट्री"),
            arrayOf("Plants", "Trees", "Orange Tree", "संतरे का पेड़", "ऑरेंज ट्री"),
            arrayOf("Plants", "Trees", "Papaya Tree", "पपीते का पेड़", "पपाया ट्री"),
            arrayOf("Plants", "Trees", "Pomegranate Tree", "अनार का पेड़", "पोमेग्रेनेट ट्री"),
            arrayOf("Plants", "Trees", "Pine Tree", "चीड़ का पेड़", "पाइन ट्री"),
            arrayOf("Plants", "Trees", "Cedar Tree", "देवदार", "सीडर ट्री"),
            arrayOf("Plants", "Trees", "Oak Tree", "बलूत का पेड़", "ओक ट्री"),
            arrayOf("Plants", "Trees", "Maple Tree", "मैपल का पेड़", "मैपल ट्री"),
            arrayOf("Plants", "Trees", "Willow Tree", "बेंत / विलो", "विलो ट्री"),
            arrayOf("Plants", "Trees", "Birch Tree", "भोजपत्र", "बर्च ट्री"),
            arrayOf("Plants", "Trees", "Cherry Blossom", "चेरी ब्लॉसम", "चेरी ब्लॉसम"),

            arrayOf("Plants", "Flowering Plants", "Rose Plant", "गुलाब का पौधा", "रोज़ प्लांट"),
            arrayOf("Plants", "Flowering Plants", "Jasmine", "चमेली", "जैस्मीन"),
            arrayOf("Plants", "Flowering Plants", "Hibiscus", "गुड़हल", "हिबिस्कस"),
            arrayOf("Plants", "Flowering Plants", "Sunflower", "सूरजमुखी", "सनफ्लावर"),
            arrayOf("Plants", "Flowering Plants", "Lotus", "कमल", "लोटस"),
            arrayOf("Plants", "Flowering Plants", "Water Lily", "कुमुदिनी", "वाटर लिली"),
            arrayOf("Plants", "Flowering Plants", "Marigold", "गेंदा", "मैरीगोल्ड"),
            arrayOf("Plants", "Flowering Plants", "Lily", "कुमुद / लिली", "लिली"),
            arrayOf("Plants", "Flowering Plants", "Daisy", "गुलबहार", "डेज़ी"),
            arrayOf("Plants", "Flowering Plants", "Daffodil", "नरगिस", "डैफ़ोडिल"),
            arrayOf("Plants", "Flowering Plants", "Tulip", "कंदपुष्प", "ट्यूलिप"),
            arrayOf("Plants", "Flowering Plants", "Orchid", "ऑर्किड", "ऑर्किड"),
            arrayOf("Plants", "Flowering Plants", "Lavender", "लैवेंडर", "लैवेंडर"),
            arrayOf("Plants", "Flowering Plants", "Chrysanthemum", "गुलदाउदी", "गुलदाउदी"),
            arrayOf("Plants", "Flowering Plants", "Bougainvillea", "बोगनवेलिया", "बोगनवेलिया"),
            arrayOf("Plants", "Flowering Plants", "Periwinkle", "सदाबहार", "पेरिविंकल"),
            arrayOf("Plants", "Flowering Plants", "Morning Glory", "महिमा फूल", "मॉर्निंग ग्लोरी"),
            arrayOf("Plants", "Flowering Plants", "Night Jasmine", "हरसिंगार", "नाइट जैस्मीन"),

            arrayOf("Plants", "Medicinal & Herbs", "Plant", "पौधा", "प्लांट"),
            arrayOf("Plants", "Medicinal & Herbs", "Shrub", "झाड़ी", "शर्ब"),
            arrayOf("Plants", "Medicinal & Herbs", "Herb", "जड़ी-बूटी", "हर्ब"),
            arrayOf("Plants", "Medicinal & Herbs", "Tulsi", "तुलसी", "तुलसी"),
            arrayOf("Plants", "Medicinal & Herbs", "Aloe Vera", "एलोवेरा", "एलोवेरा"),
            arrayOf("Plants", "Medicinal & Herbs", "Money Plant", "मनी प्लांट", "मनी प्लांट"),
            arrayOf("Plants", "Medicinal & Herbs", "Snake Plant", "स्नेक प्लांट", "स्नेक प्लांट"),
            arrayOf("Plants", "Medicinal & Herbs", "Curry Leaf Plant", "कढ़ी पत्ता", "करी लीफ़"),
            arrayOf("Plants", "Medicinal & Herbs", "Basil", "बेसिल", "बेसिल"),
            arrayOf("Plants", "Medicinal & Herbs", "Mint Plant", "पुदीना पौधा", "मिंट प्लांट"),
            arrayOf("Plants", "Medicinal & Herbs", "Coriander Plant", "धनिया पौधा", "कोरिएंडर प्लांट"),
            arrayOf("Plants", "Medicinal & Herbs", "Fern", "फ़र्न", "फ़र्न"),
            arrayOf("Plants", "Medicinal & Herbs", "Moss", "काई / मॉस", "मॉस"),

            arrayOf("Plants", "Crops & Climbers", "Climber", "आरोही लता", "क्लाइंबर"),
            arrayOf("Plants", "Crops & Climbers", "Creeper", "लता / बेल", "क्रीपर"),
            arrayOf("Plants", "Crops & Climbers", "Cactus", "नागफनी", "कैक्टस"),
            arrayOf("Plants", "Crops & Climbers", "Bamboo", "बांस", "बैम्बू"),
            arrayOf("Plants", "Crops & Climbers", "Banana Plant", "केले का पौधा", "बनाना प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Grapevine", "अंगूर की बेल", "ग्रेपवाइन"),
            arrayOf("Plants", "Crops & Climbers", "Wheat Plant", "गेहूं का पौधा", "व्हीट प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Rice Plant", "धान का पौधा", "राइस प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Corn Plant", "मक्के का पौधा", "कॉर्न प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Cotton Plant", "कपास का पौधा", "कॉटन प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Sugarcane", "गन्ना", "शुगरकेन"),
            arrayOf("Plants", "Crops & Climbers", "Tea Plant", "चाय का पौधा", "टी प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Coffee Plant", "कॉफी का पौधा", "कॉफ़ी प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Jute Plant", "पटसन", "जूट प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Pea Plant", "मटर का पौधा", "पी प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Mustard Plant", "सरसों का पौधा", "मस्टर्ड प्लांट"),
            arrayOf("Plants", "Crops & Climbers", "Mustard Greens Plant", "राई का पौधा", "मस्टर्ड ग्रीन्स"),

            // === VEGETABLES ===
            arrayOf("Vegetables", "Daily Veggies", "Potato", "आलू", "पोटैटो"),
            arrayOf("Vegetables", "Daily Veggies", "Tomato", "टमाटर", "टोमैटो"),
            arrayOf("Vegetables", "Daily Veggies", "Onion", "प्याज़", "अनियन"),
            arrayOf("Vegetables", "Daily Veggies", "Garlic", "लहसुन", "गार्लिक"),
            arrayOf("Vegetables", "Daily Veggies", "Ginger", "अदरक", "जिंजर"),
            arrayOf("Vegetables", "Daily Veggies", "Brinjal", "बैंगन", "ब्रिंजल"),
            arrayOf("Vegetables", "Daily Veggies", "Cauliflower", "फूलगोभी", "कॉलीफ़्लॉवर"),
            arrayOf("Vegetables", "Daily Veggies", "Cabbage", "पत्तागोभी", "कैबेज"),
            arrayOf("Vegetables", "Daily Veggies", "Broccoli", "हरी गोभी", "ब्रोकोली"),
            arrayOf("Vegetables", "Daily Veggies", "Ladyfinger", "भिंडी", "लेडीफिंगर"),
            arrayOf("Vegetables", "Daily Veggies", "Green Peas", "मटर", "ग्रीन पीस"),
            arrayOf("Vegetables", "Daily Veggies", "Green Beans", "हरी फलियाँ", "ग्रीन बीन्स"),
            arrayOf("Vegetables", "Daily Veggies", "French Beans", "फ्रेंच बीन्स", "फ्रेंच बीन्स"),
            arrayOf("Vegetables", "Daily Veggies", "Cluster Beans", "ग्वारफली", "क्लस्टर बीन्स"),
            arrayOf("Vegetables", "Daily Veggies", "Broad Beans", "बाकला फलियाँ", "ब्रॉड बीन्स"),
            arrayOf("Vegetables", "Daily Veggies", "Capsicum", "शिमला मिर्च", "कैप्सिकम"),
            arrayOf("Vegetables", "Daily Veggies", "Green Chilli", "हरी मिर्च", "ग्रीन चिली"),
            arrayOf("Vegetables", "Daily Veggies", "Red Chilli", "लाल मिर्च", "रेड चिली"),
            arrayOf("Vegetables", "Daily Veggies", "Mushroom", "मशरूम", "मशरूम"),
            arrayOf("Vegetables", "Daily Veggies", "Sweet Corn", "मक्का", "स्वीट कॉर्न"),
            arrayOf("Vegetables", "Daily Veggies", "Drumstick", "सहजन", "ड्रमस्टिक"),
            arrayOf("Vegetables", "Daily Veggies", "Raw Banana", "कच्चा केला", "रॉ बनाना"),
            arrayOf("Vegetables", "Daily Veggies", "Raw Papaya", "कच्चा पपीता", "रॉ पपाया"),
            arrayOf("Vegetables", "Daily Veggies", "Jackfruit", "कटहल", "कटहल"),
            arrayOf("Vegetables", "Daily Veggies", "Brussels Sprouts", "छोटी बंदगोभी", "ब्रुसेल्स स्प्राउट्स"),
            arrayOf("Vegetables", "Daily Veggies", "Zucchini", "तोरी", "ज़ुकिनी"),
            arrayOf("Vegetables", "Daily Veggies", "Artichoke", "हाथी चक", "आर्टिचोक"),

            arrayOf("Vegetables", "Leafy Greens", "Spinach", "पालक", "स्पिनच"),
            arrayOf("Vegetables", "Leafy Greens", "Fenugreek", "मेथी", "फेनुग्रीक"),
            arrayOf("Vegetables", "Leafy Greens", "Mustard Greens", "सरसों का साग", "मस्टर्ड ग्रीन्स"),
            arrayOf("Vegetables", "Leafy Greens", "Coriander", "धनिया", "कोरिएंडर"),
            arrayOf("Vegetables", "Leafy Greens", "Mint", "पुदीना", "मिंट"),
            arrayOf("Vegetables", "Leafy Greens", "Lettuce", "सलाद पत्ता", "लेट्यूस"),
            arrayOf("Vegetables", "Leafy Greens", "Radish Leaves", "मूली के पत्ते", "रैडिश लीव्स"),
            arrayOf("Vegetables", "Leafy Greens", "Spring Onion", "हरा प्याज़", "स्प्रिंग अनियन"),
            arrayOf("Vegetables", "Leafy Greens", "Celery", "अजमोदा", "सेलरी"),
            arrayOf("Vegetables", "Leafy Greens", "Kale", "केल पत्ता", "केल"),
            arrayOf("Vegetables", "Leafy Greens", "Leek", "हरा लहसुन", "लीक"),
            arrayOf("Vegetables", "Leafy Greens", "Turnip Greens", "शलजम के पत्ते", "शलगम ग्रीन्स"),

            arrayOf("Vegetables", "Gourds", "Cucumber", "खीरा", "कुकम्बर"),
            arrayOf("Vegetables", "Gourds", "Bitter Gourd", "करेला", "बिटर गॉर्ड"),
            arrayOf("Vegetables", "Gourds", "Bottle Gourd", "लौकी", "बॉटल गॉर्ड"),
            arrayOf("Vegetables", "Gourds", "Ridge Gourd", "तोरई", "रिज गॉर्ड"),
            arrayOf("Vegetables", "Gourds", "Sponge Gourd", "नेनुआ / गिलकी", "स्पंज गॉर्ड"),
            arrayOf("Vegetables", "Gourds", "Snake Gourd", "चिचिंडा", "स्नेक गॉर्ड"),
            arrayOf("Vegetables", "Gourds", "Ash Gourd", "पेठा", "ऐश गॉर्ड"),
            arrayOf("Vegetables", "Gourds", "Pumpkin", "कद्दू", "पम्पकिन"),
            arrayOf("Vegetables", "Gourds", "Round Gourd", "टिंडा", "राउंड गॉर्ड"),
            arrayOf("Vegetables", "Gourds", "Pointed Gourd", "परवल", "पॉइंटेड गॉर्ड"),
            arrayOf("Vegetables", "Gourds", "Ivy Gourd", "कुंदरू", "आइवी गॉर्ड"),

            arrayOf("Vegetables", "Roots & Tubers", "Carrot", "गाजर", "कैरट"),
            arrayOf("Vegetables", "Roots & Tubers", "Radish", "मूली", "रैडिश"),
            arrayOf("Vegetables", "Roots & Tubers", "Turnip", "शलजम", "टर्निश"),
            arrayOf("Vegetables", "Roots & Tubers", "Beetroot", "चुकंदर", "बीटरूट"),
            arrayOf("Vegetables", "Roots & Tubers", "Sweet Potato", "शकरकंद", "स्वीट पोटैटो"),
            arrayOf("Vegetables", "Roots & Tubers", "Colocasia", "अरबी", "कोलोकेसिया"),
            arrayOf("Vegetables", "Roots & Tubers", "Taro Root", "अरबी कंद", "टारो रूट"),
            arrayOf("Vegetables", "Roots & Tubers", "Lotus Stem", "कमल ककड़ी", "लोटस स्टेम"),
            arrayOf("Vegetables", "Roots & Tubers", "Water Chestnut", "सिंघाड़ा", "वाटर चेस्टनट"),
            arrayOf("Vegetables", "Roots & Tubers", "Yam", "जिमीकंद / सूरन", "याम"),
            arrayOf("Vegetables", "Roots & Tubers", "Parsnip", "सफेद गाजर", "पार्सनिप"),
            arrayOf("Vegetables", "Roots & Tubers", "Asparagus", "शतावरी", "एस्परैगस"),
            arrayOf("Vegetables", "Roots & Tubers", "Fennel", "सौंफ की जड़", "फेनल")
        )

        for (w in words) {
            val cv = ContentValues().apply {
                put("main_cat", w[0])
                put("sub_cat", w[1])
                put("english", w[2])
                put("hindi", w[3])
                put("pron", w[4])
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
            db.rawQuery(
                "SELECT id, main_cat, sub_cat, english, hindi, pron FROM words WHERE english LIKE ? OR hindi LIKE ?",
                arrayOf("%$query%", "%$query%")
            )
        } else if (category != "All") {
            // Checks both main_cat (e.g. Animals) and sub_cat (e.g. Wild Animals)
            db.rawQuery(
                "SELECT id, main_cat, sub_cat, english, hindi, pron FROM words WHERE main_cat = ? OR sub_cat = ? ORDER BY english ASC",
                arrayOf(category, category)
            )
        } else {
            db.rawQuery("SELECT id, main_cat, sub_cat, english, hindi, pron FROM words ORDER BY english ASC", null)
        }

        while (cursor.moveToNext()) {
            list.add(
                WordItem(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5)
                )
            )
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
