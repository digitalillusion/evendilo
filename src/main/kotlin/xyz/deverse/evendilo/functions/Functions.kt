package xyz.deverse.evendilo.functions

import xyz.deverse.evendilo.model.woocommerce.AttributeTerm


inline fun<T> replaceList(list: MutableList<T>, replacer: (T) -> T) {
    var replacement = list.map { replacer(it) }
    list.clear()
    replacement.forEach { list.add(it) }
}


inline fun<T> mergeDistinct(destination: MutableList<T>, list: MutableList<T>) {
    var merged = HashSet<T>()
    merged.addAll(destination)
    merged.addAll(list)
    destination.clear()
    merged.forEach { destination.add(it) }
}