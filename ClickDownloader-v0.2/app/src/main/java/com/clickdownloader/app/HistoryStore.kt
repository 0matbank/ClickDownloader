package com.clickdownloader.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class HistoryStore(context: Context) {
    private val file = context.filesDir.resolve("download_history.json")

    @Synchronized
    fun load(): List<DownloadJob> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        DownloadJob(
                            id = o.optString("id"),
                            url = o.optString("url"),
                            title = o.optString("title"),
                            preset = runCatching {
                                DownloadPreset.valueOf(o.optString("preset"))
                            }.getOrDefault(DownloadPreset.AUTO),
                            state = runCatching {
                                JobState.valueOf(o.optString("state"))
                            }.getOrDefault(JobState.COMPLETED),
                            progress = o.optInt("progress"),
                            etaSeconds = o.optLong("etaSeconds", -1),
                            error = o.optString("error"),
                            createdAt = o.optLong("createdAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(items: List<DownloadJob>) {
        val array = JSONArray()
        items.take(250).forEach { job ->
            array.put(
                JSONObject()
                    .put("id", job.id)
                    .put("url", job.url)
                    .put("title", job.title)
                    .put("preset", job.preset.name)
                    .put("state", job.state.name)
                    .put("progress", job.progress)
                    .put("etaSeconds", job.etaSeconds)
                    .put("error", job.error)
                    .put("createdAt", job.createdAt)
            )
        }
        file.writeText(array.toString())
    }

    fun clear() {
        if (file.exists()) file.delete()
    }
}
