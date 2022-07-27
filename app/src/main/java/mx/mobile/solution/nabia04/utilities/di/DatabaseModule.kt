/*
 * Copyright (C) 2020 The Android Open Source Project
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

package mx.mobile.solution.nabia04.utilities.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.data.MainDataBase
import mx.mobile.solution.nabia04.data.dao.AnnDao
import mx.mobile.solution.nabia04.data.dao.DBdao
import mx.mobile.solution.nabia04.data.dao.DuesDao
import mx.mobile.solution.nabia04.ui.activities.endpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): MainDataBase {
        return MainDataBase.getDatabase(appContext)
    }

    @Provides
    fun provideAnnDao(database: MainDataBase): AnnDao {
        return database.annDao()
    }

    @Provides
    fun provideUserDBDao(database: MainDataBase): DBdao {
        return database.dbDao()
    }

    @Provides
    fun provideUserDuesDao(database: MainDataBase): DuesDao {
        return database.duesDao()
    }

    @Provides
    @Singleton
    fun provideEndpoint(): MainEndpoint {
        return endpoint
    }

    @Provides
    @Singleton
    fun provideSharedPref(@ApplicationContext appContext: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
    }

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext appContext: Context):
            MyAlarmManager = MyAlarmManager(appContext)

}
