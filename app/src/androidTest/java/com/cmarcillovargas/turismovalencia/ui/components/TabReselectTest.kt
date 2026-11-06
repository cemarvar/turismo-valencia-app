package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Pulsar una pestaña de la barra inferior lleva al principio de su sección (`TabReselect`, `OnTabReselect`). */
class TabReselectTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun Lista(state: LazyListState, tab: String, reselect: TabReselect) {
        CompositionLocalProvider(LocalTabReselect provides reselect) {
            TabScope(tab) {
                OnTabReselect { state.animateScrollToItem(0) }
                LazyColumn(Modifier.testTag("lista"), state = state) {
                    items((0 until 100).toList()) { Text("Fila $it", Modifier.height(48.dp)) }
                }
            }
        }
    }

    @Test
    fun pulsarLaPestanaSubeLaListaHastaArriba() {
        val reselect = TabReselect()
        val state = LazyListState()
        rule.setContent { Lista(state, "inicio", reselect) }
        rule.onNodeWithTag("lista").performScrollToIndex(60)
        rule.waitForIdle()
        assertTrue("la lista debería estar abajo", state.firstVisibleItemIndex >= 50)
        reselect.fire("inicio")
        rule.waitForIdle()
        assertEquals(0, state.firstVisibleItemIndex)
    }

    @Test
    fun pulsarOtraPestanaNoMueveLaLista() {
        val reselect = TabReselect()
        val state = LazyListState()
        rule.setContent { Lista(state, "inicio", reselect) }
        rule.onNodeWithTag("lista").performScrollToIndex(60)
        rule.waitForIdle()
        reselect.fire("favoritos")
        rule.waitForIdle()
        assertTrue("la lista no debería moverse: ${state.firstVisibleItemIndex}", state.firstVisibleItemIndex >= 50)
    }

    @Test
    fun elAvisoLlegaAunqueLaPantallaAparezcaDespuesDePulsar() {
        val reselect = TabReselect()
        val state = LazyListState(firstVisibleItemIndex = 60)
        reselect.fire("inicio")
        rule.setContent { Lista(state, "inicio", reselect) }
        rule.waitForIdle()
        assertEquals(0, state.firstVisibleItemIndex)
    }

    @Test
    fun cadaPulsacionVuelveAArriba() {
        val reselect = TabReselect()
        val state = LazyListState()
        rule.setContent { Lista(state, "inicio", reselect) }
        repeat(2) {
            rule.onNodeWithTag("lista").performScrollToIndex(70)
            rule.waitForIdle()
            reselect.fire("inicio")
            rule.waitForIdle()
            assertEquals("pulsación ${it + 1}", 0, state.firstVisibleItemIndex)
        }
    }
}
