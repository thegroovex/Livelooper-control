package com.groovex.livelooper.control.update

/** Semantic version "1.2.3" or "1.2.3-beta.1" (leading "v" allowed). Pre-releases sort before the release. */
data class Version(val major: Int, val minor: Int, val patch: Int, val pre: String?) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        compareValues(major, other.major).let { if (it != 0) return it }
        compareValues(minor, other.minor).let { if (it != 0) return it }
        compareValues(patch, other.patch).let { if (it != 0) return it }
        return when {
            pre == other.pre -> 0
            pre == null -> 1
            other.pre == null -> -1
            else -> comparePre(pre, other.pre)
        }
    }

    override fun toString() = "$major.$minor.$patch" + (pre?.let { "-$it" } ?: "")

    companion object {
        fun parse(text: String): Version? {
            val t = text.trim().removePrefix("v").substringBefore("+")
            val core = t.substringBefore("-")
            val pre = t.substringAfter("-", "").ifEmpty { null }?.takeUnless { it == "debug" }
            val n = core.split(".")
            if (n.isEmpty() || n.size > 3) return null
            val nums = n.map { it.toIntOrNull() ?: return null } + List(3 - n.size) { 0 }
            return Version(nums[0], nums[1], nums[2], pre)
        }

        private fun comparePre(a: String, b: String): Int {
            val pa = a.split("."); val pb = b.split(".")
            for (i in 0 until maxOf(pa.size, pb.size)) {
                val x = pa.getOrNull(i) ?: return -1
                val y = pb.getOrNull(i) ?: return 1
                val xi = x.toIntOrNull(); val yi = y.toIntOrNull()
                val c = if (xi != null && yi != null) xi.compareTo(yi) else x.compareTo(y)
                if (c != 0) return c
            }
            return 0
        }
    }
}
