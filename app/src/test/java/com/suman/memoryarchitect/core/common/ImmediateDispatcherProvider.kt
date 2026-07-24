package com.suman.memoryarchitect.core.common

import kotlinx.coroutines.Dispatchers

/** Shared test fixture: runs everything on the caller's thread, no real dispatching. */
object ImmediateDispatcherProvider : DispatcherProvider {
    override val main = Dispatchers.Unconfined
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
}