package com.tyron.code.util

import android.view.View
import android.view.ViewGroup
import androidx.core.view.*
import com.tyron.common.util.AndroidUtilities

/**
 * UiUtils v44.1: "The Zenith Bridge - Stability Patch".
 * v44.1: Corregida referencia a AndroidUtilities.dp y tipos de datos Kotlin.
 * v44.0: Consolidación total de márgenes delegando en AndroidUtilities v28.0.
 */

val Int.dp: Int
    get() = AndroidUtilities.dp(this.toFloat())

/**
 * Establece los márgenes de la vista escalando los valores de entrada (DP) a píxeles.
 * Si un valor es null, se mantiene el margen actual de la vista.
 */
fun View.setMargins(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    val params = (layoutParams as? ViewGroup.MarginLayoutParams) ?: return
    
    // Calculamos los nuevos valores en píxeles (si se proporcionan) o mantenemos los actuales
    val l = left?.let { AndroidUtilities.dp(it.toFloat()) } ?: params.leftMargin
    val t = top?.let { AndroidUtilities.dp(it.toFloat()) } ?: params.topMargin
    val r = right?.let { AndroidUtilities.dp(it.toFloat()) } ?: params.rightMargin
    val b = bottom?.let { AndroidUtilities.dp(it.toFloat()) } ?: params.bottomMargin
    
    params.setMargins(l, t, r, b)
    
    // OMNI: Refuerzo de soporte RTL para el enjambre moderno
    if (left != null) params.marginStart = l
    if (right != null) params.marginEnd = r
    
    layoutParams = params
}

@JvmOverloads
fun View.addSystemWindowInsetToPadding(
    left: Boolean = false,
    top: Boolean = false,
    right: Boolean = false,
    bottom: Boolean = false
) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            left = initialLeft + (if (left) insets.left else 0),
            top = initialTop + (if (top) insets.top else 0),
            right = initialRight + (if (right) insets.right else 0),
            bottom = initialBottom + (if (bottom) insets.bottom else 0)
        )
        windowInsets
    }
}

@JvmOverloads
fun View.addSystemWindowInsetToMargin(
    left: Boolean = false,
    top: Boolean = false,
    right: Boolean = false,
    bottom: Boolean = false
) {
    val initialLeft = marginLeft
    val initialTop = marginTop
    val initialRight = marginRight
    val initialBottom = marginBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(
                left = initialLeft + (if (left) insets.left else 0),
                top = initialTop + (if (top) insets.top else 0),
                right = initialRight + (if (right) insets.right else 0),
                bottom = initialBottom + (if (bottom) insets.bottom else 0)
            )
        }
        windowInsets
    }
}

fun View.removeFromParent() {
   (parent as? ViewGroup)?.removeView(this)
}
