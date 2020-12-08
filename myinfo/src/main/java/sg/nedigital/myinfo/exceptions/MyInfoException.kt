package sg.nedigital.myinfo.exceptions

open class MyInfoException(message: String?) : Exception(message) {
    internal constructor(reason: String?, cause: Throwable?) : this(reason)
}