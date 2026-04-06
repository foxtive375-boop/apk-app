package com.example.simpleapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.simpleapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionButton.setOnClickListener {
            val name = binding.messageInput.text?.toString()?.trim().orEmpty()
            val displayName = if (name.isEmpty()) "there" else name
            binding.greetingText.text = getString(R.string.greeting_format, displayName)
            binding.greetingText.visibility = View.VISIBLE
        }
    }
}
