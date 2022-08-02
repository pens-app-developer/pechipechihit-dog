package com.p_en_s.wanwanpechipechihit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.*
import kotlinx.android.synthetic.main.activity_stage.*
import java.util.*
import kotlin.random.Random


class StageActivity : AppCompatActivity() {

    private var bossStage: Int = 0
    private var maxStage: Int = 0
    private var stageId: Int = 0
    private var counter: Int = 0
    private var targetCount: Int = 0
    private var timeLimit: Int = 0
    private var mTimerTask: MainTimerTask = MainTimerTask()
    private var mTimer: Timer? = null
    private var arrayImageCharacterId: ArrayList<Int> = arrayListOf()
    private var nowImageCharacterIndex: Int = 0

    private var onTapDisabled: Boolean = true

    private var mSoundPool: SoundPool? = null

    private var mSoundResIdTap: Int? = 0
    private var mSoundResIdStart: Int? = 0
    private var mSoundResIdSuccess: Int? = 0
    private var mSoundResIdFailure: Int? = 0

    private var arrayMediaPlayer: Array<MediaPlayer?>? = null
    private var threadSoundLoop: Thread? = null

    private lateinit var mInterstitialAd: InterstitialAd

    private var isViewAd: Boolean = false

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stage)

        // バナー広告の設定
        adView = AdView(this)
        adViewContainer.addView(adView)
        loadBanner()

        // 全画面広告の設定
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = getString(R.string.ad_unit_id_interstitial)
        mInterstitialAd.loadAd(AdRequest.Builder().build())
        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }

        // ステージデータをリソースから取得
        bossStage = resources.getInteger(R.integer.boss_stage)
        maxStage = resources.getInteger(R.integer.max_stage)
        val targetCounts: IntArray = resources.getIntArray(R.array.target_counts)
        val timeLimits: IntArray = resources.getIntArray(R.array.time_limits)

        // ステージIDを設定
        stageId = intent.getIntExtra("stageId", 1)
        // 目標タップ回数を設定
        targetCount = targetCounts[stageId - 1]
        progressBarTapCounter.max = targetCount
        // 制限時間を設定
        timeLimit = timeLimits[stageId - 1]
        progressBarTimeCounter.max = timeLimit

        // キャラクターの各画像IDを取得して格納
        var imageNum = 0
        while (true){
            imageNum++
            // 画像IDを取得
            val resourcesId = resources.getIdentifier(
                "character_${stageId}_${imageNum}",
                "drawable",
                packageName
            )
            // 画像IDが取得できたか判定
            if (resourcesId > 0) {
                // 取得出来ていたらキャラクターの画像IDに追加
                arrayImageCharacterId.add(resourcesId)
            } else {
                // 取得できなかったらループを終了
                break
            }
        }

        // 背景画像を取得して設定
        imageStageBg.setImageResource(resources.getIdentifier(
            "stage_bg_${stageId}",
            "drawable",
            packageName
        ))
        // 成功画像を取得して設定
        imageCharacterSuccess.setImageResource(resources.getIdentifier(
            "character_${stageId}_success",
            "drawable",
            packageName
        ))
        // 失敗画像を設定
        imageCharacterFailure.setImageResource(arrayImageCharacterId[0])

        // タップエリアを押したときの動作
        findViewById<View>(R.id.buttonTapCharacter).setOnTouchListener { v, event ->
            // 押した対象がタップエリアでイベントがシングルタップの押すかマルチタップの押すだった場合に処理
            if (v.id == R.id.buttonTapCharacter && (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)) {
                // キャラクタータップ時の動作
                onCharacterTap(event)
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()

        //　表示を初期化
        nowImageCharacterIndex = 0
        imageCharacter.setImageResource(arrayImageCharacterId[0])
        imageCharacter.visibility = View.VISIBLE
        imageCharacterSuccess.visibility = View.GONE
        imageCharacterFailure.visibility = View.GONE
        imageTextReadyGoReady.visibility = View.GONE
        imageTextReadyGoReady.scaleX = 1.0f
        imageTextReadyGoReady.scaleY = 1.0f
        imageTextReadyGoReady.alpha = 1.0f
        imageTextReadyGoGo.visibility = View.GONE
        imageTextReadyGoGo.scaleX = 1.0f
        imageTextReadyGoGo.scaleY = 1.0f
        imageTextReadyGoGo.alpha = 1.0f
        layoutResultText.visibility = View.GONE
        layoutResultText.scaleX = 4.0f
        layoutResultText.scaleY = 4.0f
        layoutResultText.alpha = 0.0f
        buttonResult.visibility = View.GONE
        buttonResult.alpha = 0.0f
        buttonReturn.visibility = View.GONE
        buttonReturn.alpha = 0.0f
        layoutBestScore.visibility = View.VISIBLE
        layoutScore.alpha = 0.0f
        layoutScore.visibility = View.GONE

        // タップ進行度を初期化
        counter = 0
        progressBarTapCounter.progress = 0

        // 制限時間を初期化
        progressBarTimeCounter.progress = 0

        // BGM再生
        if (bossStage > stageId) {
            playMediaPlayer(R.raw.bgm_stage)
        } else {
            playMediaPlayer(R.raw.bgm_stage_boss)
        }

        // 効果音のロード
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        mSoundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(4)
            .build()
        mSoundResIdTap = mSoundPool?.load(this, R.raw.se_tap,1)
        mSoundResIdStart = mSoundPool?.load(this, R.raw.se_start,1)
        mSoundResIdSuccess = mSoundPool?.load(this, R.raw.se_success,1)
        mSoundResIdFailure = mSoundPool?.load(this, R.raw.se_failure,1)
        // タイマーの設定
        mTimerTask = MainTimerTask()
        mTimerTask.setActivity(this)
        mTimerTask.setTimeLimit(timeLimit)
        mTimer = Timer(true)

        // 開始アニメーション処理
        val animatorReady = imageTextReadyGoReady.animate()
        animatorReady.duration = 2000
        animatorReady.scaleX(4.0f).scaleY(4.0f).alpha(0.0f)
        animatorReady.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                val animatorGo = imageTextReadyGoGo.animate()
                animatorGo.duration = 1500
                animatorGo.scaleX(4.0f).scaleY(4.0f).alpha(0.0f)
                imageTextReadyGoGo.visibility = View.VISIBLE
                animatorGo.start()
                // タイマーの開始
                mTimer?.scheduleAtFixedRate(mTimerTask, 0, 10)
                // 開始音を鳴らす
                mSoundPool?.play(mSoundResIdStart!!, 1.0f, 1.0f, 0,0, 1.0f)
            }
            override fun onAnimationStart(p0: Animator?) {
                imageTextReadyGoReady.visibility = View.VISIBLE
            }
        })
        animatorReady.start()

    }

    override fun onPause() {
        super.onPause()
        // タップ無効化
        onTapDisabled = true
        // BGM削除
        releaseMediaPlayer()
        // 効果音の削除
        mSoundPool?.release()
        mSoundPool = null
        // タイマー削除
        mTimer?.cancel()
        mTimer = null
    }

    private class MainTimerTask: TimerTask() {
        private var mHandler = Handler()
        private var activity: StageActivity? = null
        private var timeCounter: Int = 0
        private var timeLimit: Int = 0

        override fun run() {
            mHandler.post{
                // 初回のみタップの無効化を解除
                if (timeCounter == 0) {
                    activity?.onTapDisabled = false
                }
                // 経過時間を表示
                activity?.progressBarTimeCounter?.progress = timeCounter

                if (timeCounter == timeLimit) {
                    activity?.setFailure()
                }
                // 経過時間を更新
                timeCounter++
            }
        }

        fun setActivity(StageActivity: StageActivity) {
            activity = StageActivity
        }
        fun setTimeLimit(int: Int) {
            timeLimit = int
        }
        fun getTimeCounter(): Int {
            return timeCounter
        }
    }

    override fun onBackPressed() {
        // BGM削除
        releaseMediaPlayer()
        // 効果音の削除
        mSoundPool?.release()
        mSoundPool = null
        // タイマー削除
        mTimer?.cancel()
        mTimer = null
        // タイトルに戻る
        val intent = Intent(application, MainActivity::class.java)
        intent.putExtra("fromStage", 1)
        startActivity(intent)
        finish()
    }

    private fun onCharacterTap(event: MotionEvent) {
        // タップ動作無効化中の場合は何もしない
        if (onTapDisabled) {
            return
        }

        //　制限時間後には処理しない
        if (timeLimit <= mTimerTask.getTimeCounter()) {
            return
        }

        // タップ音を鳴らす
        mSoundPool?.play(mSoundResIdTap!!, 1.0f, 1.0f, 0,0, 1.0f)
        //　規定数以下の場合にのみ実行
        if (targetCount > counter) {
            // カウンターを1進めて表示を更新
            counter++
            progressBarTapCounter.progress = counter

            // タップエフェクトの設定
            val tapEffectId = (counter % 5) + 1
            val tapEffectType = (Random.nextInt(1, 200) % 2) + 1
            val imageTapEffect: ImageView = findViewById(resources.getIdentifier(
                "imageTapEffect${tapEffectId}",
                "id",
                packageName
            ))
            imageTapEffect.setImageResource(resources.getIdentifier(
                "tap_effect_${tapEffectType}",
                "drawable",
                packageName
            ))
            val marginX = event.rawX.toInt() - 50
            val marginY = event.rawY.toInt() - 100
            val lp = imageTapEffect.layoutParams
            val mlp = lp as MarginLayoutParams
            mlp.setMargins(marginX, marginY, 0, 0)
            imageTapEffect.layoutParams = mlp
            imageTapEffect.alpha = 1f
            imageTapEffect.translationY = 0f
            imageTapEffect.rotation = (Random.nextInt(-500, 500) / 100) * 10 * 1f
            val animatorTapEffect = AnimatorSet()
            animatorTapEffect.duration = 500
            animatorTapEffect.playTogether(
                ObjectAnimator.ofFloat(imageTapEffect, "alpha", 0f),
                ObjectAnimator.ofFloat(imageTapEffect, "translationY", -50f)
            )
            animatorTapEffect.start()

            //　キャラクターを震えさせる
            val objectAnimator = ObjectAnimator.ofFloat(imageCharacter, "translationX", 2f, -2f)
            objectAnimator.duration  = 5
            objectAnimator.start()

            if (arrayImageCharacterId.size > nowImageCharacterIndex + 1) {
                val checkCountTarget = (targetCount / arrayImageCharacterId.size) * (nowImageCharacterIndex + 1)
                if (checkCountTarget == counter) {
                    nowImageCharacterIndex++
                    //　次の画像に差し替える
                    imageCharacter.setImageResource(arrayImageCharacterId[nowImageCharacterIndex])
                }
            }

            // 規定数まで来たら実行
            if (targetCount == counter) {
                setSuccess()
            }
        }
    }

    private fun onSuccessTap() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true

            if (isViewAd) {
                if (mInterstitialAd.isLoaded) {
                    mInterstitialAd.adListener = object : AdListener() {
                        override fun onAdClosed() {
                            // 次のステージへ
                            val nextStageId = stageId + 1
                            val intent = Intent(application, StageActivity::class.java)
                            intent.putExtra("stageId", nextStageId)
                            startActivity(intent)
                            // BGM削除
                            releaseMediaPlayer()
                            // 効果音の削除
                            mSoundPool?.release()
                            mSoundPool = null
                            finish()
                        }
                    }
                    mInterstitialAd.show()
                    return
                }
            }

            // 次のステージへ
            val nextStageId = stageId + 1
            val intent = Intent(application, StageActivity::class.java)
            intent.putExtra("stageId", nextStageId)
            startActivity(intent)
            // BGM削除
            releaseMediaPlayer()
            // 効果音の削除
            mSoundPool?.release()
            mSoundPool = null
            finish()
        }
    }

    private fun onFailureTap() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true

            // もう一度同じステージを始める
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
    }

    private fun playMediaPlayer(bgmId: Int) {
        //MediaPlayerの設定
        arrayMediaPlayer = arrayOf(
            MediaPlayer.create(this, bgmId),
            MediaPlayer.create(this, bgmId)
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

    private fun setMediaVolumeDown() {
        for (i in arrayMediaPlayer!!.indices) {
            if (arrayMediaPlayer!![i] != null) {
                arrayMediaPlayer!![i]?.setVolume(0.4f, 0.4f)
            }
        }
    }

    private fun setSuccess() {
        // タイマー削除
        mTimer?.cancel()
        mTimer = null

        // BGMを小さくする
        setMediaVolumeDown()
        // 成功音を鳴らす
        mSoundPool?.play(mSoundResIdSuccess!!, 1.0f, 1.0f, 0,0, 1.0f)
        // キャラクターを成功画像に変更する
        imageCharacter.visibility = View.GONE
        imageCharacterSuccess.visibility = View.VISIBLE

        // 保存データを取得
        val dataStore: SharedPreferences = getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        // クリア済みステージ数を取得
        val clearStageId = dataStore.getInt("clearStageId", 0)
        // 現在のステージがクリア済みステージよりも大きい場合
        if (stageId > clearStageId) {
            // クリア済みステージ数を更新
            val editor = dataStore.edit()
            editor.putInt("clearStageId", stageId)
            editor.apply()
        }

        // リザルトメッセージのアニメーション
        val animatorResult = AnimatorSet()
        animatorResult.duration = 1000
        animatorResult.playTogether(
            ObjectAnimator.ofFloat(layoutResultText, "alpha", 1f),
            ObjectAnimator.ofFloat(layoutResultText, "scaleX", 1f),
            ObjectAnimator.ofFloat(layoutResultText, "scaleY", 1f)
        )
        animatorResult.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(p0: Animator?) {
                val animatorResultButton = AnimatorSet()
                animatorResultButton.duration = 500
                // 次のステージが最後の場合は広告の表示がある
                if (stageId + 1 == maxStage) {
                    // 広告表示フラグを立てる
                    isViewAd = true
                    // 広告表示のメッセージあり版でレイアウトを調整
                    animatorResultButton.playTogether(
                        ObjectAnimator.ofFloat(buttonResult, "alpha", 1f),
                        ObjectAnimator.ofFloat(buttonReturn, "alpha", 1f),
                        ObjectAnimator.ofFloat(layoutScore, "alpha", 1f),
                        ObjectAnimator.ofFloat(textStageViewAd, "alpha", 1f)
                    )
                    textStageViewAd.visibility = View.VISIBLE
                    val lp = textStageViewAd.layoutParams
                    val mlp = lp as MarginLayoutParams
                    mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, 36)
                    textStageViewAd.layoutParams = mlp
                } else {
                    // 広告表示のメッセージなし版でレイアウトを調整
                    animatorResultButton.playTogether(
                        ObjectAnimator.ofFloat(buttonResult, "alpha", 1f),
                        ObjectAnimator.ofFloat(buttonReturn, "alpha", 1f),
                        ObjectAnimator.ofFloat(layoutScore, "alpha", 1f)
                    )
                    val lp = buttonResult.layoutParams
                    val mlp = lp as MarginLayoutParams
                    mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, 36)
                    buttonResult.layoutParams = mlp
                }
                animatorResultButton.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(p0: Animator?) {
                        // 次のステージへボタンを押したときの動作
                        buttonResult.setOnClickListener {
                            // 次のステージに移動
                            onSuccessTap()
                        }
                        // タイトルに戻るボタンを押したときの動作
                        buttonReturn.setOnClickListener {
                            // タイトル画面に移動
                            onBackPressed()
                        }
                    }
                    override fun onAnimationRepeat(p0: Animator?) {}
                    override fun onAnimationCancel(p0: Animator?) {}
                    override fun onAnimationStart(p0: Animator?) {}
                })
                // リザルトボタンを成功時の画像にする
                buttonResult.setBackgroundResource(resources.getIdentifier(
                    "button_stage_result_success",
                    "drawable",
                    packageName
                ))
                // 最後のステージじゃなければ
                if (stageId < maxStage) {
                    // リザルトボタンを表示
                    buttonResult.visibility = View.VISIBLE
                }
                // 戻るボタンを表示
                buttonReturn.visibility = View.VISIBLE
                animatorResultButton.start()
            }
            override fun onAnimationRepeat(p0: Animator?) {}
            override fun onAnimationCancel(p0: Animator?) {}
            override fun onAnimationStart(p0: Animator?) {}
        })

        // 成功メッセージを3種類の中からランダムに設定する
        val resultTextType = (Random.nextInt(1, 300) % 3) + 1
        val resultExtTexts: IntArray = resources.getIntArray(R.array.result_success_ext_texts)
        val resultExtTextIndex = resultTextType - 1
        // キャラクター名の画像を設定
        imageResultCharacter.setImageResource(resources.getIdentifier(
            "text_stage_character_${stageId}",
            "drawable",
            packageName
        ))
        // 接続詞の画像を設定
        imageResultExt.setImageResource(resources.getIdentifier(
            "text_stage_result_ext_${resultExtTexts[resultExtTextIndex]}",
            "drawable",
            packageName
        ))
        // 成功メッセージの画像を設定
        imageResultText.setImageResource(resources.getIdentifier(
            "text_stage_result_success_${resultTextType}",
            "drawable",
            packageName
        ))
        // リザルトメッセージのレイアウトを表示
        layoutResultText.visibility = View.VISIBLE
        // スコア表示の処理
        setScore()
        animatorResult.start()
    }

    private fun setFailure() {
        // タイマー削除
        mTimer?.cancel()
        mTimer = null

        // BGMを小さくする
        setMediaVolumeDown()
        // 失敗音を鳴らす
        mSoundPool?.play(mSoundResIdFailure!!, 1.0f, 1.0f, 0,0, 1.0f)
        // キャラクターを失敗画像に変更する
        imageCharacter.visibility = View.GONE
        imageCharacterFailure.visibility = View.VISIBLE

        // リザルトメッセージのアニメーション
        val animatorResult = AnimatorSet()
        animatorResult.duration = 1000
        animatorResult.playTogether(
            ObjectAnimator.ofFloat(layoutResultText, "alpha", 1f),
            ObjectAnimator.ofFloat(layoutResultText, "scaleX", 1f),
            ObjectAnimator.ofFloat(layoutResultText, "scaleY", 1f)
        )
        animatorResult.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(p0: Animator) {
                val animatorResultButton = AnimatorSet()
                animatorResultButton.duration = 500
                animatorResultButton.playTogether(
                    ObjectAnimator.ofFloat(buttonResult, "alpha", 1f),
                    ObjectAnimator.ofFloat(buttonReturn, "alpha", 1f),
                    ObjectAnimator.ofFloat(layoutScore, "alpha", 1f)
                )

                val lp = buttonResult.layoutParams
                val mlp = lp as MarginLayoutParams
                mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, 36)
                buttonResult.layoutParams = mlp

                animatorResultButton.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(p0: Animator) {
                        // もう一度チャレンジボタンを押したときの動作
                        buttonResult.setOnClickListener {
                            // 同じステージに移動
                            onFailureTap()
                        }
                        // タイトルに戻るボタンを押したときの動作
                        buttonReturn.setOnClickListener {
                            // タイトル画面に移動
                            onBackPressed()
                        }
                    }
                    override fun onAnimationRepeat(p0: Animator) {}
                    override fun onAnimationCancel(p0: Animator) {}
                    override fun onAnimationStart(p0: Animator) {}
                })
                // リザルトボタンを失敗時の画像にする
                buttonResult.setBackgroundResource(resources.getIdentifier(
                    "button_stage_result_failure",
                    "drawable",
                    packageName
                ))
                // リザルトボタンを表示
                buttonResult.visibility = View.VISIBLE
                // 戻るボタンを表示
                buttonReturn.visibility = View.VISIBLE
                animatorResultButton.start()
            }
            override fun onAnimationRepeat(p0: Animator) {}
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationStart(p0: Animator) {}
        })

        // 失敗メッセージを設定する
        val resultTextType = if (bossStage > stageId) {
            // 失敗メッセージを3種類の中からランダムに設定する
            (Random.nextInt(1, 300) % 3) + 1
        } else {
            // ボスステージの場合は固定
            4
        }
        val resultExtTexts: IntArray = resources.getIntArray(R.array.result_failure_ext_texts)
        val resultExtTextIndex = resultTextType - 1

        // キャラクター名の画像を設定
        imageResultCharacter.setImageResource(resources.getIdentifier(
            "text_stage_character_${stageId}",
            "drawable",
            packageName
        ))
        // 接続詞の画像を設定
        imageResultExt.setImageResource(resources.getIdentifier(
            "text_stage_result_ext_${resultExtTexts[resultExtTextIndex]}",
            "drawable",
            packageName
        ))
        // 失敗メッセージの画像を設定
        imageResultText.setImageResource(resources.getIdentifier(
            "text_stage_result_failure_${resultTextType}",
            "drawable",
            packageName
        ))
        // リザルトメッセージのレイアウトを表示
        layoutResultText.visibility = View.VISIBLE
        // スコア表示の処理
        setScore()
        animatorResult.start()
    }

    private fun setScore() {
        // 保存データを取得
        val dataStore: SharedPreferences = getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        // 現在のステージの最高スコアを取得
        val bestScorePoint = dataStore.getInt("score_${stageId}_1", 0)
        // 今回のスコアを計算
        val nowScorePoint = ( timeLimit - mTimerTask.getTimeCounter() ) * targetCount / 10
        // テキストに変換して設定
        textBestScorePoint.text = bestScorePoint.toString()
        textNowScorePoint.text = nowScorePoint.toString()

        // 今回のスコアが最高スコアよりも大きければ
        if (bestScorePoint < nowScorePoint) {
            // 今回のスコアを最高スコアとして保存
            val editor = dataStore.edit()
            editor.putInt("score_${stageId}_1", nowScorePoint)
            editor.apply()

            // 新記録用のテキスト色に変更
            textNowScorePoint.setTextColor(ContextCompat.getColor(this, R.color.colorScorePointNewRecord))
            textNowScoreNewRecord.setTextColor(ContextCompat.getColor(this, R.color.colorScorePointNewRecord))
            // 新記録のメッセージを表示
            textNowScoreNewRecord.visibility = View.VISIBLE
        } else {
            // 新記録のメッセージを非表示
            textNowScoreNewRecord.visibility = View.GONE
        }
        // 最高スコアが0の場合
        if (bestScorePoint == 0) {
            // 最高スコアのレイアウトを非表示
            layoutBestScore.visibility = View.GONE
        }
        // スコアのレイアウトを表示
        layoutScore.visibility = View.VISIBLE
    }
}
