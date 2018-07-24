package omahoco.migrate;

import omahoco.migrate.cli.CommaSeparatedStringOptionHandler;
import omahoco.migrate.config.Config;
import omahoco.migrate.config.YamlConfig;
import omahoco.migrate.di.DseModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Optional;

public final class MigrationCli {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationCli.class);

    private MigrationCli() {
    }

    public static void main(final String[] argv) {
        final MigrationArgs migrationArgs = new MigrationArgs();
        final CmdLineParser parser = new CmdLineParser(migrationArgs);

        try {
            parser.parseArgument(argv);
            final Config config =
                    YamlConfig.load(new BufferedInputStream(new FileInputStream(migrationArgs.configFile)));
            config.setConfigPath(Optional.ofNullable(migrationArgs.getConfigFile().getParent()).orElse("./"));

            final Injector injector =
                    Guice.createInjector(new DseModule(migrationArgs, config, migrationArgs.getProfile()));

            try (SchemaMigration schemaMigration = injector.getInstance(SchemaMigration.class)) {
                if (migrationArgs.getVersion() > 0) {
                    schemaMigration.setMaxVersion(migrationArgs.getVersion());
                }

                schemaMigration.migrate();
            }
        } catch (final CmdLineException e) {
            System.err.println(MigrationCli.class.getSimpleName() + " [options...]");
            parser.printUsage(System.err);
            System.err.println("\n" + e.getMessage());
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static final class MigrationArgs {
        @Option(name = "-H", handler = CommaSeparatedStringOptionHandler.class,
                usage = "Comma-separated list of contact points",
                required = true)
        private List<String> hosts;

        @Option(name = "-c", usage = "Path to configuration file")
        private File configFile = new File("graph-migrate.yaml");

        @Option(name = "-m", usage = "Name of the configuration profile to use")
        private String profile;

        @Option(name = "-p", usage = "Connection port")
        @SuppressWarnings("checkstyle:magicnumber")
        private int port = 9042;

        @Option(name = "-s", usage = "Connect using SSL")
        private boolean ssl;

        @Option(name = "-u", usage = "Connection username", depends = "-P")
        private String userName;

        @Option(name = "-P", usage = "Connection password")
        private String password;

        @Option(name = "-v", usage = "Apply all versions up to and including")
        private int version;

        public void setHosts(final List<String> hosts) {
            this.hosts = hosts;
        }

        public List<String> getHosts() {
            return hosts;
        }

        public void setConfigFile(final File configFile) {
            this.configFile = configFile;
        }

        public File getConfigFile() {
            return configFile;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(final String profile) {
            this.profile = profile;
        }

        public int getPort() {
            return port;
        }

        public void setPort(final int port) {
            this.port = port;
        }

        public boolean isSsl() {
            return ssl;
        }

        public void setSsl(final boolean ssl) {
            this.ssl = ssl;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(final int version) {
            this.version = version;
        }
    }
}
