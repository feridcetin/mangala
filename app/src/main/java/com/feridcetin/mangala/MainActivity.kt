package com.feridcetin.mangala

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.graphics.Color
import androidx.core.content.ContextCompat
import android.content.pm.ActivityInfo
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import android.view.Gravity
import android.media.SoundPool
import android.media.AudioAttributes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.random.Random
import android.app.AlertDialog
import android.widget.EditText
import android.text.InputType

class MainActivity : AppCompatActivity() {

    // Oyunun durumunu tutan değişkenler
    private lateinit var pockets: List<Button>
    private lateinit var store1: TextView
    private lateinit var store2: TextView
    private lateinit var statusText: TextView
    //private lateinit var resetButton: Button
    private lateinit var menuButton: Button
    private lateinit var player1StonesCountText: TextView
    private lateinit var player2StonesCountText: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // Set skorları ve set bilgisi
    private lateinit var setScoreText: TextView
    private var player1SetsWon = 0
    private var player2SetsWon = 0
    private var currentSet = 1

    // Oyuncu isimleri için yeni değişkenler
    private var player1Name = "Oyuncu 1"
    private var player2Name = "Oyuncu 2"

    // Ses efektleri için SoundPool
    private lateinit var soundPool: SoundPool
    private var stoneSoundId: Int = 0

    // 0-5 Oyuncu 1 cepleri
    // 6 Oyuncu 1 haznesi
    // 7-12 Oyuncu 2 cepleri
    // 13 Oyuncu 2 haznesi
    private var board = IntArray(14)
    private var currentPlayer = 1
    private var isMoving = false
    private var isSinglePlayer = false

    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ekranı yatay moda ayarlama
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // Uygulamayı tam ekran yapmak için gerekli kod
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContentView(R.layout.activity_main)

