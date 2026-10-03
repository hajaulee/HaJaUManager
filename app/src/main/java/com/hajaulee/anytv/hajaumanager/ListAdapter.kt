package com.hajaulee.anytv.hajaumanager

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.Executors

class ListAdapter(
    context: Context,
    items: List<DownloadPackageInfo>
) : BaseAdapter() {
    private val appContext = context.applicationContext
    private val inflater = LayoutInflater.from(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val rows = buildRows(items)

    override fun getCount(): Int = rows.size

    override fun getItem(position: Int): Any = rows[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = VIEW_TYPE_COUNT

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is PackageRow.Section -> VIEW_TYPE_SECTION
        is PackageRow.Empty -> VIEW_TYPE_EMPTY
        is PackageRow.App -> VIEW_TYPE_APP
    }

    override fun isEnabled(position: Int): Boolean = rows[position] is PackageRow.App

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return when (val row = rows[position]) {
            is PackageRow.Section -> {
                val view = convertView
                    ?: inflater.inflate(R.layout.item_package_section, parent, false)
                (view as TextView).text = appContext.getString(row.title, row.count)
                view
            }
            is PackageRow.Empty -> {
                val view = convertView
                    ?: inflater.inflate(R.layout.item_package_empty, parent, false)
                (view as TextView).text = appContext.getString(row.message)
                view
            }
            is PackageRow.App -> {
                val view = convertView
                    ?: inflater.inflate(R.layout.sample_list_item, parent, false)
                bindApp(view, row.packageInfo)
                view
            }
        }
    }

    private fun bindApp(view: View, app: DownloadPackageInfo) {
        val icon = view.findViewById<ImageView>(R.id.thumbnail)
        val appName = view.findViewById<TextView>(R.id.appName)
        val latestVersion = view.findViewById<TextView>(R.id.latestVersion)
        val installedVersion = view.findViewById<TextView>(R.id.installedVersion)
        val removeButton = view.findViewById<Button>(R.id.removeButton)
        val downloadButton = view.findViewById<Button>(R.id.downloadButton)

        appName.text = app.appName
        latestVersion.text = appContext.getString(
            R.string.package_latest_version,
            app.packageVersion
        )
        installedVersion.text = app.installedVersion?.let {
            appContext.getString(R.string.package_installed_version, it)
        } ?: appContext.getString(R.string.package_not_installed)

        val installed = app.installedVersion != null
        removeButton.visibility = if (installed) View.VISIBLE else View.GONE
        removeButton.contentDescription =
            appContext.getString(R.string.action_uninstall) + ": " + app.appName
        removeButton.setOnClickListener { removePackage(app.packageName) }

        downloadButton.setText(
            when {
                !installed -> R.string.action_install
                isUpdateAvailable(app.packageVersion, app.installedVersion) ->
                    R.string.action_update
                else -> R.string.action_reinstall
            }
        )
        downloadButton.contentDescription = downloadButton.text.toString() + ": " + app.appName
        downloadButton.setOnClickListener { downloadPackage(app) }

        PackageIconLoader.load(app.iconUrl, icon)
    }

    private fun removePackage(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                appContext,
                R.string.package_uninstall_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun downloadPackage(app: DownloadPackageInfo) {
        Toast.makeText(
            appContext,
            appContext.getString(R.string.package_downloading, app.appName),
            Toast.LENGTH_SHORT
        ).show()

        PackageDownloadExecutor.execute {
            try {
                val apkFile = File(appContext.cacheDir, "${app.packageName}.apk")
                val connection = URL(app.packageUrl).openConnection().apply {
                    connectTimeout = NETWORK_TIMEOUT_MS
                    readTimeout = NETWORK_TIMEOUT_MS
                }
                connection.getInputStream().use { input ->
                    FileOutputStream(apkFile).use { output -> input.copyTo(output) }
                }

                mainHandler.post {
                    val apkUri = FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.my.package.name.provider",
                        apkFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, APK_MIME_TYPE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    appContext.startActivity(intent)
                }
            } catch (exception: Exception) {
                mainHandler.post {
                    Toast.makeText(
                        appContext,
                        appContext.getString(R.string.package_download_failed, app.appName),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun buildRows(items: List<DownloadPackageInfo>): List<PackageRow> {
        val updates = items.filter { app ->
            app.installedVersion != null &&
                isUpdateAvailable(app.packageVersion, app.installedVersion)
        }
        val installed = items.filter { app ->
            app.installedVersion != null &&
                !isUpdateAvailable(app.packageVersion, app.installedVersion)
        }
        val notInstalled = items.filter { app -> app.installedVersion == null }

        return buildList {
            add(
                PackageRow.Section(
                    title = R.string.package_section_updates,
                    count = updates.size
                )
            )
            addAppsOrEmpty(updates, R.string.package_empty_updates)

            add(
                PackageRow.Section(
                    title = R.string.package_section_installed,
                    count = installed.size
                )
            )
            addAppsOrEmpty(installed, R.string.package_empty_installed)

            add(
                PackageRow.Section(
                    title = R.string.package_section_not_installed,
                    count = notInstalled.size
                )
            )
            addAppsOrEmpty(notInstalled, R.string.package_empty_not_installed)
        }
    }

    private fun MutableList<PackageRow>.addAppsOrEmpty(
        apps: List<DownloadPackageInfo>,
        emptyMessage: Int
    ) {
        if (apps.isEmpty()) {
            add(PackageRow.Empty(emptyMessage))
        } else {
            apps.forEach { add(PackageRow.App(it)) }
        }
    }

    private fun isUpdateAvailable(latestVersion: String, installedVersion: String?): Boolean {
        if (installedVersion == null || installedVersion == latestVersion) return false

        val latestParts = latestVersion.toVersionParts() ?: return true
        val installedParts = installedVersion.toVersionParts() ?: return true
        val partCount = maxOf(latestParts.size, installedParts.size)

        for (index in 0 until partCount) {
            val latestPart = latestParts.getOrElse(index) { 0 }
            val installedPart = installedParts.getOrElse(index) { 0 }
            if (latestPart != installedPart) return latestPart > installedPart
        }
        return false
    }

    private fun String.toVersionParts(): List<Int>? {
        val parts = split('.')
        if (parts.isEmpty()) return null
        return parts.map { part -> part.toIntOrNull() ?: return null }
    }

    private sealed class PackageRow {
        data class Section(val title: Int, val count: Int) : PackageRow()
        data class Empty(val message: Int) : PackageRow()
        data class App(val packageInfo: DownloadPackageInfo) : PackageRow()
    }

    private companion object {
        const val VIEW_TYPE_SECTION = 0
        const val VIEW_TYPE_EMPTY = 1
        const val VIEW_TYPE_APP = 2
        const val VIEW_TYPE_COUNT = 3
        const val NETWORK_TIMEOUT_MS = 15_000
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}

private object PackageDownloadExecutor {
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "package-download").apply { isDaemon = true }
    }

    fun execute(task: () -> Unit) {
        executor.execute(task)
    }
}

private object PackageIconLoader {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newFixedThreadPool(3) { task ->
        Thread(task, "package-icon").apply { isDaemon = true }
    }
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount / 1024
    }

    fun load(url: String, imageView: ImageView) {
        imageView.setImageResource(R.drawable.ic_android_red_24dp)
        imageView.tag = url
        cache.get(url)?.let {
            imageView.setImageBitmap(it)
            return
        }

        val imageReference = WeakReference(imageView)
        executor.execute {
            val bitmap = try {
                val connection = URL(url).openConnection().apply {
                    connectTimeout = ICON_TIMEOUT_MS
                    readTimeout = ICON_TIMEOUT_MS
                }
                connection.getInputStream().use(BitmapFactory::decodeStream)
            } catch (_: Exception) {
                null
            }

            if (bitmap != null) {
                cache.put(url, bitmap)
                mainHandler.post {
                    imageReference.get()
                        ?.takeIf { it.tag == url }
                        ?.setImageBitmap(bitmap)
                }
            }
        }
    }

    private const val ICON_TIMEOUT_MS = 10_000
}
