package io.homeassistant.deep;


import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.DeviceInfo
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class HomeAssistantMediaPlayer : SimpleBasePlayer(Looper.getMainLooper()) {
    override fun getState(): State {
        return State.Builder()
            .setPlaybackState(STATE_IDLE)
            .setDeviceInfo(DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE).build())
            .setPlaylist(listOf(
                MediaItemData.Builder("3090e8a5-2591-40ea-976f-cab2dc72a29b")
                    .setDurationUs(10000000).build()
            ))
            .setVolume(1F)
            .setContentPositionMs(0)
            .setPlaybackParameters(
                PlaybackParameters(1F)
            ).build()
    }
}

class LocalMediaPlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    // Create your Player and MediaSession in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, HomeAssistantMediaPlayer()).build()
    }

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        mediaSession?.run {
            mediaSession!!.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
}
