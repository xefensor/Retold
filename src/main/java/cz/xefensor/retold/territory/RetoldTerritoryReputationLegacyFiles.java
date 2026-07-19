package cz.xefensor.retold.territory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class RetoldTerritoryReputationLegacyFiles {
    private RetoldTerritoryReputationLegacyFiles() {
    }

    static Path archivePath(Path legacyPath) {
        return legacyPath.resolveSibling(RetoldTerritoryConstants.REPUTATION_MIGRATED_FILE_NAME);
    }

    static Path archive(Path legacyPath) throws IOException {
        return Files.move(legacyPath, archivePath(legacyPath));
    }
}
