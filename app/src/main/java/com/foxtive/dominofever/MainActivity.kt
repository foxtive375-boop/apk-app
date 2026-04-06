package com.foxtive.dominofever

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.foxtive.dominofever.databinding.ActivityMainBinding
import com.foxtive.dominofever.model.AiDifficulty
import com.foxtive.dominofever.model.GameMode
import com.foxtive.dominofever.model.StartRule
import com.foxtive.dominofever.ui.adapter.DominoHandAdapter
import com.foxtive.dominofever.viewmodel.GameUiState
import com.foxtive.dominofever.viewmodel.GameViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: GameViewModel by viewModels()

    private lateinit var handAdapter: DominoHandAdapter

    private val modeItems = GameMode.entries.toList()
    private val startRuleItems = StartRule.entries.toList()
    private val difficultyItems = AiDifficulty.entries.toList()

    private var lastPromptToken = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        setupHandList()
        setupActions()
        collectState()
    }

    private fun setupSpinners() {
        setupSpinner(
            spinner = binding.spinnerMode,
            labels = modeItems.map { it.title }
        )
        setupSpinner(
            spinner = binding.spinnerStartRule,
            labels = startRuleItems.map {
                when (it) {
                    StartRule.DOUBLE_FIRST -> "С дубля"
                    StartRule.MIN_LOWEST -> "С минимальной"
                }
            }
        )
        setupSpinner(
            spinner = binding.spinnerDifficulty,
            labels = difficultyItems.map { it.name }
        )

        binding.spinnerMode.setSelection(modeItems.indexOf(GameMode.DRAW))
        binding.spinnerStartRule.setSelection(startRuleItems.indexOf(StartRule.DOUBLE_FIRST))
        binding.spinnerDifficulty.setSelection(difficultyItems.indexOf(AiDifficulty.MEDIUM))
    }

    private fun setupSpinner(spinner: android.widget.Spinner, labels: List<String>) {
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
    }

    private fun setupHandList() {
        handAdapter = DominoHandAdapter { index ->
            viewModel.onDominoClicked(index)
        }
        binding.rvHand.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = handAdapter
            itemAnimator = null
        }
    }

    private fun setupActions() {
        binding.btnQuickPlay.setOnClickListener {
            val mode = modeItems[binding.spinnerMode.selectedItemPosition]
            val startRule = startRuleItems[binding.spinnerStartRule.selectedItemPosition]
            val difficulty = difficultyItems[binding.spinnerDifficulty.selectedItemPosition]
            viewModel.startQuickPlay(mode, startRule, difficulty)
        }

        binding.btnTournament.setOnClickListener {
            viewModel.startTournament()
        }

        binding.btnResetCareer.setOnClickListener {
            viewModel.resetCareer()
        }

        binding.btnDrawPass.setOnClickListener {
            viewModel.onDrawOrPassClicked()
        }

        binding.btnNext.setOnClickListener {
            viewModel.onNextClicked()
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: GameUiState) {
        binding.tvTournament.text = state.tournamentText
        binding.tvMode.text = state.modeText
        binding.tvScore.text = state.scoreText
        binding.tvStatus.text = state.statusText

        handAdapter.submit(
            hand = state.hand,
            playableIndexes = state.playableIndexes,
            selectedIndex = state.selectedDominoIndex
        )

        binding.boardView.setTableState(state.table, state.lastPlacedDominoId)
        binding.boardView.setHighlightedBranches(state.highlightedBranchIds)

        binding.btnDrawPass.isEnabled = state.canDrawOrPass
        binding.btnNext.isEnabled = state.canAdvance

        if (state.pendingBranchChoices.size > 1 && state.branchPromptToken != lastPromptToken) {
            lastPromptToken = state.branchPromptToken
            showBranchChoiceDialog(state)
        }

        val transient = state.transientMessage
        if (!transient.isNullOrBlank()) {
            Toast.makeText(this, transient, Toast.LENGTH_SHORT).show()
            viewModel.consumeTransientMessage()
        }
    }

    private fun showBranchChoiceDialog(state: GameUiState) {
        val choices = state.pendingBranchChoices
        if (choices.isEmpty()) return

        val labels = choices.map { option ->
            "Ветка #${option.branchId} (конец ${option.matchedValue})"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Куда поставить кость?")
            .setItems(labels) { _, which ->
                viewModel.placeSelectedDomino(choices[which].branchId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
