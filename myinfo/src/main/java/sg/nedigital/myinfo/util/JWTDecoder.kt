package sg.nedigital.myinfo.util

import com.auth0.android.jwt.JWT

class JWTDecoder {
    fun decode(jwt: String): JWT {
        return JWT(jwt)
    }

    fun isExpired(jwt: JWT): Boolean {
        return jwt.expiresAt != null && jwt.expiresAt?.time!! < System.currentTimeMillis()
    }

    fun getClaim(jwt: JWT): String? {
        return jwt.getClaim("sub").asString()
    }
}
