package omahoco.migrate.io;

import omahoco.migrate.config.Config;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class FileMigrationSource implements MigrationSource {
    private final Config config;

    @Inject
    public FileMigrationSource(final Config config) {
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public List<File> getMigrations() {
        try {
            Path migrationPath = new File(config.getConfigPath(),
                    config.getMigrationPath()).getCanonicalFile().toPath();

            return Files.find(migrationPath, Integer.MAX_VALUE, (path, attributes) -> attributes.isRegularFile())
                    .map(Path::toFile)
                    .filter(t -> t.getName().endsWith(".gremlin") || t.getName().endsWith(".groovy"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("An error occurred locating migration files, using migrationPath: "
                    + config.getMigrationPath(), e);
        }
    }
}
