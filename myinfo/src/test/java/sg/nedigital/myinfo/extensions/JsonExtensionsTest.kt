package sg.nedigital.myinfo.extensions

import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JsonExtensionsTest {
    val json = JSONObject("{\"name\":{\"lastupdated\":\"2020-10-01\",\"source\":\"1\",\"classification\":\"C\",\"value\":\"MY.INFO:CC\"},\"dob\":{\"lastupdated\":\"2020-10-01\",\"source\":\"1\",\"classification\":\"C\",\"value\":\"1948-02-01\"},\"sex\":{\"lastupdated\":\"2020-10-01\",\"code\":\"M\",\"source\":\"1\",\"classification\":\"C\",\"desc\":\"MALE\"},\"nationality\":{\"lastupdated\":\"2020-10-01\",\"code\":\"IN\",\"source\":\"1\",\"classification\":\"C\",\"desc\":\"INDIAN\"}}")

    @Test
    fun getDob() {
        val person = json.getDob()
        assertEquals("2020-10-01", person.lastupdated)
        assertEquals("1", person.source)
        assertEquals("C", person.classification)
        assertEquals("1948-02-01", person.value)
    }

    @Test
    fun getName() {
        val person = json.getName()
        assertEquals("2020-10-01", person.lastupdated)
        assertEquals("1", person.source)
        assertEquals("C", person.classification)
        assertEquals("MY.INFO:CC", person.value)
    }

    @Test
    fun getSex() {
        val person = json.getSex()
        assertEquals("2020-10-01", person.lastupdated)
        assertEquals("1", person.source)
        assertEquals("C", person.classification)
        assertEquals("MALE", person.desc)
        assertEquals("M", person.code)
    }

    @Test
    fun getNationality() {
        val person = json.getNationality()
        assertEquals("2020-10-01", person.lastupdated)
        assertEquals("1", person.source)
        assertEquals("C", person.classification)
        assertEquals("INDIAN", person.desc)
        assertEquals("IN", person.code)
    }
}