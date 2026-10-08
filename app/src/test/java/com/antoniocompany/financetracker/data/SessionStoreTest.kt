package com.antoniocompany.financetracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.antoniocompany.financetracker.data.model.TransactionType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionStoreTest {

    private lateinit var context: Context
    private lateinit var store: SessionStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = SessionStore(context)
        store.clear()
    }

    @After
    fun tearDown() = DashboardCache.clear()

    @Test
    fun `starts without session`() {
        assertNull(store.token)
        assertNull(store.email)
        assertFalse(store.hasSession)
    }

    @Test
    fun `token and email survive a new instance`() {
        store.token = "abc"
        store.email = "antonio@example.com"

        val reopened = SessionStore(context)

        assertEquals("abc", reopened.token)
        assertEquals("antonio@example.com", reopened.email)
        assertTrue(reopened.hasSession)
    }

    @Test
    fun `setting null removes the value`() {
        store.token = "abc"
        store.token = null

        assertNull(store.token)
        assertFalse(store.hasSession)
    }

    @Test
    fun `clear removes the session`() {
        store.token = "abc"
        store.email = "antonio@example.com"

        store.clear()

        assertNull(store.token)
        assertNull(store.email)
    }

    @Test
    fun `clear also empties the dashboard cache so another account never sees it`() {
        DashboardCache.transactions = emptyList()
        DashboardCache.selectedMonth = "2026-10"
        DashboardCache.filterType = TransactionType.INCOME
        DashboardCache.filterSearch = "cena"

        store.clear()

        assertNull(DashboardCache.transactions)
        assertNull(DashboardCache.selectedMonth)
        assertNull(DashboardCache.filterType)
        assertEquals("", DashboardCache.filterSearch)
    }
}
