
package com.konain.codealpha_flashcardquiz

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class Flashcard(
    val question: String,
    val answer: String
)

private const val PREFS_NAME = "flashcard_preferences"
private const val CARDS_KEY = "flashcards"

private fun loadCards(context: Context): List<Flashcard> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(CARDS_KEY, null) ?: return listOf(
        Flashcard("What is Kotlin?", "Kotlin is a modern programming language."),
        Flashcard("What is Android Studio?", "It is an IDE for Android app development."),
        Flashcard("What is Jetpack Compose?", "It is Android's modern UI toolkit.")
    )

    return try {
        val array = JSONArray(json)
        List(array.length()) { i ->
            val item = array.getJSONObject(i)
            Flashcard(
                question = item.getString("question"),
                answer = item.getString("answer")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveCards(context: Context, cards: List<Flashcard>) {
    val array = JSONArray()
    cards.forEach { card ->
        array.put(
            JSONObject().apply {
                put("question", card.question)
                put("answer", card.answer)
            }
        )
    }

    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(CARDS_KEY, array.toString())
        .apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FlashcardApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardApp() {
    val context = LocalContext.current
    var cards by remember { mutableStateOf(loadCards(context)) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var questionInput by remember { mutableStateOf("") }
    var answerInput by remember { mutableStateOf("") }

    fun updateCards(updated: List<Flashcard>) {
        cards = updated
        saveCards(context, updated)
        currentIndex = if (updated.isEmpty()) 0
        else currentIndex.coerceIn(0, updated.lastIndex)
        showAnswer = false
    }

    val currentCard = cards.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Flashcard Quiz",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Study smarter, one card at a time!",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                if (cards.isEmpty()) "No cards yet"
                else "Card ${currentIndex + 1} of ${cards.size}",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (showAnswer) "ANSWER" else "QUESTION",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = when {
                            currentCard == null -> "Add your first flashcard!"
                            showAnswer -> currentCard.answer
                            else -> currentCard.question
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = { showAnswer = !showAnswer },
                enabled = currentCard != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showAnswer) "Show Question" else "Show Answer")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (cards.isNotEmpty()) {
                            currentIndex =
                                (currentIndex - 1 + cards.size) % cards.size
                            showAnswer = false
                        }
                    },
                    enabled = cards.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Previous")
                }

                OutlinedButton(
                    onClick = {
                        if (cards.isNotEmpty()) {
                            currentIndex = (currentIndex + 1) % cards.size
                            showAnswer = false
                        }
                    },
                    enabled = cards.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                }
            }

            HorizontalDivider()

            Text(
                "Manage Flashcards",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    editingIndex = -1
                    questionInput = ""
                    answerInput = ""
                    dialogOpen = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋ Add Flashcard")
            }

            OutlinedButton(
                onClick = {
                    currentCard?.let {
                        editingIndex = currentIndex
                        questionInput = it.question
                        answerInput = it.answer
                        dialogOpen = true
                    }
                },
                enabled = currentCard != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Current Card")
            }

            OutlinedButton(
                onClick = {
                    if (cards.isNotEmpty()) {
                        updateCards(cards.filterIndexed { i, _ ->
                            i != currentIndex
                        })
                    }
                },
                enabled = currentCard != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Current Card")
            }
        }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = {
                Text(
                    if (editingIndex == -1) "Add Flashcard"
                    else "Edit Flashcard"
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        label = { Text("Enter question") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it },
                        label = { Text("Enter answer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (
                            questionInput.isNotBlank() &&
                            answerInput.isNotBlank()
                        ) {
                            val newCard = Flashcard(
                                questionInput.trim(),
                                answerInput.trim()
                            )

                            val updated = cards.toMutableList()

                            if (editingIndex == -1) {
                                updated.add(newCard)
                                updateCards(updated)
                                currentIndex = updated.lastIndex
                            } else {
                                updated[editingIndex] = newCard
                                updateCards(updated)
                                currentIndex = editingIndex
                            }

                            showAnswer = false
                            dialogOpen = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
