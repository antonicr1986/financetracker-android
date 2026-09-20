package com.antoniocompany.financetracker

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.TransactionRepository
import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.TransactionInput
import com.antoniocompany.financetracker.data.model.TransactionType
import com.antoniocompany.financetracker.databinding.ActivityNewTransactionBinding
import com.antoniocompany.financetracker.ui.formatShortDate
import com.antoniocompany.financetracker.ui.isoToUtcMillis
import com.antoniocompany.financetracker.ui.todayIso
import com.antoniocompany.financetracker.ui.utcMillisToIso
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Alta de un movimiento.
 *
 * Pantalla completa y no una hoja inferior: son cinco campos con teclado de por
 * medio, y a pantalla completa no hay que pelearse con el teclado tapando lo
 * que se escribe.
 */
class NewTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewTransactionBinding
    private lateinit var repository: TransactionRepository

    private var categories: List<CategoryDto> = emptyList()
    private var selectedType = TransactionType.EXPENSE
    private var selectedCategory: CategoryDto? = null
    private var selectedDate = todayIso()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionStore(this).hasSession) {
            finish()
            return
        }

        binding = ActivityNewTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = TransactionRepository(ApiClient.get(this))

        binding.typeGroup.check(R.id.expenseButton)
        binding.typeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            selectedType = if (checkedId == R.id.incomeButton) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }

            // La categoria elegida era del tipo anterior: la API rechazaria el
            // movimiento, asi que se descarta y se rellena el desplegable con
            // las del tipo nuevo.
            selectedCategory = null
            binding.categoryInput.setText("", false)
            showCategoriesOfSelectedType()
        }

        showDate()
        binding.dateInput.setOnClickListener { pickDate() }
        binding.saveButton.setOnClickListener { save() }

        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                categories = repository.getCategories()
                showCategoriesOfSelectedType()
            } catch (error: HttpException) {
                showError(getString(R.string.error_categories_failed))
            } catch (error: IOException) {
                showError(getString(R.string.error_categories_failed))
            }
        }
    }

    private fun showCategoriesOfSelectedType() {
        val available = categories.filter { it.type == selectedType }

        binding.categoryInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                available.map { it.name }
            )
        )

        binding.categoryInput.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = available[position]
            showError(null)
        }

        // Sin categorias del tipo elegido no hay nada que ofrecer: se dice por
        // que en lugar de dejar un desplegable vacio que no se abre.
        binding.categoryLayout.helperText = if (available.isEmpty()) {
            getString(R.string.new_transaction_no_categories)
        } else {
            null
        }
    }

    private fun pickDate() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.new_transaction_pick_date)
            .setSelection(isoToUtcMillis(selectedDate))
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = utcMillisToIso(millis)
            showDate()
        }

        picker.show(supportFragmentManager, "date")
    }

    private fun showDate() {
        binding.dateInput.setText(formatShortDate(selectedDate))
    }

    private fun save() {
        val description = binding.descriptionInput.text?.toString()?.trim().orEmpty()

        // La coma es lo que teclea cualquiera en espanol; toDouble() solo
        // entiende el punto.
        val amount = binding.amountInput.text?.toString()
            ?.replace(",", ".")
            ?.toDoubleOrNull()

        val category = selectedCategory

        when {
            description.isEmpty() -> {
                showError(getString(R.string.error_write_description))
                return
            }

            amount == null || amount <= 0 -> {
                showError(getString(R.string.error_amount_positive))
                return
            }

            category == null -> {
                showError(getString(R.string.error_choose_category))
                return
            }
        }

        showError(null)
        setSaving(true)

        lifecycleScope.launch {
            try {
                repository.create(
                    TransactionInput(
                        description = description,
                        amount = amount!!,
                        date = selectedDate,
                        type = selectedType,
                        categoryId = category!!.id
                    )
                )

                Toast.makeText(
                    this@NewTransactionActivity,
                    R.string.new_transaction_saved,
                    Toast.LENGTH_SHORT
                ).show()

                // El panel recarga al volver; se le avisa con el resultado.
                setResult(RESULT_OK)
                finish()
            } catch (error: HttpException) {
                showError(getString(R.string.error_save_failed))
            } catch (error: IOException) {
                showError(getString(R.string.error_save_failed))
            } finally {
                setSaving(false)
            }
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
}
