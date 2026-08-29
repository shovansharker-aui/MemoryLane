package com.memorylane.app.ui

import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator

/**
 * Adds a gentle "press in, bounce back" scale animation to any view,
 * on top of whatever click listener you already set. Gives buttons/cards
 * a soft, tactile, iOS-like feel instead of Android's flat ripple alone.
 */
fun View.withBouncyPress() {
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(220)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .start()
            }
        }
        false // let the normal click listener still fire
    }
}
