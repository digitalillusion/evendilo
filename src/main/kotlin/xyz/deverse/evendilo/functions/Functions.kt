package xyz.deverse.evendilo.functions

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import java.lang.IllegalStateException


inline fun<T> replaceList(list: MutableList<T>, replacer: (T) -> T) {
    val tempList = mutableListOf<T>()
    list.forEach { tempList.add(it) }
    val replacement = tempList.map { replacer(it) }
    list.clear()
    replacement.forEach { list.add(it) }
}

inline fun<T> replaceListIndexed(list: MutableList<T>, replacer: (Int, T) -> T) {
    val tempList = mutableListOf<T>()
    list.forEach { tempList.add(it) }
    val replacement = tempList.mapIndexed { index, t ->  replacer(index, t) }
    list.clear()
    replacement.forEach { list.add(it) }
}

fun<T> mergeDistinct(destination: MutableList<T>, list: MutableList<T>) {
    val merged = HashSet<T>()
    merged.addAll(destination)
    merged.addAll(list)
    destination.clear()
    merged.forEach { destination.add(it) }
}

fun getAuthentication(): OAuth2AuthenticationToken {
    val authentication = SecurityContextHolder.getContext().authentication ?: throw IllegalStateException("Authentication is not present in security context")
    return authentication as OAuth2AuthenticationToken
}