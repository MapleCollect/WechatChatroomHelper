package com.zdy.project.wechat_chatroom_helper.ui.helper.uisetting

import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.zdy.project.wechat_chatroom_helper.R
import com.zdy.project.wechat_chatroom_helper.utils.ColorUtils

/**
 * Created by zhudo on 2017/12/2.
 */
class SettingFragment : Fragment() {


    private lateinit var settingViewHolder: SettingViewModel

    private lateinit var thisActivity: UISettingActivity
    private lateinit var contentLayout: LinearLayout
    private var titles = arrayOf("助手ToolBar颜色", "助手背景颜色", "会话列表标题颜色", "会话列表内容颜色", "会话列表时间颜色")

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        thisActivity = context as UISettingActivity
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        contentLayout = LinearLayout(thisActivity)
        contentLayout.orientation = LinearLayout.VERTICAL

        for (i in 0 until 5) {
            val itemView = LayoutInflater.from(thisActivity).inflate(R.layout.layout_color_setting_item, contentLayout, false)
            itemView.findViewById<TextView>(android.R.id.text1).text = titles[i]
            contentLayout.addView(itemView)
        }

        val scrollView = ScrollView(thisActivity)
        scrollView.addView(contentLayout)
        return scrollView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        settingViewHolder = UISettingActivity.obtainSettingViewModel(thisActivity)

        setupSettingColor()
    }

    override fun onResume() {
        super.onResume()
        settingViewHolder.start()
    }

    private fun setupSettingColor() {
        settingViewHolder.helperColor.observe(thisActivity, Observer<String> { setItemColor(0, it) })
        settingViewHolder.toolbarColor.observe(thisActivity, Observer<String> { setItemColor(1, it) })
        settingViewHolder.nicknameColor.observe(thisActivity, Observer<String> { setItemColor(2, it) })
        settingViewHolder.contentColor.observe(thisActivity, Observer<String> { setItemColor(3, it) })
        settingViewHolder.timeColor.observe(thisActivity, Observer<String> { setItemColor(4, it) })
    }

    private fun setItemColor(index: Int, value: String?) {
        val text = contentLayout.getChildAt(index).findViewById<TextView>(android.R.id.text2)
        text.text = value
        text.setTextColor(ColorUtils.getColorInt(value.toString()))
    }


}