package com.antoniocompany.financetracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.DashboardCache
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.TransactionRepository
import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType
import com.antoniocompany.financetracker.databinding.ItemBudgetBinding
import com.antoniocompany.financetracker.databinding.ActivityMainBinding
import com.antoniocompany.financetracker.domain.BudgetTone
import com.antoniocompany.financetracker.domain.availableMonths
import com.antoniocompany.financetracker.domain.budgetPercentage
import com.antoniocompany.financetracker.domain.budgetTone
import com.antoniocompany.financetracker.domain.budgetsOfMonth
import com.antoniocompany.financetracker.domain.budgetsWithinLimit
import com.antoniocompany.financetracker.domain.categoryNamesOf
import com.antoniocompany.financetracker.domain.filterTransactions
import com.antoniocompany.financetracker.domain.summaryOf
import com.antoniocompany.financetracker.domain.transactionsOfMonth
import com.antoniocompany.financetracker.ui.TransactionAdapter
import com.antoniocompany.financetracker.ui.bind
import com.antoniocompany.financetracker.ui.formatCurrency
import com.antoniocompany.financetracker.ui.formatMonth
import com.antoniocompany.financetracker.ui.formatShortMonth
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Panel del mes mas reciente con datos: totales y lista de movimientos.
 *
 * Se piden todos los movimientos y se agrupa en el dispositivo, igual que hace
 * la version web. Filtrar en el servidor obligaria a una peticion por mes y no
 * dejaria saber de que meses hay datos sin preguntar.
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionStore
    private lateinit var repository: TransactionRepository

    /** Pulsar un movimiento lo abre en la misma pantalla del alta, para editar o borrar. */
    private val adapter = TransactionAdapter { transaction ->
        editTransaction.launch(NewTransactionActivity.editIntent(this, transaction))
    }

    /** Historico completo. Los chips y los totales se derivan de aqui. */
    private var allTransactions: List<TransactionDto> = emptyList()

    /** Todos los meses; se filtran al pintar. Se piden aparte de los movimientos. */
    private var allBudgets: List<BudgetDto> = emptyList()
    private var selectedMonth: String? = null

    /**
     * Filtros de la lista de movimientos, en cliente sobre el mes ya cargado,
     * igual que la web. Se leen de `DashboardCache` al construir la pantalla
     * para sobrevivir al reinicio por tema o idioma (ver `BaseActivity`).
     */
    private var filterType: TransactionType? = DashboardCache.filterType
    private var filterCategory: String? = DashboardCache.filterCategory
    private var filterSearch: String = DashboardCache.filterSearch

    /** "Sin categoría" traducido, para los filtros y para agrupar sin categoría. */
    private lateinit var noCategoryLabel: String

    /**
     * El alta devuelve RESULT_OK cuando ha guardado. Se recarga entonces, y
     * solo entonces: si el usuario se arrepiente y vuelve atras no tiene
     * sentido volver a pedir los mismos datos.
     */
    private val newTransaction = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) load()
    }

    /** Alta o edicion de un presupuesto: al volver con cambios, se recarga. */
    private val budgetForm = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) loadBudgets()
    }

    /** Igual que el alta: si se guardo o se borro, se recarga. */
    private val editTransaction = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) load()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionStore(this)

        if (!session.hasSession) {
            goToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = TransactionRepository(ApiClient.get(this))
        noCategoryLabel = getString(R.string.filters_no_category)

        binding.emailText.text = session.email.orEmpty()

        binding.topBar.bind(this)

        binding.transactionsList.layoutManager = LinearLayoutManager(this)
        binding.transactionsList.adapter = adapter
        // Ya no tiene su propio scroll: ahora es el NestedScrollView de fuera
        // el que mueve la pantalla entera, lista incluida.
        binding.transactionsList.isNestedScrollingEnabled = false

        binding.addButton.setOnClickListener {
            newTransaction.launch(Intent(this, NewTransactionActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener { load(fromSwipe = true) }
        binding.retryButton.setOnClickListener { load() }

        binding.monthChips.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val month = group.findViewById<Chip>(checkedId)?.tag as? String
                ?: return@setOnCheckedStateChangeListener

            // Al reconstruir los chips se marca uno y esto se dispara solo: sin
            // la comprobacion se repintaria el mes que ya se esta viendo.
            if (month != selectedMonth) {
                selectedMonth = month
                DashboardCache.selectedMonth = month
                renderMonth(month)
            }
        }

        // Si la pantalla viene de un cambio de tema o idioma y ya habia datos,
        // se pintan al momento; en cualquier otro caso se piden a la API.
        binding.budgetsHeader.setOnClickListener {
            DashboardCache.budgetsExpanded = !DashboardCache.budgetsExpanded
            applyBudgetsExpanded()
        }
        applyBudgetsExpanded()
        binding.newBudgetButton.setOnClickListener {
            selectedMonth?.let { budgetForm.launch(BudgetActivity.newIntent(this, it)) }
        }

        binding.filtersHeader.setOnClickListener {
            DashboardCache.filtersExpanded = !DashboardCache.filtersExpanded
            applyFiltersExpanded()
        }
        applyFiltersExpanded()
        setupFilters()

        val cached = DashboardCache.transactions
        if (restartedForLook && cached != null) {
            selectedMonth = DashboardCache.selectedMonth
            allBudgets = DashboardCache.budgets.orEmpty()
            render(cached)
        } else {
            load()
        }
    }

    /**
     * `BaseActivity` devuelve aqui el texto que tenian las vistas justo antes
     * de un reinicio por tema o idioma (ver `window.restoreHierarchyState`).
     * Eso vale para un `EditText` de texto libre, pero los desplegables de
     * tipo y categoria no lo son: su texto se calcula siempre a partir del
     * filtro elegido, en el idioma actual. Sin este arreglo, un cambio de
     * idioma dejaba la etiqueta vieja puesta encima ("All" con el resto de
     * la pantalla ya en espanol) y el desplegable con la flecha atascada.
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!::binding.isInitialized) return

        binding.filterTypeInput.dismissDropDown()
        binding.filterTypeInput.setText(typeFilterLabel(filterType), false)

        selectedMonth?.let { month ->
            val allLabel = getString(R.string.filters_all_categories)
            binding.filterCategoryInput.dismissDropDown()
            binding.filterCategoryInput.setText(filterCategory ?: allLabel, false)
        }
    }

    /**
     * `fromSwipe` distingue las dos formas de recargar: al tirar hacia abajo ya
     * hay una rueda girando arriba, y encender ademas la del centro se ve como
     * si la pantalla se reiniciara.
     */
    private fun load(fromSwipe: Boolean = false) {
        if (!fromSwipe) setLoading(true)
        showMessage(null)
        loadBudgets()

        lifecycleScope.launch {
            try {
                render(repository.getAll())
            } catch (error: HttpException) {
                // Aquí un 401 si es una sesion caducada: había token y la API
                // lo ha rechazado. Se borra y se vuelve al acceso.
                if (error.code() == 401) {
                    session.clear()
                    goToLogin(sessionExpired = true)
                } else {
                    showMessage(getString(R.string.error_load), canRetry = true)
                }
            } catch (error: IOException) {
                showMessage(getString(R.string.error_load), canRetry = true)
            } finally {
                setLoading(false)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    /**
     * Los presupuestos van en su propia peticion, en paralelo con los
     * movimientos. Si fallan, el panel sigue funcionando sin la tarjeta: son
     * un complemento, no lo principal. Un 401 lo gestiona la carga de
     * movimientos, que lleva al acceso.
     */
    private fun loadBudgets() {
        lifecycleScope.launch {
            try {
                allBudgets = repository.getBudgets()
                DashboardCache.budgets = allBudgets
            } catch (error: HttpException) {
                allBudgets = emptyList()
            } catch (error: IOException) {
                allBudgets = emptyList()
            }
            selectedMonth?.let { renderBudgets(it) }
        }
    }

    /**
     * Tarjeta de presupuestos del mes. Sin presupuestos se ve igual, con el
     * aviso y el boton para crear uno, como en la web.
     */
    private fun renderBudgets(month: String) {
        val ofMonth = budgetsOfMonth(allBudgets, month)
        binding.budgetsCard.visibility = View.VISIBLE
        binding.budgetsEmpty.visibility = if (ofMonth.isEmpty()) View.VISIBLE else View.GONE
        binding.budgetsSummary.text = if (ofMonth.isEmpty()) {
            ""
        } else {
            getString(R.string.budgets_summary, budgetsWithinLimit(ofMonth), ofMonth.size)
        }

        binding.budgetsList.removeAllViews()
        ofMonth.forEach { budget ->
            val row = ItemBudgetBinding.inflate(layoutInflater, binding.budgetsList, false)
            val percentage = budgetPercentage(budget)
            val (bar, text) = when (budgetTone(percentage)) {
                BudgetTone.OVER -> R.color.budget_bar_over to R.color.budget_text_over
                BudgetTone.WARNING -> R.color.budget_bar_warning to R.color.budget_text_warning
                BudgetTone.ON_TRACK -> R.color.budget_bar_ok to R.color.budget_text_ok
            }

            row.budgetName.text = budget.name
            row.budgetCategory.text =
                budget.categoryName ?: getString(R.string.budgets_all_categories)
            row.budgetSpent.text = getString(
                R.string.budgets_spent_of,
                formatCurrency(budget.spentAmount),
                formatCurrency(budget.amount)
            )
            row.budgetBar.progress = percentage.coerceIn(0, 100)
            row.budgetBar.setIndicatorColor(ContextCompat.getColor(this, bar))
            row.budgetPercent.text = "$percentage%"
            row.budgetPercent.setTextColor(ContextCompat.getColor(this, text))
            row.budgetRemaining.text = if (budget.remainingAmount < 0) {
                getString(R.string.budgets_exceeded, formatCurrency(-budget.remainingAmount))
            } else {
                getString(R.string.budgets_remaining, formatCurrency(budget.remainingAmount))
            }

            row.root.setOnClickListener {
                budgetForm.launch(BudgetActivity.editIntent(this, budget))
            }
            binding.budgetsList.addView(row.root)
        }
    }

    private fun applyBudgetsExpanded() {
        val expanded = DashboardCache.budgetsExpanded
        binding.budgetsBody.visibility = if (expanded) View.VISIBLE else View.GONE
        binding.budgetsChevron.rotation = if (expanded) 0f else 180f
    }

    private fun applyFiltersExpanded() {
        val expanded = DashboardCache.filtersExpanded
        binding.filtersBody.visibility = if (expanded) View.VISIBLE else View.GONE
        binding.filtersChevron.rotation = if (expanded) 0f else 180f
    }

    private fun render(all: List<TransactionDto>) {
        allTransactions = all
        DashboardCache.transactions = all

        val months = availableMonths(all)

        if (months.isEmpty()) {
            binding.monthScroll.visibility = View.GONE
            binding.totalsRow.visibility = View.GONE
            binding.budgetsCard.visibility = View.GONE
            binding.monthText.visibility = View.GONE
            showMessage(getString(R.string.dashboard_empty))
            return
        }

        // Se conserva el mes que se estaba viendo si sigue existiendo; si no,
        // se cae al mas reciente.
        val month = selectedMonth.takeIf { it in months } ?: months.last()
        selectedMonth = month
        DashboardCache.selectedMonth = month

        binding.monthScroll.visibility = View.VISIBLE
        buildMonthChips(months, month)
        renderMonth(month)
    }

    /**
     * Un chip por mes con datos, del mas antiguo al mas reciente, y el elegido
     * marcado. Se reconstruyen enteros en cada carga porque la lista de meses
     * puede cambiar al anadir o quitar movimientos.
     */
    private fun buildMonthChips(months: List<String>, selected: String) {
        binding.monthChips.removeAllViews()

        months.forEach { month ->
            val chip = layoutInflater
                .inflate(R.layout.item_month_chip, binding.monthChips, false) as Chip

            chip.id = View.generateViewId()
            chip.text = formatShortMonth(month)
            chip.tag = month
            chip.isChecked = month == selected

            binding.monthChips.addView(chip)
        }

        // El mes elegido suele ser el ultimo, que nace fuera de pantalla a la
        // derecha. Se espera a que el grupo este medido para poder desplazarlo.
        binding.monthScroll.post {
            val checked = binding.monthChips.findViewById<Chip>(binding.monthChips.checkedChipId)
            if (checked != null) binding.monthScroll.smoothScrollTo(checked.left, 0)
        }
    }

    private fun renderMonth(month: String) {
        val ofMonth = transactionsOfMonth(allTransactions, month)
        val summary = summaryOf(ofMonth)

        showMessage(null)
        binding.monthText.visibility = View.VISIBLE
        binding.totalsRow.visibility = View.VISIBLE
        binding.monthText.text = formatMonth(month)

        binding.incomeText.text = formatCurrency(summary.totalIncome)
        binding.incomeText.setTextColor(ContextCompat.getColor(this, R.color.income))

        binding.expenseText.text = formatCurrency(summary.totalExpense)
        binding.expenseText.setTextColor(ContextCompat.getColor(this, R.color.expense))

        binding.balanceText.text = formatCurrency(summary.balance)
        binding.balanceText.setTextColor(
            if (summary.balance < 0) {
                ContextCompat.getColor(this, R.color.expense)
            } else {
                MaterialColors.getColor(
                    binding.balanceText,
                    com.google.android.material.R.attr.colorOnSurface
                )
            }
        )

        binding.filtersCard.visibility = if (ofMonth.isEmpty()) View.GONE else View.VISIBLE
        if (ofMonth.isNotEmpty()) updateCategoryFilterOptions(month, ofMonth)
        applyFilters(month)
        renderBudgets(month)
    }

    /**
     * Se llama a los tres cambios (buscador, tipo, categoria) y cada vez que
     * se pinta un mes. La categoria puede no existir en el mes que se ve: se
     * cae a "todas" en vez de reiniciar el filtro desde un efecto, que es lo
     * que hacia la web para no disparar el aviso del linter.
     */
    private fun applyFilters(month: String) {
        val ofMonth = transactionsOfMonth(allTransactions, month)
        val filtered = filterTransactions(ofMonth, filterType, filterCategory, filterSearch, noCategoryLabel)

        val hasFilters = filterType != null || filterCategory != null || filterSearch.isNotBlank()
        binding.filterClearButton.visibility = if (hasFilters) View.VISIBLE else View.GONE

        // Siempre visible, en una sola linea (maxLines+ellipsize en el layout):
        // es lo unico que se ve del todo cuando la tarjeta esta plegada, asi
        // que tiene que decir algo aunque no haya ningun filtro puesto.
        binding.filterShowingText.visibility = View.VISIBLE
        binding.filterShowingText.text = if (hasFilters) {
            getString(R.string.dashboard_showing, filtered.size, ofMonth.size)
        } else if (ofMonth.size == 1) {
            getString(R.string.dashboard_movement_count_one)
        } else {
            getString(R.string.dashboard_movement_count, ofMonth.size)
        }

        binding.filterNoMatchesText.visibility =
            if (ofMonth.isNotEmpty() && filtered.isEmpty()) View.VISIBLE else View.GONE

        adapter.submitList(filtered)
    }

    /**
     * Reconstruye el desplegable de categoria con las que hay en el mes que
     * se ve. Se reconstruye en cada mes porque las categorias disponibles
     * cambian con el.
     */
    private fun updateCategoryFilterOptions(month: String, ofMonth: List<TransactionDto>) {
        val names = categoryNamesOf(ofMonth, noCategoryLabel)
        val allLabel = getString(R.string.filters_all_categories)

        binding.filterCategoryInput.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf(allLabel) + names)
        )

        if (filterCategory != null && filterCategory !in names) {
            filterCategory = null
            DashboardCache.filterCategory = null
        }

        binding.filterCategoryInput.setText(filterCategory ?: allLabel, false)
        binding.filterCategoryInput.setOnItemClickListener { _, _, position, _ ->
            filterCategory = if (position == 0) null else names[position - 1]
            DashboardCache.filterCategory = filterCategory
            applyFilters(month)
        }
    }

    /** Texto del filtro de tipo para el valor actual: "Todos", "Ingresos" o "Gastos". */
    private fun typeFilterLabel(type: TransactionType?): String = when (type) {
        TransactionType.INCOME -> getString(R.string.dashboard_income)
        TransactionType.EXPENSE -> getString(R.string.dashboard_expenses)
        null -> getString(R.string.filters_all_types)
    }

    /** Conecta el buscador y los dos desplegables. La categoria se rellena por mes (ver renderMonth). */
    private fun setupFilters() {
        binding.filterSearchInput.setText(filterSearch)
        binding.filterSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(editable: Editable?) {
                filterSearch = editable?.toString().orEmpty()
                DashboardCache.filterSearch = filterSearch
                selectedMonth?.let { applyFilters(it) }
            }
        })

        binding.filterTypeInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf(
                    getString(R.string.filters_all_types),
                    getString(R.string.dashboard_income),
                    getString(R.string.dashboard_expenses)
                )
            )
        )
        binding.filterTypeInput.setText(typeFilterLabel(filterType), false)
        binding.filterTypeInput.setOnItemClickListener { _, _, position, _ ->
            filterType = when (position) {
                1 -> TransactionType.INCOME
                2 -> TransactionType.EXPENSE
                else -> null
            }
            DashboardCache.filterType = filterType
            selectedMonth?.let { applyFilters(it) }
        }

        binding.filterClearButton.setOnClickListener {
            filterType = null
            filterCategory = null
            filterSearch = ""
            DashboardCache.filterType = null
            DashboardCache.filterCategory = null
            DashboardCache.filterSearch = ""

            binding.filterSearchInput.setText("")
            binding.filterTypeInput.setText(getString(R.string.filters_all_types), false)

            selectedMonth?.let { month ->
                updateCategoryFilterOptions(month, transactionsOfMonth(allTransactions, month))
                applyFilters(month)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    /**
     * `canRetry` separa los dos mensajes que puede ver el usuario: un mes sin
     * movimientos no es un fallo y no debe ofrecer un boton de reintentar, que
     * sugeriria que algo ha ido mal.
     */
    private fun showMessage(message: String?, canRetry: Boolean = false) {
        binding.messageText.text = message.orEmpty()
        binding.messageText.visibility = if (message == null) View.GONE else View.VISIBLE
        binding.retryButton.visibility = if (canRetry) View.VISIBLE else View.GONE
    }

    private fun goToLogin(sessionExpired: Boolean = false) {
        val intent = Intent(this, LoginActivity::class.java)
        if (sessionExpired) intent.putExtra(EXTRA_SESSION_EXPIRED, true)
        startActivity(intent)
        finish()
    }

    companion object {
        /**
         * Distingue un 401 real (sesion caducada) de un cierre de sesion
         * normal, para que el login pueda avisar con el mensaje adecuado.
         */
        const val EXTRA_SESSION_EXPIRED = "com.antoniocompany.financetracker.SESSION_EXPIRED"
    }
}
