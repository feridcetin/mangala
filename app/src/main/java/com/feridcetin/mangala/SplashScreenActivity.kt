package com.feridcetin.mangala

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Handler kullanarak 3 saniyelik bir gecikme oluştur
        Handler(Looper.getMainLooper()).postDelayed({
            // Gecikme sonrası MainActivity'e geçiş yap
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Açılış ekranını sonlandır, böylece geri tuşuna basıldığında tekrar görünmez
            finish()
        }, 3000) // 3000 milisaniye = 3 saniye
    }
}
