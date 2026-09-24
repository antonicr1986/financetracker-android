package com.antoniocompany.financetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.TransactionRepository
import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.BudgetInput
import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.TransactionType
import com.antoniocompany.financetracker.databinding.ActivityBudgetBinding
import com.antoniocompany.financetracker.domain.formatAmountForInput
import com.antoniocompany.financetracker.domain.monthKeyOf
import com.antoniocompany.financetracker.domain.monthKeysAround
import com.antoniocompany.financetracker.domain.parseAmount
import com.antoniocompany.financetracker.ui.LanguagePreference
import com.antoniocompany.financetracker.ui.bind
import com.antoniocompany.financetracker.ui.formatMonth
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Alta y edicion de un presupuesto, como BudgetDialog en la web.
 *
 * Mismos campos que la API: nombre, limite, mes, tipo y categoria (o todas las
 * del tipo). Lo gastado no se toca aqui: lo recalcula la API al volver al panel.
 */
class BudgetActivity : BaseActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private lateinit var repository: TransactionRepository

    private var categories: List<CategoryDto> = emptyList()
    private var selectedType = TransactionType.EXPENSE
    private lateinit var selectedMonth: String

    /** null = todas las categorias del tipo. */
    private var selectedCategoryId: Int? = null

    private val editingId: Int? by lazy {
        intent.getIntExtra(EXTRA_ID, -1).takeIf { it != -1 }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionStore(this).hasSession) {
            finish()
            return
        }

        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.bind(this)

        repository = TransactionRepository(ApiClient.get(this))

        selectedMonth = intent.getStringExtra(EXTRA_MONTH) ?: monthKeyOf(2026, 1)

        if (editingId != null) {
            binding.titleText.setText(R.string.budget_title_edit)
            if (savedInstanceState == null) prefillFromIntent()
        }

        // Lo elegido antes de un reinicio (tema, idioma o giro del movil).
        savedInstanceState?.let { state ->
            state.getString(KEY_MONTH)?.let { selectedMonth = it }
            if (state.getString(KEY_TYPE) == TransactionType.INCOME.name) {
                selectedType = TransactionType.INCOME
            }
            selectedCategoryId = state.getInt(KEY_CATEGORY, -1).takeIf { it != -1 }
        }

        binding.typeGroup.check(
            if (selectedType == TransactionType.INCOME) R.id.incomeButton else R.id.expenseButton
        )
        binding.typeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedType =
                if (checkedId == R.id.incomeButton) TransactionType.INCOME else TransactionType.EXPENSE
            // La categoria era del otro tipo: la API responderia
            // category_type_mismatch. Se vuelve a "todas".
            selectedCategoryId = null
            showCategories()
        }

        showMonths()
        binding.saveButton.setOnClickListener { save() }

        showCategories()
        loadCategories()
    }

    private fun prefillFromIntent() {
        binding.nameInput.setText(intent.getStringExtra(EXTRA_NAME))
        binding.amountInput.setText(
            formatAmountForInput(
                intent.getDoubleExtra(EXTRA_AMOUNT, 0.0),
                decimalComma = LanguagePreference.current() == LanguagePreference.SPANISH
            )
        )
        if (intent.getStringExtra(EXTRA_TYPE) == TransactionType.INCOME.name) {
            selectedType = TransactionType.INCOME
        }
        selectedCategoryId = intent.getIntExtra(EXTRA_CATEGORY, -1).takeIf { it != -1 }
    }

    /** Un año atras y uno adelante del mes del presupuesto. */
    private fun showMonths() {
        val months = monthKeysAround(selectedMonth)
        binding.monthInput.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, months.map { formatMonth(it) })
        )
        binding.monthInput.setText(formatMonth(selectedMonth), false)
        binding.monthInput.setOnItemClickListener { _, _, position, _ ->
            selectedMonth = months[position]
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                categories = repository.getCategories()
                showCategories()
            } catch (error: HttpException) {
                showError(getString(R.string.error_categories_failed))
            } catch (error: IOException) {
                showError(getString(R.string.error_categories_failed))
            }
        }
    }

    /** "Todas las categorias" primero, despues las del tipo elegido. */
    private fun showCategories() {
        val ofType = categories.filter { it.type == selectedType }
        val options: List<CategoryDto?> = listOf(null) + ofType
        val labels = options.map { it?.name ?: getString(R.string.budgets_all_categories) }

        binding.categoryInput.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        )
        binding.categoryInput.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = options[position]?.id
        }

        // Si la elegida aun no ha llegado (o ya no existe), se ensena "todas".
        val current = ofType.firstOrNull { it.id == selectedCategoryId }
        if (current == null && categories.isNotEmpty()) selectedCategoryId = null
        binding.categoryInput.setText(
            current?.name ?: getString(R.string.budgets_all_categories),
            false
        )
    }

    private fun save() {
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val amount = parseAmount(binding.amountInput.text?.toString())

        when {
            name.isEmpty() -> return showError(getString(R.string.error_write_budget_name))
            amount == null || amount <= 0 -> return showError(getString(R.string.error_amount_positive))
        }

        val input = BudgetInput(
            name = name,
            amount = amount!!,
            month = selectedMonth.drop(5).take(2).toInt(),
            year = selectedMonth.take(4).toInt(),
            type = selectedType,
            categoryId = selectedCategoryId
        )
        val id = editingId

        showError(null)
        setSaving(true)

        lifecycleScope.launch {
            try {
                if (id == null) repository.createBudget(input) else repository.updateBudget(id, input)
                Toast.makeText(
                    this@BudgetActivity,
                    if (id == null) R.string.budget_saved else R.string.budget_updated,
                    Toast.LENGTH_SHORT
                ).show()
                setResult(RESULT_OK)
                finish()
            } catch (error: HttpException) {
                showError(getString(describe(error)))
            } catch (error: IOException) {
                showError(getString(R.string.error_save_budget_failed))
            } finally {
                setSaving(false)
            }
        }
    }

    private fun describe(error: HttpException): Int {
        if (error.code() == 404) return R.string.error_budget_gone
        val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        return if (body.contains("category_not_found") || body.contains("category_type_mismatch")) {
            R.string.error_budget_category_invalid
        } else {
            R.string.error_save_budget_failed
        }
    }

    private fun setSaving(saving: Boolean) {
        binding.saveButton.isEnabled = !saving
        binding.saveButton.setText(
            if (saving) R.string.new_transaction_saving else R.string.new_transaction_save
        )
    }

    private fun showError(message: String?) {
        binding.errorText.text = message.orEmpty()
        binding.errorText.visibility = if (message == null) View.GONE else View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_MONTH, selectedMonth)
        outState.putString(KEY_TYPE, selectedType.name)
        selectedCategoryId?.let { outState.putInt(KEY_CATEGORY, it) }
    }

    companion object {
        private const val KEY_MONTH = "month"
        private const val KEY_TYPE = "type"
        private const val KEY_CATEGORY = "category"

        private const val EXTRA_ID = "id"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_AMOUNT = "amount"
        private const val EXTRA_MONTH = "month"
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_CATEGORY = "categoryId"

        /** Alta, con el mes que se esta viendo en el panel ya elegido. */
        fun newIntent(context: Context, month: String): Intent =
            Intent(context, BudgetActivity::class.java).putExtra(EXTRA_MONTH, month)

        /** Edicion: el presupuesto entero, ya cargado en el panel. */
        fun editIntent(context: Context, budget: BudgetDto): Intent =
            Intent(context, BudgetActivity::class.java)
                .putExtra(EXTRA_ID, budget.id)
                .putExtra(EXTRA_NAME, budget.name)
                .putExtra(EXTRA_AMOUNT, budget.amount)
                .putExtra(EXTRA_MONTH, monthKeyOf(budget.year, budget.month))
                .putExtra(EXTRA_TYPE, budget.type.name)
                .putExtra(EXTRA_CATEGORY, budget.categoryId ?: -1)
    }
}
