/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mx.mobile.solution.nabia04_beta1.utilities

import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import mx.mobile.solution.nabia04_beta1.App.Companion.applicationContext
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Utility class that decides whether we should fetch some data or not.
 */

class RateLimiter {
    companion object {
        val sharedP: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext())

        fun shouldFetch(key: String, tOut: Int, tUnit: TimeUnit): Boolean {
            val timeout = tUnit.toMillis(tOut.toLong())
            val lastFetched = sharedP.getLong(key, 0L)
            val now = now()
            if (lastFetched == 0L) {
                sharedP.edit().putLong(key, now).apply()
                return true
            }
            if (now - lastFetched > timeout) {
                sharedP.edit().putLong(key, now).apply()
                return true
            }
            return false
        }

        private fun now() = System.currentTimeMillis()
        fun reset(key: String) {
            sharedP.edit().putLong(key, now()).apply()
            Log.i("RateLimiter", "$key reset to : ${Date(now())}")
        }

        fun allow(key: String) {
            sharedP.edit().putLong(key, 0).apply()
            Log.i("RateLimiter", "$key Allowed to : ${Date(0)}")
        }

    }
}
