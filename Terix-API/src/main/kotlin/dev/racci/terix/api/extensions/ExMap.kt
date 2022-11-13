package dev.racci.terix.api.extensions // ktlint-disable filename

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentHashMap.newKeySet

public inline fun <reified K, reified V> concurrentMultimap(): Multimap<K, V> = Multimaps.newSetMultimap<K, V>(ConcurrentHashMap(), ::newKeySet)
