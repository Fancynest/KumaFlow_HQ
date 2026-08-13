package com.mdd.prepaid_lib_flutter_null_safety

import android.content.Context
import androidx.preference.PreferenceManager

class PreferenceManagers {
    fun setData(key: String?, data: String?, context: Context?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val prefEditor = prefs.edit()
        prefEditor.putString(key, data)
        prefEditor.apply()
    }

    fun getData(key: String?, context: Context?): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(key, null)
    }

    fun clearDataWithKey(key: String?, context: Context?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        if (prefs.contains(key)) {
            editor.remove(key)
            editor.apply()
        }
    }

    fun setDataWithSameKey(key: String?, data: String?, context: Context?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        if (prefs.contains(key)) {
            editor.remove(key)
            editor.apply()
        }
        editor.putString(key, data)
        editor.commit()
    }

    fun clear(context: Context?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }

    fun hasData(key: String?, context: Context?): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(key, null) != null
    }
}