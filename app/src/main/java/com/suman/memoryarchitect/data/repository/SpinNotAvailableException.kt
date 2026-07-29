package com.suman.memoryarchitect.data.repository

/** Thrown from inside a Firestore transaction (see [FirestoreShopRemoteSource.spin]) when
 * [com.suman.memoryarchitect.domain.repository.SpinSource.FREE]/[com.suman.memoryarchitect.domain.repository.SpinSource.AD]
 * is requested but that allowance was already spent today - re-checked server-side against
 * freshly-read [com.suman.memoryarchitect.domain.model.LuckySpinState] rather than trusting the
 * client's own button-enabled state, the same "recognize, don't just trust" discipline
 * [InsufficientInventoryException] already uses for a ticket-funded spin. */
class SpinNotAvailableException : RuntimeException("This Lucky Spin allowance was already used today")
