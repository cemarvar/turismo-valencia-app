package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.cmarcillovargas.turismovalencia.data.model.Category

fun Category.icon(): ImageVector = when (this) {
    Category.QueVer -> Icons.Outlined.AccountBalance
    Category.Gastronomia -> Icons.Outlined.Restaurant
    Category.Alojamientos -> Icons.Outlined.Hotel
    Category.Eventos -> Icons.Outlined.LocalFireDepartment
    Category.Sostenible -> Icons.Outlined.Eco
}
