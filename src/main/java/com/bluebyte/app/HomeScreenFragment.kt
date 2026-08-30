package com.bluebyte.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bluebyte.app.databinding.FragmentHomeScreenBinding

class HomeScreenFragment : Fragment() {
    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HomeTilesAdapter
    private var currentTileSize = 120

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            // Take persistable permission
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it, 
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            saveWallpaperUri(it)
            updateWallpaper(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshPinnedApps()
        
        binding.iconDrawer.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        binding.iconSettings.setOnClickListener {
            showSettingsMenu(it)
        }

        // Load saved wallpaper
        getSavedWallpaperUri()?.let {
            updateWallpaper(it)
        }
    }

    fun refreshPinnedApps() {
        if (_binding == null) return
        
        currentTileSize = getSavedTileSize()
        val allApps = AppUtils.fetchApps(requireContext())
        val pinnedPackageNames = getPinnedPackageNames()
        
        val homeApps = if (pinnedPackageNames.isEmpty()) {
            // Default apps if none pinned
            allApps.take(8)
        } else {
            allApps.filter { pinnedPackageNames.contains(it.packageName.toString()) }
        }
        
        adapter = HomeTilesAdapter(requireContext(), homeApps, currentTileSize)
        
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 4
        binding.tilesRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        binding.tilesRecyclerView.adapter = adapter
    }

    private fun getPinnedPackageNames(): Set<String> {
        val prefs = requireContext().getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("pinned_apps", emptySet()) ?: emptySet()
    }

    private fun showSettingsMenu(view: View) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popup.menu.add("Change Wallpaper")
        popup.menu.add(getString(R.string.adjust_tile_size))
        popup.menu.add(getString(R.string.change_idle_timer))
        
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Change Wallpaper" -> {
                    pickImageLauncher.launch(arrayOf("image/*"))
                    true
                }
                getString(R.string.adjust_tile_size) -> {
                    showTileSizeDialog()
                    true
                }
                getString(R.string.change_idle_timer) -> {
                    showIdleTimerDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showTileSizeDialog() {
        val seekBar = SeekBar(requireContext())
        seekBar.max = 200 // Max size
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            seekBar.min = 40 // Min size
        }
        seekBar.progress = currentTileSize
        seekBar.setPadding(48, 48, 48, 48)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.adjust_tile_size))
            .setView(seekBar)
            .setPositiveButton("Save") { _, _ ->
                currentTileSize = seekBar.progress
                saveTileSize(currentTileSize)
                adapter.updateTileSize(currentTileSize)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showIdleTimerDialog() {
        val seekBar = SeekBar(requireContext())
        seekBar.max = 60 // Max 60s
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            seekBar.min = 15 // Min 15s
        }
        val prefs = requireContext().getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        seekBar.progress = prefs.getInt("idle_timeout", 30)
        seekBar.setPadding(48, 48, 48, 48)

        AlertDialog.Builder(requireContext())
            .setTitle("Set Idle Timeout (15-60s)")
            .setView(seekBar)
            .setPositiveButton("Save") { _, _ ->
                var seconds = seekBar.progress
                if (seconds < 15) seconds = 15 // Fallback for older Android versions
                prefs.edit { putInt("idle_timeout", seconds) }
                Toast.makeText(context, getString(R.string.idle_timer_set, seconds), Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.resetIdleTimer()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateWallpaper(uri: Uri) {
        try {
            binding.wallpaperView.setImageURI(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveWallpaperUri(uri: Uri) {
        val prefs = requireContext().getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        prefs.edit { putString("wallpaper_uri", uri.toString()) }
    }

    private fun getSavedWallpaperUri(): Uri? {
        val prefs = requireContext().getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        val uriString = prefs.getString("wallpaper_uri", null)
        return uriString?.toUri()
    }

    private fun saveTileSize(size: Int) {
        val prefs = requireContext().getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        prefs.edit { putInt("tile_size", size) }
    }

    private fun getSavedTileSize(): Int {
        val prefs = requireContext().getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        return prefs.getInt("tile_size", 120)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
