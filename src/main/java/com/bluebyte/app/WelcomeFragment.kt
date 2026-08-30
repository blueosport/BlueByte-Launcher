package com.bluebyte.app

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.bluebyte.app.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private enum class State {
        WELCOME, PEACE_TRUST, NO_ADS
    }

    private var currentState = State.WELCOME

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()

        binding.btnAction.setOnClickListener {
            when (currentState) {
                State.WELCOME -> {
                    currentState = State.PEACE_TRUST
                    updateUI()
                }
                State.PEACE_TRUST -> {
                    openHomeSettings()
                    // Allow manual progression if settings fail to open or detect
                    if (isDefaultLauncher()) {
                        currentState = State.NO_ADS
                        updateUI()
                    }
                }
                State.NO_ADS -> {
                    val prefs = requireContext().getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
                    prefs.edit { putBoolean("first_run_done", true) }
                    (activity as? MainActivity)?.showLockScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentState == State.PEACE_TRUST && isDefaultLauncher()) {
            currentState = State.NO_ADS
            updateUI()
        }
    }

    private fun updateUI() {
        when (currentState) {
            State.WELCOME -> {
                binding.welcomeText.text = getString(R.string.welcome_message)
                binding.btnAction.text = getString(R.string.btn_start)
            }
            State.PEACE_TRUST -> {
                binding.welcomeText.text = getString(R.string.mascot_default_launcher)
                binding.btnAction.text = getString(R.string.btn_set_default)
            }
            State.NO_ADS -> {
                binding.welcomeText.text = getString(R.string.mascot_no_ads)
                binding.btnAction.text = getString(R.string.btn_make_pin)
            }
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = requireContext().packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == requireContext().packageName
    }

    private fun openHomeSettings() {
        try {
            // Priority 1: Official Role Manager (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = requireContext().getSystemService(Context.ROLE_SERVICE) as RoleManager
                if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    startActivity(intent)
                    return
                }
            }
            
            // Priority 2: Direct Home Settings
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: Exception) {
            try {
                // Priority 3: Default Apps Settings
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } catch (_: Exception) {
                try {
                    // Priority 4: Home intent to trigger chooser
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } catch (_: Exception) {
                    // Priority 5: General Settings
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
