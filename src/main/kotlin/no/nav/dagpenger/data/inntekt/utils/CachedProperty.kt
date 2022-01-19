package no.nav.dagpenger.data.inntekt.utils

import java.time.Duration
import java.time.Instant
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class CachedProperty<T>(private val timeToLive: Duration, private val initializer: () -> T) : ReadOnlyProperty<Any, T> {
    private var value = initializer()
    private var expiresAt = expires()

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (Instant.now().isAfter(expiresAt)) {
            value = initializer()
            expiresAt = expires()
        }

        return value
    }

    private fun expires() = Instant.now() + timeToLive
}
