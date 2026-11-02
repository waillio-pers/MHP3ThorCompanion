package com.waillio.mhp3rdcompanion

import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InlineImeInputTest {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun interceptorDisablesExtractUiWithoutChangingImeActionOrConnection() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val originalRequest = object : PlatformTextInputMethodRequest {
            override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
                outAttributes.imeOptions = EditorInfo.IME_ACTION_SEARCH
                outAttributes.inputType = android.text.InputType.TYPE_CLASS_TEXT
                return BaseInputConnection(View(context), false)
            }
        }
        val session = object : PlatformTextInputSession {
            override val view: View = View(context)

            override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
                val editorInfo = EditorInfo()
                val connection = request.createInputConnection(editorInfo)
                assertTrue(editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_EXTRACT_UI != 0)
                assertTrue(editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_FULLSCREEN != 0)
                assertEquals(EditorInfo.IME_ACTION_SEARCH, editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION)
                assertNotNull(connection)
                throw RequestReachedSystem()
            }
        }

        try {
            inlineImeInterceptor.interceptStartInputMethod(originalRequest, session)
            throw AssertionError("The interceptor should forward the input request to the platform session")
        } catch (_: RequestReachedSystem) {
            // The fake platform session was called after the EditorInfo was decorated.
        }
    }

    private class RequestReachedSystem : RuntimeException()
}
