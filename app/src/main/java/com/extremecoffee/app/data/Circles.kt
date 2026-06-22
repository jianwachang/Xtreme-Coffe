package com.extremecoffee.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CircleMember(val id: String, val name: String)
data class Circle(val id: String, val name: String, val members: List<CircleMember>)

/** Cerchie salvate con nome (gruppi di amici registrati), persistite localmente in JSON. */
object Circles {
    private const val PREFS = "extreme_coffee_circles"
    private const val KEY = "list"

    fun all(context: Context): List<Circle> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val mArr = o.optJSONArray("members") ?: JSONArray()
                val members = (0 until mArr.length()).map { j ->
                    val m = mArr.getJSONObject(j)
                    CircleMember(m.optString("id"), m.optString("name"))
                }
                Circle(o.optString("id"), o.optString("name"), members)
            }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, circle: Circle) {
        val list = all(context).toMutableList().apply { add(circle) }
        save(context, list)
    }

    fun remove(context: Context, id: String) {
        save(context, all(context).filterNot { it.id == id })
    }

    private fun save(context: Context, list: List<Circle>) {
        val arr = JSONArray()
        for (c in list) {
            val mArr = JSONArray()
            for (m in c.members) mArr.put(JSONObject().put("id", m.id).put("name", m.name))
            arr.put(JSONObject().put("id", c.id).put("name", c.name).put("members", mArr))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
