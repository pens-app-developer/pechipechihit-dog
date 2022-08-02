package com.p_en_s.wanwanpechipechihit

import android.media.MediaPlayer
import java.util.*

class RunnableSoundLoop(
    var arrayMediaPlayer: Array<MediaPlayer?>,
    var preTime: Long
) :
    Runnable {
    var soundLoopTimer: Timer? = null
    var musicLength: IntArray
    var playTime: Int
    var playNumber: Int
    override fun run() {
        soundLoopTimer = Timer(true)
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                if (arrayMediaPlayer[playNumber] != null) {
                    //再生時間を加算
                    playTime += (System.currentTimeMillis() - preTime.toInt()).toInt()

                    //再生時間が曲の長さを超えた場合
                    if (playTime >= musicLength[playNumber]) {
                        //再生時間をリセット
                        playTime = 0

                        //次のMediaPlayerを再生する
                        playNumber += 1
                        if (playNumber >= arrayMediaPlayer.size) {
                            playNumber = 0
                        }
                        arrayMediaPlayer[playNumber]!!.start()
                    }

                    //現在の時間をシステム時間で取得
                    preTime = System.currentTimeMillis()
                } else {
                    //MediaPlayerが解放された場合はTimerを止める
                    soundLoopTimer?.cancel()
                    soundLoopTimer?.purge()
                    soundLoopTimer = null
                }
            }
        }
        soundLoopTimer?.schedule(timerTask, 0, FRAME_TIME)
    }

    companion object {
        const val FPS: Long = 40
        const val FRAME_TIME = 1000 / FPS
    }

    init {
        musicLength = IntArray(arrayMediaPlayer.size)
        for (i in arrayMediaPlayer.indices) {
            musicLength[i] = arrayMediaPlayer[i]!!.duration
        }
        playTime = 0
        playNumber = 0
    }
}