package com.cmarcillovargas.turismovalencia.ui.screens.estados

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.ui.components.SkeletonBox
import com.cmarcillovargas.turismovalencia.ui.screens.explorar.HomeSearchField
import com.cmarcillovargas.turismovalencia.ui.theme.CardShape
import com.cmarcillovargas.turismovalencia.ui.theme.PillShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing

// Medidas del diseño (H2).
private val FeatureHeight = 176.dp
private val ChipHeight = 36.dp
private val CardImageHeight = 124.dp
private val CardHeight = 205.dp
private val FeatureShape = RoundedCornerShape(20.dp)

/** H2 · Esqueleto del inicio mientras carga el contenido: buscador real y bloques grises donde irán las tarjetas. */
@Composable
fun InicioCargando(onOpenSearch: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HomeSearchField(onOpenSearch, Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s))
        SkeletonBox(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m).height(FeatureHeight),
            shape = FeatureShape,
        )
        Row(
            modifier = Modifier.padding(start = Spacing.m, top = Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            listOf(96, 120, 118).forEach { width ->
                SkeletonBox(Modifier.width(width.dp).height(ChipHeight), PillShape)
            }
        }
        SkeletonBox(Modifier.padding(start = 20.dp, top = Spacing.l).width(140.dp).height(22.dp), PillShape)
        Spacer(Modifier.height(Spacing.l))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            repeat(2) { SkeletonCard(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SkeletonCard(modifier: Modifier = Modifier) {
    Column(modifier.height(CardHeight).background(MaterialTheme.colorScheme.surface, CardShape)) {
        SkeletonBox(Modifier.fillMaxWidth().height(CardImageHeight), CardShape)
        Column(Modifier.padding(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SkeletonBox(Modifier.fillMaxWidth(0.8f).height(Spacing.m), PillShape)
            SkeletonBox(Modifier.fillMaxWidth(0.55f).height(Spacing.s), PillShape)
            SkeletonBox(Modifier.fillMaxWidth(0.35f).height(Spacing.s), PillShape)
        }
    }
}
