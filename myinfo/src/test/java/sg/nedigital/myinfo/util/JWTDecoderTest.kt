package sg.nedigital.myinfo.util

import com.auth0.android.jwt.Claim
import com.auth0.android.jwt.JWT
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class JWTDecoderTest {

    @Test
    fun decode() {
        val jwt =
            JWTDecoder().decode("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Il9SQzZ4d09NdmJ0dDZhald1WmU2R2xncy1qM3dtNXJpQXlDVW9SYXNhLUkifQ.eyJzdWIiOiJkZjB3ZjdmMC1mOHN3LWUyOTItZjlzZC1kMGZ3YmFhYmQ4ZjciLCJqdGkiOiJHSkRvMmFGUG1sV2JUWldTUDRyR05vcHFrT2lXeWF1U25XTHJtTk1UIiwic2NvcGUiOlsibmFtZSIsImRvYiIsInNleCIsIm5hdGlvbmFsaXR5Il0sInRva2VuTmFtZSI6ImFjY2Vzc190b2tlbiIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJncmFudF90eXBlIjoiYXV0aG9yaXphdGlvbl9jb2RlIiwiZXhwaXJlc19pbiI6MTgwMCwiYXVkIjoiU1RHLVQxOENTMDAwMUUtTlRVQy1GQUlSUFJJQ0UiLCJyZWFsbSI6Im15aW5mby1jb20iLCJpc3MiOiJodHRwczovL3Rlc3QuYXBpLm15aW5mby5nb3Yuc2cvc2VydmljZWF1dGgvbXlpbmZvLWNvbSIsImlhdCI6MTYwNzkyMjc4MiwibmJmIjoxNjA3OTIyNzgyLCJleHAiOjE2MDc5MjQ1ODJ9.FWxC6oyEgogh-UBGWHjFfk5vguaC0e-m_RSwTK8d4PSHF2iBBEKKnHOGzMwLtotUb3YMK0LacN5AxMY0zTie5JsJ9Zn9h4kj76gGSruc182u_rdG-ZKTdSesX8dTf00oiR_dQFVqupEULAe9RE-VTGMXKS6btWxCYvPEXv4ctb8Mhm6uWTmg1478_NuvUPwU1DdG7bfDJiBs3F2lLz9_u6rEjbFKbtOj-pD_9Gyq8bV_ixBMNy_7ulyCWj6D2mHLJqyzp2WKktxT_17X7qT6POiNaWdkiVq6-XYqxkWuxmtm8c3LketlaUp9lZ0e8J4JTdSlf7w-Dr1JbWKXCwNLIA")
        assertNotNull(jwt)
    }

    @Test
    fun isExpired() {
        val mock: JWT = mock()
        mock.stub {
            on { expiresAt }.thenReturn(Date())
        }
        assert(JWTDecoder().isExpired(mock))
    }

    @Test
    fun getClaim() {
        val mock: JWT = mock()
        val claim: Claim = mock()
        claim.stub {
            on { asString() }.thenReturn("123")
        }
        mock.stub {
            on { getClaim(any()) }.thenReturn(claim)
        }
        assertEquals("123", JWTDecoder().getClaim(mock))
    }
}