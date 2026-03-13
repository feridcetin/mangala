package com.feridcetin.mangala

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlin.random.Random

// Google Mobile Ads
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

// (İstersen splash'i tamamen bırak demiştin; sende hâlâ var. Kalsın istiyorsan sorun yok.)
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.ads.RequestConfiguration

class MainActivity : AppCompatActivity() {


    private var bannerAdView: AdView? = null

    // UI
    private lateinit var pockets: List<Button>
    private lateinit var store1: TextView
    private lateinit var store2: TextView
    private lateinit var statusText: TextView
    private lateinit var menuButton: Button
    private lateinit var player1StonesCountText: TextView
    private lateinit var player2StonesCountText: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var setScoreText: TextView

    // Game state
    private var player1SetsWon = 0
    private var player2SetsWon = 0
    private var currentSet = 1

    private var player1Name = "Oyuncu 1"
    private var player2Name = "Oyuncu 2"

    private lateinit var soundPool: SoundPool
    private var stoneSoundId: Int = 0

    private var board = IntArray(14)
    private var currentPlayer = 1
    private var isMoving = false
    private var isSinglePlayer = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sharedPreferences: SharedPreferences

    // Ads
    private var interstitialAd: InterstitialAd? = null
    private var lastInterstitialShownAt = 0L

    // Pocket pulse map
    private val pocketButtonByBoardIndex: Array<Button?> = arrayOfNulls(14)


    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("OyunAyarlari", Context.MODE_PRIVATE)
        player1Name = sharedPreferences.getString("player1Name", "Oyuncu 1")!!
        player2Name = sharedPreferences.getString("player2Name", "Oyuncu 2")!!

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContentView(R.layout.activity_main)

        // UI bind
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        menuButton = findViewById(R.id.button_open_menu)

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
        player1StonesCountText = findViewById(R.id.textView_sayi1)
        player2StonesCountText = findViewById(R.id.textView_sayi2)
        setScoreText = findViewById(R.id.textView_set_score)

        // ✅ 1) Families / under-age / rating ayarları (global) — initialize ve reklam yüklemeden önce
        configureMobileAdsForFamilies()

        // ✅ 2) Mobile Ads SDK init
        MobileAds.initialize(this)

        // ✅ 3) Ads yüklemeleri
        preloadInterstitial()
        loadBanner()   // senin mevcut banner fonksiyonun

        // Sound
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()
        stoneSoundId = soundPool.load(this, R.raw.stone_sound, 1)

        // Menu open (görünmez/arkada kalma sorununa karşı güçlendirilmiş)
        menuButton.setOnClickListener {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            navigationView.bringToFront()
            navigationView.translationZ = 50f
            navigationView.elevation = 50f
            drawerLayout.setDrawerElevation(50f)
            drawerLayout.requestLayout()
            drawerLayout.openDrawer(navigationView, true)
        }

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
                    player2Name = "Bilgisayar"
                    Toast.makeText(this, "Tekli oyun modu başlatıldı.", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawers()
                    updateUI()
                    true
                }
                R.id.nav_multi_player -> {
                    resetGame()
                    isSinglePlayer = false
                    player2Name = "Oyuncu 2"
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
                R.id.nav_how_to_play -> {
                    showHowToPlayDialog()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.action_logout -> {
                    finishAffinity()
                    true
                }
                else -> false
            }
        }

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

