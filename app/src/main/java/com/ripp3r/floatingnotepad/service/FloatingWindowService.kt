package com.ripp3r.floatingnotepad.service

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ripp3r.floatingnotepad.R
import com.ripp3r.floatingnotepad.data.NoteRepository
import com.ripp3r.floatingnotepad.utils.ThemeManager
import android.widget.LinearLayout
import android.util.Log

class FloatingWindowService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var paletteView: View? = null
    private var editText: EditText? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var currentText = ""
    private var lastBubbleX = 100
    private var lastBubbleY = 100
    private var lastPaletteWidth = 800
    private var lastPaletteHeight = 600
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d("FloatingService", "Service onCreate called")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        currentText = NoteRepository.loadDraft(this)
        createNotificationChannel()
        startForeground(1, createNotification())
        showBubble()
        Log.d("FloatingService", "Bubble should be visible now")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "floating_notepad",
                "Floating Notepad",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, "floating_notepad")
        .setContentTitle("Floating Notepad")
        .setContentText("Notepad is running")
        .setSmallIcon(android.R.drawable.ic_menu_edit)
        .build()
    
    private fun showBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null)
        
        val bubbleSize = (ThemeManager.getBubbleSize(this) * resources.displayMetrics.density).toInt()
        
        val params = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lastBubbleX
            y = lastBubbleY
        }
        
        bubbleView?.setOnTouchListener(BubbleTouchListener(params))
        windowManager.addView(bubbleView, params)
    }
    
    private fun showPalette() {
        paletteView = LayoutInflater.from(this).inflate(R.layout.floating_palette, null)
        
        val isDark = ThemeManager.isDarkMode(this)
        val bgColor = if (isDark) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
        val textColor = if (isDark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val navBarColor = if (isDark) 0xFF1565C0.toInt() else 0xFF2196F3.toInt()
        
        val container = paletteView?.findViewById<LinearLayout>(R.id.paletteContainer)
        container?.background = resources.getDrawable(R.drawable.palette_background, null).apply {
            setTint(bgColor)
        }
        
        val navBar = paletteView?.findViewById<View>(R.id.navBar)
        navBar?.background = resources.getDrawable(R.drawable.nav_bar_background, null).apply {
            setTint(navBarColor)
        }
        
        val params = WindowManager.LayoutParams(
            lastPaletteWidth,
            lastPaletteHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        windowManager.addView(paletteView, params)
        
        editText = paletteView?.findViewById<EditText>(R.id.editText)?.apply {
            setText(currentText)
            setTextColor(textColor)
            setBackgroundColor(bgColor)
            setHintTextColor(if (isDark) 0xFF888888.toInt() else 0xFF666666.toInt())
            textSize = ThemeManager.getFontSize(this@FloatingWindowService)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    currentText = s.toString()
                    NoteRepository.saveDraft(this@FloatingWindowService, currentText)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        
        paletteView?.findViewById<ImageButton>(R.id.btnMinimize)?.setOnClickListener {
            hidePalette()
            showBubble()
        }
        
        paletteView?.findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener {
            hidePalette()
            stopSelf()
        }
        
        paletteView?.findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            showMenu(it)
        }
        
        navBar?.setOnTouchListener(PaletteTouchListener(params))
        
        val resizeHandle = paletteView?.findViewById<View>(R.id.resizeHandle)
        resizeHandle?.setOnTouchListener(ResizeTouchListener(params))
    }
    
    private fun showMenu(anchor: View) {
        val items = arrayOf("Font Size", "Save to Documents")
        
        AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog)
            .setTitle("Menu")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showFontSizeDialog()
                    1 -> showSaveDialog()
                }
            }
            .create()
            .apply {
                window?.setType(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE
                )
            }
            .show()
    }
    
    private fun showFontSizeDialog() {
        val currentSize = ThemeManager.getFontSize(this)
        
        val previewText = android.widget.TextView(this).apply {
            text = "Preview Text"
            textSize = currentSize
            gravity = android.view.Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }
        
        val sizeLabel = android.widget.TextView(this).apply {
            text = "${currentSize.toInt()}sp"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 10)
        }
        
        val seekBar = android.widget.SeekBar(this).apply {
            max = 12
            progress = (currentSize - 12).toInt()
            setPadding(50, 20, 50, 20)
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(previewText)
            addView(sizeLabel)
            addView(seekBar)
        }
        
        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val size = 12f + progress
                sizeLabel.text = "${size.toInt()}sp"
                previewText.textSize = size
                editText?.textSize = size
                ThemeManager.setFontSize(this@FloatingWindowService, size)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Font Size")
            .setView(layout)
            .setPositiveButton("Done", null)
            .create()
            .apply {
                window?.setType(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE
                )
            }
            .show()
    }
    
    private fun showSaveDialog() {
        val input = EditText(this).apply {
            hint = "filename.txt"
        }
        
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Save File")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val fileName = input.text.toString().ifEmpty { "note.txt" }
                val success = NoteRepository.saveToDocuments(fileName, currentText)
                Toast.makeText(
                    this,
                    if (success) "Saved to Documents/$fileName" else "Save failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                window?.setType(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE
                )
            }
            .show()
    }
    
    private fun changeFontSize(delta: Float) {
        editText?.apply {
            val currentSize = textSize / resources.displayMetrics.scaledDensity
            textSize = currentSize + delta
        }
    }
    
    private fun hideBubble() {
        bubbleView?.let {
            val params = it.layoutParams as WindowManager.LayoutParams
            lastBubbleX = params.x
            lastBubbleY = params.y
            windowManager.removeView(it)
            bubbleView = null
        }
    }
    
    private fun hidePalette() {
        paletteView?.let {
            val params = it.layoutParams as WindowManager.LayoutParams
            lastPaletteWidth = params.width
            lastPaletteHeight = params.height
            windowManager.removeView(it)
            paletteView = null
        }
        editText = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideBubble()
        hidePalette()
    }
    
    private inner class BubbleTouchListener(
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {
        private var moved = false
        
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        moved = true
                        params.x = initialX + deltaX.toInt()
                        params.y = initialY + deltaY.toInt()
                        windowManager.updateViewLayout(v, params)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        showPalette()
                        hideBubble()
                        return true
                    }
                    val displayMetrics = resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val bubbleWidth = v.width
                    val centerX = params.x + bubbleWidth / 2
                    
                    params.x = if (centerX < screenWidth / 2) 0 else screenWidth - bubbleWidth
                    windowManager.updateViewLayout(v, params)
                    return true
                }
            }
            return false
        }
    }
    
    private inner class PaletteTouchListener(
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(paletteView, params)
                    return true
                }
            }
            return false
        }
    }
    
    private inner class ResizeTouchListener(
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {
        private var initialWidth = 0
        private var initialHeight = 0
        
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialWidth = params.width
                    initialHeight = params.height
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.width = (initialWidth + (event.rawX - initialTouchX).toInt()).coerceIn(400, 1200)
                    params.height = (initialHeight + (event.rawY - initialTouchY).toInt()).coerceIn(300, 1000)
                    windowManager.updateViewLayout(paletteView, params)
                    return true
                }
            }
            return false
        }
    }
}
