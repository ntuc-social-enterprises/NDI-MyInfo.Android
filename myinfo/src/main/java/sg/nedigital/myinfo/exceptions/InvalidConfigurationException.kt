package sg.nedigital.myinfo.exceptions

class InvalidConfigurationException constructor(reason: String) : MyInfoException(reason) {
    internal constructor(reason: String, cause: Throwable?) : this(reason) {}
}
