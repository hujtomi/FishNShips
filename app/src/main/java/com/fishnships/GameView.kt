package com.fishnships

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

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

    // Ship properties
    private val ships = mutableListOf<RectF>()
    private val shipPaint = Paint().apply { color = Color.DKGRAY }
    private val shipWidth = 200f
    private val shipHeight = 100f

    // Net properties
    private val nets = mutableListOf<RectF>()
    private val netPaint = Paint().apply { color = Color.BLACK }
    private val netWidth = 150f
    private val netHeight = 80f

    // Obstacle speed
    private val obstacleSpeed = 10f

    // Score
    private var score = 0
    private val scorePaint = Paint().apply {
        color = Color.BLACK
        textSize = 60f
        textAlign = Paint.Align.LEFT
    }
    private var scoreUpdateCounter = 0L

    private var isGameOver = false
    private val gameOverPaint = Paint().apply {
        color = Color.RED
        textSize = 100f
        textAlign = Paint.Align.CENTER
    }
    private val restartTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }

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

        // Draw nets
        nets.forEach { canvas.drawRect(it, netPaint) }

        // Draw fish
        canvas.drawCircle(fishX, fishY, fishRadius, fishPaint)

        // Draw ships
        ships.forEach { canvas.drawRect(it, shipPaint) }

        // Draw score
        canvas.drawText("Score: $score", 50f, 100f, scorePaint)

        if (isGameOver) {
            canvas.drawText("Game Over", screenWidth / 2f, screenHeight / 2f, gameOverPaint)
            canvas.drawText("Tap to restart", screenWidth / 2f, screenHeight / 2f + 120, restartTextPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        fishX = screenWidth / 4f
        fishY = screenHeight / 2f
        amplitude = h / 4f

        initObstacles()
    }

    private fun initObstacles() {
        ships.clear()
        nets.clear()
        score = 0
        scoreUpdateCounter = 0L
        // Initialize ships
        for (i in 0..2) {
            val shipX = screenWidth + i * 500f + Random.nextInt(300)
            val shipY = Random.nextFloat() * (screenHeight / 5f)
            ships.add(RectF(shipX, shipY, shipX + shipWidth, shipY + shipHeight))
        }

        // Initialize nets
        for (i in 0..2) {
            val netX = screenWidth + i * 500f + Random.nextInt(300)
            val netY = screenHeight - (Random.nextFloat() * (screenHeight / 5f)) - netHeight
            nets.add(RectF(netX, netY, netX + netWidth, netY + netHeight))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                isGameOver = false
                initObstacles()
            }
            return true
        }

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

    private fun checkCollisions() {
        val fishRect = RectF(fishX - fishRadius, fishY - fishRadius, fishX + fishRadius, fishY + fishRadius)

        // Ship collision
        ships.forEach {
            if (RectF.intersects(it, fishRect)) {
                isGameOver = true
                return
            }
        }

        // Net collision
        nets.forEach {
            if (RectF.intersects(it, fishRect)) {
                isGameOver = true
                return
            }
        }
    }

    private fun update() {
        if (isGameOver) return

        if (!isUserTouching) {
            // Update fish position for sine wave movement
            angle += frequency
            fishY = (screenHeight / 2f) + (amplitude * sin(angle)).toFloat()
        }

        // Update ships
        ships.forEach { ship ->
            ship.left -= obstacleSpeed
            ship.right -= obstacleSpeed
            if (ship.right < 0) {
                ship.left = screenWidth.toFloat() + Random.nextInt(500)
                ship.right = ship.left + shipWidth
                ship.top = Random.nextFloat() * (screenHeight / 5f)
                ship.bottom = ship.top + shipHeight
            }
        }

        // Update nets
        nets.forEach { net ->
            net.left -= obstacleSpeed
            net.right -= obstacleSpeed
            if (net.right < 0) {
                net.left = screenWidth.toFloat() + Random.nextInt(500)
                net.right = net.left + netWidth
                net.top = screenHeight - (Random.nextFloat() * (screenHeight / 5f)) - netHeight
                net.bottom = net.top + netHeight
            }
        }

        // Update score - every 100ms (roughly 6 frames at 60fps)
        scoreUpdateCounter++
        if (scoreUpdateCounter % 6 == 0L) {
            score++
        }

        checkCollisions()
    }
}
