package xyz.deverse.evendilo.functions


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