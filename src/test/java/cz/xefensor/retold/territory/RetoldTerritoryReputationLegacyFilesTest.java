package cz.xefensor.retold.territory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldTerritoryReputationLegacyFilesTest {
    @TempDir
    Path tempDirectory;

    @Test
    void legacySourceRemainsUntilArchivalIsExplicitlyRequested() throws IOException {
        Path legacyPath = writeLegacyFile("legacy reputation");

        assertTrue(Files.exists(legacyPath));
        assertFalse(Files.exists(RetoldTerritoryReputationLegacyFiles.archivePath(legacyPath)));
    }

    @Test
    void confirmedMigrationMovesSourceToRetainedBackup() throws IOException {
        Path legacyPath = writeLegacyFile("legacy reputation");
        Path archivePath = RetoldTerritoryReputationLegacyFiles.archive(legacyPath);

        assertFalse(Files.exists(legacyPath));
        assertTrue(Files.exists(archivePath));
        assertEquals("legacy reputation", Files.readString(archivePath, StandardCharsets.UTF_8));
    }

    @Test
    void existingBackupIsNeverOverwrittenAndSourceIsNotDeleted() throws IOException {
        Path legacyPath = writeLegacyFile("new source");
        Path archivePath = RetoldTerritoryReputationLegacyFiles.archivePath(legacyPath);
        Files.writeString(archivePath, "existing backup", StandardCharsets.UTF_8);

        assertThrows(
                FileAlreadyExistsException.class,
                () -> RetoldTerritoryReputationLegacyFiles.archive(legacyPath)
        );

        assertTrue(Files.exists(legacyPath));
        assertEquals("existing backup", Files.readString(archivePath, StandardCharsets.UTF_8));
    }

    private Path writeLegacyFile(String contents) throws IOException {
        Path legacyPath = tempDirectory.resolve(RetoldTerritoryConstants.REPUTATION_SAVE_FILE_NAME);
        Files.writeString(legacyPath, contents, StandardCharsets.UTF_8);
        return legacyPath;
    }
}
