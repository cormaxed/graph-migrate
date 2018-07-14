package omahoco.migrate.di;

import omahoco.migrate.MigrationCli;
import omahoco.migrate.MigrationMetadata;
import omahoco.migrate.config.Config;
import omahoco.migrate.config.Profile;
import omahoco.migrate.graph.GraphMigrationMetadata;
import omahoco.migrate.graph.GraphSchema;
import omahoco.migrate.graph.Schema;
import omahoco.migrate.io.FileMigrationSource;
import omahoco.migrate.io.MigrationSource;
import omahoco.migrate.util.Delayer;
import omahoco.migrate.util.ThreadSleepDelayer;
import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.graph.GraphResultSet;
import com.datastax.driver.dse.graph.GraphStatement;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import java.util.Objects;

public final class DseModule extends AbstractModule {
    private final MigrationCli.MigrationArgs migrationArgs;
    private final Config config;
    private final String profileName;
    private DseCluster dseCluster;

    public DseModule(
            final MigrationCli.MigrationArgs migrationArgs,
            final Config config,
            final String profileName) {
        this.migrationArgs = Objects.requireNonNull(migrationArgs);
        this.config = Objects.requireNonNull(config);
        this.profileName = profileName;
    }

    @Override
    protected void configure() {
        bind(Delayer.class).to(ThreadSleepDelayer.class);

        dseCluster = getClusterBuilder(migrationArgs).build();
        bind(DseCluster.class).toInstance(dseCluster);
        bind(DseSession.class).toInstance(dseCluster.connect());

        bind(Profile.class).toInstance(getProfile(profileName));
        bind(Config.class).toInstance(config);

        bind(GraphSchema.SchemaInfo.class).toInstance(new GraphSchema.SchemaInfo(config.getSchema()));
        bind(new TypeLiteral<Schema<GraphResultSet, GraphStatement>>() {
        }).to(GraphSchema.class);
        bind(MigrationMetadata.class).to(GraphMigrationMetadata.class);

        bind(MigrationSource.class).to(FileMigrationSource.class);
    }

    private static DseCluster.Builder getClusterBuilder(
            final MigrationCli.MigrationArgs migrationArgs) {
        final DseCluster.Builder builder = DseCluster.builder().withPort(migrationArgs.getPort());
        migrationArgs.getHosts().forEach(t -> builder.addContactPoint(t));

        if (migrationArgs.isSsl()) {
            builder.withSSL();
        }

        if (migrationArgs.getUserName() != null) {
            builder.withCredentials(migrationArgs.getUserName(), migrationArgs.getPassword());
        }

        return builder;
    }

    private Profile getProfile(final String profileName) {
        if (config.getProfiles() == null) {
            return null;
        }

        if (profileName == null) {
            return config.getProfiles().get(config.getProfiles().keySet().iterator().next());
        }

        return config.getProfiles().get(profileName);
    }
}
