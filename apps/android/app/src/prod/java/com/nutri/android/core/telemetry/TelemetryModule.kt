package com.nutri.android.core.telemetry

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** prod flavor (ADR-014): no Firebase, no telemetry. */
@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {
    @Provides
    fun telemetry(): Telemetry = NoopTelemetry
}
