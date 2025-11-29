package com.fishnships

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sin

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val backgroundPaint = Paint().apply {
        color = Color.CYAN
    }

    // Fish properties
    private var fishX: Float = 0f
    private var fishY: Float = 0f
    private val fishRadius = 50f
    private val fishPaint = Paint().apply {
        color = Color.YELLOW // Fish color
    }

    // Sine wave properties
    private var angle = 0.0
    private var amplitude = 100f
    private val frequency = 0.05

    // Game loop
    private var gameThread: Thread? = null
    private var isRunning = false
    private var screenWidth = 0
    private var screenHeight = 0

    private var isUserTouching = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw fish
        canvas.drawCircle(fishX, fishY, fishRadius, fishPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        fishX = screenWidth / 4f
        fishY = screenHeight / 2f
        amplitude = h / 4f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isUserTouching = true
            }
            MotionEvent.ACTION_UP -> {
                isUserTouching = false
            }
        }
        return true // We handled the touch event
    }

    fun resume() {
        isRunning = true
        gameThread = Thread {
            while (isRunning) {
                update()
                postInvalidate() // Redraw the view on the UI thread
                Thread.sleep(16) // ~60 FPS
            }
        }
        gameThread?.start()
    }

    fun pause() {
        isRunning = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                // Try again
            }
        }
    }

    private fun update() {
        if (!isUserTouching) {
            // Update fish position for sine wave movement
            angle += frequency
            fishY = (screenHeight / 2f) + (amplitude * sin(angle)).toFloat()
        }
        // In the future, we will update other game objects here
    }
}
