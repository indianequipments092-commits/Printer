package com.indianequipments.usbscanner

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdsApplication : Application() {
    companion object {
        private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val REAL_BANNER_ID = "ca-app-pub-7161528961319519/5539637210"
        private const val REAL_INTERSTITIAL_ID = "ca-app-pub-7161528961319519/4034983856"
        private const val MIN_INTERSTITIAL_INTERVAL_MS = 120_000L
        private const val BACKGROUND_THRESHOLD_MS = 60_000L
    }

    private val bannerAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_ID else REAL_BANNER_ID

    private val interstitialAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else REAL_INTERSTITIAL_ID

    private var interstitialAd: InterstitialAd? = null
    private var lastInterstitialShownAt = 0L
    private var lastStoppedAt = 0L
    private var currentActivity: Activity? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) { loadInterstitial() }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, state: Bundle?) {
                currentActivity = activity
                handler.post { attachBanner(activity) }
            }

            override fun onActivityStarted(activity: Activity) { currentActivity = activity }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
                handler.post {
                    attachBanner(activity)
                    if (lastStoppedAt > 0L && System.currentTimeMillis() - lastStoppedAt >= BACKGROUND_THRESHOLD_MS) {
                        handler.postDelayed({ showInterstitialIfReady(activity) }, 500L)
                    }
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                if (!activity.isChangingConfigurations) lastStoppedAt = System.currentTimeMillis()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity === activity) currentActivity = null
            }
        })
    }

    private fun attachBanner(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = if (content.childCount > 0) content.getChildAt(0) as? ViewGroup else null
        if (root == null) return
        if (root.findViewWithTag<AdView>("usb_scanner_banner") != null) return

        val banner = AdView(activity).apply {
            tag = "usb_scanner_banner"
            adUnitId = bannerAdUnitId
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity,
                    (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()
                )
            )
        }

        // Keep the app's own content buttons fully visible. MainActivity's root has
        // the scrollable app content first and the navigation bar last, so the banner
        // is inserted BETWEEN them. This makes the ScrollView shrink naturally instead
        // of covering any button at the bottom of the app content.
        val bannerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val insertIndex = if (root.childCount > 0) root.childCount - 1 else 0
        root.addView(banner, insertIndex, bannerParams)
        banner.loadAd(AdRequest.Builder().build())
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            this,
            interstitialAdUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitial()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                            loadInterstitial()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    handler.postDelayed({ loadInterstitial() }, 30_000L)
                }
            }
        )
    }

    private fun showInterstitialIfReady(activity: Activity) {
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownAt < MIN_INTERSTITIAL_INTERVAL_MS) return
        val ad = interstitialAd ?: return
        lastInterstitialShownAt = now
        ad.show(activity)
    }
}