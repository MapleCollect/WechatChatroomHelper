package ui

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.*
import com.google.gson.JsonParser
import com.zdy.project.wechat_chatroom_helper.Constants
import com.zdy.project.wechat_chatroom_helper.Constants.WRITE_EXTERNAL_STORAGE_RESULT_CODE
import com.zdy.project.wechat_chatroom_helper.R
import manager.PermissionHelper
import network.ApiManager
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var thisActivity: MainActivity

    private lateinit var settingFragment: SettingFragment
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var textView: TextView
    private lateinit var clickMe: Button
    private lateinit var detail: TextView

    private lateinit var alertDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        thisActivity = this@MainActivity
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView) as TextView
        clickMe = findViewById(R.id.button) as Button
        detail = findViewById(R.id.detail) as TextView

        val fragmentContent: FrameLayout = findViewById(R.id.fragment_content) as FrameLayout

        sharedPreferences = getSharedPreferences(this.packageName + "_preferences", Context.MODE_PRIVATE)

        settingFragment = SettingFragment()

        fragmentManager.beginTransaction().replace(fragmentContent.id, settingFragment).commit()

        ApiManager.sendRequestForHomeInfo(
                getHelperVersionCode(this@MainActivity).toString(), object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                val result = response.body()?.string()
                this@MainActivity.runOnUiThread {
                    val webView = WebView(thisActivity)
                    webView.loadData(result, "text/html; charset=UTF-8", null)
                    alertDialog = AlertDialog.Builder(thisActivity).setView(webView).create()
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {}
        })

        clickMe.setOnClickListener { alertDialog.show() }

        //是否已经适配了合适的数据 的标记
        val hasSuitWechatData = sharedPreferences.getBoolean("has_suit_wechat_data", false)

        //play开关是否打开 的标记
        val playVersion = sharedPreferences.getBoolean("play_version", false)

        //当前主程序的版本号
        val helperVersionCode = sharedPreferences.getInt("helper_versionCode", 0)

        //当前保存的微信版本号
        val wechatVersion = sharedPreferences.getInt("wechat_version", 0)

        val wechatVersionCode = getWechatVersionCode()

        //如果没有适合的数据，或者刚刚更新了主程序版本
        if (wechatVersionCode != wechatVersion && wechatVersion != 0 || !hasSuitWechatData
                || helperVersionCode != getHelperVersionCode(this)) {

            //则发送数据请求
            sendRequest(wechatVersionCode, playVersion)
        } else {

            //否则则取出上次保存的合适的信息
            detail.setTextColor(0xFF888888.toInt())
            detail.text = sharedPreferences.getString("show_info", "")
        }

        permissionHelper = PermissionHelper.check(thisActivity)
    }

    private var permissionHelper: PermissionHelper? = null

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onRestart() {
        super.onRestart()
        permissionHelper = PermissionHelper.check(thisActivity)
    }


    private fun sendRequest(versionCode: Int, play_version: Boolean) {

        val requestBody = FormBody.Builder()
                .add("versionCode", versionCode.toString())
                .add("isPlayVersion", if (play_version) "1" else "0").build()

        val request = Request.Builder()
                .url(ApiManager.UrlPath.CLASS_MAPPING)
                .post(requestBody)
                .build()

        ApiManager.okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {

            }

            override fun onResponse(call: Call?, response: Response?) {

                try {
                    val string = response!!.body()!!.string()
                    Log.v("result = ", string)

                    val jsonObject = JsonParser().parse(string).asJsonObject

                    val code = jsonObject.get("code").asInt
                    val msg = jsonObject.get("msg").asString

                    val edit = sharedPreferences.edit()
                    if (code == 0) {
                        edit.putString("json", jsonObject.get("data").toString())
                        setSuccessText(msg)

                    } else {
                        edit.putString("json", "")
                        setFailText(msg)
                    }

                    edit.putInt("helper_versionCode", getHelperVersionCode(thisActivity))
                    edit.apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                    setFailText("从服务器获取数据失败，请联系开发者解决问题")
                }

            }
        })
    }


    private fun setFailText(msg: String) {
        runOnUiThread {
            detail.text = msg
            detail.setTextColor(0xFFFF0000.toInt())

            val edit = sharedPreferences.edit()
            edit!!.putBoolean("has_suit_wechat_data", false)
                    .putString("show_info", msg)
            edit.apply()
        }
    }

    private fun setSuccessText(msg: String) {
        runOnUiThread {
            detail.setTextColor(0xFF888888.toInt())
            detail.text = msg

            val edit = sharedPreferences.edit()
            edit.putBoolean("has_suit_wechat_data", true)
            edit.putInt("wechat_version", getWechatVersionCode())
            edit.putString("show_info", msg)
            edit.apply()
        }
    }

    private fun getWechatVersionCode(): Int {
        val list = packageManager.getInstalledPackages(0) as List<PackageInfo>

        var wechatVersionCode = -1

        try {
            for (packageInfo in list) {
                if (packageInfo.packageName == Constants.WECHAT_PACKAGE_NAME) {
                    wechatVersionCode = if (packageInfo.versionName == "6.5.14") 1101
                    else packageInfo.versionCode
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return wechatVersionCode
    }

    private fun getHelperVersionCode(context: Context): Int {
        val packageManager = context.packageManager as PackageManager
        var versionCode = 0

        try {
            versionCode = packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return versionCode
    }

    class SettingFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            addPreferencesFromResource(R.xml.pref_setting)

            val toolbarColor = findPreference("toolbar_color") as EditTextPreference
            val playVersion = findPreference("play_version") as SwitchPreference

            setToolbarColor(toolbarColor)
            setCheckPlayVersion(playVersion)
        }

        private fun setCheckPlayVersion(play_version: SwitchPreference) {

            play_version.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val activity = activity as MainActivity
                activity.sendRequest(activity.getWechatVersionCode(), newValue as Boolean)
                true
            }
        }


        private fun setToolbarColor(preference: EditTextPreference) {

            val watcher = PreferenceTextWatcher(preference)
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->

                val toolbarColor = preference.sharedPreferences.getString("toolbar_color", Constants.DEFAULT_TOOLBAR_COLOR)
                val toolbarColorInt = getColorInt(toolbarColor)

                val editText = preference.editText as EditText

                editText.backgroundTintList = ColorStateList.valueOf(toolbarColorInt)
                editText.backgroundTintMode = PorterDuff.Mode.SRC_IN
                editText.setTextColor(toolbarColorInt)
                editText.hint = "当前值" + toolbarColor
                editText.setSingleLine()
                editText.setSelection(editText.text.length)
                editText.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View?) {
                        editText.addTextChangedListener(watcher)
                    }

                    override fun onViewDetachedFromWindow(v: View?) {
                        editText.removeTextChangedListener(watcher)
                    }
                })

                false
            }

            preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                try {
                    getColorInt(newValue.toString())
                    Toast.makeText(activity, "颜色已更新", Toast.LENGTH_SHORT).show()
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }


        private fun getColorInt(colorString: CharSequence?): Int {
            return Color.parseColor("#" + colorString!!)
        }

        private inner class PreferenceTextWatcher internal
        constructor(internal var preference: EditTextPreference) : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val button = preference.dialog.findViewById(android.R.id.button1)
                if (s.length == 6)
                    try {
                        val color = getColorInt(s)
                        val editText = preference.editText
                        editText.setTextColor(color)
                        editText.backgroundTintList = ColorStateList.valueOf(color)
                        editText.backgroundTintMode = PorterDuff.Mode.SRC_IN
                        button.isEnabled = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        button.isEnabled = false
                    }
                else {
                    button.isEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable) {
            }
        }
    }
}