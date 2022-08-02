package com.p_en_s.wanwanpechipechihit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private var onTapDisabled: Boolean = false

    private var mSoundPool: SoundPool? = null

    private var mSoundResIdStageSelect: Int? = 0

    private var arrayMediaPlayer: Array<MediaPlayer?>? = null
    private var threadSoundLoop: Thread? = null

    private var displayState: Int = 1

    private lateinit var adView: AdView

    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = adViewContainer.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private fun loadBanner() {
        adView.adUnitId = getString(R.string.ad_unit_id_banner)
        adView.adSize = adSize
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // バナー広告の設定
        adView = AdView(this)
        adViewContainer.addView(adView)
        loadBanner()

        // ステージデータをリソースから取得
        val bossStage = resources.getInteger(R.integer.boss_stage)
        val maxStage = resources.getInteger(R.integer.max_stage)

        // 保存データを取得
        val dataStore: SharedPreferences = getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        // クリア済みのステージIDを取得
        val clearStageId = dataStore.getInt("clearStageId", 0)

        // ステージから戻ってきたら初期状態をステージ選択にする
        val fromStage = intent.getIntExtra("fromStage", 0)
        if (fromStage == 1) {
            displayState = 2
            layoutTitle.alpha = 0f
            layoutTitle.visibility = View.GONE
            layoutStageSelect.alpha = 1f
            layoutStageSelect.visibility = View.VISIBLE
        }

        // タイトル画面のキャラクター表示とステージボタンの制御
        for (imageCharacterId in 0..maxStage) {
            // クリア済みのステージ分はタイトル画面にキャラクターを表示して次のステージまでボタンを有効化する
            if (clearStageId >= imageCharacterId) {
                val buttonStageSelectId = imageCharacterId + 1
                if (imageCharacterId > 0) {
                    val imageCharacter: ImageView = findViewById(resources.getIdentifier(
                        "imageCharacter${imageCharacterId}",
                        "id",
                        packageName
                    ))
                    // タイトル画面にキャラクターを表示
                    imageCharacter.visibility = View.VISIBLE
                    // キャラクターのアニメーションを設定
                    if (imageCharacterId < bossStage) {
                        setAnimator(imageCharacter)
                    } else {
                        setAnimatorSecret(imageCharacter)
                    }
                }

                if (buttonStageSelectId <= maxStage) {
                    val buttonStageSelect: Button = findViewById(resources.getIdentifier(
                        "buttonStageSelect${buttonStageSelectId}",
                        "id",
                        packageName
                    ))
                    // ボタンのテキストをクリア済みの色に変更
                    buttonStageSelect.setTextColor(ContextCompat.getColor(this, R.color.colorButtonClearText))
                    // ボタンの有効化
                    buttonStageSelect.isEnabled = true
                    // ボタンを押したときの動作
                    buttonStageSelect.setOnClickListener {
                        onStageClick(buttonStageSelectId)
                    }
                }
            }
        }
        // スタートボタンを押したときの動作
        buttonStart.setOnClickListener {
            // ステージ選択画面を表示
            viewStageSelect()
        }
        // クレジットボタンを押したときの動作
        buttonCredit.setOnClickListener {
            // クレジットを表示
            viewCredit()
        }
        // クレジットの背景を押したときの動作
        creditOverView.setOnClickListener {
            // クレジットを非表示
            hideCredit()
        }
        // チュートリアルボタンを押したときの動作
        buttonTutorial.setOnClickListener {
            // チュートリアルに移動
            onTutorialClick(0)
        }
    }

    override fun onResume() {
        super.onResume()
        // BGM再生
        playMediaPlayer()

        // 効果音のロード
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        mSoundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(1)
            .build()
        mSoundResIdStageSelect = mSoundPool?.load(this, R.raw.se_stage_select,1)
    }

    override fun onPause() {
        super.onPause()
        // BGM削除
        releaseMediaPlayer()
        // 効果音の削除
        mSoundPool?.release()
        mSoundPool = null
    }

    override fun onBackPressed() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            when (displayState) {
                1 -> {
                    // アプリの終了
                    moveTaskToBack(true)
                }
                2 -> {
                    // タイトル画面の表示
                    viewTitle()
                }
                3 -> {
                    // クレジットの非表示
                    hideCredit()
                }
                4 -> {
                    // クレジットの非表示
                    hideCredit()
                }
            }
        }
    }

    private fun setAnimator(image: ImageView) {
        // 左右移動の範囲
        val translationX1: Float = Random.nextInt(10, 30) * 1f
        val translationX2: Float = Random.nextInt(10, 30) * -1f
        // 上下移動の範囲
        val translationY1: Float = Random.nextInt(10, 30) * 1f
        val translationY2: Float = Random.nextInt(10, 30) * -1f
        // 傾きの範囲
        val rotation1: Float = Random.nextInt(5, 10) * 1f
        val rotation2: Float = Random.nextInt(5, 10) * -1f
        // 各動作の繰り返し時間
        val durationX: Long = Random.nextLong(30, 50) * 100
        val durationY: Long = Random.nextLong(30, 50) * 100
        val durationR: Long = Random.nextLong(30, 50) * 100

        // 左右移動のアニメーション
        val objectAnimatorX = ObjectAnimator.ofFloat(image, "translationX", 0f, translationX1, 0f, translationX2, 0f)
        objectAnimatorX.duration  = durationX
        objectAnimatorX.repeatCount = -1
        // 上下移動のアニメーション
        val objectAnimatorY = ObjectAnimator.ofFloat(image, "translationY", 0f, translationY1, 0f, translationY2, 0f)
        objectAnimatorY.duration  = durationY
        objectAnimatorY.repeatCount = -1
        // 傾きのアニメーション
        val objectAnimatorR = ObjectAnimator.ofFloat(image, "rotation", 0f, rotation1, 0f, rotation2, 0f)
        objectAnimatorR.duration  = durationR
        objectAnimatorR.repeatCount = -1

        // 各アニメーションを同時に実行させる
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            objectAnimatorX,
            objectAnimatorY,
            objectAnimatorR
        )
        animatorSet.start()
    }

    private fun setAnimatorSecret(image: ImageView) {
        // 左右移動（動作停止）の時間
        val durationY: Long = Random.nextLong(50, 80) * 100

        // 上下移動のアニメーション
        val objectAnimatorX = ObjectAnimator.ofFloat(image, "translationY", 0f, -5f, 0f, -5f, 0f)
        objectAnimatorX.duration = 500
        // 左右移動（動作停止）のアニメーション
        val objectAnimatorY = ObjectAnimator.ofFloat(image, "translationX", 0f)
        objectAnimatorY.duration = durationY

        // 各アニメーションを順番に実行させる
        val animatorSet = AnimatorSet()
        animatorSet.interpolator = LinearInterpolator()
        animatorSet.playSequentially(
            objectAnimatorX,
            objectAnimatorY
        )
        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(p0: Animator?) {
                // アニメーション終了時に次のアニメーションを開始
                setAnimatorSecret(image)
            }
            override fun onAnimationRepeat(p0: Animator?) {}
            override fun onAnimationCancel(p0: Animator?) {}
            override fun onAnimationStart(p0: Animator?) {}
        })
        animatorSet.start()
    }


    private fun viewTitle() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true
            // 画面表示のステータスを変更
            displayState = 1
            // ステージ選択を非表示にしてタイトルを表示させるアニメーション
            val animatorSet = AnimatorSet()
            animatorSet.interpolator = LinearInterpolator()
            animatorSet.duration = 500
            animatorSet.playSequentially(
                ObjectAnimator.ofFloat(layoutStageSelect, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(layoutTitle, "alpha", 0f, 1f)
            )
            animatorSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(p0: Animator?) {
                    // アニメーション終了後にステージ選択のレイアウトの存在を消す
                    layoutStageSelect.visibility = View.GONE
                    // タップの無効化を解除
                    onTapDisabled = false
                }
                override fun onAnimationRepeat(p0: Animator?) {}
                override fun onAnimationCancel(p0: Animator?) {}
                override fun onAnimationStart(p0: Animator?) {
                    // アニメーション開始時にタイトルのレイアウトを有効な状態にする
                    layoutTitle.visibility = View.VISIBLE
                }
            })
            animatorSet.start()
        }
    }


    private fun viewStageSelect() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true
            // 画面表示のステータスを変更
            displayState = 2
            // タイトルを非表示にしてステージ選択を表示させるアニメーション
            val animatorSet = AnimatorSet()
            animatorSet.interpolator = LinearInterpolator()
            animatorSet.duration = 500
            animatorSet.playSequentially(
                ObjectAnimator.ofFloat(layoutTitle, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(layoutStageSelect, "alpha", 0f, 1f)
            )
            animatorSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(p0: Animator?) {
                    // アニメーション終了後にタイトルのレイアウトの存在を消す
                    layoutTitle.visibility = View.GONE
                    // タップの無効化を解除
                    onTapDisabled = false
                }
                override fun onAnimationRepeat(p0: Animator?) {}
                override fun onAnimationCancel(p0: Animator?) {}
                override fun onAnimationStart(p0: Animator?) {
                    // アニメーション開始時にステージ選択のレイアウトを有効な状態にする
                    layoutStageSelect.visibility = View.VISIBLE
                }
            })
            animatorSet.start()
        }
    }

    private fun viewCredit() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true
            // 画面表示のステータスを変更
            displayState = 3
            // クレジットを表示させるアニメーション
            val animatorSet = AnimatorSet()
            animatorSet.interpolator = LinearInterpolator()
            animatorSet.duration = 500
            animatorSet.playSequentially(
                ObjectAnimator.ofFloat(creditOverView, "alpha", 0f, 1f)
            )
            animatorSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(p0: Animator?) {
                    // タップの無効化を解除
                    onTapDisabled = false
                }
                override fun onAnimationRepeat(p0: Animator?) {}
                override fun onAnimationCancel(p0: Animator?) {}
                override fun onAnimationStart(p0: Animator?) {
                    // アニメーション開始時にクレジットのレイアウトを有効な状態にする
                    creditOverView.visibility = View.VISIBLE
                }
            })
            animatorSet.start()
        }
    }

    private fun hideCredit() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true
            // 画面表示のステータスを変更
            displayState = 2
            // クレジットを非表示にするアニメーション
            val animatorSet = AnimatorSet()
            animatorSet.interpolator = LinearInterpolator()
            animatorSet.duration = 500
            animatorSet.playSequentially(
                ObjectAnimator.ofFloat(creditOverView, "alpha", 1f, 0f)
            )
            animatorSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(p0: Animator?) {
                    // アニメーション終了後にクレジットのレイアウトの存在を消す
                    creditOverView.visibility = View.GONE
                    // タップの無効化を解除
                    onTapDisabled = false
                }
                override fun onAnimationRepeat(p0: Animator?) {}
                override fun onAnimationCancel(p0: Animator?) {}
                override fun onAnimationStart(p0: Animator?) {}
            })
            animatorSet.start()
        }
    }

    private fun onStageClick(stageId: Int) {
        // 保存データを取得
        val dataStore: SharedPreferences = getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        // チュートリアルへの移動が必要か判定して、必要ならチュートリアルに移動させる
        val gotoTutorial = dataStore.getInt("gotoTutorial", 1)
        if (gotoTutorial == 1) {
            onTutorialClick(stageId)
        }
        // タップ動作無効化されていなければ処理
       if (!onTapDisabled) {
           // タップを無効化
            onTapDisabled = true
            // タップ音を鳴らす
            mSoundPool?.play(mSoundResIdStageSelect!!, 1.0f, 1.0f, 0, 0, 1.0f)
           // 画面を徐々に暗くするアニメーション
            val animatorReady = layoutStageSelectOverView.animate()
            animatorReady.duration = 2000
            animatorReady.alpha(1f)
            animatorReady.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    // ステージに移動
                    val intent = Intent(application, StageActivity::class.java)
                    intent.putExtra("stageId", stageId)
                    startActivity(intent)
                    // BGM削除
                    releaseMediaPlayer()
                    // 効果音の削除
                    mSoundPool?.release()
                    mSoundPool = null
                    finish()
                }
                override fun onAnimationStart(p0: Animator?) {
                    // アニメーション開始時に暗くするためのレイアウトを有効な状態にする
                    layoutStageSelectOverView.visibility = View.VISIBLE
                }
            })
            animatorReady.start()
        }
    }
    private fun onTutorialClick(stageId: Int) {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true
            // チュートリアルに移動
            val intent = Intent(application, TutorialActivity::class.java)
            intent.putExtra("returnStageId", stageId)
            startActivity(intent)
            // BGM削除
            releaseMediaPlayer()
            // 効果音の削除
            mSoundPool?.release()
            mSoundPool = null
            finish()
        }
    }

    private fun playMediaPlayer() {
        //MediaPlayerの設定
        arrayMediaPlayer = arrayOf(
            MediaPlayer.create(this, R.raw.bgm_title),
            MediaPlayer.create(this, R.raw.bgm_title)
        )

        //初回再生時のノイズ除去処理
        for (i in arrayMediaPlayer!!.indices) {
            arrayMediaPlayer!![i]?.setVolume(0f, 0f)
            arrayMediaPlayer!![i]?.isLooping = false
            arrayMediaPlayer!![i]?.seekTo(arrayMediaPlayer!![i]?.duration!! - 200 * (i + 1))
            arrayMediaPlayer!![i]?.setOnCompletionListener { mediaPlayer ->
                mediaPlayer.setVolume(1f, 1f)
                mediaPlayer.seekTo(0)
                mediaPlayer.setOnCompletionListener { mediaPlayer2 -> mediaPlayer2.seekTo(0) }
                if (threadSoundLoop == null) {
                    threadSoundLoop = Thread(
                        RunnableSoundLoop(
                            arrayMediaPlayer!!,
                            System.currentTimeMillis()
                        )
                    )
                    threadSoundLoop!!.isDaemon = true
                    threadSoundLoop!!.start()
                    mediaPlayer.start()
                }
            }
            arrayMediaPlayer!![i]?.start()
        }
    }

    private fun releaseMediaPlayer() {
        for (i in arrayMediaPlayer!!.indices) {
            if (arrayMediaPlayer!![i] != null) {
                arrayMediaPlayer!![i]?.stop()
                arrayMediaPlayer!![i]?.release()
                arrayMediaPlayer!![i] = null
            }
        }
        threadSoundLoop = null
    }
}