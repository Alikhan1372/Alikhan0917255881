package com.example.data

import java.util.Locale

object UsernameAliasMapper {
    private val aliasLocalParts = mapOf(
        "vahid" to "vahid",
        "mohammad" to "mohammad",
        "yavari" to "yavari",
        "amir" to "amir",
        "technical" to "technical"
    )

    private val domainPattern = Regex(
        "^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$"
    )

    fun toFirebaseEmail(username: String, aliasDomain: String): String? {
        val localPart = aliasLocalParts[username.trim().lowercase(Locale.ROOT)] ?: return null
        val domain = normalizedDomain(aliasDomain) ?: return null
        return "$localPart@$domain"
    }

    fun usernameFromFirebaseEmail(email: String?, aliasDomain: String): String? {
        val domain = normalizedDomain(aliasDomain) ?: return null
        val parts = email?.trim()?.lowercase(Locale.ROOT)?.split('@') ?: return null
        if (parts.size != 2 || parts[1] != domain) return null
        return aliasLocalParts.entries.firstOrNull { it.value == parts[0] }?.key
    }

    fun isValidAliasDomain(aliasDomain: String): Boolean = normalizedDomain(aliasDomain) != null

    private fun normalizedDomain(aliasDomain: String): String? {
        val domain = aliasDomain.trim().lowercase(Locale.ROOT)
        if (!domainPattern.matches(domain)) return null
        return domain
    }
}