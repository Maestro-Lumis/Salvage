package com.application.salvage.di

import app.salvage.presentation.viewmodel.ItemListViewModel
import org.koin.dsl.module

val presentationModule = module {
    factory { ItemListViewModel(get(), get()) }
}

val appModules = sharedLogicModules + listOf(presentationModule)