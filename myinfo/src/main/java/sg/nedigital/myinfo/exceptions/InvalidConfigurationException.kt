package sg.nedigital.myinfo.exceptions

class InvalidConfigurationException(reason: String, cause: Throwable? = null) :
    MyInfoException(reason, cause)
