package sg.nedigital.myinfo.util

import sg.nedigital.myinfo.exceptions.MyInfoException

interface MyInfoCallback<T> {
    fun onSuccess(payload: T?)
    fun onError(throwable: MyInfoException)
}
