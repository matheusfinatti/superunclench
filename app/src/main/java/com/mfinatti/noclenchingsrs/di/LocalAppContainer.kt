package com.mfinatti.noclenchingsrs.di

import androidx.compose.runtime.staticCompositionLocalOf

/** Provided once in MainActivity; route-level composables use it to build their ViewModels. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided")
}
