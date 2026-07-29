package com.example.memoring.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 화면(Activity/ViewModel) 수명과 무관하게 반드시 완료돼야 하는 작업용 앱 전역 스코프.
 * 예: 단어 저장 — 저장 도중 화면을 나가도 취소되지 않아야 한다.
 */
object AppScope {
    val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
