package dev.justme.busket.helpers

import java.util.UUID

class SessionStore {
    companion object {
        val sessionUUID = UUID.randomUUID().toString()
    }
}