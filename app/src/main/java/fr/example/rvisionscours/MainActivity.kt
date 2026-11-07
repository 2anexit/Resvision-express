package fr.example.rvisionscours

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, GarminWatchManager.Listener {

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var rootView: ScrollView
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var menuContainer: LinearLayout
    private lateinit var subcategoryContainer: LinearLayout
    private lateinit var subcategoryTitleTextView: TextView
    private lateinit var subcategoryButtonsContainer: LinearLayout
    private lateinit var quizContainer: LinearLayout
    private lateinit var resultContainer: LinearLayout
    private lateinit var watchModeStatusTextView: TextView
    private lateinit var categoryTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var inputModeTextView: TextView
    private lateinit var questionTextView: TextView
    private lateinit var feedbackTextView: TextView
    private lateinit var resultCategoryTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var scoreMessageTextView: TextView
    private lateinit var watchModeButton: MaterialButton
    private lateinit var submitButton: MaterialButton
    private lateinit var soundToggleButton: MaterialButton
    private lateinit var retryButton: MaterialButton
    private lateinit var backToMenuButton: MaterialButton
    private lateinit var backToCategoriesButton: MaterialButton
    private lateinit var abandonQuizButton: MaterialButton
    private lateinit var optionButtons: List<MaterialButton>
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var garminWatchManager: GarminWatchManager

    private val categories = listOf(
        QuestionCategory(
            R.id.mathsButton,
            R.string.category_maths,
            subcategories = listOf(
                QuestionSubcategory(R.string.subcategory_all_questions, "tout_melange_maths.csv"),
                QuestionSubcategory(R.string.maths_1e, "maths_1e.csv"),
                QuestionSubcategory(R.string.maths_term, "maths_term.csv")
            )
        ),
        QuestionCategory(
            R.id.francaisButton,
            R.string.category_francais,
            subcategories = listOf(QuestionSubcategory(R.string.series_french_basic, "francais.csv"))
        ),
        QuestionCategory(
            R.id.anglaisButton,
            R.string.category_anglais,
            subcategories = listOf(QuestionSubcategory(R.string.series_english_basic, "anglais.csv"))
        ),
        QuestionCategory(
            R.id.physiqueChimieButton,
            R.string.category_physique_chimie,
            subcategories = listOf(
                QuestionSubcategory(R.string.subcategory_all_questions, "tout_melange_physique.csv"),
                QuestionSubcategory(R.string.physique_gen, "physique_gen.csv"),
                QuestionSubcategory(R.string.physique_1e, "physique_1e.csv"),
                QuestionSubcategory(R.string.physique_term, "physique_term.csv")
            )
        ),
        QuestionCategory(
            R.id.histoireGeographieButton,
            R.string.category_histoire_geographie,
            subcategories = listOf(
                QuestionSubcategory(R.string.subcategory_all_questions, "tout_melange_histoire.csv"),
                QuestionSubcategory(R.string.histoire_gen, "histoire_gen.csv"),
                QuestionSubcategory(R.string.histoire_term, "histoire_term.csv")
            )
        ),
        QuestionCategory(R.id.toutMelangeButton, R.string.category_tout_melange, "tout_melange.csv")
    )

    private var sessionQuestions: MutableList<SessionQuestion> = mutableListOf()
    private var currentQuiz: QuizSelection? = null
    private var sessionQuestionCount = SESSION_QUESTION_COUNT
    private var currentQuestionIndex = 0
    private var selectedOptionIndex: Int? = null
    private var answerLocked = false
    private var correctAnswersCount = 0
    private var soundEnabled = true
    private var isTtsReady = false
    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingMillis = SERIES_DURATION_MS
    private var seriesEndedByTimeout = false
    private var isWatchModeEnabled = false
    private var garminStatus = GarminWatchManager.GarminStatus(
        ready = false,
        messageType = GarminWatchManager.GarminStatusMessage.INITIALIZING
    )

    private val nextQuestionRunnable = Runnable { advanceAfterAnswer() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        rootView = findViewById(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()
        textToSpeech = TextToSpeech(this, this)
        garminWatchManager = GarminWatchManager(this, this)
        garminWatchManager.initialize()
        showMenu()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopTimer()
        stopSpeaking()
        textToSpeech.stop()
        textToSpeech.shutdown()
        garminWatchManager.shutdown()
        super.onDestroy()
    }

    private fun bindViews() {
        titleTextView = findViewById(R.id.titleTextView)
        subtitleTextView = findViewById(R.id.subtitleTextView)
        menuContainer = findViewById(R.id.menuContainer)
        subcategoryContainer = findViewById(R.id.subcategoryContainer)
        subcategoryTitleTextView = findViewById(R.id.subcategoryTitleTextView)
        subcategoryButtonsContainer = findViewById(R.id.subcategoryButtonsContainer)
        quizContainer = findViewById(R.id.quizContainer)
        resultContainer = findViewById(R.id.resultContainer)
        watchModeStatusTextView = findViewById(R.id.watchModeStatusTextView)
        categoryTextView = findViewById(R.id.categoryTextView)
        progressTextView = findViewById(R.id.progressTextView)
        timerTextView = findViewById(R.id.timerTextView)
        inputModeTextView = findViewById(R.id.inputModeTextView)
        questionTextView = findViewById(R.id.questionTextView)
        feedbackTextView = findViewById(R.id.feedbackTextView)
        resultCategoryTextView = findViewById(R.id.resultCategoryTextView)
        scoreTextView = findViewById(R.id.scoreTextView)
        scoreMessageTextView = findViewById(R.id.scoreMessageTextView)
        watchModeButton = findViewById(R.id.watchModeButton)
        submitButton = findViewById(R.id.submitButton)
        soundToggleButton = findViewById(R.id.soundToggleButton)
        retryButton = findViewById(R.id.retryButton)
        backToMenuButton = findViewById(R.id.backToMenuButton)
        backToCategoriesButton = findViewById(R.id.backToCategoriesButton)
        findViewById<MaterialButton>(R.id.createMoreButton).setOnClickListener { showCreateMoreMenu() }
        findViewById<MaterialButton>(R.id.mySeriesButton).setOnClickListener { showMySeries() }
        abandonQuizButton = findViewById(R.id.abandonQuizButton)

        optionButtons = listOf(
            findViewById(R.id.optionButton1),
            findViewById(R.id.optionButton2),
            findViewById(R.id.optionButton3),
            findViewById(R.id.optionButton4)
        )

        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener { onOptionSelected(index) }
        }

        categories.forEach { category ->
            val categoryButton = findViewById<MaterialButton>(category.buttonId)
            categoryButton.setOnClickListener {
                selectCategory(category)
            }
            if (category.labelRes != R.string.category_tout_melange) {
                categoryButton.setOnLongClickListener {
                    val name = getString(category.labelRes)
                    showCategoryActions(name, customCategory = false, onOpen = { selectCategory(category) })
                    true
                }
            }
        }
        refreshCustomCategoryButtons()

        watchModeButton.setOnClickListener { toggleWatchMode() }
        submitButton.setOnClickListener { validatePhoneAnswer() }
        soundToggleButton.setOnClickListener { toggleSound() }
        retryButton.setOnClickListener {
            currentQuiz?.let(::startQuiz)
        }
        backToMenuButton.setOnClickListener { showMenu() }
        abandonQuizButton.setOnClickListener { showMenu() }

        updateSoundToggleLabel()
        updateWatchModeButton()
        updateWatchModeStatus()
        updateTimerDisplay(SERIES_DURATION_MS)
        backToCategoriesButton.setOnClickListener { showMenu() }
    }

    private fun showMenu() {
        handler.removeCallbacks(nextQuestionRunnable)
        stopTimer()
        stopSpeaking()
        currentQuiz = null
        currentQuestionIndex = 0
        correctAnswersCount = 0
        sessionQuestions.clear()
        selectedOptionIndex = null
        answerLocked = false
        timeRemainingMillis = SERIES_DURATION_MS
        seriesEndedByTimeout = false

        titleTextView.text = getString(R.string.app_name)
        subtitleTextView.text = getString(R.string.menu_subtitle)
        menuContainer.visibility = View.VISIBLE
        refreshCustomCategoryButtons()
        subcategoryContainer.visibility = View.GONE
        quizContainer.visibility = View.GONE
        resultContainer.visibility = View.GONE
        updateTimerDisplay(SERIES_DURATION_MS)
        updateWatchModeButton()
        updateWatchModeStatus()
        garminWatchManager.syncIdleState()
        scrollToTop()
    }

    private fun showCreateMoreMenu() {
        val choices = arrayOf("Créer une série dans une catégorie", "Éditer une série existante", "Créer une nouvelle catégorie")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Créer plus")
            .setItems(choices) { _, which ->
                when (which) {
                    0 -> chooseCategoryForSeries(false)
                    1 -> chooseCategoryForSeries(true)
                    2 -> createCategory()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showMySeries() {
        val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
        val prefsCategories = userCategories() + categories.map { getString(it.labelRes) }
        val categoriesWithSeries = prefsCategories.sorted().mapNotNull { category ->
            val names = prefs.getStringSet("series:$category", emptySet()).orEmpty().sorted()
            if (names.isEmpty()) null else category to names
        }
        if (categoriesWithSeries.isEmpty()) {
            Toast.makeText(this, "Tu n'as pas encore créé de série", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = categoriesWithSeries.flatMap { (category, names) -> names.map { "$category · $it" } }
        val entries = categoriesWithSeries.flatMap { (category, names) -> names.map { category to it } }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Mes séries")
            .setItems(labels.toTypedArray()) { _, index ->
                val (category, name) = entries[index]
        val count = prefs.getString("questions:$category:$name", "").orEmpty().lines().count { it.isNotBlank() }
                androidx.appcompat.app.AlertDialog.Builder(this).setTitle(name)
                    .setMessage("Catégorie : $category\nQuestions : $count")
                    .setNeutralButton("Jouer") { _, _ ->
                val selection = QuizSelection(QuestionCategory(0, 0), null, "custom:$category:$name")
                        startQuiz(selection)
                    }
                    .setPositiveButton("Modifier") { _, _ -> editCustomSeries(category, name) }
                    .setNegativeButton("Fermer", null).show()
            }.setNegativeButton("Fermer", null).show()
    }

    private fun userCategories(): MutableSet<String> = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
        .getStringSet("categories", emptySet())!!.toMutableSet()

    private fun refreshCustomCategoryButtons() {
        val customContainer = findViewById<LinearLayout>(R.id.customCategoriesContainer)
        customContainer.removeAllViews()
        userCategories().sorted().forEach { name ->
            val button = MaterialButton(this).apply {
                text = name
                isAllCaps = false
                minHeight = dpToPx(56)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(12) }
                setOnClickListener { showCustomCategorySeries(name) }
                setOnLongClickListener {
                    showCategoryActions(name, customCategory = true, onOpen = { showCustomCategorySeries(name) })
                    true
                }
            }
            customContainer.addView(button)
        }
    }

    private fun showCategoryActions(categoryName: String, customCategory: Boolean, onOpen: () -> Unit) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(8))
        }
        val subtitle = TextView(this).apply {
            text = if (customCategory) "Gère cette catégorie et ses séries." else "Ouvre la catégorie ou ajoute-lui une série personnalisée."
            textSize = 14f
            setTextColor(getColorCompat(R.color.quiz_feedback_neutral))
            setPadding(0, 0, 0, dpToPx(12))
        }
        content.addView(subtitle)
        var actionDialog: androidx.appcompat.app.AlertDialog? = null
        fun addAction(label: String, onClick: () -> Unit) {
            val button = MaterialButton(this).apply {
                text = label
                isAllCaps = false
                minHeight = dpToPx(48)
                setOnClickListener {
                    actionDialog?.dismiss()
                    onClick()
                }
            }
            content.addView(button, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(6) })
        }
        addAction("Ouvrir la catégorie", onOpen)
        addAction("Créer une nouvelle série") { editCustomSeries(categoryName, null) }
        if (customCategory) {
            addAction("Renommer la catégorie") { renameCustomCategory(categoryName) }
            addAction("Supprimer la catégorie") { deleteCustomCategory(categoryName) }
        }
        actionDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(categoryName)
            .setView(content)
            .setNegativeButton("Fermer", null)
            .create()
        actionDialog?.show()
    }

    private fun showCustomCategorySeries(category: String) {
        val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
        val names = prefs.getStringSet("series:$category", emptySet()).orEmpty().sorted()
        subcategoryTitleTextView.text = category
        subcategoryButtonsContainer.removeAllViews()
        val allButton = MaterialButton(this).apply {
            text = getString(R.string.subcategory_all_questions)
            isAllCaps = false
            setOnClickListener {
                startQuiz(QuizSelection(QuestionCategory(0, 0), QuestionSubcategory(R.string.subcategory_all_questions, ""), "customAll:$category"))
            }
        }
        subcategoryButtonsContainer.addView(allButton)
        names.forEach { name ->
            val button = MaterialButton(this).apply {
                text = name
                isAllCaps = false
                setOnClickListener { startQuiz(QuizSelection(QuestionCategory(0, 0), null, "custom:$category:$name")) }
            }
            subcategoryButtonsContainer.addView(button)
        }
        menuContainer.visibility = View.GONE
        quizContainer.visibility = View.GONE
        resultContainer.visibility = View.GONE
        subcategoryContainer.visibility = View.VISIBLE
    }

    private fun renameCustomCategory(oldName: String) {
        val input = EditText(this).apply { setText(oldName); hint = "Nom de la catégorie" }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Renommer la catégorie").setView(input)
            .setNegativeButton("Annuler", null).setPositiveButton("Renommer") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isBlank() || newName == oldName) return@setPositiveButton
                val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
                val categories = userCategories().apply { remove(oldName); add(newName) }
                val series = prefs.getStringSet("series:$oldName", emptySet())!!.toSet()
                val editor = prefs.edit().remove("series:$oldName").putStringSet("categories", categories).putStringSet("series:$newName", series)
                series.forEach { name ->
                    editor.putString("questions:$newName:$name", prefs.getString("questions:$oldName:$name", ""))
                    editor.putInt("count:$newName:$name", prefs.getInt("count:$oldName:$name", 20))
                    editor.remove("questions:$oldName:$name").remove("count:$oldName:$name")
                }
                editor.apply()
                refreshCustomCategoryButtons()
            }.show()
    }

    private fun deleteCustomCategory(category: String) {
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Supprimer la catégorie ?")
            .setMessage("Toutes ses séries seront également supprimées.")
            .setNegativeButton("Annuler", null).setPositiveButton("Supprimer") { _, _ ->
                val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
                val editor = prefs.edit().putStringSet("categories", userCategories().apply { remove(category) }).remove("series:$category")
                prefs.getStringSet("series:$category", emptySet()).orEmpty().forEach { name ->
                    editor.remove("questions:$category:$name").remove("count:$category:$name")
                }
                editor.apply()
                refreshCustomCategoryButtons()
            }.show()
    }

    private fun chooseCategoryForSeries(edit: Boolean) {
        val names = categories.map { getString(it.labelRes) } + userCategories().sorted()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(if (edit) "Choisir une catégorie" else "Ajouter une série à")
            .setItems(names.toTypedArray()) { _, index ->
                val name = names[index]
                if (edit) chooseSeriesToEdit(name) else editCustomSeries(name, null)
            }.setNegativeButton("Annuler", null).show()
    }

    private fun createCategory() {
        val input = EditText(this).apply { hint = "Nom de la catégorie" }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Nouvelle catégorie").setView(input)
            .setNegativeButton("Annuler", null).setPositiveButton("Créer") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    userCategories().apply { add(name) }.also { getSharedPreferences("custom_quizzes", MODE_PRIVATE).edit().putStringSet("categories", it).apply() }
                    Toast.makeText(this, "Catégorie créée", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun chooseSeriesToEdit(category: String) {
        val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
        val names = prefs.getStringSet("series:$category", emptySet())!!.toList()
        if (names.isEmpty()) { Toast.makeText(this, "Aucune série personnalisée dans cette catégorie", Toast.LENGTH_SHORT).show(); return }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Choisir une série")
            .setItems(names.toTypedArray()) { _, i -> editCustomSeries(category, names[i]) }.setNegativeButton("Annuler", null).show()
    }

    private fun editCustomSeries(category: String, existing: String?) {
        val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
        val nameInput = EditText(this).apply { hint = "Nom de la série"; setText(existing ?: "") }
        val questionCount = EditText(this).apply { hint = "Nombre de questions dans la série"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(prefs.getInt("count:$category:${existing ?: ""}", 20).toString()) }
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 8, 32, 0); addView(nameInput); addView(questionCount) }
        val questions = (if (existing == null) emptyList() else loadCustomQuestions("custom:$category:$existing")).toMutableList()
        if (questions.isEmpty()) questions += QuizQuestion("", listOf("", "", "", ""), 0)
        val positionText = TextView(this).apply { gravity = android.view.Gravity.CENTER; textSize = 16f }
        val questionInput = EditText(this).apply { hint = "Question" }
        val opts = (1..4).map { i -> EditText(this).apply { hint = "Réponse $i" } }
        val correct = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Bonne réponse : 1", "Bonne réponse : 2", "Bonne réponse : 3", "Bonne réponse : 4"))
        }
        val navigation = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val previousButton = MaterialButton(this).apply { text = "Précédente"; isAllCaps = false }
        val nextButton = MaterialButton(this).apply { text = "Question suivante"; isAllCaps = false }
        navigation.addView(previousButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        navigation.addView(nextButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        fields.addView(positionText); fields.addView(questionInput); opts.forEach { fields.addView(it) }; fields.addView(correct); fields.addView(navigation)
        var questionIndex = 0
        fun saveCurrentQuestion(): Boolean {
            if (questionInput.text.isBlank() || opts.any { it.text.isBlank() }) return false
            questions[questionIndex] = QuizQuestion(questionInput.text.toString(), opts.map { it.text.toString() }, correct.selectedItemPosition)
            return true
        }
        fun showQuestion() {
            val current = questions[questionIndex]
            positionText.text = "Question ${questionIndex + 1} / ${questions.size}"
            questionInput.setText(current.question)
            opts.forEachIndexed { i, input -> input.setText(current.options[i]) }
            correct.setSelection(current.correctIndex)
            previousButton.isEnabled = questionIndex > 0
        }
        previousButton.setOnClickListener {
            val totallyEmpty = questionInput.text.isBlank() && opts.all { it.text.isBlank() }
            if (totallyEmpty || saveCurrentQuestion()) { questionIndex--; showQuestion() }
            else Toast.makeText(this, "Complète la question avant de changer", Toast.LENGTH_SHORT).show()
        }
        nextButton.setOnClickListener {
            if (!saveCurrentQuestion()) { Toast.makeText(this, "Complète la question avant de continuer", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (questionIndex == questions.lastIndex) questions += QuizQuestion("", listOf("", "", "", ""), 0)
            questionIndex++
            showQuestion()
        }
        showQuestion()
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).setTitle(if (existing == null) "Nouvelle série" else "Modifier : $existing")
            .setView(fields).setNegativeButton("Annuler", null).setPositiveButton("Enregistrer") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (!saveCurrentQuestion() || name.isBlank()) { Toast.makeText(this, "Complète le nom et toutes les questions", Toast.LENGTH_LONG).show(); return@setPositiveButton }
                val amount = questionCount.text.toString().toIntOrNull()?.coerceIn(1, 200) ?: 20
                val key = "questions:$category:$name"
                prefs.edit().putString(key, questions.joinToString("\n") { (listOf(it.question) + it.options + (it.correctIndex + 1).toString()).joinToString("|") })
                    .putInt("count:$category:$name", amount)
                    .putStringSet("series:$category", (prefs.getStringSet("series:$category", emptySet())!!.toMutableSet() + name)).apply()
                refreshCustomCategoryButtons()
                Toast.makeText(this, "Série enregistrée", Toast.LENGTH_SHORT).show()
            }
        if (existing != null) dialog.setNeutralButton("Supprimer", null)
        val alert = dialog.create()
        alert.setOnShowListener {
            if (existing != null) alert.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                alert.dismiss()
                deleteCustomSeries(category, existing)
            }
        }
        alert.show()
    }

    private fun deleteCustomSeries(category: String, name: String) {
        val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Supprimer la série ?")
            .setMessage("Cette action supprimera « $name » et ses questions.")
            .setNegativeButton("Annuler", null).setPositiveButton("Supprimer") { _, _ ->
                val names = prefs.getStringSet("series:$category", emptySet())!!.toMutableSet().apply { remove(name) }
                prefs.edit().remove("questions:$category:$name").remove("count:$category:$name").putStringSet("series:$category", names).apply()
            }.show()
    }

    private fun selectCategory(category: QuestionCategory) {
        val hasCustomSeries = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
            .getStringSet("series:${getString(category.labelRes)}", emptySet()).orEmpty().isNotEmpty()
        if (category.subcategories.isNotEmpty() || hasCustomSeries) {
            showSubcategoryMenu(category)
            return
        }

        startQuiz(QuizSelection(category, null, category.fileName ?: return))
    }

    private fun showSubcategoryMenu(category: QuestionCategory) {
        titleTextView.text = getString(category.labelRes)
        subtitleTextView.text = getString(R.string.subcategory_subtitle)
        subcategoryTitleTextView.text = getString(R.string.subcategory_title)
        subcategoryButtonsContainer.removeAllViews()
        val subcategories = if (category.subcategories.any { it.labelRes == R.string.subcategory_all_questions }) {
            category.subcategories
        } else {
            listOf(QuestionSubcategory(R.string.subcategory_all_questions, "")) + category.subcategories
        }
        subcategories.forEach { subcategory ->
            val button = MaterialButton(this).apply {
                text = getString(subcategory.labelRes)
                isAllCaps = false
                minHeight = dpToPx(56)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(12)
                }
                setOnClickListener {
                    val fileName = if (subcategory.labelRes == R.string.subcategory_all_questions) {
                        "allCategory:${getString(category.labelRes)}"
                    } else subcategory.fileName
                    startQuiz(QuizSelection(category, subcategory, fileName))
                }
            }
            subcategoryButtonsContainer.addView(button)
        }
        val customSeries = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
            .getStringSet("series:${getString(category.labelRes)}", emptySet()).orEmpty().sorted()
        customSeries.forEach { seriesName ->
            val button = MaterialButton(this).apply {
                text = seriesName
                isAllCaps = false
                minHeight = dpToPx(56)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setOnClickListener { startQuiz(QuizSelection(category, null, "custom:${getString(category.labelRes)}:$seriesName")) }
            }
            subcategoryButtonsContainer.addView(button)
        }

        menuContainer.visibility = View.GONE
        subcategoryContainer.visibility = View.VISIBLE
        quizContainer.visibility = View.GONE
        resultContainer.visibility = View.GONE
        scrollToTop()
    }

    private fun startQuiz(selection: QuizSelection) {
        if (isWatchModeEnabled) {
            Toast.makeText(this, getString(R.string.watch_opening_app), Toast.LENGTH_SHORT).show()
            garminWatchManager.prepareWatchSession { ready ->
                if (ready) {
                    startQuizInternal(selection)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.watch_mode_requires_ready_watch),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            startQuizInternal(selection)
        }
    }

    private fun startQuizInternal(selection: QuizSelection) {
        handler.removeCallbacks(nextQuestionRunnable)
        stopTimer()
        stopSpeaking()

        val isAllQuestions = selection.subcategory?.labelRes == R.string.subcategory_all_questions
        val isAllMix = selection.category.labelRes == R.string.category_tout_melange
        val isCustomAll = selection.fileName.startsWith("customAll:")
        val customQuestions = if (selection.fileName.startsWith("custom:")) loadCustomQuestions(selection.fileName) else emptyList()
        val sourceQuestions = when {
            isCustomAll -> loadCustomCategoryQuestions(selection.fileName.removePrefix("customAll:"))
            selection.fileName.startsWith("custom:") -> customQuestions
            isAllMix -> loadAllSeriesQuestions()
            isAllQuestions -> loadAllCategoryQuestions(selection.category, selection.fileName, selection.fileName.removePrefix("allCategory:"))
            else -> loadQuestions(selection.fileName)
        }
        if (sourceQuestions.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.missing_category_questions, getQuizLabel(selection)),
                Toast.LENGTH_SHORT
            ).show()
            showMenu()
            return
        }

        currentQuiz = selection
        currentQuestionIndex = 0
        correctAnswersCount = 0
        selectedOptionIndex = null
        answerLocked = false
        timeRemainingMillis = SERIES_DURATION_MS
        seriesEndedByTimeout = false
        sessionQuestionCount = if (isAllQuestions || isAllMix) {
            SESSION_QUESTION_COUNT
        } else if (selection.fileName.startsWith("custom:")) {
            val customParts = selection.fileName.split(":", limit = 3)
            if (customParts.size == 3) {
                getSharedPreferences("custom_quizzes", MODE_PRIVATE)
                    .getInt("count:${customParts[1]}:${customParts[2]}", sourceQuestions.size)
                    .coerceIn(1, 200)
            } else sourceQuestions.size
        } else SESSION_QUESTION_COUNT
        sessionQuestions = if (isAllQuestions || isAllMix) {
            buildRandomUniqueSession(sourceQuestions, SESSION_QUESTION_COUNT)
        } else if (selection.fileName.startsWith("custom:")) {
            buildSessionQuestions(sourceQuestions, sessionQuestionCount)
        } else buildSessionQuestions(sourceQuestions)

        titleTextView.text = getString(R.string.app_name)
        subtitleTextView.text = getString(R.string.quiz_subtitle)
        menuContainer.visibility = View.GONE
        subcategoryContainer.visibility = View.GONE
        quizContainer.visibility = View.VISIBLE
        resultContainer.visibility = View.GONE
        updateQuizInputModeUi()
        startTimer()
        showCurrentQuestion()
        scrollToTop()
    }

    private fun showCurrentQuestion() {
        if (sessionQuestions.isEmpty()) {
            showMenu()
            return
        }

        val question = sessionQuestions[currentQuestionIndex]
        selectedOptionIndex = null
        answerLocked = false

        categoryTextView.text = getString(
            R.string.category_label,
            currentQuiz?.let(::getQuizLabel) ?: getString(R.string.category_tout_melange)
        )
        progressTextView.text = getString(
            R.string.question_progress,
            currentQuestionIndex + 1,
            sessionQuestionCount
        )
        updateTimerDisplay(timeRemainingMillis)
        updateQuizInputModeUi()
        questionTextView.text = question.question
        feedbackTextView.text = if (isWatchModeEnabled) {
            getString(R.string.watch_mode_waiting)
        } else {
            ""
        }
        feedbackTextView.setTextColor(getColorCompat(R.color.quiz_feedback_neutral))
        submitButton.isEnabled = !isWatchModeEnabled

        optionButtons.forEachIndexed { index, button ->
            button.text = question.options[index]
            button.isEnabled = !isWatchModeEnabled
            button.visibility = if (isWatchModeEnabled) View.GONE else View.VISIBLE
            applyButtonStyle(button, R.color.quiz_option_default)
        }

        submitButton.visibility = if (isWatchModeEnabled) View.GONE else View.VISIBLE
        speakQuestion(question.question)
        syncCurrentQuestionToGarmin()
    }

    private fun onOptionSelected(index: Int) {
        if (answerLocked || isWatchModeEnabled) {
            return
        }

        selectedOptionIndex = index
        optionButtons.forEachIndexed { buttonIndex, button ->
            val colorRes = if (buttonIndex == index) {
                R.color.quiz_option_selected
            } else {
                R.color.quiz_option_default
            }
            applyButtonStyle(button, colorRes)
        }
    }

    private fun validatePhoneAnswer() {
        if (isWatchModeEnabled) {
            return
        }

        val chosenIndex = selectedOptionIndex
        if (chosenIndex == null) {
            Toast.makeText(this, getString(R.string.select_answer_prompt), Toast.LENGTH_SHORT).show()
            return
        }

        submitAnswer(chosenIndex, AnswerSource.PHONE)
    }

    private fun submitAnswer(chosenIndex: Int, answerSource: AnswerSource) {
        if (answerLocked || currentQuestionIndex !in sessionQuestions.indices) {
            return
        }

        val question = sessionQuestions[currentQuestionIndex]
        if (chosenIndex !in question.options.indices) {
            return
        }

        val isCorrect = chosenIndex == question.correctIndex
        if (isCorrect) {
            correctAnswersCount += 1
        }

        selectedOptionIndex = chosenIndex
        answerLocked = true
        submitButton.isEnabled = false

        if (answerSource == AnswerSource.GARMIN) {
            Toast.makeText(this, getString(R.string.watch_answer_received), Toast.LENGTH_SHORT).show()
        }

        optionButtons.forEach { it.isEnabled = false }
        optionButtons.forEachIndexed { index, button ->
            val colorRes = when {
                index == question.correctIndex -> R.color.quiz_correct
                index == chosenIndex -> R.color.quiz_wrong
                else -> R.color.quiz_option_default
            }
            applyButtonStyle(button, colorRes)
        }

        if (isCorrect) {
            feedbackTextView.text = getString(R.string.correct_answer)
            feedbackTextView.setTextColor(getColorCompat(R.color.quiz_correct))
        } else {
            feedbackTextView.text = getString(
                R.string.wrong_answer,
                question.options[question.correctIndex]
            )
            feedbackTextView.setTextColor(getColorCompat(R.color.quiz_wrong))
        }

        syncCurrentQuestionToGarmin()
        handler.postDelayed(nextQuestionRunnable, FEEDBACK_DELAY_MS)
    }

    private fun advanceAfterAnswer() {
        currentQuestionIndex += 1
        if (currentQuestionIndex >= sessionQuestionCount) {
            showResult()
        } else {
            showCurrentQuestion()
        }
    }

    private fun showResult() {
        val quiz = currentQuiz ?: return
        val percent = (correctAnswersCount * 100) / sessionQuestionCount

        handler.removeCallbacks(nextQuestionRunnable)
        stopTimer()
        stopSpeaking()
        titleTextView.text = getString(R.string.app_name)
        subtitleTextView.text = getString(R.string.result_subtitle)
        menuContainer.visibility = View.GONE
        subcategoryContainer.visibility = View.GONE
        quizContainer.visibility = View.GONE
        resultContainer.visibility = View.VISIBLE

        resultCategoryTextView.text = getString(
            R.string.category_label,
            getQuizLabel(quiz)
        )
        scoreTextView.text = getString(
            R.string.score_format,
            correctAnswersCount,
            sessionQuestionCount
        )
        scoreMessageTextView.text = buildScoreMessage(percent, seriesEndedByTimeout)
        if (isWatchModeEnabled) {
            garminWatchManager.syncResultState(
                category = getQuizLabel(quiz),
                score = correctAnswersCount,
                totalQuestions = sessionQuestionCount,
                timedOut = seriesEndedByTimeout
            )
        }
        scrollToTop()
    }

    private fun buildScoreMessage(percent: Int, timedOut: Boolean): String {
        val messageRes = when {
            percent >= 90 -> R.string.score_message_excellent
            percent >= 70 -> R.string.score_message_good
            percent >= 50 -> R.string.score_message_average
            else -> R.string.score_message_retry
        }
        val prefix = if (timedOut) {
            getString(R.string.time_up_result_prefix) + "\n"
        } else {
            ""
        }
        return prefix + getString(R.string.score_percent_format, percent) + "\n" + getString(messageRes)
    }

    private fun buildSessionQuestions(sourceQuestions: List<QuizQuestion>, targetCount: Int = SESSION_QUESTION_COUNT): MutableList<SessionQuestion> {
        val session = mutableListOf<SessionQuestion>()

        while (session.size < targetCount) {
            sourceQuestions.shuffled().forEach { question ->
                if (session.size < targetCount) {
                    session += question.toSessionQuestion()
                }
            }
        }

        return session
    }

    private fun QuizQuestion.toSessionQuestion(): SessionQuestion {
        val shuffledOptions = options.mapIndexed { index, option ->
            AnswerOption(option, index == correctIndex)
        }.shuffled()

        return SessionQuestion(
            question = question,
            options = shuffledOptions.map { it.text },
            correctIndex = shuffledOptions.indexOfFirst { it.isCorrect }
        )
    }

    private fun loadCustomQuestions(fileName: String): List<QuizQuestion> {
        val parts = fileName.split(":", limit = 3)
        if (parts.size != 3) return emptyList()
        val saved = getSharedPreferences("custom_quizzes", MODE_PRIVATE).getString("questions:${parts[1]}:${parts[2]}", "").orEmpty()
        return saved.lineSequence().filter { it.isNotBlank() }.mapNotNull { row ->
            val cells = row.split("|")
            val correct = cells.getOrNull(5)?.toIntOrNull()?.minus(1) ?: return@mapNotNull null
            if (cells.size != 6 || correct !in 0..3) null else QuizQuestion(cells[0], cells.subList(1, 5), correct)
        }.toList()
    }

    private fun loadAllCategoryQuestions(category: QuestionCategory, allQuestionsFile: String, categoryName: String): List<QuizQuestion> {
        val candidates = category.subcategories
            .filter { it.labelRes != R.string.subcategory_all_questions }
            .flatMap { loadQuestions(it.fileName) }
            .toMutableList()
        if (candidates.isEmpty()) candidates += loadQuestions(allQuestionsFile)

        val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
        prefs.getStringSet("series:$categoryName", emptySet()).orEmpty().forEach { seriesName ->
            candidates += loadCustomQuestions("custom:$categoryName:$seriesName")
        }
        return candidates.distinctBy { listOf(it.question) + it.options + it.correctIndex.toString() }
    }

    private fun loadCustomCategoryQuestions(categoryName: String): List<QuizQuestion> {
        val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
        return prefs.getStringSet("series:$categoryName", emptySet()).orEmpty()
            .flatMap { loadCustomQuestions("custom:$categoryName:$it") }
            .distinctBy { listOf(it.question, it.options.joinToString("|"), it.correctIndex.toString()) }
    }

    private fun loadAllSeriesQuestions(): List<QuizQuestion> {
        val assetFiles = categories.flatMap { category ->
            listOfNotNull(category.fileName) + category.subcategories
                .filter { it.labelRes != R.string.subcategory_all_questions }
                .map { it.fileName }
        }.distinct()
        val questions = assetFiles.flatMap(::loadQuestions).toMutableList()
        val prefs = getSharedPreferences("custom_quizzes", MODE_PRIVATE)
        val customCategories = userCategories() + categories.map { getString(it.labelRes) }
        customCategories.forEach { categoryName ->
            prefs.getStringSet("series:$categoryName", emptySet()).orEmpty().forEach { seriesName ->
                questions += loadCustomQuestions("custom:$categoryName:$seriesName")
            }
        }
        return questions.distinctBy { listOf(it.question, it.options.joinToString("|"), it.correctIndex.toString()) }
    }

    private fun buildRandomUniqueSession(sourceQuestions: List<QuizQuestion>, targetCount: Int): MutableList<SessionQuestion> {
        val shuffled = sourceQuestions.shuffled().take(targetCount).map { it.toSessionQuestion() }.toMutableList()
        if (shuffled.size < targetCount && sourceQuestions.isNotEmpty()) {
            while (shuffled.size < targetCount) shuffled += sourceQuestions.random().toSessionQuestion()
        }
        return shuffled
    }

    private fun loadQuestions(fileName: String): List<QuizQuestion> {
        return runCatching {
            assets.open(fileName).bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines
                    .drop(1)
                    .mapNotNull(::parseQuestionLine)
                    .toList()
            }
        }.getOrDefault(emptyList())
    }

    private fun parseQuestionLine(line: String): QuizQuestion? {
        return QuizCsvParser.parse(line)
    }

    private fun applyButtonStyle(button: MaterialButton, colorRes: Int) {
        button.backgroundTintList = ColorStateList.valueOf(getColorCompat(colorRes))
        button.setTextColor(getColorCompat(R.color.quiz_button_text))
    }

    private fun getColorCompat(colorRes: Int): Int {
        return ContextCompat.getColor(this, colorRes)
    }

    private fun toggleWatchMode() {
        isWatchModeEnabled = !isWatchModeEnabled
        if (isWatchModeEnabled) {
            soundEnabled = true
            garminWatchManager.refreshStatus()
        } else {
            garminWatchManager.syncIdleState()
        }
        updateSoundToggleLabel()
        updateWatchModeButton()
        updateWatchModeStatus()
    }

    private fun updateWatchModeButton() {
        watchModeButton.text = getString(
            if (isWatchModeEnabled) R.string.watch_mode_on else R.string.watch_mode_off
        )
    }

    private fun updateWatchModeStatus() {
        watchModeStatusTextView.text = if (!isWatchModeEnabled) {
            getString(R.string.watch_mode_status_off)
        } else {
            when (garminStatus.messageType) {
                GarminWatchManager.GarminStatusMessage.OFF ->
                    getString(R.string.watch_mode_status_off)

                GarminWatchManager.GarminStatusMessage.INITIALIZING ->
                    getString(R.string.watch_mode_status_initializing)

                GarminWatchManager.GarminStatusMessage.READY ->
                    getString(
                        R.string.watch_mode_status_ready,
                        garminStatus.deviceName ?: getString(R.string.category_tout_melange)
                    )

                GarminWatchManager.GarminStatusMessage.NO_DEVICE ->
                    getString(R.string.watch_mode_status_no_device)

                GarminWatchManager.GarminStatusMessage.SDK_UNAVAILABLE ->
                    getString(R.string.watch_mode_status_sdk_unavailable)

                GarminWatchManager.GarminStatusMessage.APP_MISSING ->
                    getString(R.string.watch_mode_status_app_missing)
            }
        }
    }

    private fun updateQuizInputModeUi() {
        inputModeTextView.text = getString(
            if (isWatchModeEnabled) {
                R.string.watch_mode_garmin_answers
            } else {
                R.string.watch_mode_phone_answers
            }
        )
    }

    private fun syncCurrentQuestionToGarmin() {
        val quiz = currentQuiz
        if (!isWatchModeEnabled || quiz == null || currentQuestionIndex !in sessionQuestions.indices) {
            return
        }

        val question = sessionQuestions[currentQuestionIndex]
        garminWatchManager.syncQuestionState(
            category = getQuizLabel(quiz),
            questionIndex = currentQuestionIndex,
            totalQuestions = sessionQuestionCount,
            question = question.question,
            options = question.options,
            remainingMillis = timeRemainingMillis,
            feedback = feedbackTextView.text.toString(),
            selectedIndex = selectedOptionIndex,
            correctIndex = if (answerLocked) question.correctIndex else null,
            answerLocked = answerLocked
        )
    }

    private fun toggleSound() {
        soundEnabled = !soundEnabled
        updateSoundToggleLabel()

        if (!soundEnabled) {
            stopSpeaking()
        } else if (quizContainer.visibility == View.VISIBLE && currentQuestionIndex in sessionQuestions.indices) {
            speakQuestion(sessionQuestions[currentQuestionIndex].question)
        }
    }

    private fun updateSoundToggleLabel() {
        soundToggleButton.text = getString(
            if (soundEnabled) R.string.sound_on else R.string.sound_off
        )
    }

    private fun speakQuestion(question: String) {
        if (!soundEnabled || !isTtsReady) {
            return
        }

        textToSpeech.stop()
        textToSpeech.speak(question, TextToSpeech.QUEUE_FLUSH, null, "question_$currentQuestionIndex")
    }

    private fun stopSpeaking() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
        }
    }

    private fun startTimer() {
        stopTimer()
        countDownTimer = object : CountDownTimer(SERIES_DURATION_MS, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished
                updateTimerDisplay(millisUntilFinished)
                syncCurrentQuestionToGarmin()
            }

            override fun onFinish() {
                timeRemainingMillis = 0L
                updateTimerDisplay(0L)
                seriesEndedByTimeout = true
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.time_up_message),
                    Toast.LENGTH_SHORT
                ).show()
                showResult()
            }
        }.start()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun updateTimerDisplay(millis: Long) {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        timerTextView.text = getString(R.string.timer_format, minutes, seconds)
    }

    private fun scrollToTop() {
        rootView.post { rootView.smoothScrollTo(0, 0) }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getQuizLabel(quiz: QuizSelection): String {
        if (quiz.fileName.startsWith("customAll:")) return quiz.fileName.removePrefix("customAll:") + " · " + getString(R.string.subcategory_all_questions)
        if (quiz.fileName.startsWith("allCategory:")) return quiz.fileName.removePrefix("allCategory:") + " · " + getString(R.string.subcategory_all_questions)
        if (quiz.fileName.startsWith("custom:")) {
            val customParts = quiz.fileName.split(":", limit = 3)
            return customParts.drop(1).joinToString(" · ")
        }
        val categoryLabel = getString(quiz.category.labelRes)
        val subcategory = quiz.subcategory ?: return categoryLabel
        return getString(R.string.category_with_subcategory, categoryLabel, getString(subcategory.labelRes))
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            isTtsReady = false
            Toast.makeText(this, getString(R.string.speech_unavailable), Toast.LENGTH_SHORT).show()
            return
        }

        val result = textToSpeech.setLanguage(Locale.FRANCE)
        isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED

        if (!isTtsReady) {
            Toast.makeText(this, getString(R.string.speech_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onGarminStatusChanged(status: GarminWatchManager.GarminStatus) {
        garminStatus = status
        updateWatchModeStatus()
    }

    override fun onGarminAnswerReceived(questionIndex: Int, answerIndex: Int) {
        if (!isWatchModeEnabled || quizContainer.visibility != View.VISIBLE) {
            return
        }
        if (questionIndex != currentQuestionIndex) {
            return
        }
        submitAnswer(answerIndex, AnswerSource.GARMIN)
    }

    private data class QuestionCategory(
        val buttonId: Int,
        val labelRes: Int,
        val fileName: String? = null,
        val subcategories: List<QuestionSubcategory> = emptyList()
    )

    private data class QuestionSubcategory(
        val labelRes: Int,
        val fileName: String
    )

    private data class QuizSelection(
        val category: QuestionCategory,
        val subcategory: QuestionSubcategory?,
        val fileName: String
    )

    private data class SessionQuestion(
        val question: String,
        val options: List<String>,
        val correctIndex: Int
    )

    private data class AnswerOption(
        val text: String,
        val isCorrect: Boolean
    )

    private enum class AnswerSource {
        PHONE,
        GARMIN
    }

    companion object {
        private const val SESSION_QUESTION_COUNT = 20
        private const val FEEDBACK_DELAY_MS = 1400L
        private const val SERIES_DURATION_MS = 5 * 60 * 1000L
    }
}
