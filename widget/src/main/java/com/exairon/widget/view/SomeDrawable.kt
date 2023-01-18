package com.exairon.widget.view

import android.graphics.drawable.GradientDrawable


class SomeDrawable(
    pStartColor: Int,
    pCenterColor: Int,
    pEndColor: Int,
    pStrokeWidth: Int,
    pStrokeColor: Int,
    cornerRadius: Double,
) :
    GradientDrawable(Orientation.BOTTOM_TOP, intArrayOf(pStartColor, pCenterColor, pEndColor)) {
    init {
        setStroke(pStrokeWidth, pStrokeColor)
        shape = RECTANGLE
        setCornerRadius(cornerRadius.toFloat())
    }
}