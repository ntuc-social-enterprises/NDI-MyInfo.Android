package sg.nedigital.myinfo.exceptions

class InvalidConfigurationException constructor(reason: String, cause: Throwable? = null) : MyInfoException(reason, cause)
