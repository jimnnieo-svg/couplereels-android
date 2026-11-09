package com.couplereels.app
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.*
import org.junit.Test
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class UserMessageTest {
 @Test fun networkFailureProvidesRetryGuidance() { assertTrue(FirebaseNetworkException("raw service details").userMessage().contains("인터넷")) }
 @Test fun badCredentialsDoNotExposeServicePayload() { val text = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "secret payload").userMessage(); assertTrue(text.contains("비밀번호")); assertFalse(text.contains("secret")) }
 @Test fun permissionFailureExplainsCoupleAccess() { assertTrue(FirebaseFirestoreException("raw", FirebaseFirestoreException.Code.PERMISSION_DENIED).userMessage().contains("커플 연결")) }
 @Test fun expiredLoginRequestsReauthentication() { assertTrue(FirebaseFirestoreException("raw", FirebaseFirestoreException.Code.UNAUTHENTICATED).userMessage().contains("다시 로그인")) }
 @Test fun unknownServiceErrorDoesNotExposeDetails() { assertFalse(Exception("sensitive diagnostic").userMessage().contains("sensitive")) }
 @Test fun domainValidationMessageIsPreserved() { assertEquals("먼저 커플을 연결하세요.", IllegalStateException("먼저 커플을 연결하세요.").userMessage()) }
}
