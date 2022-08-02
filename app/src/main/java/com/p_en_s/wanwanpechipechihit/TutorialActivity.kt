package com.p_en_s.wanwanpechipechihit

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.LinearInterpolator
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AppCompatActivity() {

    private var onTapDisabled: Boolean = false

    private var returnStageId: Int = 0

    private var nowTutorialNumber: Int = 1

    private var arrayMediaPlayer: Array<MediaPlayer?>? = null
    private var threadSoundLoop: Thread? = null

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
        setContentView(R.layout.activity_tutorial)

        // バナー広告の設定
        adView = AdView(this)
        adViewContainer.addView(adView)
        loadBanner()

        // 次に移動するステージIDを取得する
        returnStageId = intent.getIntExtra("returnStageId", 0)

        // 戻るボタンの動作
        buttonBack.setOnClickListener {
            // 1つ前のチュートリアルを表示する
            onBackClick()
        }
        // 次へボタンの動作
        buttonNext.setOnClickListener {
            // 1つ先のチュートリアルを表示する
            onNextClick()
        }
        // スキップボタンの動作
        buttonSkip.setOnClickListener {
            // チュートリアルを終了する
            onSkipClick()
        }
        // スタートボタンの動作
        buttonStart.setOnClickListener {
            // チュートリアルを終了する
            onSkipClick()
        }
        // タイトルに戻るボタンの動作
        buttonReturn.setOnClickListener {
            // チュートリアルを終了する
            onSkipClick()
        }

        val dataStore: SharedPreferences = getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        val editor = dataStore.edit()
        editor.putInt("gotoTutorial", 0)
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        // BGM再生
        playMediaPlayer()
    }

    override fun onPause() {
        super.onPause()
        // BGM削除
        releaseMediaPlayer()
    }

    private fun onBackClick() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true
            // 現在表示中のページを戻す
            nowTutorialNumber--
            // 表示内容の更新
            viewTutorial()
        }
    }

    private fun onNextClick() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true
            // 現在表示中のページを進める
            nowTutorialNumber++
            // 表示内容の更新
            viewTutorial()
        }
    }

    private fun onSkipClick() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true

            // BGM削除
            releaseMediaPlayer()

            val intent: Intent
            if (returnStageId == 0) {
                // タイトルに戻る
                intent = Intent(application, MainActivity::class.java)
                intent.putExtra("fromStage", 1)
            } else {
                // ステージに移動
                intent = Intent(application, StageActivity::class.java)
                intent.putExtra("stageId", returnStageId)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun viewTutorial() {
        // チュートリアル内容を消すアニメーション
        val animatorHidden = AnimatorSet()
        animatorHidden.interpolator = LinearInterpolator()
        animatorHidden.duration = 500
        animatorHidden.playTogether(
            ObjectAnimator.ofFloat(layoutImage, "alpha", 0f),
            ObjectAnimator.ofFloat(layoutText, "alpha", 0f)
        )
        animatorHidden.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(p0: Animator?) {
                // 1つ前の内容を取得
                val backCheck = resources.getIdentifier(
                    "tutorial_${nowTutorialNumber - 1}",
                    "string",
                    packageName
                )
                if (backCheck == 0) {
                    // 取得できない場合は戻るボタンを非表示
                    buttonBack.visibility = View.GONE
                } else {
                    // 取得できた場合は戻るボタンを表示
                    buttonBack.visibility = View.VISIBLE
                }
                // 1つ先の内容を取得
                val nextCheck = resources.getIdentifier(
                "tutorial_${nowTutorialNumber + 1}",
                "string",
                packageName
                )
                if (nextCheck == 0) {
                    // 取得できない場合は次へボタンとスキップボタンを非表示
                    buttonNext.visibility = View.GONE
                    buttonSkip.visibility = View.GONE
                    if (returnStageId == 0) {
                        //　戻り先のステージIDがなければタイトルに戻るボタンを表示
                        buttonReturn.visibility = View.VISIBLE
                    } else {
                        // 戻り先のステージIDがあればスタートボタンを表示
                        buttonStart.visibility = View.VISIBLE
                    }
                } else {
                    // 取得できない場合は次へボタンとスキップボタンを表示
                    buttonNext.visibility = View.VISIBLE
                    buttonSkip.visibility = View.VISIBLE
                    // タイトルに戻るとスタートボタンは非表示
                    buttonReturn.visibility = View.GONE
                    buttonStart.visibility = View.GONE
                }
                // チュートリアルのテキストを取得
                textTutorial.setText(resources.getIdentifier(
                    "tutorial_${nowTutorialNumber}",
                    "string",
                    packageName
                ))
                // チュートリアルの画像を取得
                tutorialImage.setImageResource(resources.getIdentifier(
                    "tutorial_${nowTutorialNumber}",
                    "drawable",
                    packageName
                ))

                // チュートリアル内容を表示するアニメーション
                val animatorDisplay = AnimatorSet()
                animatorDisplay.interpolator = LinearInterpolator()
                animatorDisplay.duration = 500
                animatorDisplay.playTogether(
                    ObjectAnimator.ofFloat(layoutImage, "alpha", 1f),
                    ObjectAnimator.ofFloat(layoutText, "alpha", 1f)
                )
                animatorDisplay.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(p0: Animator?) {
                        onTapDisabled = false
                    }
                    override fun onAnimationRepeat(p0: Animator?) {}
                    override fun onAnimationCancel(p0: Animator?) {}
                    override fun onAnimationStart(p0: Animator?) {}
                })
                animatorDisplay.start()
            }
            override fun onAnimationRepeat(p0: Animator?) {}
            override fun onAnimationCancel(p0: Animator?) {}
            override fun onAnimationStart(p0: Animator?) {}
        })
        animatorHidden.start()
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