        // Ses efektleri için SoundPool'u başlat
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()
        stoneSoundId = soundPool.load(this, R.raw.stone_sound, 1)

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
            R.id.button_pocket11.let { findViewById(it) },
            findViewById(R.id.button_pocket12)
        )
        store1 = findViewById(R.id.textView_store1)
        store2 = findViewById(R.id.textView_store2)
        statusText = findViewById(R.id.textView_status)
        //resetButton = findViewById(R.id.button_reset)
        menuButton = findViewById(R.id.button_open_menu)
        player1StonesCountText = findViewById(R.id.textView_sayi1)
        player2StonesCountText = findViewById(R.id.textView_sayi2)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        setScoreText = findViewById(R.id.textView_set_score)

        // Menü butonu tıklama dinleyicisi
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Navigation View menü öğesi tıklama dinleyicisi
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_reset -> {
                    resetGame()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_single_player -> {
                    resetGame()
                    isSinglePlayer = true
                    player2Name = "Bilgisayar" // Tek oyunculu modda 2. oyuncu ismini ayarla
                    Toast.makeText(this, "Tekli oyun modu başlatıldı.", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawers()
                    updateUI()
                    true
                }
                R.id.nav_multi_player -> {
                    resetGame()
                    isSinglePlayer = false
                    player2Name = "Oyuncu 2" // Çok oyunculu modda 2. oyuncu ismini sıfırla
                    Toast.makeText(this, "İkili oyun modu başlatıldı.", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawers()
                    updateUI()
                    true
                }
                R.id.nav_change_names -> {
                    showNameChangeDialog()
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }

        // Butonlara tıklama olay dinleyicisi ekle
        pockets.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (isMoving) return@setOnClickListener
                val pocketIndex = if (index < 6) index else index + 1
                if (board[pocketIndex] > 0) {
                    if ((currentPlayer == 1 && pocketIndex in 0..5) || (currentPlayer == 2 && pocketIndex in 7..12)) {
                        playTurn(pocketIndex)
                    }
                }
            }
        }

        /*resetButton.setOnClickListener {
            resetGame()
        }*/

        // Oyunu başlat
        resetGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }

    // Oyuncu isimlerini değiştirmek için diyalog penceresi gösterir
    private fun showNameChangeDialog() {
        val dialogView = LinearLayout(this)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(50, 50, 50, 50)

        val player1Input = EditText(this)
        player1Input.hint = "Oyuncu 1 Adı"
        player1Input.inputType = InputType.TYPE_CLASS_TEXT
        player1Input.setText(player1Name)
        dialogView.addView(player1Input)

        val player2Input = EditText(this)
        player2Input.hint = "Oyuncu 2 Adı"
        player2Input.inputType = InputType.TYPE_CLASS_TEXT
        player2Input.setText(player2Name)
        dialogView.addView(player2Input)

        AlertDialog.Builder(this)
            .setTitle("Oyuncu İsimlerini Değiştir")
            .setView(dialogView)
            .setPositiveButton("Tamam") { dialog, which ->
                val newPlayer1Name = player1Input.text.toString().trim()
                val newPlayer2Name = player2Input.text.toString().trim()

                if (newPlayer1Name.isNotEmpty()) {
                    player1Name = newPlayer1Name
                }
                if (newPlayer2Name.isNotEmpty()) {
                    player2Name = newPlayer2Name
                }
                Toast.makeText(this, "İsimler güncellendi!", Toast.LENGTH_SHORT).show()
                updateUI()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // Oyunu sıfırlama fonksiyonu (Toplam maçı sıfırlar)
    private fun resetGame() {
        player1SetsWon = 0
        player2SetsWon = 0
        currentSet = 1
        resetSet()
    }

    // Seti sıfırlama fonksiyonu
    private fun resetSet() {
        board = intArrayOf(4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 0)
        currentPlayer = 1
        isMoving = false
        player1StonesCountText.visibility = View.INVISIBLE
        player2StonesCountText.visibility = View.INVISIBLE
        updateUI()
    }

    // Oyuncunun hamlesini başlatan fonksiyon
    private fun playTurn(startIndex: Int) {
        if (isMoving) return
        isMoving = true

        val stonesInPocket = board[startIndex]

        // KURAL: Cebe tek taş varsa, sadece o taşı alıp bir sonraki cebe at.
        if (stonesInPocket == 1) {
            board[startIndex] = 0 // Başlangıç cebini boşalt

            var nextIndex = (startIndex + 1) % board.size

            // Rakibin haznesini atla
            if (currentPlayer == 1 && nextIndex == 13) {
                nextIndex = (nextIndex + 1) % board.size
            } else if (currentPlayer == 2 && nextIndex == 6) {
                nextIndex = (nextIndex + 1) % board.size
            }

            board[nextIndex]++ // Bir sonraki cebe taşı koy
            soundPool.play(stoneSoundId, 1.0f, 1.0f, 1, 0, 1.0f)

            isMoving = false
            applyGameRules(nextIndex) // Kuralları uygula
        }
        // Standart kural: 1'den fazla taş varsa, bir tane bırakıp diğerlerini dağıt.
        else {
            board[startIndex] = 1
            val remainingStones = stonesInPocket - 1

            // Taş sayısını ekranda göster
            if (currentPlayer == 1) {
                player1StonesCountText.text = remainingStones.toString()
                player1StonesCountText.visibility = View.VISIBLE
                player2StonesCountText.visibility = View.INVISIBLE
            } else {
                player2StonesCountText.text = remainingStones.toString()
                player2StonesCountText.visibility = View.VISIBLE
                player1StonesCountText.visibility = View.INVISIBLE
            }

            updateUI()

            distributeSeedsAnimated(startIndex, remainingStones)
        }
    }

    // Taşları animasyonlu bir şekilde dağıtma
    private fun distributeSeedsAnimated(startIndex: Int, remainingStones: Int) {
        var currentIndex = startIndex
        var stonesLeft = remainingStones
        var lastIndex = -1

        val runnable = object : Runnable {
            override fun run() {
                if (stonesLeft == 0) {
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

                board[currentIndex]++
                soundPool.play(stoneSoundId, 1.0f, 1.0f, 1, 0, 1.0f)

                lastIndex = currentIndex
                stonesLeft--

                // Kalan taş sayısını güncelle
                if (currentPlayer == 1) {
                    player1StonesCountText.text = stonesLeft.toString()
                } else {
                    player2StonesCountText.text = stonesLeft.toString()
                }

                updateUI()
                handler.postDelayed(this, 300)
            }
        }
        handler.post(runnable)
    }

    // Oyun kurallarını uygulayan fonksiyon
    private fun applyGameRules(lastIndex: Int) {
        var nextPlayer = if (currentPlayer == 1) 2 else 1

        when {
            // KURAL 1: Son taş kendi hazinesine gelirse bir tur daha oynar.
            (currentPlayer == 1 && lastIndex == 6) || (currentPlayer == 2 && lastIndex == 13) -> {
                nextPlayer = currentPlayer
            }

            // KURAL 2: Son taş rakibin bölgesinde çift sayıda taş olan bir kuyuya düşerse, tüm taşları alır.
            (currentPlayer == 1 && lastIndex in 7..12 && board[lastIndex] % 2 == 0) -> {
                board[6] += board[lastIndex]
                board[lastIndex] = 0
                nextPlayer = 2
            }
            (currentPlayer == 2 && lastIndex in 0..5 && board[lastIndex] % 2 == 0) -> {
                board[13] += board[lastIndex]
                board[lastIndex] = 0
                nextPlayer = 1
            }

            // KURAL 3: Son taş kendi bölgesindeki boş kuyuya denk gelirse ve karşısında taş varsa, hem o taşı hem de karşıdaki taşları alır.
            (currentPlayer == 1 && lastIndex in 0..5 && board[lastIndex] == 1) -> {
                val oppositePocketIndex = 12 - lastIndex
                if (board[oppositePocketIndex] > 0) {
                    board[6] += board[oppositePocketIndex] + 1
                    board[oppositePocketIndex] = 0
                    board[lastIndex] = 0
                }
                nextPlayer = 2
            }
            (currentPlayer == 2 && lastIndex in 7..12 && board[lastIndex] == 1) -> {
                val oppositePocketIndex = 12 - lastIndex
                if (board[oppositePocketIndex] > 0) {
                    board[13] += board[oppositePocketIndex] + 1
                    board[oppositePocketIndex] = 0
                    board[lastIndex] = 0
                }
                nextPlayer = 1
            }
        }

        currentPlayer = nextPlayer
        checkGameOver()
        updateUI()

        // Tekli oyun modu aktifse, sıra bilgisayardaysa hamle yapar
        if (isSinglePlayer && currentPlayer == 2 && !isMoving) {
            handler.postDelayed({
                playComputerTurn()
            }, 1500)
        }
    }

    // Basit Yapay Zeka (Bilgisayar) Hamlesi
    private fun playComputerTurn() {
        var bestMoveIndex = -1

        // Kural 1: Kendi haznesine son taşı getiren hamleyi ara
        for (i in 7..12) {
            if (board[i] > 0) {
                // Son taşın düşeceği kuyunun indeksi
                val finalPocketIndex = (i + board[i] - 1) % board.size
                if (finalPocketIndex == 13) {
                    bestMoveIndex = i
                    break
                }
            }
        }

        // Kural 2: Rakibin taşını çalma fırsatı varsa
        if (bestMoveIndex == -1) {
            for (i in 7..12) {
                if (board[i] > 0) {
                    val finalPocketIndex = (i + board[i] - 1) % board.size
                    if (finalPocketIndex in 0..5) {
                        // Eğer son taş, rakibin cebindeki taşları çift yapıyorsa
                        if ((board[finalPocketIndex] + 1) % 2 == 0) {
                            bestMoveIndex = i
                            break
                        }
                    }
                }
            }
        }

        // Kural 3: Kendi boş kuyusuna son taşı getirme ve karşıdan taş çalma fırsatı varsa
        if (bestMoveIndex == -1) {
            for (i in 7..12) {
                if (board[i] > 0) {
                    val finalPocketIndex = (i + board[i] - 1) % board.size
                    if (finalPocketIndex in 7..12 && board[finalPocketIndex] == 0) {
                        val oppositePocketIndex = 12 - finalPocketIndex
                        if (board[oppositePocketIndex] > 0) {
                            bestMoveIndex = i
                            break
                        }
                    }
                }
            }
        }

        // En çok taşa sahip cepten oyna (varsayılan)
        if (bestMoveIndex == -1) {
            var maxStones = -1
            var possibleMoves = mutableListOf<Int>()
            for (i in 7..12) {
                if (board[i] > 0) {
                    if (board[i] > maxStones) {
                        maxStones = board[i]
                        possibleMoves.clear()
                        possibleMoves.add(i)
                    } else if (board[i] == maxStones) {
                        possibleMoves.add(i)
                    }
                }
            }
            if (possibleMoves.isNotEmpty()) {
                bestMoveIndex = possibleMoves[Random.nextInt(possibleMoves.size)]
            }
        }

        if (bestMoveIndex != -1) {
            playTurn(bestMoveIndex)
        }
    }

    // Setin bitip bitmediğini kontrol etme
    private fun checkGameOver() {
        val player1PocketsEmpty = (0..5).all { board[it] == 0 }
        val player2PocketsEmpty = (7..12).all { board[it] == 0 }

        if (player1PocketsEmpty || player2PocketsEmpty) {
            // KURAL 4: Oyunculardan herhangi birinin bölgesi boşaldığında, rakibinin bölgesindeki tüm taşları kazanır.
            if (player1PocketsEmpty) {
                for (i in 7..12) {
                    board[6] += board[i]
                    board[i] = 0
                }
            } else if (player2PocketsEmpty) {
                for (i in 0..5) {
                    board[13] += board[i]
                    board[i] = 0
                }
            }

            // Set sonu
            val setWinnerMessage = when {
                board[6] > board[13] -> {
                    player1SetsWon++
                    "Set Bitti! $player1Name Kazandı!"
                }
                board[13] > board[6] -> {
                    player2SetsWon++
                    "Set Bitti! $player2Name Kazandı!"
                }
                else -> "Set Berabere Bitti!"
            }

            // Genel oyunun bitip bitmediğini kontrol et
            if (currentSet < 5) {
                Toast.makeText(this, setWinnerMessage, Toast.LENGTH_SHORT).show()
                handler.postDelayed({
                    currentSet++
                    resetSet()
                }, 2000)
            } else {
                // Oyunun genel kazananını belirle
                val finalWinnerMessage = when {
                    player1SetsWon > player2SetsWon -> "Maç Bitti! $player1Name ${player1SetsWon}-${player2SetsWon} ile Kazandı!"
                    player2SetsWon > player1SetsWon -> "Maç Bitti! $player2Name ${player2SetsWon}-${player1SetsWon} ile Kazandı!"
                    else -> "Maç Berabere Bitti!"
                }
                statusText.text = finalWinnerMessage
                pockets.forEach { it.isEnabled = false }
                isMoving = true
            }
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
                button.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            } else {
                button.isEnabled = false
                button.alpha = 0.5f
                button.setTextColor(Color.WHITE)
            }
        }
        store1.text = board[6].toString()
        store2.text = board[13].toString()

        // Sıra metnini güncelle
        if (!isMoving) {
            if (currentPlayer == 1) {
                statusText.text = "Sıra: $player1Name"
                statusText.rotation = 0f
            } else {
                statusText.text = "Sıra: $player2Name"
                statusText.rotation = 180f
            }
        }
        setScoreText.text = "Set: $currentSet / 5\nSkor: $player1SetsWon - $player2SetsWon"
    }
}
