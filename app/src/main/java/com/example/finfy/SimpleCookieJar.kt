package com.example.finfy

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class SimpleCookieJar : CookieJar {

    // host -> lista de cookies
    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val current = store[host] ?: mutableListOf()

        // substitui cookies com mesmo name+path
        cookies.forEach { newCookie ->
            current.removeAll { it.name == newCookie.name && it.path == newCookie.path }
            current.add(newCookie)
        }

        // remove expirados
        val now = System.currentTimeMillis()
        current.removeAll { it.expiresAt < now }

        store[host] = current
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val now = System.currentTimeMillis()

        val cookies = store[host] ?: return emptyList()
        cookies.removeAll { it.expiresAt < now }

        return cookies.toList()
    }

    fun dump(host: String): String {
        val cookies = store[host] ?: return "(nenhum cookie armazenado)"
        return cookies.joinToString(separator = "\n") { c ->
            "${c.name}=<hidden>; domain=${c.domain}; path=${c.path}; secure=${c.secure}; httpOnly=${c.httpOnly}"
        }
    }

    fun clear() {
        store.clear()
    }
}
