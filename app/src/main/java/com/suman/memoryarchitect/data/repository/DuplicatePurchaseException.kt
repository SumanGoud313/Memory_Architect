package com.suman.memoryarchitect.data.repository

/** Same replay-guard role as [DuplicateSubmissionException], for purchase/spin receipts. */
class DuplicatePurchaseException : RuntimeException("Purchase already processed")
