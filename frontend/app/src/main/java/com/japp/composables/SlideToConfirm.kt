package com.japp.composables

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Slider height for the track container
 */
private val SLIDER_HEIGHT = 60.dp

/**
 * Size of the draggable thumb button
 */
private val THUMB_SIZE = 52.dp

/**
 * Threshold (from 0.0 to 1.0) at which the slide is considered complete
 */
private const val SLIDE_THRESHOLD = 0.85f

/**
 * A swipeable confirmation button that requires the user to slide a thumb across
 * the track to confirm an action. Provides haptic feedback at progress milestones
 * and animates completion with scale and fade effects.
 *
 * @param onConfirm Callback invoked when slider completes (reaches 85% threshold)
 * @param modifier Modifier for the root container
 * @param text Label displayed on the track (e.g., "Slide to confirm")
 * @param enabled Whether the slider is interactive (disabled state grays out thumb)
 * @param trackColor Starting color of the track background
 * @param confirmColor Target color when slider reaches end
 * @param thumbColor Background color of the draggable thumb
 * @param thumbContent Composable content for the thumb (defaults to arrow icon)
 */
// Most of the code comes from the following source:
// https://proandroiddev.com/6-steps-to-make-a-slide-to-unlock-button-in-jetpack-compose-ee9398cecf5f
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun SlideToConfirm(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Slide to confirm",
    enabled: Boolean = true,
    trackColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    thumbContent: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Slide",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    /**
     * ----------------------------------------
     * Step 1: Core State Variables
     *  - sliderPositionPx: Current horizontal position of thumb in pixels
     *  - containerWidthPx: Total width of the track container
     *  - thumbSizePx: Thumb size converted to pixels for calculations
     * ----------------------------------------
     */
    var sliderPositionPx by remember { mutableFloatStateOf(0f) }
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val thumbSizePx = with(density) { THUMB_SIZE.toPx() }

    /**
     * ----------------------------------------
     * Step 2: Animation State
     * - sliderComplete: Flag indicating slide reached threshold
     * - lastProgressMilestone: Tracks last haptic feedback milestone to avoid repeats
     * ----------------------------------------
     */
    var sliderComplete by remember { mutableStateOf(false) }
    var lastProgressMilestone by remember { mutableIntStateOf(0) }

    /**
     * ----------------------------------------
     * Step 3: Calculate Maximum Slide Distance
     * Maximum distance thumb can travel = containerWidth - thumbSize - padding
     * ----------------------------------------
     */
    val maxSlide = (containerWidthPx - thumbSizePx - with(density) { 8.dp.toPx() })
        .coerceAtLeast(0f)

    /**
     * ----------------------------------------
     * Step 4: Progress Calculation (from 0.0 to 1.0)
     * Represents how far the user has dragged the thumb as a percentage
     * ----------------------------------------
     */
    val dragProgress = remember(sliderPositionPx, maxSlide) {
        if (maxSlide > 0) {
            (sliderPositionPx / maxSlide).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    /**
     * ----------------------------------------
     * Step 5: Completion Animations
     *  - trackScale: Shrinks track horizontally when complete
     *  - sliderAlpha: Fades out thumb when complete
     * ----------------------------------------
     */
    val trackScale by animateFloatAsState(
        targetValue = if (sliderComplete) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "trackScale"
    )

    val sliderAlpha by animateFloatAsState(
        targetValue = if (sliderComplete) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "sliderAlpha"
    )

    /**
     * ----------------------------------------
     * Step 6: Dynamic Text Transparency
     * Label fades out as user drags (reaches 0 at 33% progress)
     * ----------------------------------------
     */
    val textAlpha = (1f - dragProgress * 3f).coerceIn(0f, 1f)

    /**
     * ----------------------------------------
     * Step 7: Track Color Interpolation
     * Smoothly transitions from trackColor to confirmColor based on progress
     * ----------------------------------------
     */
    val trackBackgroundColor = remember(dragProgress) {
        lerp(trackColor, confirmColor, dragProgress)
    }

    /**
     * ----------------------------------------
     * Step 8: Haptic Feedback at Milestones
     * Triggers haptic feedback at 50% and 80% progress (fires once per milestone)
     * ----------------------------------------
     */
    LaunchedEffect(dragProgress) {
        val currentMilestone = when {
            dragProgress >= 0.8f -> 2
            dragProgress >= 0.5f -> 1
            else -> 0
        }

        if (currentMilestone > lastProgressMilestone) {
            when (currentMilestone) {
                1 -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                2 -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            lastProgressMilestone = currentMilestone
        }
    }

    /**
     * ----------------------------------------
     * Step 9: Completion Detection
     * Once drag reaches 85% threshold, mark as complete and invoke callback
     * ----------------------------------------
     */
    LaunchedEffect(dragProgress) {
        if (dragProgress >= SLIDE_THRESHOLD && !sliderComplete) {
            sliderComplete = true
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onConfirm()
        }
    }

    /**
     * ----------------------------------------
     * Step 10: Root Container
     * Captures width and scales horizontally on completion
     * ----------------------------------------
     */
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(SLIDER_HEIGHT)
            .onSizeChanged { size ->
                containerWidthPx = size.width
            }
    ) {
        /**
         * ----------------------------------------
         * Step 11: Track Background
         * The base layer that displays the label and background color
         * ----------------------------------------
         */
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(scaleX = trackScale, scaleY = 1f)
                .clip(RoundedCornerShape(SLIDER_HEIGHT / 2))
                .background(trackBackgroundColor)
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .alpha(textAlpha),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                letterSpacing = 0.4.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        /**
         * ----------------------------------------
         * Step 12: Draggable Thumb
         * The slider button that user drags across the track
         * ----------------------------------------
         */
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .offset(x = with(density) { sliderPositionPx.toDp() })
                .graphicsLayer(alpha = sliderAlpha)
                .draggable(
                    enabled = enabled && !sliderComplete,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newPosition = sliderPositionPx + delta
                        sliderPositionPx = newPosition.coerceIn(0f, maxSlide)
                    },
                    onDragStarted = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    },
                    onDragStopped = {
                        /**
                         * ----------------------------------------
                         * Step 13: Reset with Spring Animation
                         * If threshold not reached, animate thumb back to start
                         * ----------------------------------------
                         */
                        if (dragProgress < SLIDE_THRESHOLD && !sliderComplete) {
                            scope.launch {
                                val animatable = Animatable(sliderPositionPx)
                                animatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    sliderPositionPx = value
                                }
                            }
                            lastProgressMilestone = 0
                        }
                    }
                )
        ) {
            SliderThumb(
                size = THUMB_SIZE,
                backgroundColor = if (enabled) thumbColor else MaterialTheme.colorScheme.outline,
                content = thumbContent
            )
        }
    }
}

/**
 * The draggable thumb component that slides across the track.
 *
 * @param size Diameter of the circular thumb
 * @param backgroundColor Background color of the thumb
 * @param content Composable content displayed in the center
 */
@Composable
private fun SliderThumb(
    size: Dp,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
