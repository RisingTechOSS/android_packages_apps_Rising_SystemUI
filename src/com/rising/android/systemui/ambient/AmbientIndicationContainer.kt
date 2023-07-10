package com.rising.android.systemui.ambient

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.PendingIntent
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.MathUtils
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

import com.android.systemui.AutoReinflateContainer
import com.android.systemui.Dependency
import com.android.systemui.R
import com.android.systemui.animation.Interpolators
import com.android.systemui.doze.DozeReceiver
import com.android.systemui.plugins.statusbar.StatusBarStateController
import com.android.systemui.statusbar.NotificationMediaManager
import com.android.systemui.statusbar.phone.CentralSurfaces
import com.android.systemui.util.wakelock.DelayedWakeLock
import com.android.systemui.util.wakelock.WakeLock

class AmbientIndicationContainer(
    private val context: Context,
    attrs: AttributeSet
) : AutoReinflateContainer(context, attrs), DozeReceiver, StatusBarStateController.StateListener, NotificationMediaManager.MediaListener {

    private val iconBounds = Rect()
    private val wakeLock: WakeLock by lazy {
        DelayedWakeLock(Handler(Looper.getMainLooper()), WakeLock.createPartial(context, "AmbientIndication"))
    }
    private var ambientIconOverride: Drawable? = null
    private var ambientIndicationIconSize: Int = 0
    private lateinit var ambientMusicAnimation: Drawable
    private lateinit var ambientMusicNoteIcon: Drawable
    private var ambientMusicNoteIconSize: Int = 0
    private var ambientMusicText: String? = null
    private var ambientSkipUnlock: Boolean = false
    private var bottomMarginPx: Int = 0
    private var dozing: Boolean = false
    private var favoritingIntent: PendingIntent? = null
    private var iconDescription: String? = null
    private var iconOverride: Int = -1
    private lateinit var iconView: ImageView
    private var indicationTextMode: Int = 0
    private var mediaPlaybackState: Int = 0
    private var openIntent: PendingIntent? = null
    private lateinit var centralSurfaces: CentralSurfaces
    private var centralSurfacesState: Int = 0
    private var textColor: Int = 0
    private var textColorAnimator: ValueAnimator? = null
    private lateinit var textView: TextView
    private var reverseChargingMessage: String = ""
    
    private lateinit var mAudioManager: AudioManager
    private lateinit var mSessionManager: MediaSessionManager

    private val handler: Handler = Handler(Looper.getMainLooper())

    fun initializeView(centralSurfaces: CentralSurfaces) {
        this.centralSurfaces = centralSurfaces
        addInflateListener(object : AutoReinflateContainer.InflateListener {
            override fun onInflated(view: View) {
                textView = view.findViewById(R.id.ambient_indication_text)
                iconView = view.findViewById(R.id.ambient_indication_icon)
                ambientMusicAnimation = context.getDrawable(R.anim.audioanim_animation)!!
                ambientMusicNoteIcon = context.getDrawable(R.drawable.ic_music_note)!!
                textColor = textView.currentTextColor
                ambientIndicationIconSize = resources.getDimensionPixelSize(R.dimen.ambient_indication_icon_size)
                ambientMusicNoteIconSize = resources.getDimensionPixelSize(R.dimen.ambient_indication_note_icon_size)
                textView?.isEnabled = !dozing
                updateColors()
                updatePill()
                textView?.setOnClickListener { v -> onTextClick(v) }
                iconView?.setOnClickListener { v -> onIconClick(v) }
            }
        })
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateBottomSpacing()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Dependency.get(StatusBarStateController::class.java).addCallback(this)
        Dependency.get(NotificationMediaManager::class.java).addCallback(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Dependency.get(StatusBarStateController::class.java).removeCallback(this)
        Dependency.get(NotificationMediaManager::class.java).removeCallback(this)
    }

    fun setAmbientMusic(
        text: String?,
        openIntent: PendingIntent?,
        favoriteIntent: PendingIntent?,
        skipUnlock: Boolean,
        iconOverride: Int,
        iconDescription: String?
    ) {
        if (ambientMusicText != text || this.openIntent != openIntent ||
            favoritingIntent != favoriteIntent || this.iconOverride != iconOverride ||
            ambientSkipUnlock != skipUnlock || this.iconDescription != iconDescription
        ) {
            ambientMusicText = text
            this.openIntent = openIntent
            favoritingIntent = favoriteIntent
            ambientSkipUnlock = skipUnlock
            this.iconOverride = iconOverride
            this.iconDescription = iconDescription
            ambientIconOverride = getAmbientIconOverride(iconOverride)
            updatePill()
        }
    }

    private fun getAmbientIconOverride(iconOverride: Int): Drawable? {
        return when (iconOverride) {
            1 -> context.getDrawable(R.drawable.ic_music_search)
            2 -> null
            3 -> context.getDrawable(R.drawable.ic_music_not_found)
            4 -> context.getDrawable(R.drawable.ic_cloud_off)
            5 -> context.getDrawable(R.drawable.ic_favorite)
            6 -> context.getDrawable(R.drawable.ic_favorite_border)
            7 -> context.getDrawable(R.drawable.ic_error)
            8 -> context.getDrawable(R.drawable.ic_favorite_note)
            else -> null
        }
    }

    private fun updatePill() {
        if (isMediaPlaying()) {
            hideAmbientMusic()
            return;
        }
        val oldIndicationTextMode = indicationTextMode
        var updatePill = true
        indicationTextMode = 1
        var text = ambientMusicText
        val textVisible = textView?.visibility == View.VISIBLE
        var icon: Drawable? = if (textVisible) ambientMusicNoteIcon else ambientMusicAnimation
        if (ambientIconOverride != null) {
            icon = ambientIconOverride
        }
        var showAmbientMusicText = ambientMusicText != null && ambientMusicText!!.isEmpty()
        textView?.isClickable = openIntent != null
        iconView?.isClickable = favoritingIntent != null || openIntent != null
        var iconDescription = if (TextUtils.isEmpty(iconDescription)) text else this.iconDescription
        if (!TextUtils.isEmpty(reverseChargingMessage)) {
            indicationTextMode = 2
            text = reverseChargingMessage
            icon = null
            textView?.isClickable = false
            iconView?.isClickable = false
            showAmbientMusicText = false
            iconDescription = null
        }
        textView?.text = text
        textView?.contentDescription = text
        iconView?.contentDescription = iconDescription
        var drawableWrapper: Drawable? = null
        if (icon != null) {
            iconBounds.set(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            MathUtils.fitRect(iconBounds, if (icon == ambientMusicNoteIcon) ambientMusicNoteIconSize else ambientIndicationIconSize)
            drawableWrapper = object : DrawableWrapper(icon) {
                override fun getIntrinsicWidth(): Int {
                    return iconBounds.width()
                }

                override fun getIntrinsicHeight(): Int {
                    return iconBounds.height()
                }
            }
            val endPadding: Int = if (!TextUtils.isEmpty(text)) (resources.displayMetrics.density * 24).toInt() else 0
            textView?.setPaddingRelative(textView?.paddingStart ?: 0, textView?.paddingTop ?: 0, endPadding, textView?.paddingBottom ?: 0)
        } else {
            textView?.setPaddingRelative(textView?.paddingStart ?: 0, textView?.paddingTop ?: 0, 0, textView?.paddingBottom ?: 0)
        }
        iconView?.setImageDrawable(drawableWrapper)
        if (TextUtils.isEmpty(text) && !showAmbientMusicText) {
            updatePill = false
        }
        val vis = if (updatePill) View.VISIBLE else View.GONE
        textView?.visibility = vis
        iconView?.visibility = if (icon == null) View.GONE else vis
        if (!updatePill) {
            textView?.animate()?.cancel()
            if (icon is AnimatedVectorDrawable) {
                icon.reset()
            }
            this@AmbientIndicationContainer.handler.post(wakeLock.wrap { })
        } else if (!textVisible) {
            wakeLock.acquire("AmbientIndication")
            if (icon is AnimatedVectorDrawable) {
                icon.start()
            }
            textView?.translationY = textView?.height?.toFloat() ?: 0F / 2F
            textView?.alpha = 0.0f
            textView?.animate()
                ?.alpha(1.0f)
                ?.translationY(0.0f)
                ?.setStartDelay(150L)
                ?.setDuration(100L)
                ?.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animator: Animator) {
                        wakeLock.release("AmbientIndication")
                        textView?.animate()?.setListener(null)
                    }
                })
                ?.setInterpolator(Interpolators.DECELERATE_QUINT)
                ?.start()
        } else if (oldIndicationTextMode != this.indicationTextMode) {
            if (icon is AnimatedVectorDrawable) {
                wakeLock.acquire("AmbientIndication")
                icon.start()
                wakeLock.release("AmbientIndication")
            }
        } else {
            this@AmbientIndicationContainer.handler.post(wakeLock.wrap { })
        }
        updateBottomSpacing()
    }

    private fun updateBottomSpacing() {
        val marginBottom = resources.getDimensionPixelSize(R.dimen.ambient_indication_margin_bottom)
        if (bottomMarginPx != marginBottom) {
            bottomMarginPx = marginBottom
            (layoutParams as FrameLayout.LayoutParams).bottomMargin = bottomMarginPx
        }
        centralSurfaces.notificationPanelViewController.setAmbientIndicationTop(
            top,
            textView?.visibility == View.VISIBLE
        )
    }

    fun hideAmbientMusic() {
        setAmbientMusic(null, null, null, false, 0, null)
    }

    fun onTextClick(view: View) {
        openIntent?.let {
            centralSurfaces.wakeUpIfDozing(
                SystemClock.uptimeMillis(),
                view,
                "AMBIENT_MUSIC_CLICK",
                PowerManager.WAKE_REASON_GESTURE
            )
            if (ambientSkipUnlock) {
                sendBroadcastWithoutDismissingKeyguard(it)
            } else {
                centralSurfaces.startPendingIntentDismissingKeyguard(openIntent)
            }
        }
    }

    fun onIconClick(view: View) {
        favoritingIntent?.let {
            centralSurfaces.wakeUpIfDozing(
                SystemClock.uptimeMillis(),
                view,
                "AMBIENT_MUSIC_CLICK",
                PowerManager.WAKE_REASON_GESTURE
            )
            sendBroadcastWithoutDismissingKeyguard(it)
            return
        }
        onTextClick(view)
    }

    override fun onDozingChanged(isDozing: Boolean) {
        dozing = isDozing
        updateVisibility()
        textView?.isEnabled = !isDozing
        updateColors()
    }

    override fun dozeTimeTick() {
        updatePill()
    }

    fun updateColors() {
        textColorAnimator?.cancel()
        val defaultColor = textView.textColors.defaultColor
        val dozeColor = if (dozing) -1 else textColor
        if (dozeColor == defaultColor) {
            textView?.setTextColor(dozeColor)
            iconView?.imageTintList = ColorStateList.valueOf(dozeColor)
        }
        textColorAnimator = ValueAnimator.ofArgb(defaultColor, dozeColor).apply {
            interpolator = Interpolators.LINEAR_OUT_SLOW_IN
            duration = 500L
            addUpdateListener { _ ->
                textView?.setTextColor(animatedValue as Int)
                iconView?.imageTintList = ColorStateList.valueOf(animatedValue as Int)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    textColorAnimator = null
                }
            })
            start()
        }
    }

    override fun onStateChanged(state: Int) {
        centralSurfacesState = state
        updateVisibility()
    }

    private fun sendBroadcastWithoutDismissingKeyguard(pendingIntent: PendingIntent) {
        if (pendingIntent.isActivity) {
            return
        }
        try {
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            Log.w("AmbientIndication", "Sending intent failed: $e")
        }
    }

    private fun updateVisibility() {
        visibility = if (centralSurfacesState == 1) View.VISIBLE else View.INVISIBLE
    }

    override fun onPrimaryMetadataOrStateChanged(mediaMetadata: MediaMetadata?, mediaState: Int) {
        if (mediaPlaybackState != mediaState) {
            mediaPlaybackState = mediaState
            updatePill()
        }
    }

    private fun isMediaPlaying(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        if (sessionManager != null) {
            val controllers: List<MediaController> = sessionManager.getActiveSessions(null)
            for (controller in controllers) {
                val state: PlaybackState? = controller.playbackState
                if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                    return true
                }
            }
        }
        return audioManager?.isMusicActive == true
    }

    fun setReverseChargingMessage(message: String) {
        if (TextUtils.isEmpty(message) && reverseChargingMessage == message) {
            return
        }
        reverseChargingMessage = message
        updatePill()
    }
}
