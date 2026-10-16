package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.InferenceConfig
import com.example.ai.LocalModelProviderImpl
import com.example.rea.REAAdapterImpl
import com.example.tools.ToolExecutionRequest
import com.example.tools.ToolExecutionResult
import com.example.tools.ToolRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NICK AI", appName)
  }

  @Test
  fun `local model provider hardware check returns valid metrics`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val provider = LocalModelProviderImpl(context)
    val hw = provider.checkDeviceHardware()

    assertTrue("RAM should be positive", hw.totalRamMb > 0)
    assertTrue("Storage should be positive", hw.freeStorageMb >= 0)
    assertNotNull(provider.getAvailableCatalog())
    assertTrue("Catalog should contain TinyLlama", provider.getAvailableCatalog().any { it.id == "tinyllama-1.1b" })
  }

  @Test
  fun `local model streaming generates offline response`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val provider = LocalModelProviderImpl(context)
    val chunks = mutableListOf<String>()

    provider.generateStreaming("Hello NICK AI", InferenceConfig()).collect { chunk ->
      chunks.add(chunk)
    }

    val fullResponse = chunks.joinToString("")
    assertTrue("Response should contain NICK AI", fullResponse.contains("NICK AI"))
  }

  @Test
  fun `tool registry rejects dangerous arbitrary commands`() = runBlocking {
    val registry = ToolRegistry()
    val request = ToolExecutionRequest(
      toolName = "dispatch_termux_command",
      arguments = mapOf("command" to "rm -rf /"),
      userConfirmed = true
    )

    val result = registry.executeTool(request, hasGlobalConfirmation = true) { _, _ -> "Executed" }
    assertTrue("Arbitrary command must be denied", result is ToolExecutionResult.Denied)
  }

  @Test
  fun `rea adapter parses doctor json diagnostics accurately`() = runBlocking {
    val rea = REAAdapterImpl()
    val doctorJson = """
      {
        "status": "ok",
        "node_supported": true,
        "tools": ["rea_inspect_tree", "rea_summarize_code"]
      }
    """.trimIndent()

    val result = rea.runDiagnostics(doctorJson)
    assertTrue("Diagnostics should succeed for valid doctor output", result.success)
    assertEquals(2, result.toolsDetected.size)
  }

  @Test
  fun `termux bridge manager initializes with token and url`() {
    val manager = com.example.termux.TermuxBridgeManager(
      baseUrl = "http://127.0.0.1:8765/",
      initialToken = "test-token-123"
    )
    assertTrue(manager.isConfigured())
    assertEquals("test-token-123", manager.getToken())
    assertEquals("http://127.0.0.1:8765/", manager.getBaseUrl())
  }

  @Test
  fun `termux bridge manager security rejects dangerous command`() = runBlocking {
    val manager = com.example.termux.TermuxBridgeManager(
      baseUrl = "http://127.0.0.1:8765/",
      initialToken = "test-token"
    )
    val result = manager.executeCommand("rm -rf /")
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is SecurityException)
  }

  @Test
  fun `termux bridge manager allows verified command prefixes`() = runBlocking {
    val manager = com.example.termux.TermuxBridgeManager(
      baseUrl = "http://127.0.0.1:8765/",
      initialToken = "test-token"
    )
    // Command is permitted by security validator; network will fail gracefully if no loopback daemon is bound
    val result = manager.executeCommand("node --version")
    assertFalse("Should not fail with SecurityException", result.exceptionOrNull() is SecurityException)
  }

  @Test
  fun `nick ai view model instantiates successfully without exception`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.NickAiViewModel(app)
    assertNotNull(viewModel)
    assertNotNull(viewModel.termuxIntegration)
    assertNotNull(viewModel.termuxBridgeManager)
    assertNotNull(viewModel.localModelProvider)
    assertNotNull(viewModel.reaAdapter)
    assertEquals("dashboard", viewModel.currentScreen.value)
  }
}
