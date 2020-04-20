package top.easelink.lcg.ui.main.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.item_lanzous_search_view.view.*
import timber.log.Timber
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.discover.model.LanzousSearchModel

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
                        Timber.d(search_content.text.toString())
                        // do search
                        true
                    }
                    else -> false
                }
            }
        }
    }
}