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
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        currentText = NoteRepository.loadDraft(this)
        createNotificationChannel()
        startForeground(1, createNotification())
        showBubble()
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
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
        
        bubbleView?.setOnTouchListener(BubbleTouchListener(params))
        windowManager.addView(bubbleView, params)
        
        bubbleView?.setOnClickListener {
            showPalette()
            hideBubble()
        }
    }
    
    private fun showPalette() {
        paletteView = LayoutInflater.from(this).inflate(R.layout.floating_palette, null)
        
        val params = WindowManager.LayoutParams(
            800,
            600,
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
        
        val navBar = paletteView?.findViewById<View>(R.id.navBar)
        navBar?.setOnTouchListener(PaletteTouchListener(params))
        
        val resizeHandle = paletteView?.findViewById<View>(R.id.resizeHandle)
        resizeHandle?.setOnTouchListener(ResizeTouchListener(params))
    }
    
    private fun showMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Save to Documents")
            menu.add("Font Size +")
            menu.add("Font Size -")
            
            setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Save to Documents" -> showSaveDialog()
                    "Font Size +" -> changeFontSize(2f)
                    "Font Size -" -> changeFontSize(-2f)
                }
                true
            }
            show()
        }
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
        editText?.textSize = (editText?.textSize ?: 16f) + delta
    }
    
    private fun hideBubble() {
        bubbleView?.let {
            windowManager.removeView(it)
            bubbleView = null
        }
    }
    
    private fun hidePalette() {
        paletteView?.let {
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
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
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
