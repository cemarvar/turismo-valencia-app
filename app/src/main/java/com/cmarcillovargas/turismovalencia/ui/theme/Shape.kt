package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardShape = RoundedCornerShape(16.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val PillShape = RoundedCornerShape(percent = 50)

// Los roles de Material 3 apuntan a estas formas para que los componentes estándar las hereden:
// chips → small (píldora), tarjetas → medium/large (16dp), bottom sheet y diálogos → extraLarge (24dp).
// Los botones ya son píldora por defecto y extraSmall (menús, campos de texto) se deja en 4dp.
val Shapes = Shapes(
    small = PillShape,
    medium = CardShape,
    large = CardShape,
    extraLarge = RoundedCornerShape(24.dp),
)
