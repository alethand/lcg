package top.easelink.lcg.ui.main.discover.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.item_lanzous_search_view.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import top.easelink.framework.threadpool.CommonPool
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.discover.model.LanzousSearchModel
import top.easelink.lcg.ui.main.discover.source.ChenxinSearchEngine
import top.easelink.lcg.ui.webview.view.WebViewActivity
import java.net.URLDecoder
import java.net.URLEncoder

class LanzousSearchBinder: BaseNavigationBinder<LanzousSearchModel, LanzousSearchVH>() {
    override fun onBindViewHolder(holder: LanzousSearchVH, item: LanzousSearchModel) {
        holder.initView()
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): LanzousSearchVH {
        return LanzousSearchVH(inflater, parent)
    }
}


class LanzousSearchVH(inflater: LayoutInflater, parentView: ViewGroup): BaseNavigationViewHolder(
    inflater.inflate(R.layout.item_lanzous_search_view, parentView, false)
) {
    fun initView() {
        itemView.run {
            search_content.setOnEditorActionListener { _, actionId, _ ->
                when(actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {

                        doSearch(context, search_content.text.toString())
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun doSearch(context: Context, key: String) {
        if (key.isBlank() || key.length < 2) return
        val url = "http://pan.ischenxin.com/search/%s.html".format(key)
//        WebViewActivity.startWebViewWith(url, context)
        GlobalScope.launch(CommonPool) {
            val result = ChenxinSearchEngine.search(key)
            result?.forEach {
                Timber.d("${it.key} : ${it.value}")
            }
        }
    }
}