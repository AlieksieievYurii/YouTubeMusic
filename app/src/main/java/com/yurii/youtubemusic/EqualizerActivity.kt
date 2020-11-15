package com.yurii.youtubemusic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import com.yurii.youtubemusic.ui.EqualizerView
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.viewmodels.EqualizerViewModel

class EqualizerActivity : AppCompatActivity() {
    private val viewModel: EqualizerViewModel by viewModels {
        Injector.provideEqualizerViewModel(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizer)
        val eq = findViewById<EqualizerView>(R.id.equalizer)
        val bands = ArrayList<Int>().apply {
            addAll(listOf(60, 230, 910, 14000))
        }
        viewModel.printDupa()
        eq.setBands(bands)
        eq.setMax(500)
        eq.draw()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true);
        supportActionBar!!.title = "Equalizer"

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.equalizer_menu, menu)
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}