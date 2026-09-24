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
import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.CategoryInput
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionInput
import com.antoniocompany.financetracker.data.model.TransactionType
import com.antoniocompany.financetracker.databinding.ActivityNewTransactionBinding
import com.antoniocompany.financetracker.databinding.DialogNewCategoryBinding
import com.antoniocompany.financetracker.domain.formatAmountForInput
import com.antoniocompany.financetracker.domain.isDuplicateCategoryName
import com.antoniocompany.financetracker.domain.parseAmount
import com.antoniocompany.financetracker.ui.LanguagePreference
import com.antoniocompany.financetracker.ui.bind
import com.antoniocompany.financetracker.ui.formatShortDate
import com.antoniocompany.financetracker.ui.isoToUtcMillis
import com.antoniocompany.financetracker.ui.todayIso
import com.antoniocompany.financetracker.ui.utcMillisToIso
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
class NewTransactionActivity : BaseActivity() {

    private lateinit var binding: ActivityNewTransactionBinding
    private lateinit var repository: TransactionRepository

    private var categories: List<CategoryDto> = emptyList()
    private var selectedType = TransactionType.EXPENSE
    private var selectedCategory: CategoryDto? = null
    private var selectedDate = todayIso()

    /** Categoria a recuperar cuando lleguen las categorias (tras reinicio). */
    private var pendingCategoryId: Int? = null

    /**
     * Id del movimiento que se edita, o null si es un alta. La misma pantalla
     * sirve para las dos cosas: los campos y las reglas son identicos.
     */
    private val editingId: Int? by lazy {
        intent.getIntExtra(EXTRA_ID, -1).takeIf { it != -1 }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionStore(this).hasSession) {
            finish()
            return
        }

        binding = ActivityNewTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.bind(this)

        repository = TransactionRepository(ApiClient.get(this))

        if (editingId != null) {
            binding.titleText.setText(R.string.edit_transaction_title)
            binding.deleteButton.visibility = View.VISIBLE
            binding.deleteButton.setOnClickListener { confirmDelete() }

            // Solo la primera vez: tras un reinicio los campos ya traen lo que
            // el usuario hubiera cambiado, y eso manda sobre el original.
            if (savedInstanceState == null) prefillFromIntent()
        }

        // Lo elegido antes de un reinicio (tema, idioma o giro del movil).
        // Los textos de los campos los restaura Android solo; esto no.
        savedInstanceState?.let { state ->
            state.getString(KEY_DATE)?.let { selectedDate = it }
            if (state.getString(KEY_TYPE) == TransactionType.INCOME.name) {
                selectedType = TransactionType.INCOME
            }
            pendingCategoryId = state.getInt(KEY_CATEGORY, -1).takeIf { it != -1 }
        }

        binding.typeGroup.check(
            if (selectedType == TransactionType.INCOME) R.id.incomeButton else R.id.expenseButton
        )
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
        binding.newCategoryButton.setOnClickListener { showNewCategoryDialog() }

        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                categories = repository.getCategories()
                showCategoriesOfSelectedType()
                pendingCategoryId?.let { id ->
                    selectedCategory = categories.firstOrNull { it.id == id && it.type == selectedType }
                    selectedCategory?.let { binding.categoryInput.setText(it.name, false) }
                    pendingCategoryId = null
                }
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

        val amount = parseAmount(binding.amountInput.text?.toString())

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

        val input = TransactionInput(
            description = description,
            amount = amount!!,
            date = selectedDate,
            type = selectedType,
            categoryId = category!!.id
        )
        val id = editingId

