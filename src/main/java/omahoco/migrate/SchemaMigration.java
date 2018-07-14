package omahoco.migrate;

import omahoco.migrate.config.Profile;
import omahoco.migrate.graph.Schema;
import omahoco.migrate.io.MigrationFileLoader;
import omahoco.migrate.io.MigrationSource;
import omahoco.migrate.parse.SimpleStatementParser;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.graph.GraphResultSet;
import com.datastax.driver.dse.graph.GraphStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SchemaMigration implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaMigration.class);
    private final Profile profile;
    private final MigrationFileLoader migrationFileLoader =
            new MigrationFileLoader(new SimpleStatementParser());
    private final DseSession dseSession;
    private int maxVersion = Integer.MAX_VALUE;
    private final Schema<GraphResultSet, GraphStatement> schema;
    private final MigrationSource migrationSource;
    private final MigrationMetadata migrationMetadata;

    @Inject
    SchemaMigration(
            final DseSession dseSession,
            final Profile profile,
            final Schema<GraphResultSet, GraphStatement> schema,
            final MigrationMetadata migrationMetadata,
            final MigrationSource migrationSource) {
        this.dseSession = dseSession;
        this.profile = profile;
        this.schema = schema;
        this.migrationMetadata = migrationMetadata;
        this.migrationSource = migrationSource;
    }

    public void setMaxVersion(final int maxVersion) {
        this.maxVersion = maxVersion;
    }

    public void migrate() {
        schema.create(profile.getOptions());

        final List<Migration> fileMigrations =
                migrationSource
                        .getMigrations()
                        .stream()
                        .map(migrationFileLoader::load)
                        .collect(Collectors.toList());

        LOGGER.info("Found {} migration files.", fileMigrations.size());

        final Map<String, Migration> appliedMigrations =
                migrationMetadata
                        .getMigrations()
                        .stream()
                        .collect(Collectors.toMap(t -> t.getFileName(), Function.identity()));

        final List<Migration> migrationsToApply =
                fileMigrations
                        .stream()
                        .filter(t -> applyMigrationFilter(appliedMigrations, t, maxVersion))
                        .collect(Collectors.toList());

        LOGGER.info("{} migration files need to be applied to the database.", migrationsToApply.size());

        applyMigrations(appliedMigrations, migrationsToApply);
    }

    private void applyMigrations(final Map<String, Migration> appliedMigrations,
                                 final List<Migration> migrationsToApply) {
        for (final Migration migration : migrationsToApply) {
            final Migration dbMigration = appliedMigrations.get(migration.getFileName());
            if (dbMigration != null && !migration.getCheckSum().equals(dbMigration.getCheckSum())) {
                throw new IllegalStateException(
                        MessageFormat.format(
                                "Migration {0} already applied with checksum {1} at {2}",
                                migration.getFileName(), dbMigration.getCheckSum(), dbMigration.getAppliedAt()));
            }

            LOGGER.info("Applying migration file {}", migration.getFileName());
            int executedStatements = 0;
            try {
                for (final String statement : migration.getStatements()) {
                    try {
                        schema.execute(statement);
                        executedStatements++;
                    } catch (final Exception e) {
                        LOGGER.error("Statement execution failed on statement # {}", executedStatements, e);
                        LOGGER.error("'{}'", statement);
                    }
                }
            } finally {
                LOGGER.info(
                        "{} - Applied {} of {} statements.",
                        migration.getFileName(),
                        executedStatements,
                        migration.getStatements().size());
            }

            migrationMetadata.saveMigration(migration);
        }
    }

    private boolean applyMigrationFilter(
            final Map<String, Migration> appliedMigrations,
            final Migration migration,
            final int maxVersion) {
        final Optional<Migration> migrationApplied =
                Optional.ofNullable(appliedMigrations.get(migration.getFileName()));

        return (!migrationApplied.isPresent()
                || !migrationApplied.get().getCheckSum().equals(migration.getCheckSum()))
                && migration.getVersion() <= maxVersion;
    }

    @Override
    public void close() throws Exception {
        dseSession.getCluster().close();
    }
}
