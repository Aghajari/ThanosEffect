package com.aghajari.thanoseffect

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aghajari.thanoseffect.opengl.ThanosEffectOpenGLRenderer

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThanosEffect.renderer = ThanosEffectOpenGLRenderer
        //ThanosEffect.renderer = ThanosEffectCanvasRenderer

        //initializeView()
        initializeCompose()
    }

    private fun initializeView() {
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.view).setOnClickListener { v: View ->
            ThanosEffect.start(v)
        }
    }

    private fun initializeCompose() {
        setContent {
            MainScreen()
        }
    }
}

