package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.TransactionType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardCacheTest {

    @After
    fun tearDown() = DashboardCache.clear()

    @Test
    fun `clear restores every field to its default`() {
        DashboardCache.transactions = emptyList()
        DashboardCache.selectedMonth = "2026-10"
        DashboardCache.budgets = emptyList()
        DashboardCache.budgetsExpanded = false
        DashboardCache.filterType = TransactionType.EXPENSE
        DashboardCache.filterCategory = "Ocio"
        DashboardCache.filterSearch = "cine"
        DashboardCache.filtersExpanded = true
        DashboardCache.breakdownExpanded = false

        DashboardCache.clear()

        assertNull(DashboardCache.transactions)
        assertNull(DashboardCache.selectedMonth)
        assertNull(DashboardCache.budgets)
        assertTrue(DashboardCache.budgetsExpanded)
        assertNull(DashboardCache.filterType)
        assertNull(DashboardCache.filterCategory)
        assertEquals("", DashboardCache.filterSearch)
        assertFalse(DashboardCache.filtersExpanded)
        assertTrue(DashboardCache.breakdownExpanded)
    }
}