        resetGame()
        bindPocketMap()
    }

    /**
     * ✅ Global ad request konfigürasyonu:
     * - COPPA child-directed treatment
     * - Under age of consent (EEA)
     * - Max ad content rating: G (Families / general audience)
     *
     * setRequestConfiguration() globaldir ve her AdRequest için geçerlidir. [1](https://developers.google.com/admob/android/reference/com/google/android/gms/ads/MobileAds)[2](https://developers.google.com/ad-manager/mobile-ads-sdk/android/reference/com/google/android/gms/ads/RequestConfiguration)
     */
    private fun configureMobileAdsForFamilies() {
        val requestConfiguration = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) // [2](https://developers.google.com/ad-manager/mobile-ads-sdk/android/reference/com/google/android/gms/ads/RequestConfiguration)[3](https://developers.google.com/admob/android/reference/com/google/android/gms/ads/RequestConfiguration.Builder)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)         // [2](https://developers.google.com/ad-manager/mobile-ads-sdk/android/reference/com/google/android/gms/ads/RequestConfiguration)[3](https://developers.google.com/admob/android/reference/com/google/android/gms/ads/RequestConfiguration.Builder)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)                        // [2](https://developers.google.com/ad-manager/mobile-ads-sdk/android/reference/com/google/android/gms/ads/RequestConfiguration)[3](https://developers.google.com/admob/android/reference/com/google/android/gms/ads/RequestConfiguration.Builder)
            .build()

        MobileAds.setRequestConfiguration(requestConfiguration) // [1](https://developers.google.com/admob/android/reference/com/google/android/gms/ads/MobileAds)
    }


    // ============ ADS ============
    private fun loadBanner() {
        val adContainer = findViewById<FrameLayout>(R.id.ad_view_container)
        val adView = AdView(this)
        adView.adUnitId = "ca-app-pub-2120666198065087/5263827432"
        adView.setAdSize(AdSize.BANNER)

        adContainer.removeAllViews()
        adContainer.addView(adView)

        adView.loadAd(AdRequest.Builder().build())
        bannerAdView = adView
    }

    private fun preloadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-2120666198065087/8926430766",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialIfReady(onContinue: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownAt < 60_000) {
            onContinue()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            onContinue()
            preloadInterstitial()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial()
                onContinue()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                preloadInterstitial()
                onContinue()
            }
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAt = System.currentTimeMillis()
            }
        }

        ad.show(this)
    }

    override fun onPause() {
        bannerAdView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        bannerAdView?.resume()
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        soundPool.release()
        super.onDestroy()
    }

    // ============ OYUN (mevcut kodun) ============

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
            .setPositiveButton("Tamam") { _, _ ->
                val newPlayer1Name = player1Input.text.toString().trim()
                val newPlayer2Name = player2Input.text.toString().trim()

                if (newPlayer1Name.isNotEmpty()) {
                    player1Name = newPlayer1Name
                    sharedPreferences.edit().putString("player1Name", newPlayer1Name).apply()
                }
                if (newPlayer2Name.isNotEmpty()) {
                    player2Name = newPlayer2Name
                    sharedPreferences.edit().putString("player2Name", newPlayer2Name).apply()
                }
                Toast.makeText(this, "İsimler güncellendi!", Toast.LENGTH_SHORT).show()
                updateUI()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showHowToPlayDialog() {
        val rulesText = """
            <b>Oyunun Amacı:</b> Rakibinizden daha fazla taş toplamak.
            <br><br>
            <b>Oyunun Kuralları:</b>
            <br><br>
            1. <b>Başlangıç:</b> Her cepte 4 taş bulunur. Sırasıyla oyuncular kendi ceplerinden taş alıp dağıtır.
            <br><br>
            2. <b>Hamle:</b> Seçtiğiniz cebin içindeki tüm taşları alıp, saatin tersi yönünde her cebe birer tane bırakırsınız.
            <br><br>
            3. <b>Ekstra Hamle:</b> Son taşınızı kendi hazinenize (büyük kuyu) bırakırsanız, bir tur daha oynama hakkı kazanırsınız.
            <br><br>
            4. <b>Taş Çalma (Çift Kuralı):</b> Son taşınız rakip bölgesinde çift olursa o taşları alırsınız.
            <br><br>
            5. <b>Taş Çalma (Boş Cep Kuralı):</b> Son taş kendi boş cebinize düşerse karşı cebi alırsınız.
            <br><br>
            6. <b>Oyunun Sonu:</b> Bir taraf boşalınca oyun biter.
            <br><br>
            <b>Kazanan:</b> 5 set sonunda en çok seti kazanan maçı kazanır.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Nasıl Oynanır?")
            .setMessage(Html.fromHtml(rulesText, Html.FROM_HTML_MODE_COMPACT))
            .setPositiveButton("Anladım", null)
            .show()
    }

    private fun resetGame() {
        player1SetsWon = 0
        player2SetsWon = 0
        currentSet = 1
        resetSet()
    }

    private fun resetSet() {
        board = intArrayOf(4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 0)
        currentPlayer = 1
        isMoving = false
        player1StonesCountText.visibility = View.INVISIBLE
        player2StonesCountText.visibility = View.INVISIBLE
        updateUI()
    }

    private fun playTurn(startIndex: Int) {
        if (isMoving) return
        isMoving = true

        val stonesInPocket = board[startIndex]

        if (stonesInPocket == 1) {
            board[startIndex] = 0
            var nextIndex = (startIndex + 1) % board.size

            if (currentPlayer == 1 && nextIndex == 13) {
                nextIndex = (nextIndex + 1) % board.size
            } else if (currentPlayer == 2 && nextIndex == 6) {
                nextIndex = (nextIndex + 1) % board.size
            }

            board[nextIndex]++
            soundPool.play(stoneSoundId, 1.0f, 1.0f, 1, 0, 1.0f)

            isMoving = false
            applyGameRules(nextIndex)
        } else {
            board[startIndex] = 1
            val remainingStones = stonesInPocket - 1

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

                if (currentPlayer == 1 && currentIndex == 13) {
                    currentIndex = (currentIndex + 1) % board.size
                } else if (currentPlayer == 2 && currentIndex == 6) {
                    currentIndex = (currentIndex + 1) % board.size
                }

                board[currentIndex]++
                soundPool.play(stoneSoundId, 1.0f, 1.0f, 1, 0, 1.0f)

                pulsePocket(currentIndex)

                lastIndex = currentIndex
                stonesLeft--

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

    private fun applyGameRules(lastIndex: Int) {
        var nextPlayer = if (currentPlayer == 1) 2 else 1

        when {
            (currentPlayer == 1 && lastIndex == 6) || (currentPlayer == 2 && lastIndex == 13) -> {
                nextPlayer = currentPlayer
            }
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

        if (isSinglePlayer && currentPlayer == 2 && !isMoving) {
            handler.postDelayed({ playComputerTurn() }, 1500)
        }
    }

    private fun playComputerTurn() {
        var bestMoveIndex = -1

        for (i in 7..12) {
            if (board[i] > 0) {
                val finalPocketIndex = (i + board[i] - 1) % board.size
                if (finalPocketIndex == 13) { bestMoveIndex = i; break }
            }
        }

        if (bestMoveIndex == -1) {
            for (i in 7..12) {
                if (board[i] > 0) {
                    val finalPocketIndex = (i + board[i] - 1) % board.size
                    if (finalPocketIndex in 0..5) {
                        if ((board[finalPocketIndex] + 1) % 2 == 0) { bestMoveIndex = i; break }
                    }
                }
            }
        }

        if (bestMoveIndex == -1) {
            for (i in 7..12) {
                if (board[i] > 0) {
                    val finalPocketIndex = (i + board[i] - 1) % board.size
                    if (finalPocketIndex in 7..12 && board[finalPocketIndex] == 0) {
                        val oppositePocketIndex = 12 - finalPocketIndex
                        if (board[oppositePocketIndex] > 0) { bestMoveIndex = i; break }
                    }
                }
            }
        }

        if (bestMoveIndex == -1) {
            var maxStones = -1
            val possibleMoves = mutableListOf<Int>()
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

        if (bestMoveIndex != -1) playTurn(bestMoveIndex)
    }

    private fun checkGameOver() {
        val player1PocketsEmpty = (0..5).all { board[it] == 0 }
        val player2PocketsEmpty = (7..12).all { board[it] == 0 }

        if (player1PocketsEmpty || player2PocketsEmpty) {
            if (player1PocketsEmpty) {
                for (i in 7..12) { board[6] += board[i]; board[i] = 0 }
            } else if (player2PocketsEmpty) {
                for (i in 0..5) { board[13] += board[i]; board[i] = 0 }
            }

            val setWinnerMessage = when {
                board[6] > board[13] -> { player1SetsWon++; "Set Bitti! $player1Name Kazandı!" }
                board[13] > board[6] -> { player2SetsWon++; "Set Bitti! $player2Name Kazandı!" }
                else -> "Set Berabere Bitti!"
            }

            if (currentSet < 5) {
                Toast.makeText(this, setWinnerMessage, Toast.LENGTH_SHORT).show()

                // ✅ Set sonu doğal geçiş -> interstitial burada (policy uyumlu) [2](https://sgs-my.sharepoint.com/personal/ferid_cetin_sgs_com/Documents/Microsoft%20Copilot%20Chat%20Files/ic_person.xml)[3](https://sgs-my.sharepoint.com/personal/ferid_cetin_sgs_com/Documents/Microsoft%20Copilot%20Chat%20Files/pocket_background_oval_player1.xml)[4](https://deepwiki.com/googleads/googleads-mobile-android-examples/4-banner-ads)
                handler.postDelayed({
                    showInterstitialIfReady {
                        currentSet++
                        resetSet()
                    }
                }, 2000)

            } else {
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

    private fun updateUI() {
        pockets.forEachIndexed { index, button ->
            val pocketIndex = if (index < 6) index else index + 1
            button.text = board[pocketIndex].toString()

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

    private fun bindPocketMap() {
        pocketButtonByBoardIndex[0] = findViewById(R.id.button_pocket0)
        pocketButtonByBoardIndex[1] = findViewById(R.id.button_pocket1)
        pocketButtonByBoardIndex[2] = findViewById(R.id.button_pocket2)
        pocketButtonByBoardIndex[3] = findViewById(R.id.button_pocket3)
        pocketButtonByBoardIndex[4] = findViewById(R.id.button_pocket4)
        pocketButtonByBoardIndex[5] = findViewById(R.id.button_pocket5)

        pocketButtonByBoardIndex[7] = findViewById(R.id.button_pocket7)
        pocketButtonByBoardIndex[8] = findViewById(R.id.button_pocket8)
        pocketButtonByBoardIndex[9] = findViewById(R.id.button_pocket9)
        pocketButtonByBoardIndex[10] = findViewById(R.id.button_pocket10)
        pocketButtonByBoardIndex[11] = findViewById(R.id.button_pocket11)
        pocketButtonByBoardIndex[12] = findViewById(R.id.button_pocket12)
    }

    private fun pulsePocket(boardIndex: Int) {
        val v = pocketButtonByBoardIndex[boardIndex] ?: return
        v.animate().cancel()

        v.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(90)
            .withEndAction {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }
}