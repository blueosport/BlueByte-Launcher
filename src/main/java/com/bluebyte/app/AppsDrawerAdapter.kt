package com.bluebyte.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.bluebyte.app.databinding.ItemAppBinding
import java.util.Locale

class AppsDrawerAdapter(private val context: Context) : RecyclerView.Adapter<AppsDrawerAdapter.ViewHolder>() {
    private val fullAppsList = AppUtils.fetchApps(context)
    private var filteredList = fullAppsList.toMutableList()

    fun filter(query: String) {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        filteredList = if (query.isEmpty()) {
            fullAppsList.toMutableList()
        } else {
            fullAppsList.asSequence().filter {
                it.label.toString().lowercase(Locale.getDefault()).contains(lowerCaseQuery)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredList[position]
        holder.binding.appLabel.text = app.label
        holder.binding.appIcon.setImageDrawable(app.icon)

        // Sci-fi holographic effect in cyan-peacock
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        
        val cyanPeacock = ContextCompat.getColor(context, R.color.cyan_peacock)
        val r = Color.red(cyanPeacock) / 255f
        val g = Color.green(cyanPeacock) / 255f
        val b = Color.blue(cyanPeacock) / 255f
        
        val colorScaleMatrix = ColorMatrix(
            floatArrayOf(
                r, 0f, 0f, 0f, 0f,
                0f, g, 0f, 0f, 0f,
                0f, 0f, b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        matrix.postConcat(colorScaleMatrix)
        holder.binding.appIcon.colorFilter = ColorMatrixColorFilter(matrix)

        holder.binding.root.setOnClickListener {
            context.packageManager.getLaunchIntentForPackage(app.packageName.toString())?.let {
                context.startActivity(it)
            }
        }

        holder.binding.root.setOnLongClickListener {
            showAppOptions(it, app.packageName.toString())
            true
        }
    }

    private fun showAppOptions(view: android.view.View, packageName: String) {
        val popup = PopupMenu(context, view)
        popup.menu.add(0, 1, 0, "App Info")
        
        val isPinned = isPinned(packageName)
        val pinLabel = if (isPinned) "Unpin from main screen" else "Pin to main screen"
        popup.menu.add(0, 2, 0, pinLabel)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    true
                }
                2 -> {
                    togglePin(packageName)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun isPinned(packageName: String): Boolean {
        val prefs = context.getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        val pinned = prefs.getStringSet("pinned_apps", null)
        return if (pinned == null) {
            // Check default set
            val allApps = AppUtils.fetchApps(context)
            allApps.take(8).any { it.packageName.toString() == packageName }
        } else {
            pinned.contains(packageName)
        }
    }

    private fun togglePin(packageName: String) {
        val prefs = context.getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        val pinned = prefs.getStringSet("pinned_apps", null)?.toMutableSet() ?: run {
            // First time toggling, need to init with defaults
            val allApps = AppUtils.fetchApps(context)
            allApps.take(8).map { it.packageName.toString() }.toMutableSet()
        }

        if (pinned.contains(packageName)) {
            pinned.remove(packageName)
        } else {
            pinned.add(packageName)
        }
        
        prefs.edit { putStringSet("pinned_apps", pinned) }
        
        // Refresh home screen
        ((context as? MainActivity)?.supportFragmentManager?.findFragmentById(R.id.container) as? HomeScreenFragment)?.refreshPinnedApps()
    }

    override fun getItemCount(): Int = filteredList.size

    class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)
}
