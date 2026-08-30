package com.bluebyte.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bluebyte.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val idleHandler = Handler(Looper.getMainLooper())
    private var isLocked = true

    private val idleRunnable = Runnable {
        lockSystem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            val prefs = getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
            val firstRunDone = prefs.getBoolean("first_run_done", false)

            val initialFragment = if (!firstRunDone) {
                WelcomeFragment()
            } else {
                LockScreenFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, initialFragment)
                .commit()
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.start_menu_container, AppsDrawerFragment())
                .commit()
            
            // Initially locked
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                refreshDrawerFortune()
            }
        })

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
                        when (currentFragment) {
                            is HomeScreenFragment -> {
                                // Stay on home
                            }
                            is LockScreenFragment, is WelcomeFragment -> {
                                finish()
                            }
                            else -> {
                                isEnabled = false
                                onBackPressedDispatcher.onBackPressed()
                                isEnabled = true
                            }
                        }
                    }
                }
            },
        )
    }

    private fun refreshDrawerFortune() {
        val fragment = supportFragmentManager.findFragmentById(R.id.start_menu_container) as? AppsDrawerFragment
        fragment?.refreshFortune()
    }

    fun showLockScreen() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.container, LockScreenFragment())
            .commit()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        resetIdleTimer()
        return super.dispatchTouchEvent(ev)
    }

    fun resetIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable)
        if (!isLocked) {
            val prefs = getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
            val timeoutSeconds = prefs.getInt("idle_timeout", 30) // Default 30s
            idleHandler.postDelayed(idleRunnable, timeoutSeconds * 1000L)
        }
    }

    fun unlock() {
        isLocked = false
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.container, HomeScreenFragment())
            .commit()
        resetIdleTimer()
    }

    fun lockSystem() {
        if (isLocked) return
        isLocked = true
        idleHandler.removeCallbacks(idleRunnable)
        
        // Close drawer if open and lock it
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.container, LockScreenFragment())
            .commit()
    }

    fun openDrawer() {
        if (!isLocked) {
            refreshDrawerFortune()
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onResume() {
        super.onResume()
        resetIdleTimer()
    }

    override fun onPause() {
        super.onPause()
        idleHandler.removeCallbacks(idleRunnable)
    }
}
