package com.couplereels.app

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestoreException

/** Never display raw service exception payloads in the UI. */
fun Throwable.userMessage(): String = when (this) {
 is FirebaseNetworkException -> "인터넷 연결을 확인한 후 다시 시도하세요."
 is FirebaseTooManyRequestsException -> "요청이 많아 잠시 제한되었습니다. 잠시 후 다시 시도하세요."
 is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> "이메일과 비밀번호를 확인하거나 다시 로그인하세요."
 is FirebaseAuthUserCollisionException -> "이미 가입된 이메일입니다. 로그인해 주세요."
 is FirebaseAuthWeakPasswordException -> "비밀번호는 6자 이상으로 입력하세요."
 is FirebaseFirestoreException -> when (code) {
  FirebaseFirestoreException.Code.PERMISSION_DENIED -> "데이터 접근 권한이 없습니다. 커플 연결 상태와 보안 규칙을 확인하세요."
  FirebaseFirestoreException.Code.UNAUTHENTICATED -> "로그인이 만료되었습니다. 다시 로그인하세요."
  FirebaseFirestoreException.Code.UNAVAILABLE -> "서버에 연결할 수 없습니다. 인터넷을 확인한 후 다시 시도하세요."
  FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "서버 응답이 늦습니다. 저장 상태를 확인한 후 다시 시도하세요."
  else -> "데이터 처리에 실패했습니다. 연결 상태를 확인한 후 다시 시도하세요."
 }
 is IllegalArgumentException, is IllegalStateException -> message?.takeIf { it.isNotBlank() } ?: "입력값과 현재 연결 상태를 확인하세요."
 else -> "요청을 처리하지 못했습니다. 다시 시도하세요."
}
