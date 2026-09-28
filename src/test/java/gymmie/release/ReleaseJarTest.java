package gymmie.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Tests the release JAR's manifest and bundled platform resources. */
class ReleaseJarTest {
    @Test
    @EnabledIfSystemProperty(named = "gymmie.releaseTests", matches = "true")
    void includesApplicationManifestAndPlatformNatives() throws IOException {
        String jarPath = System.getProperty("gymmie.releaseJarPath");
        assertNotNull(jarPath, "Gradle must provide the release JAR path.");

        try (JarFile releaseJar = new JarFile(jarPath)) {
            Manifest manifest = releaseJar.getManifest();
            assertNotNull(manifest);
            assertEquals("gymmie.Launcher", manifest.getMainAttributes().getValue("Main-Class"));
            assertEquals(
                    "ALL-UNNAMED",
                    manifest.getMainAttributes().getValue("Enable-Native-Access"));

            assertEntry(releaseJar, "gymmie/Launcher.class");
            assertEntry(releaseJar, "glass.dll");
            assertEntry(releaseJar, "libglass.dylib");
            assertEntry(releaseJar, "libglass.so");
            assertEntry(releaseJar, "org/sqlite/native/Windows/x86_64/sqlitejdbc.dll");
            assertEntry(releaseJar, "org/sqlite/native/Mac/aarch64/libsqlitejdbc.dylib");
            assertEntry(releaseJar, "org/sqlite/native/Linux/x86_64/libsqlitejdbc.so");
        }
    }

    private static void assertEntry(JarFile jarFile, String entryName) {
        assertNotNull(jarFile.getEntry(entryName), "Missing release JAR entry: " + entryName);
    }
}
