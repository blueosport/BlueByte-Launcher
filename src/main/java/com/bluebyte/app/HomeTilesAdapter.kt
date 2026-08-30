package com.bluebyte.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.bluebyte.app.databinding.ItemTileBinding

class HomeTilesAdapter(private val context: Context, private var appsList: List<AppInfo>, initialTileSize: Int) : 
    RecyclerView.Adapter<HomeTilesAdapter.ViewHolder>() {

    private var tileSizeDp: Int = initialTileSize

    fun updateTileSize(newSize: Int) {
        tileSizeDp = newSize
        notifyItemRangeChanged(0, appsList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTileBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = appsList[position]
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

        // Adjust tile size
        val params = holder.binding.tileContainer.layoutParams
        params.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            tileSizeDp.toFloat(), 
            context.resources.displayMetrics
        ).toInt()
        holder.binding.tileContainer.layoutParams = params

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
        popup.menu.add(0, 2, 0, "Unpin from main screen")
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
                    unpinApp(packageName)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun unpinApp(packageName: String) {
        val prefs = context.getSharedPreferences("BlueBytePrefs", Context.MODE_PRIVATE)
        val pinned = prefs.getStringSet("pinned_apps", null)?.toMutableSet() ?: mutableSetOf()
        
        if (pinned.isEmpty()) {
            // If it was default (take 8), we need to populate pinned set first excluding this one
            val allApps = AppUtils.fetchApps(context)
            val defaultPinned = allApps.take(8).map { it.packageName.toString() }.toMutableSet()
            defaultPinned.remove(packageName)
            prefs.edit { putStringSet("pinned_apps", defaultPinned) }
        } else {
            pinned.remove(packageName)
            prefs.edit { putStringSet("pinned_apps", pinned) }
        }
        
        ((context as? MainActivity)?.supportFragmentManager?.findFragmentById(R.id.container) as? HomeScreenFragment)?.refreshPinnedApps()
    }

    override fun getItemCount(): Int = appsList.size

    class ViewHolder(val binding: ItemTileBinding) : RecyclerView.ViewHolder(binding.root)
}
