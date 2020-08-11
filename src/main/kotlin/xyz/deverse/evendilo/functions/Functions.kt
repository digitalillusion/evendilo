package xyz.deverse.evendilo.functions


inline fun<T> replaceList(list: MutableList<T>, replacer: (T) -> T) {
    var replacement = list.map { replacer(it) }
    list.clear()
    replacement.forEach { list.add(it) }
}
