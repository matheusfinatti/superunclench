package com.mfinatti.noclenchingsrs.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

/** Writes [value] under [key], or removes the key when [value] is null. */
internal fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
    if (value == null) {
        remove(key)
    } else {
        set(key, value)
    }
}

/** Parses a persisted enum name, falling back to [default] for missing/unknown values. */
internal inline fun <reified E : Enum<E>> String?.toEnumOrDefault(default: E): E {
    if (this == null) return default
    return enumValues<E>().firstOrNull { it.name == this } ?: default
}
