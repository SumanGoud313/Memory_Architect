package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.domain.model.AppError
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit/OkHttp exceptions are a data-layer concern; domain only ever sees [AppError]. Also the
 * single choke point every repository's catch block already flows through, so it's where
 * genuinely unexpected failures get reported to Crashlytics as non-fatals - [AppError.Network] is
 * deliberately excluded (a user being offline is an expected, routine condition, not a bug; a
 * Crashlytics dashboard full of "no internet" reports would drown out anything actually worth
 * looking at).
 */
@Singleton
class ErrorMapper @Inject constructor(private val crashReporter: CrashReporter) {
    fun Throwable.toAppError(): AppError = when (this) {
        is HttpException -> AppError.Server(code(), message()).also { crashReporter.recordException(this) }
        is IOException -> AppError.Network(this)
        else -> AppError.Unknown(this).also { crashReporter.recordException(this) }
    }
}
