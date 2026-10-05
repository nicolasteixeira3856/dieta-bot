package app.fibrai.android.core.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import app.fibrai.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** dev flavor (ADR-014): Crashlytics + Analytics on fibrai-dev. */
class FirebaseTelemetry(
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics,
) : Telemetry {
    init {
        crashlytics.setCustomKey(TelemetryEvents.KEY_ENV, BuildConfig.ENV)
    }

    override fun event(name: String, params: Map<String, Any>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is Long -> bundle.putLong(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(name, bundle)
        crashlytics.log("event $name $params")
    }

    override fun breadcrumb(message: String) = crashlytics.log(message)

    override fun nonFatal(error: Throwable) = crashlytics.recordException(error)

    override fun setKey(key: String, value: String) = crashlytics.setCustomKey(key, value)
}

@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {
    @Provides
    @Singleton
    fun telemetry(@ApplicationContext context: Context): Telemetry =
        FirebaseTelemetry(FirebaseAnalytics.getInstance(context), FirebaseCrashlytics.getInstance())
}
