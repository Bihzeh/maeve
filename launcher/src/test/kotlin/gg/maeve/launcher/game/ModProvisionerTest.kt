package gg.maeve.launcher.game

import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModProvisionerTest {

    @Test
    fun `local override is copied to maeve_jar`(@TempDir mods: Path) {
        val local = Files.createTempFile(mods, "dev-mod", ".jar").also { it.writeBytes(byteArrayOf(1, 2, 3, 4)) }

        val installed = installMaeveMod(mods, local, openBundled = { error("must not read bundled when override present") })

        assertTrue(installed)
        val target = mods.resolve("maeve.jar")
        assertTrue(target.exists())
        assertContentEquals(byteArrayOf(1, 2, 3, 4), target.readBytes())
    }

    @Test
    fun `bundled resource is extracted when no override`(@TempDir mods: Path) {
        val bundled = byteArrayOf(10, 20, 30)

        val installed = installMaeveMod(mods, localOverride = null, openBundled = { ByteArrayInputStream(bundled) })

        assertTrue(installed)
        assertContentEquals(bundled, mods.resolve("maeve.jar").readBytes())
    }

    @Test
    fun `falls back to bundled when override path does not exist`(@TempDir mods: Path) {
        val bundled = byteArrayOf(7, 7, 7)

        val installed = installMaeveMod(mods, mods.resolve("nope.jar"), openBundled = { ByteArrayInputStream(bundled) })

        assertTrue(installed)
        assertContentEquals(bundled, mods.resolve("maeve.jar").readBytes())
    }

    @Test
    fun `returns false and writes nothing when neither override nor bundle present`(@TempDir mods: Path) {
        val installed = installMaeveMod(mods, localOverride = null, openBundled = { null })

        assertFalse(installed)
        assertFalse(mods.resolve("maeve.jar").exists())
    }

    @Test
    fun `status callback reports local vs bundled source`(@TempDir root: Path) {
        val a = Files.createDirectory(root.resolve("a"))
        val b = Files.createDirectory(root.resolve("b"))
        val local = Files.createTempFile(root, "dev-mod", ".jar").also { it.writeBytes(byteArrayOf(1)) }
        val statuses = mutableListOf<String>()

        installMaeveMod(a, local, openBundled = { null }, onStatus = { statuses.add(it) })
        installMaeveMod(b, null, openBundled = { ByteArrayInputStream(byteArrayOf(2)) }, onStatus = { statuses.add(it) })

        assertEquals(listOf("Mod: maeve (local)", "Mod: maeve"), statuses)
    }

    @Test
    fun `bundledModStream finds a present resource and returns null for a missing one`() {
        // launcher/src/test/resources/bundled-mods/maeve.jar is on the test classpath.
        assertNotNull(bundledModStream("bundled-mods/maeve.jar"))
        assertNull(bundledModStream("bundled-mods/definitely-absent.jar"))
    }

    @Test
    fun `findDevModJar picks the newest non-sources jar`(@TempDir dir: Path) {
        val old = dir.resolve("mod-0.0.1.jar").also { it.writeBytes(byteArrayOf(0)) }
        val new = dir.resolve("mod-0.1.4.jar").also { it.writeBytes(byteArrayOf(0)) }
        val sources = dir.resolve("mod-0.1.4-sources.jar").also { it.writeBytes(byteArrayOf(0)) }
        Files.setLastModifiedTime(old, FileTime.fromMillis(1_000))
        Files.setLastModifiedTime(new, FileTime.fromMillis(2_000))
        Files.setLastModifiedTime(sources, FileTime.fromMillis(9_000)) // newest, but excluded as -sources

        assertEquals(new, findDevModJar(dir))
    }

    @Test
    fun `findDevModJar returns null for a missing directory`(@TempDir dir: Path) {
        assertNull(findDevModJar(dir.resolve("does-not-exist")))
    }

    @Test
    fun `selectBundledMods always installs required deps even when toggles exclude them`() {
        // Regression: the UI passes only {sodium, lithium}; fabric-api + fabric-language-kotlin
        // are REQUIRED deps of the Maeve mod and must install or Fabric aborts with
        // "requires fabric-api / fabric-language-kotlin, which is missing".
        assertEquals(
            listOf("fabric-api", "fabric-language-kotlin", "sodium", "lithium"),
            selectBundledMods(setOf("sodium", "lithium")),
        )
        assertEquals(listOf("fabric-api", "fabric-language-kotlin"), selectBundledMods(emptySet()))
    }

    @Test
    fun `selectBundledMods with null installs all bundled mods`() {
        assertEquals(
            listOf("fabric-api", "fabric-language-kotlin", "sodium", "lithium"),
            selectBundledMods(null),
        )
    }

    @Test
    fun `selectBundledMods honors a single optional toggle but keeps required deps`() {
        assertEquals(listOf("fabric-api", "fabric-language-kotlin", "sodium"), selectBundledMods(setOf("sodium")))
    }
}
