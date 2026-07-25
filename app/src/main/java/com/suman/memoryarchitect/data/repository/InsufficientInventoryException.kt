package com.suman.memoryarchitect.data.repository

/** Thrown from inside a Firestore transaction (see [FirestoreMissionRemoteSource.consumeInventoryItem])
 * when a consume would take a quantity below zero - the same "reject inside the transaction
 * against freshly-read state" discipline [MissionAlreadyClaimedException]/
 * [DailyRewardAlreadyClaimedException] already use, so two rapid consumes of the last unit of an
 * item can never both succeed. */
class InsufficientInventoryException : RuntimeException("Not enough of this inventory item to consume")
