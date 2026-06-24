package gg.maeve.launcher.auth

import gg.maeve.launcher.game.MaevePaths
import java.nio.file.Files
import java.nio.file.Path

/**
 * Resolves the Azure application (client) ID. Until a settings UI exists, it comes
 * from the MAEVE_AZURE_CLIENT_ID env var or a `azure_client_id.txt` file in the
 * launcher data dir. The client ID is a public-client identifier (not a secret).
 */
object AuthConfig {
    const val ENV = "MAEVE_AZURE_CLIENT_ID"
    const val SCOPE = "XboxLive.signin offline_access"

    fun clientId(dataDir: Path = MaevePaths.default().root): String? {
        System.getenv(ENV)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val file = dataDir.resolve("azure_client_id.txt")
        return if (Files.exists(file)) Files.readString(file).trim().ifEmpty { null } else null
    }
}
