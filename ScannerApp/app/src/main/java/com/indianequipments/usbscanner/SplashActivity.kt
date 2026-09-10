package com.indianequipments.usbscanner

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var finished = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.statusBarColor = Color.rgb(4, 8, 15)
        window.navigationBarColor = Color.rgb(4, 8, 15)
        window.decorView.systemUiVisibility = 0

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(4, 8, 15))
        }

        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(center, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))

        val logoFrame = FrameLayout(this)
        val logoSize = (minOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels) * 0.48f).toInt().coerceIn(dp(170), dp(300))
        center.addView(logoFrame, LinearLayout.LayoutParams(logoSize, logoSize))

        val glow = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(3), Color.argb(65, 30, 150, 255))
            }
            alpha = 0f
        }
        logoFrame.addView(glow, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.usb_scanner_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0f
            scaleX = 0.72f
            scaleY = 0.72f
        }
        logoFrame.addView(logo, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))

        val scanBeam = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                Color.TRANSPARENT,
                Color.argb(35, 40, 165, 255),
                Color.argb(230, 85, 190, 255),
                Color.argb(35, 40, 165, 255),
                Color.TRANSPARENT
            ))
            alpha = 0f
        }
        val beamParams = FrameLayout.LayoutParams(dp(16), -1)
        beamParams.leftMargin = -dp(16)
        logoFrame.addView(scanBeam, beamParams)

        val title = TextView(this).apply {
            text = "USB SCANNER"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
            letterSpacing = 0.08f
            alpha = 0f
            translationY = dp(12).toFloat()
        }
        center.addView(title, LinearLayout.LayoutParams(-1, dp(46)).apply {
            topMargin = dp(18)
        })

        val tagline = TextView(this).apply {
            text = "SCAN  •  SAVE  •  SHARE"
            textSize = 12f
            setTextColor(Color.rgb(75, 175, 255))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
            letterSpacing = 0.18f
            alpha = 0f
            translationY = dp(8).toFloat()
        }
        center.addView(tagline, LinearLayout.LayoutParams(-1, dp(32)).apply {
            topMargin = -dp(2)
        })

        val scanLine = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(2).toFloat()
                setColor(Color.rgb(40, 160, 255))
            }
            alpha = 0f
        }
        val lineParams = FrameLayout.LayoutParams(dp(150), dp(2), Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
        lineParams.bottomMargin = dp(54)
        root.addView(scanLine, lineParams)

        setContentView(root)
        startAnimation(logo, glow, scanBeam, title, tagline, scanLine)
    }

    private fun startAnimation(
        logo: View,
        glow: View,
        beam: View,
        title: View,
        tagline: View,
        scanLine: View
    ) {
        val intro = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.72f, 1f),
                ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.72f, 1f),
                ObjectAnimator.ofFloat(glow, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(title, View.TRANSLATION_Y, dp(12).toFloat(), 0f),
                ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(tagline, View.TRANSLATION_Y, dp(8).toFloat(), 0f),
                ObjectAnimator.ofFloat(scanLine, View.ALPHA, 0f, 1f)
            )
            duration = 650
            interpolator = DecelerateInterpolator()
        }
        intro.start()

        handler.postDelayed({
            beam.alpha = 1f
            beam.translationX = -dp(16).toFloat()
            val beamMove = ObjectAnimator.ofFloat(beam, View.TRANSLATION_X, -dp(16).toFloat(), dp(320).toFloat())
            beamMove.duration = 850
            beamMove.interpolator = AccelerateDecelerateInterpolator()
            beamMove.start()

            val pulse = ValueAnimator.ofFloat(0.55f, 1f, 0.55f).apply {
                duration = 850
                addUpdateListener { glow.alpha = it.animatedValue as Float }
            }
            pulse.start()
        }, 480)

        handler.postDelayed({
            val fade = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(logo, View.ALPHA, 1f, 0f),
                    ObjectAnimator.ofFloat(glow, View.ALPHA, glow.alpha, 0f),
                    ObjectAnimator.ofFloat(title, View.ALPHA, 1f, 0f),
                    ObjectAnimator.ofFloat(tagline, View.ALPHA, 1f, 0f),
                    ObjectAnimator.ofFloat(scanLine, View.ALPHA, 1f, 0f),
                    ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.04f),
                    ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 1.04f)
                )
                duration = 420
                interpolator = AccelerateDecelerateInterpolator()
            }
            fade.start()
            fade.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    openMain()
                }
            })
        }, 2250)
    }

    private fun openMain() {
        if (finished) return
        finished = true
        handler.removeCallbacksAndMessages(null)
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        // Keep the launch animation uninterrupted.
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
