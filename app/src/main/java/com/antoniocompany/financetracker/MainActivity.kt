package com.antoniocompany.financetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.TransactionRepository
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.databinding.ActivityMainBinding
import com.antoniocompany.financetracker.domain.availableMonths
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
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionStore
    private lateinit var repository: TransactionRepository

    private val adapter = TransactionAdapter()

    /** Historico completo. Los chips y los totales se derivan de aqui. */
    private var allTransactions: List<TransactionDto> = emptyList()
    private var selectedMonth: String? = null

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

        binding.emailText.text = session.email.orEmpty()

        binding.topBar.bind(this)

        binding.transactionsList.layoutManager = LinearLayoutManager(this)
        binding.transactionsList.adapter = adapter

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
                renderMonth(month)
            }
        }

        load()
    }

    /**
     * `fromSwipe` distingue las dos formas de recargar: al tirar hacia abajo ya
     * hay una rueda girando arriba, y encender ademas la del centro se ve como
     * si la pantalla se reiniciara.
     */
    private fun load(fromSwipe: Boolean = false) {
        if (!fromSwipe) setLoading(true)
        showMessage(null)

        lifecycleScope.launch {
            try {
                render(repository.getAll())
            } catch (error: HttpException) {
                // Aqui un 401 si es una sesion caducada: habia token y la API
                // lo ha rechazado. Se borra y se vuelve al acceso.
                if (error.code() == 401) {
                    session.clear()
                    goToLogin()
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

    private fun render(all: List<TransactionDto>) {
        allTransactions = all

        val months = availableMonths(all)

        if (months.isEmpty()) {
            binding.monthScroll.visibility = View.GONE
            binding.totalsRow.visibility = View.GONE
            binding.monthText.visibility = View.GONE
            showMessage(getString(R.string.dashboard_empty))
            return
        }

        // Se conserva el mes que se estaba viendo si sigue existiendo; si no,
        // se cae al mas reciente.
        val month = selectedMonth.takeIf { it in months } ?: months.last()
        selectedMonth = month

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

        adapter.submitList(ofMonth)
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

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
