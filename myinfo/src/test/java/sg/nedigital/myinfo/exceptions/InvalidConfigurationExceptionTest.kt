package sg.nedigital.myinfo.exceptions

import java.io.IOException
import org.junit.Assert
import org.junit.Test

class InvalidConfigurationExceptionTest {
    @Test
    fun createTest() {
        val cause = IOException()
        val exception = InvalidConfigurationException("message", cause)
        Assert.assertEquals(cause, exception.cause)
        Assert.assertEquals("message", exception.message)
    }
}
