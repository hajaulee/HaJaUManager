package com.hajaulee.anytv.hajaumanager

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ListAdapter(context: Context, private val mResource: Int, items: List<DownloadPackageInfo>) :
    ArrayAdapter<DownloadPackageInfo?>(context, mResource, items) {
    private val TAG = ListAdapter::class.simpleName
    private val mItems: List<DownloadPackageInfo>
    private val mInflater: LayoutInflater

    init {
        mItems = items
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: mInflater.inflate(mResource, null)

        val item: DownloadPackageInfo = mItems[position]

        val thumbnail = view.findViewById<ImageView>(R.id.thumbnail)
        val appName = view.findViewById<TextView>(R.id.appName)
        val latestVersion = view.findViewById<TextView>(R.id.latestVersion)
        val installedVersion = view.findViewById<TextView>(R.id.installedVersion)
        val removeButton = view.findViewById<ImageButton>(R.id.removeButton)
        val downloadButton = view.findViewById<ImageButton>(R.id.downloadButton)

        appName.text = item.appName
        latestVersion.text = "Latest: ${item.packageVersion}"
        installedVersion.text = "Installed: ${item.installedVersion}"
        if (item.installedVersion == null){
            disableButton(removeButton)
        }

        removeButton.setOnClickListener {
            removePackage(item.packageName)
        }

//        if (item.installedVersion == item.packageVersion){
//            disableButton(downloadButton)
//        }
        downloadButton.setOnClickListener {
            downloadPackage(item)
        }

        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val bitmap = getBitmap(item.iconUrl)
            handler.post {
                thumbnail.setImageBitmap(bitmap)
            }
        }

        return view
    }

    private fun removePackage(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE )
            intent.data = Uri.parse("package:$packageName")
            context.startActivity(intent)
        }catch (_: Exception){
            Toast.makeText(context, "Can not uninstall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadPackage(packageInfo: DownloadPackageInfo){
        try {

            val executor: ExecutorService = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())

            executor.execute {

                val url = URL(packageInfo.packageUrl)
                val input = url.openConnection().getInputStream()

                val fileUri = "${context.cacheDir}/${packageInfo.packageName}.apk"
                val output = FileOutputStream(fileUri)
                input.copyTo(output)
                output.close()

                handler.post {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val mFileUri = FileProvider.getUriForFile(
                        context,
                        context.applicationContext.packageName + ".my.package.name.provider",
                        File(fileUri)
                    )
                    intent.setDataAndType(mFileUri, "application/vnd.android.package-archive")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(context, "Can not install", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBitmap(bitmapUrl: String?): Bitmap? {
        return try {
            val url = URL(bitmapUrl)
            BitmapFactory.decodeStream(url.openConnection().getInputStream())
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    private fun disableButton(view: View){
        view.isEnabled = false
        view.alpha = 0.3F
    }
}