        lifecycleScope.launch {
            try {
                if (id == null) repository.create(input) else repository.update(id, input)

                Toast.makeText(
                    this@NewTransactionActivity,
                    if (id == null) R.string.new_transaction_saved else R.string.edit_transaction_saved,
                    Toast.LENGTH_SHORT
                ).show()

                // El panel recarga al volver; se le avisa con el resultado.
                setResult(RESULT_OK)
                finish()
            } catch (error: HttpException) {
                // 404 al editar: lo borraron desde otro sitio (la web, otro movil).
                showError(
                    getString(
                        if (error.code() == 404) R.string.error_transaction_gone
                        else R.string.error_save_failed
                    )
                )
            } catch (error: IOException) {
                showError(getString(R.string.error_save_failed))
            } finally {
                setSaving(false)
            }
        }
    }

    /**
     * Dialogo para crear una categoria del tipo elegido. El boton "Crear" no
     * cierra el dialogo por su cuenta: si falta el nombre o la API lo rechaza,
     * el error se ve dentro del dialogo y lo escrito no se pierde.
     */
    private fun showNewCategoryDialog() {
        val dialogBinding = DialogNewCategoryBinding.inflate(layoutInflater)
        val type = selectedType

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(
                if (type == TransactionType.INCOME) R.string.new_category_title_income
                else R.string.new_category_title_expense
            )
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.edit_transaction_cancel, null)
            .setPositiveButton(R.string.new_category_create, null)
            .create()

        dialog.setOnShowListener {
            dialogBinding.nameInput.requestFocus()
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.nameInput.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    dialogBinding.nameLayout.error = getString(R.string.error_write_category_name)
                    return@setOnClickListener
                }
                // Evita duplicados evidentes sin gastar una peticion.
                if (isDuplicateCategoryName(categories, name, type)) {
                    dialogBinding.nameLayout.error = getString(R.string.error_category_exists)
                    return@setOnClickListener
                }
                dialogBinding.nameLayout.error = null
                createCategory(name, type, dialog, dialogBinding)
            }
        }
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    private fun createCategory(
        name: String,
        type: TransactionType,
        dialog: androidx.appcompat.app.AlertDialog,
        dialogBinding: DialogNewCategoryBinding
    ) {
        val createButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
        createButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val created = repository.createCategory(CategoryInput(name, type))
                categories = categories + created

                // Queda elegida si sigue siendo del tipo que hay en pantalla.
                if (created.type == selectedType) {
                    showCategoriesOfSelectedType()
                    selectedCategory = created
                    binding.categoryInput.setText(created.name, false)
                    showError(null)
                }
                Toast.makeText(
                    this@NewTransactionActivity,
                    R.string.new_category_created,
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            } catch (error: HttpException) {
                dialogBinding.nameLayout.error = getString(R.string.error_create_category_failed)
            } catch (error: IOException) {
                dialogBinding.nameLayout.error = getString(R.string.error_create_category_failed)
            } finally {
                createButton.isEnabled = true
            }
        }
    }

    /** Rellena el formulario con el movimiento que se va a editar. */
    private fun prefillFromIntent() {
        binding.descriptionInput.setText(intent.getStringExtra(EXTRA_DESCRIPTION))

        binding.amountInput.setText(
            formatAmountForInput(
                intent.getDoubleExtra(EXTRA_AMOUNT, 0.0),
                decimalComma = LanguagePreference.current() == LanguagePreference.SPANISH
            )
        )

        intent.getStringExtra(EXTRA_DATE)?.let { selectedDate = it.take(10) }
        if (intent.getStringExtra(EXTRA_TYPE) == TransactionType.INCOME.name) {
            selectedType = TransactionType.INCOME
        }
        pendingCategoryId = intent.getIntExtra(EXTRA_CATEGORY, -1).takeIf { it != -1 }
    }

    /** Borrar no se puede deshacer: se pregunta antes. */
    private fun confirmDelete() {
        val description = binding.descriptionInput.text?.toString().orEmpty()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_transaction_confirm_title)
            .setMessage(getString(R.string.edit_transaction_confirm_message, description))
            .setNegativeButton(R.string.edit_transaction_cancel, null)
            .setPositiveButton(R.string.edit_transaction_confirm_delete) { _, _ -> delete() }
            .show()
    }

    private fun delete() {
        val id = editingId ?: return
        showError(null)
        setDeleting(true)

        lifecycleScope.launch {
            try {
                repository.delete(id)
                finishAfterDelete(R.string.edit_transaction_deleted)
            } catch (error: HttpException) {
                if (error.code() == 404) {
                    // Ya estaba borrado: el resultado es el que se queria.
                    finishAfterDelete(R.string.error_transaction_gone)
                } else {
                    showError(getString(R.string.error_delete_failed))
                }
            } catch (error: IOException) {
                showError(getString(R.string.error_delete_failed))
            } finally {
                setDeleting(false)
            }
        }
    }

    private fun finishAfterDelete(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun setDeleting(deleting: Boolean) {
        binding.deleteButton.isEnabled = !deleting
        binding.saveButton.isEnabled = !deleting
        binding.deleteButton.setText(
            if (deleting) R.string.edit_transaction_deleting else R.string.edit_transaction_delete
        )
    }

    private fun setSaving(saving: Boolean) {
        binding.saveButton.isEnabled = !saving
        binding.deleteButton.isEnabled = !saving
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
        outState.putString(KEY_DATE, selectedDate)
        outState.putString(KEY_TYPE, selectedType.name)
        selectedCategory?.let { outState.putInt(KEY_CATEGORY, it.id) }
    }

    companion object {
        private const val KEY_DATE = "date"
        private const val KEY_TYPE = "type"
        private const val KEY_CATEGORY = "category"

        private const val EXTRA_ID = "id"
        private const val EXTRA_DESCRIPTION = "description"
        private const val EXTRA_AMOUNT = "amount"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_CATEGORY = "categoryId"

        /**
         * Abre la pantalla en modo edicion. Se pasa el movimiento entero en
         * lugar de solo el id: ya esta cargado en el panel, y asi el formulario
         * aparece relleno al instante, sin otra peticion a la API.
         */
        fun editIntent(context: Context, transaction: TransactionDto): Intent =
            Intent(context, NewTransactionActivity::class.java)
                .putExtra(EXTRA_ID, transaction.id)
                .putExtra(EXTRA_DESCRIPTION, transaction.description)
                .putExtra(EXTRA_AMOUNT, transaction.amount)
                .putExtra(EXTRA_DATE, transaction.date)
                .putExtra(EXTRA_TYPE, transaction.type.name)
                .putExtra(EXTRA_CATEGORY, transaction.categoryId ?: -1)
    }
}
