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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.mobile.solution.nabia04.ui.adapters.AnnListAdapter
import mx.mobile.solution.nabia04.ui.adapters.ContListAdapter
import mx.mobile.solution.nabia04.ui.adapters.DBcurrentListAdapter
import mx.mobile.solution.nabia04.ui.adapters.DBdepartedListAdapter
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AdapterModule {
    @Provides
    fun provideAdapter(@ApplicationContext appContext: Context): AnnListAdapter {
        return AnnListAdapter(appContext)
    }

    @Provides
    @Singleton
    fun provideCurrentListAdapter(@ApplicationContext appContext: Context): DBcurrentListAdapter {
        return DBcurrentListAdapter(appContext)
    }

    @Provides
    @Singleton
    fun provideDepartedListAdapter(@ApplicationContext appContext: Context): DBdepartedListAdapter {
        return DBdepartedListAdapter(appContext)
    }

    @Provides
    @Singleton
    fun provideContListAdapter(@ApplicationContext appContext: Context): ContListAdapter {
        return ContListAdapter(appContext)
    }
}
