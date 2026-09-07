package com.example.supermetrics

import com.example.supermetrics.model.CartItem
import com.example.supermetrics.model.ScannedCandidate
import com.example.supermetrics.ui.CartViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class CartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CartViewModel

    @Before
    fun setUp() {
        viewModel = CartViewModel()
    }

    @Test
    fun initialState_isEmpty() {
        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())
        assertEquals(0, state.itemCount)
        assertEquals(0.0, state.total, 0.001)
        assertNull(state.currentCandidate)
        assertFalse(state.hasCameraPermission)
    }

    @Test
    fun setCandidate_updatesState() {
        viewModel.setCandidate("Leche", 2.50)

        val state = viewModel.uiState.value
        assertEquals("Leche", state.currentCandidate?.name)
        assertEquals(2.50, state.currentCandidate?.price ?: 0.0, 0.001)
    }

    @Test
    fun addScannedItem_addsCandidateAndClearsCurrentCandidate() {
        viewModel.setCandidate("Cereal", 3.99)
        viewModel.addScannedItem()

        val state = viewModel.uiState.value
        assertEquals(1, state.itemCount)
        assertEquals(1, state.items.size)
        assertEquals("Cereal", state.items.first().name)
        assertEquals(3.99, state.items.first().price, 0.001)
        assertEquals(3.99, state.total, 0.001)
        assertNull(state.currentCandidate)
    }

    @Test
    fun addScannedItem_withNoCandidate_doesNothing() {
        viewModel.addScannedItem()

        val state = viewModel.uiState.value
        assertEquals(0, state.itemCount)
        assertEquals(0.0, state.total, 0.001)
    }

    @Test
    fun removeItem_removesSpecificItemAndRecalculatesTotal() {
        viewModel.setCandidate("Manzanas", 4.50)
        viewModel.addScannedItem()
        viewModel.setCandidate("Pan", 1.50)
        viewModel.addScannedItem()

        assertEquals(2, viewModel.uiState.value.itemCount)
        assertEquals(6.00, viewModel.uiState.value.total, 0.001)

        val itemToRemove = viewModel.uiState.value.items.first()
        viewModel.removeItem(itemToRemove)

        val state = viewModel.uiState.value
        assertEquals(1, state.itemCount)
        assertEquals("Pan", state.items.first().name)
        assertEquals(1.50, state.total, 0.001)
    }

    @Test
    fun undoLast_removesLastAddedItem() {
        viewModel.setCandidate("Item 1", 10.00)
        viewModel.addScannedItem()
        viewModel.setCandidate("Item 2", 20.00)
        viewModel.addScannedItem()
        viewModel.setCandidate("Item 3", 30.00)
        viewModel.addScannedItem()

        assertEquals(3, viewModel.uiState.value.itemCount)
        assertEquals(60.00, viewModel.uiState.value.total, 0.001)

        viewModel.undoLast()

        val state = viewModel.uiState.value
        assertEquals(2, state.itemCount)
        assertEquals("Item 2", state.items.last().name)
        assertEquals(30.00, state.total, 0.001)
    }

    @Test
    fun clearCart_emptiesEverything() {
        viewModel.setCandidate("Item 1", 10.00)
        viewModel.addScannedItem()
        viewModel.setCandidate("Item 2", 20.00)
        viewModel.addScannedItem()

        viewModel.clearCart()

        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())
        assertEquals(0, state.itemCount)
        assertEquals(0.0, state.total, 0.001)
    }

    @Test
    fun calculation_maintainsDecimalPrecision() {
        viewModel.addItem("A", 0.10)
        viewModel.addItem("B", 0.20)

        val state = viewModel.uiState.value
        assertEquals(0.30, state.total, 0.0001)
    }
}
