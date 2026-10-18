package com.kichikan.ak1.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

/**
 * Draws one of the hand-illustrated PNG icons from `res/drawable-nodpi`.
 *
 * The files are 236x236 px with a transparent background, so they are always scaled
 * down to the requested [size]; [ContentScale.Fit] keeps their aspect ratio.
 * Icons are decorative next to a text label, so [contentDescription] stays null by default.
 */
@Composable
fun AppIcon(
    @DrawableRes id: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Image(
        painter = painterResource(id),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}
