package com.bluebyte.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bluebyte.app.databinding.FragmentLockScreenBinding

class LockScreenFragment : Fragment() {
    private var _binding: FragmentLockScreenBinding? = null
    private val binding get() = _binding!!
    private var firstPin: String? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if ((level != -1) && (scale != -1)) {
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                binding.batteryStatus.text = getString(R.string.battery_status, batteryPct)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLockScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        val savedPin = prefs.getString("lock_pin", null)

        if (savedPin == null) {
            binding.lockInstruction.text = getString(R.string.mascot_message)
            binding.lockFortuneText.visibility = View.GONE
        } else {
            binding.lockInstruction.text = getString(R.string.enter_pin_to_unlock)
            binding.lockFortuneText.text = FortuneUtils.getFortune()
            binding.lockFortuneText.visibility = View.VISIBLE
        }

        binding.btnUnlock.setOnClickListener {
            val enteredPin = binding.pinInput.text.toString()
            if (enteredPin.isEmpty()) {
                Toast.makeText(context, "Please enter PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (savedPin == null) {
                // Setting up PIN
                if (firstPin == null) {
                    firstPin = enteredPin
                    binding.lockInstruction.text = getString(R.string.mascot_fixing_ui)
                    binding.pinInput.text.clear()
                    
                    // Hide mascot during confirmation
                    binding.lockMascotContainer.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction {
                            binding.lockMascotContainer.visibility = View.GONE
                        }
                        .start()
                    
                    Toast.makeText(context, getString(R.string.confirm_pin), Toast.LENGTH_SHORT).show()
                } else {
                    if (enteredPin == firstPin) {
                        prefs.edit { putString("lock_pin", enteredPin) }
                        Toast.makeText(context, getString(R.string.pin_set_success), Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.unlock()
                    } else {
                        firstPin = null
                        binding.lockInstruction.text = getString(R.string.mascot_message)
                        binding.pinInput.text.clear()
                        binding.lockMascotContainer.visibility = View.VISIBLE
                        binding.lockMascotContainer.animate().alpha(1f).setDuration(500).start()
                        Toast.makeText(context, getString(R.string.pins_dont_match), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Verifying PIN
                if (enteredPin == savedPin) {
                    (activity as? MainActivity)?.unlock()
                } else {
                    Toast.makeText(context, getString(R.string.wrong_pin), Toast.LENGTH_SHORT).show()
                    binding.pinInput.text.clear()
                    // Refresh fortune on wrong pin
                    binding.lockFortuneText.text = FortuneUtils.getFortune()
                }
            }
        }

        // Load wallpaper if exists
        getSavedWallpaperUri()?.let {
            binding.lockWallpaper.setImageURI(it)
        }
    }

    private fun getSavedWallpaperUri(): Uri? {
        val prefs = requireContext().getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        val uriString = prefs.getString("wallpaper_uri", null)
        return uriString?.toUri()
    }

    override fun onStart() {
        super.onStart()
        requireContext().registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(batteryReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
