package com.example.countries.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.countries.Country
import com.example.countries.CountryViewModel
import com.example.countries.CountriesUiState
import com.example.countries.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

class QuizFragment : Fragment() {

    private lateinit var viewModel: CountryViewModel
    private var allCountries = listOf<Country>()
    private var questions = mutableListOf<QuizQuestion>()
    private var currentQuestionIndex = 0
    private var score = 0

    private lateinit var quizQuestion: TextView
    private lateinit var quizScore: TextView
    private lateinit var quizProgress: ProgressBar
    private lateinit var questionNumberText: TextView
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var btnOption3: Button
    private lateinit var btnOption4: Button
    private lateinit var btnNext: Button
    private lateinit var btnRestart: Button
    private lateinit var resultContainer: View
    private lateinit var resultText: TextView

    data class QuizQuestion(
        val question: String,
        val options: List<String>,
        val correctAnswer: String
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CountryViewModel::class.java]

        quizQuestion = view.findViewById(R.id.quizQuestion)
        quizScore = view.findViewById(R.id.scoreText)
        quizProgress = view.findViewById(R.id.quizProgress)
        questionNumberText = view.findViewById(R.id.questionNumberText)
        resultText = view.findViewById(R.id.resultText)
        btnOption1 = view.findViewById(R.id.btnOption1)
        btnOption2 = view.findViewById(R.id.btnOption2)
        btnOption3 = view.findViewById(R.id.btnOption3)
        btnOption4 = view.findViewById(R.id.btnOption4)
        btnNext = view.findViewById(R.id.btnNext)
        btnRestart = view.findViewById(R.id.btnRestart)
        resultContainer = view.findViewById(R.id.resultContainer)

        btnOption1.setOnClickListener { checkAnswer(btnOption1.text.toString()) }
        btnOption2.setOnClickListener { checkAnswer(btnOption2.text.toString()) }
        btnOption3.setOnClickListener { checkAnswer(btnOption3.text.toString()) }
        btnOption4.setOnClickListener { checkAnswer(btnOption4.text.toString()) }

        btnNext.setOnClickListener {
            currentQuestionIndex++
            if (currentQuestionIndex < questions.size) {
                displayQuestion()
            } else {
                showResult()
            }
        }

        btnRestart.setOnClickListener {
            currentQuestionIndex = 0
            score = 0
            generateQuestions()
            displayQuestion()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CountriesUiState.Success -> {
                        allCountries = state.countries
                        if (questions.isEmpty()) {
                            generateQuestions()
                            displayQuestion()
                        }
                    }
                    is CountriesUiState.Error -> {
                        quizQuestion.text = "Unable to load country data. Please go back and retry."
                        setOptionsEnabled(false)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun generateQuestions() {
        if (allCountries.size < 4) return
        questions.clear()

        val capitalQuestions = allCountries.filter { it.capital != "Not available" }.shuffled().take(5)
        capitalQuestions.forEach { country ->
            val wrong = allCountries.filter { it.commonName != country.commonName }
                .shuffled().take(3).map { it.commonName }
            val options = (wrong + country.commonName).shuffled()
            questions.add(QuizQuestion(
                "What is the capital of ${country.commonName}?",
                options,
                country.commonName
            ))
        }

        val continentQuestions = allCountries.filter { it.continents.isNotEmpty() }.shuffled().take(5)
        continentQuestions.forEach { country ->
            val correct = country.continents.firstOrNull() ?: "Unknown"
            val allContinents = listOf("Africa", "Asia", "Europe", "North America", "South America", "Oceania")
            val wrong = allContinents.filter { it != correct }.shuffled().take(3)
            val options = (wrong + correct).shuffled()
            questions.add(QuizQuestion(
                "Which continent is ${country.commonName} in?",
                options,
                correct
            ))
        }

        questions = questions.shuffled().take(10).toMutableList()
    }

    private fun displayQuestion() {
        if (questions.isEmpty() || currentQuestionIndex >= questions.size) return
        val q = questions[currentQuestionIndex]

        quizQuestion.text = q.question
        quizScore.text = "Score: $score / ${questions.size}"
        quizProgress.max = questions.size
        quizProgress.progress = currentQuestionIndex + 1
        questionNumberText.text = "Question ${currentQuestionIndex + 1} of ${questions.size}"

        btnOption1.visibility = View.VISIBLE
        btnOption2.visibility = View.VISIBLE
        btnOption3.visibility = View.VISIBLE
        btnOption4.visibility = View.VISIBLE
        btnOption1.text = q.options[0]
        btnOption2.text = q.options[1]
        btnOption3.text = q.options[2]
        btnOption4.text = q.options[3]

        resultContainer.visibility = View.GONE
        btnNext.visibility = View.GONE
        btnRestart.visibility = View.GONE
        setOptionsEnabled(true)
    }

    private fun checkAnswer(answer: String) {
        val q = questions[currentQuestionIndex]
        if (answer == q.correctAnswer) {
            score++
        }
        quizScore.text = "Score: $score / ${questions.size}"
        setOptionsEnabled(false)
        btnNext.visibility = View.VISIBLE
    }

    private fun setOptionsEnabled(enabled: Boolean) {
        btnOption1.isEnabled = enabled
        btnOption2.isEnabled = enabled
        btnOption3.isEnabled = enabled
        btnOption4.isEnabled = enabled
    }

    private fun showResult() {
        resultContainer.visibility = View.VISIBLE
        btnOption1.visibility = View.GONE
        btnOption2.visibility = View.GONE
        btnOption3.visibility = View.GONE
        btnOption4.visibility = View.GONE
        btnNext.visibility = View.GONE
        btnRestart.visibility = View.VISIBLE

        resultText.text = "Quiz Complete!\nScore: $score / ${questions.size}"
        quizQuestion.text = "Results"
        questionNumberText.text = ""
        quizProgress.progress = questions.size
    }
}
