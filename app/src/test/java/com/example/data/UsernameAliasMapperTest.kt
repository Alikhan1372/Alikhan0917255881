package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsernameAliasMapperTest {
    @Test
    fun `maps only the five provisioned usernames to configured aliases`() {
        val domain = "accounts.example.invalid"

        assertEquals("vahid@$domain", UsernameAliasMapper.toFirebaseEmail("vahid", domain))
        assertEquals("mohammad@$domain", UsernameAliasMapper.toFirebaseEmail("mohammad", domain))
        assertEquals("yavari@$domain", UsernameAliasMapper.toFirebaseEmail("yavari", domain))
        assertEquals("amir@$domain", UsernameAliasMapper.toFirebaseEmail("amir", domain))
        assertEquals("technical@$domain", UsernameAliasMapper.toFirebaseEmail("technical", domain))
    }

    @Test
    fun `normalizes username and domain casing and whitespace`() {
        assertEquals(
            "vahid@accounts.example.invalid",
            UsernameAliasMapper.toFirebaseEmail("  VAHID ", " Accounts.Example.Invalid ")
        )
    }

    @Test
    fun `does not map unknown usernames`() {
        assertNull(UsernameAliasMapper.toFirebaseEmail("unknown", "accounts.example.invalid"))
    }

    @Test
    fun `requires a configured fully qualified alias domain`() {
        assertNull(UsernameAliasMapper.toFirebaseEmail("vahid", ""))
        assertNull(UsernameAliasMapper.toFirebaseEmail("vahid", "localhost"))
        assertNull(UsernameAliasMapper.toFirebaseEmail("vahid", "@example.invalid"))
    }
}