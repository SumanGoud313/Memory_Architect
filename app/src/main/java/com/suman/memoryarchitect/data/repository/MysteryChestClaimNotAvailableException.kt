package com.suman.memoryarchitect.data.repository

/** Thrown from inside a Firestore transaction (see
 * [FirestoreShopRemoteSource.claimAdMysteryChest]) when every one of today's
 * [com.suman.memoryarchitect.domain.progression.MysteryChestAdRules.maxClaimsPerDay] ad-gated
 * claims is already spent - re-checked server-side against freshly-read
 * [com.suman.memoryarchitect.domain.model.MysteryChestAdState] rather than trusting the client's
 * own button-enabled state, the same "recognize, don't just trust" discipline
 * [SpinNotAvailableException] already uses for Lucky Spin's own daily allowances. */
class MysteryChestClaimNotAvailableException : RuntimeException("Every Mystery Chest ad claim was already used today")
