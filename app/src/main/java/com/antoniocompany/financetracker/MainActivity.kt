package com.antoniocompany.financetracker

import android.content.Intent
import android.os.Bundle
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
import com.antoniocompany.financetracker.ui.formatCurrency
import com.antoniocompany.financetracker.ui.formatMonth
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

        binding.signOutButton.setOnClickListener {
            session.clear()
            goToLogin()
        }

        binding.transactionsList.layoutManager = LinearLayoutManager(this)
        binding.transactionsList.adapter = adapter

        load()
    }

    private fun load() {
        setLoading(true)
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
                    showMessage(getString(R.string.error_load))
                }
            } catch (error: IOException) {
                showMessage(getString(R.string.error_load))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun render(all: List<TransactionDto>) {
        val month = availableMonths(all).lastOrNull()

        if (month == null) {
            binding.totalsRow.visibility = View.GONE
            binding.monthText.visibility = View.GONE
            showMessage(getString(R.string.dashboard_empty))
            return
        }

        val ofMonth = transactionsOfMonth(all, month)
        val summary = summaryOf(ofMonth)

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

    private fun showMessage(message: String?) {
        binding.messageText.text = message.orEmpty()
        binding.messageText.visibility = if (message == null) View.GONE else View.VISIBLE
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
