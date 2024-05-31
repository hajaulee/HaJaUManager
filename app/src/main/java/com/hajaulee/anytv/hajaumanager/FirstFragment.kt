package com.hajaulee.anytv.hajaumanager

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hajaulee.anytv.hajaumanager.ExtensionsLoader.Companion.getPackageContext
import com.hajaulee.anytv.hajaumanager.ExtensionsLoader.Companion.getVersion
import com.hajaulee.anytv.hajaumanager.databinding.FragmentFirstBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private val TAG = "FirstFragment"
    private var _binding: FragmentFirstBinding? = null
    private val infoUrl = "https://raw.githubusercontent.com/hajaulee/HaJaUManager/main/packages.json"
    private val listPackages: ArrayList<DownloadPackageInfo> = ArrayList()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            loadPackageInfo()
            swipeRefreshLayout.isRefreshing = false
        }

        loadPackageInfo()
    }

    @SuppressLint("StaticFieldLeak")
    fun loadPackageInfo(){
        object : AsyncTask<String?, String?, String>() {
            override fun doInBackground(vararg strings: String?): String {
                val url = strings[0]
                val webContent = StringBuilder()
                try {
                    val web = URL(url)
                    val bufferedReader = BufferedReader(
                        InputStreamReader(
                            web.openStream())
                    )
                    Log.d(TAG, "$url")
                    var inputLine: String?
                    while (bufferedReader.readLine().also { inputLine = it } != null) {
                        webContent.append(inputLine)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, e.toString())
                }
                return webContent.toString()
            }

            public override fun onPostExecute(jsonContent: String) {
                updateListPackages(jsonContent)
                updateListView()
            }
        }.execute(infoUrl)
    }


    fun updateListPackages(jsonContent: String){
        listPackages.clear()
        try{
            val pm: PackageManager = context!!.packageManager

            Log.d(TAG, "jsonContent${jsonContent}")
            val packages = JSONArray(jsonContent)
            for (i in 0 until packages.length()){
                val item = packages.getJSONObject(i)
                val installedVersion = getVersion(pm, item.getString("packageName"))
                listPackages.add(
                    DownloadPackageInfo(
                        item.getString("packageName"),
                        item.getString("packageVersion"),
                        item.getString("appName"),
                        item.getString("iconUrl"),
                        item.getString("packageUrl"),
                        installedVersion
                    )
                )
            }
            Log.d(TAG, "Have all ${listPackages.size} packages.")
        }catch (e: Exception){
            e.printStackTrace()
        }

    }

    fun updateListView(){
        val listView: ListView = binding.listview

        val adapter = ListAdapter(activity!!, R.layout.sample_list_item, listPackages)
        listView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}