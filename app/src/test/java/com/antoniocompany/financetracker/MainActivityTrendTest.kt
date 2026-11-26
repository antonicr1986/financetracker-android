package com.antoniocompany.financetracker

import android.content.Context
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.test.core.app.ApplicationProvider
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.DashboardCache
import com.antoniocompany.financetracker.data.FakeApi
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.PagedResult
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType
import com.antoniocompany.financetracker.data.model.TransactionType.EXPENSE
import com.antoniocompany.financetracker.data.model.TransactionType.INCOME
import com.antoniocompany.financetracker.ui.formatCurrency
import com.antoniocompany.financetracker.ui.formatMonth
import com.antoniocompany.financetracker.ui.formatShortMonthYear
import com.google.android.material.chip.ChipGroup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/**
 * Grafico de evolucion mensual del panel, con una API falsa: lo que se ve,
 * el mes resaltado, el detalle al tocar y el plegado.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityTrendTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sessionStore = SessionStore(context)

    private var nextId = 1

    private fun transaction(date: String, amount: Double, type: TransactionType) = TransactionDto(
        id = nextId++, description = "Movimiento", amount = amount, date = date,
        type = type, categoryId = 1, categoryName = "Varios"
    )

    // Diciembre 2025: 1000 de ingresos y 50,25 de gastos.
    // Enero 2026: 1200 de ingresos y 399,75 de gastos.
    private var transactions: List<TransactionDto> = emptyList()

    private val twoMonths = listOf(
        transaction("2025-12-05T00:00:00", 1000.0, INCOME),
        transaction("2025-12-31T00:00:00", 50.25, EXPENSE),
        transaction("2026-01-20T00:00:00", 1200.0, INCOME),
        transaction("2026-01-10T00:00:00", 300.0, EXPENSE),
        transaction("2026-01-25T00:00:00", 99.75, EXPENSE),
    )

    private val api = object : FakeApi() {
        override suspend fun getTransactions(pageNumber: Int, pageSize: Int) =
            PagedResult(transactions, transactions.size, 1, pageSize, 1)

        override suspend fun getBudgets(): List<BudgetDto> = emptyList()
    }

    @Before
    fun setUp() {
        DashboardCache.clear()
        transactions = twoMonths
        sessionStore.token = "jwt-123"
        sessionStore.email = "ana@test.com"
        ApiClient.replaceForTests(api)
    }

    @After
    fun tearDown() {
        ApiClient.replaceForTests(null)
        sessionStore.clear()
        DashboardCache.clear()
    }

    private fun launch(): ActivityController<MainActivity> =
        Robolectric.buildActivity(MainActivity::class.java).setup().also { idle() }

    private fun MainActivity.columns(): List<View> =
        findViewById<LinearLayout>(R.id.trendBars).children.toList()

    private fun View.label(): String = findViewById<TextView>(R.id.trendMonthLabel).text.toString()

    @Test
    fun `the trend card shows one column per month, oldest first, with the range in the header`() {
        val activity = launch().get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.trendCard).visibility)
        assertEquals(
            listOf(formatShortMonthYear("2025-12"), formatShortMonthYear("2026-01")),
            activity.columns().map { it.label() }
        )
        assertEquals(
            "${formatShortMonthYear("2025-12")} – ${formatShortMonthYear("2026-01")}",
            activity.textOf(R.id.trendSummary)
        )
    }

    @Test
    fun `choosing another month chip highlights it in the chart`() {
        val activity = launch().get()

        // Al cargar se elige el mes mas reciente.
        assertEquals(listOf(false, true), activity.columns().map { it.isSelected })

        val chips = activity.findViewById<ChipGroup>(R.id.monthChips)
        chips.children.first { it.tag == "2025-12" }.performClick()
        idle()

        assertEquals(listOf(true, false), activity.columns().map { it.isSelected })
    }

    @Test
    fun `tapping a month shows its exact income and expenses`() {
        val activity = launch().get()

        activity.columns().first().performClick()
        idle()

        assertEquals(
            context.getString(
                R.string.trend_detail,
                formatMonth("2025-12"),
                formatCurrency(1000.0),
                formatCurrency(50.25)
            ),
            activity.textOf(R.id.trendDetail)
        )
    }

    @Test
    fun `the trend card folds and stays folded when the screen is recreated`() {
        val controller = launch()
        controller.get().click(R.id.trendHeader)
        assertFalse(controller.get().isShown(R.id.trendBody))

        controller.recreate()
        idle()

        assertFalse(controller.get().isShown(R.id.trendBody))
    }

    @Test
    fun `with many months each month keeps a readable width and the chart scrolls`() {
        // Un año entero: en un movil no caben 12 meses a lo ancho sin apinarse.
        transactions = (1..12).map { month -> transaction("2026-%02d-10T00:00:00".format(month), 100.0, INCOME) }

        val activity = launch().get()

        val minWidth = (48 * activity.resources.displayMetrics.density).toInt()
        assertEquals(12, activity.columns().size)
        activity.columns().forEach { column -> assertTrue(column.layoutParams.width >= minWidth) }
        assertTrue(activity.findViewById<View>(R.id.trendBars).parent is HorizontalScrollView)
    }
}
