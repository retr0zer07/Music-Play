package com.spheremusic.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.spheremusic.app.MainActivity

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        EqualizerControl.initialize(this)
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                EqualizerControl.attach(audioSessionId)
            }
        })
        EqualizerControl.attach(player.audioSessionId)
        val activity = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val sessionPlayer = object : ForwardingPlayer(player) {
            override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
                if (shuffleModeEnabled && !player.shuffleModeEnabled) {
                    player.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(
                        shuffleSequence(player.mediaItemCount, player.currentMediaItemIndex),
                        kotlin.random.Random.nextLong(),
                    ))
                    if (player.repeatMode == Player.REPEAT_MODE_ONE) player.repeatMode = Player.REPEAT_MODE_OFF
                }
                player.shuffleModeEnabled = shuffleModeEnabled
            }
        }
        session = MediaSession.Builder(this, sessionPlayer)
            .setSessionActivity(activity)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult =
                    if (controller.isTrusted || controller.packageName == packageName) {
                        super.onConnect(session, controller)
                    } else {
                        MediaSession.ConnectionResult.reject()
                    }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        EqualizerControl.release()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
