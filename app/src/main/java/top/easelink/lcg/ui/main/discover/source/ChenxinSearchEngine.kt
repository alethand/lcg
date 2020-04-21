package top.easelink.lcg.ui.main.discover.source

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import top.easelink.lcg.network.ApiClient
import java.net.URLEncoder

object ChenxinSearchEngine {

    suspend fun search(key: String): Map<String, String>? {
        val encodedKey = URLEncoder.encode(key)
        return ApiClient.sendGetRequestWithUrl("http://pan.ischenxin.com/xs/index.php?k=%s&limit=1".format(encodedKey))
            ?.let {
                Timber.d(it.body().text())
                return Gson().fromJson(it.body().text(), genericType<String>())
            }
    }

    private inline fun <reified T> genericType() = object: TypeToken<T>() {}.type
}