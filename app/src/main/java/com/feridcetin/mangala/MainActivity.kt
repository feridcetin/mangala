package com.feridcetin.mangala

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    // Oyunun durumunu tutan değişkenler
    private lateinit var pockets: List<Button>
    private lateinit var store1: TextView
    private lateinit var store2: TextView
    private lateinit var statusText: TextView
    private lateinit var resetButton: Button
    private lateinit var player1StonesCountText: TextView
    private lateinit var player2StonesCountText: TextView

    // 0-5 Oyuncu 1 cepleri
    // 6 Oyuncu 1 haznesi
    // 7-12 Oyuncu 2 cepleri
    // 13 Oyuncu 2 haznesi
    private var board = IntArray(14)
    private var currentPlayer = 1
    private var isMoving = false

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI bileşenlerini bağla
        pockets = listOf(
            findViewById(R.id.button_pocket0),
            findViewById(R.id.button_pocket1),
            findViewById(R.id.button_pocket2),
            findViewById(R.id.button_pocket3),
            findViewById(R.id.button_pocket4),
            findViewById(R.id.button_pocket5),
            findViewById(R.id.button_pocket7),
            findViewById(R.id.button_pocket8),
            findViewById(R.id.button_pocket9),
            findViewById(R.id.button_pocket10),
            findViewById(R.id.button_pocket11),
            findViewById(R.id.button_pocket12)
        )
        store1 = findViewById(R.id.textView_store1)
        store2 = findViewById(R.id.textView_store2)
        statusText = findViewById(R.id.textView_status)
        resetButton = findViewById(R.id.button_reset)
        player1StonesCountText = findViewById(R.id.textView_sayi1)
        player2StonesCountText = findViewById(R.id.textView_sayi2)

        // Butonlara tıklama olay dinleyicisi ekle
        pockets.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (isMoving) return@setOnClickListener

                // UI'daki buton indeksi ile board dizisi indeksi arasındaki eşleşme
                val pocketIndex = if (index < 6) index else index + 1

                // Hamle geçerliliğini kontrol et
                if (board[pocketIndex] > 0) {
                    if ((currentPlayer == 1 && pocketIndex in 0..5) || (currentPlayer == 2 && pocketIndex in 7..12)) {
                        playTurn(pocketIndex)
                    } else {
                        Toast.makeText(this, "Yanlış cebe tıkladınız.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Boş cepten taş alamazsınız.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        resetButton.setOnClickListener {
            resetGame()
        }

        // Oyunu başlat
        resetGame()
    }

    // Oyunu sıfırlama fonksiyonu
    private fun resetGame() {
        board = intArrayOf(4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 0)
        currentPlayer = 1
        isMoving = false
        player1StonesCountText.visibility = View.INVISIBLE
        player2StonesCountText.visibility = View.INVISIBLE
        updateUI()
        statusText.text = "Sıra: Oyuncu 1"
    }

    // Oyuncunun hamlesini başlatan fonksiyon
    private fun playTurn(startIndex: Int) {
        if (isMoving) return
        isMoving = true

        val stonesToDistribute = board[startIndex]
        board[startIndex] = 0

        // Seçilen cepten alınan taş sayısını ekranda göster
        if (currentPlayer == 1) {
            player1StonesCountText.text = stonesToDistribute.toString()
            player1StonesCountText.visibility = View.VISIBLE
            player2StonesCountText.visibility = View.INVISIBLE
        } else {
            player2StonesCountText.text = stonesToDistribute.toString()
            player2StonesCountText.visibility = View.VISIBLE
            player1StonesCountText.visibility = View.INVISIBLE
        }

        updateUI()

        distributeSeedsAnimated(startIndex, stonesToDistribute)
    }

    // Taşları animasyonlu bir şekilde dağıtma
    private fun distributeSeedsAnimated(startIndex: Int, stonesToDistribute: Int) {
        var currentIndex = startIndex
        var remainingStones = stonesToDistribute
        var lastIndex = startIndex

        val runnable = object : Runnable {
            override fun run() {
                if (remainingStones == 0) {
                    isMoving = false
                    player1StonesCountText.visibility = View.INVISIBLE
                    player2StonesCountText.visibility = View.INVISIBLE
                    applyGameRules(lastIndex)
                    return
                }

                currentIndex = (currentIndex + 1) % board.size

                // Rakibin haznesini atla
                if (currentPlayer == 1 && currentIndex == 13) {
                    currentIndex = (currentIndex + 1) % board.size
                } else if (currentPlayer == 2 && currentIndex == 6) {
                    currentIndex = (currentIndex + 1) % board.size
                }

                // Kuralı ekle: Oyuncu kendi haznesine ekleme yapamaz
                if ((currentPlayer == 1 && currentIndex == 6) || (currentPlayer == 2 && currentIndex == 13)) {
                    // Oyuncunun kendi haznesi atlanır
                    currentIndex = (currentIndex + 1) % board.size
                }

                board[currentIndex]++
                lastIndex = currentIndex
                remainingStones--

                updateUI()

                // Animasyon için kısa bir gecikme
                handler.postDelayed(this, 1300)
            }
        }

        handler.post(runnable)
    }

    // Oyun kurallarını uygulayan fonksiyon
    private fun applyGameRules(lastIndex: Int) {
        val nextPlayer = when {
            // Kural 1: Son taş kendi haznesine gelirse bir tur daha oynar
            (currentPlayer == 1 && lastIndex == 6) || (currentPlayer == 2 && lastIndex == 13) -> currentPlayer

            // Kural 2: Son taş kendi tarafındaki boş cebe gelirse, karşıdaki cepten taşları alır
            (currentPlayer == 1 && lastIndex in 0..5 && board[lastIndex] == 1) ||
                    (currentPlayer == 2 && lastIndex in 7..12 && board[lastIndex] == 1) -> {
                val oppositePocketIndex = 12 - lastIndex
                if (board[oppositePocketIndex] > 0) {
                    val capturedStones = board[oppositePocketIndex] + board[lastIndex]
                    board[oppositePocketIndex] = 0
                    board[lastIndex] = 0
                    if (currentPlayer == 1) {
                        board[6] += capturedStones
                    } else {
                        board[13] += capturedStones
                    }
                    Toast.makeText(this, "Karşı cepten taşları yakaladınız!", Toast.LENGTH_SHORT).show()
                }
                if (currentPlayer == 1) 2 else 1
            }

            // Kural 3: Son taş dolu bir cebe gelirse, o cepten oynamaya devam eder (animasyonsuz)
            else -> {
                if (board[lastIndex] > 1) {
                    playTurn(lastIndex)
                    return
                }
                // Kural 4: Diğer tüm durumlarda sıra diğer oyuncuya geçer
                if (currentPlayer == 1) 2 else 1
            }
        }

        currentPlayer = nextPlayer
        checkGameOver()
        updateUI()
    }

    // Oyun bitti mi kontrol etme
    private fun checkGameOver() {
        val player1PocketsEmpty = (0..5).all { board[it] == 0 }
        val player2PocketsEmpty = (7..12).all { board[it] == 0 }

        if (player1PocketsEmpty || player2PocketsEmpty) {
            // Oyun bitti - kalan taşlar diğer oyuncunun haznesine gider
            for (i in 0..5) {
                board[6] += board[i]
                board[i] = 0
            }
            for (i in 7..12) {
                board[13] += board[i]
                board[i] = 0
            }

            val winnerMessage = when {
                board[6] > board[13] -> "Oyuncu 1 Kazandı! Skor: ${board[6]} - ${board[13]}"
                board[13] > board[6] -> "Oyuncu 2 Kazandı! Skor: ${board[13]} - ${board[6]}"
                else -> "Oyun Berabere Bitti! Skor: ${board[6]} - ${board[13]}"
            }
            statusText.text = winnerMessage
            pockets.forEach { it.isEnabled = false } // Oyun bitince cepleri devre dışı bırak
            isMoving = true // Oyun bittiğinde hamle yapılmasını engelle
        }
    }

    // Arayüzü güncelleme fonksiyonu
    private fun updateUI() {
        pockets.forEachIndexed { index, button ->
            val pocketIndex = if (index < 6) index else index + 1
            button.text = board[pocketIndex].toString()

            // Aktif oyuncu ceplerini vurgula
            if ((currentPlayer == 1 && pocketIndex in 0..5) || (currentPlayer == 2 && pocketIndex in 7..12)) {
                button.isEnabled = true
                button.alpha = 1.0f
            } else {
                button.isEnabled = false
                button.alpha = 0.5f
            }
        }
        store1.text = board[6].toString()
        store2.text = board[13].toString()

        if (!isMoving) {
            statusText.text = "Sıra: Oyuncu $currentPlayer"
        }
    }
}
