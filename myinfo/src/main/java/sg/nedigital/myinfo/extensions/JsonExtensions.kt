package sg.nedigital.myinfo.extensions

import com.google.gson.Gson
import org.json.JSONObject
import sg.nedigital.myinfo.entities.Person

fun JSONObject.getDob(): Person {
    return Gson().fromJson(getJSONObject("dob").toString(), Person::class.java)
}

fun JSONObject.getName(): Person {
    return Gson().fromJson(getJSONObject("name").toString(), Person::class.java)
}

fun JSONObject.getSex(): Person {
    return Gson().fromJson(getJSONObject("sex").toString(), Person::class.java)
}

fun JSONObject.getNationality(): Person {
    return Gson().fromJson(getJSONObject("nationality").toString(), Person::class.java)
}
