package omahoco.migrate.graph;

import omahoco.migrate.util.Delayer;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.graph.GraphResultSet;
import com.datastax.driver.dse.graph.GraphStatement;
import com.datastax.driver.dse.graph.SimpleGraphStatement;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class GraphSchema implements Schema<GraphResultSet, GraphStatement> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphSchema.class);
    private static final String COMMAND_SEPARATOR = ".";
    private static final String GRAPH_STATEMENT = "system.graph('%s')";
    private static final String OPTION_STATEMENT = "option(\"%s\")";
    private static final String SET_STATEMENT = "set(\"%s\")";
    private static final String CREATE_COMMAND = "ifNotExists().create()";
    private static final String DROP_COMMAND = "drop()";
    private static final int SCHEMA_AGREEMENT_ATTEMPTS = 15;
    private static final int RETRY_DELAY_MILLIS = 1000;

    private final DseSession dseSession;
    private final Delayer delayer;
    private final SchemaInfo schemaInfo;

    @Inject
    public GraphSchema(final Delayer delayer, final SchemaInfo schemaInfo, final DseSession session) {
        this.dseSession = Objects.requireNonNull(session);
        this.schemaInfo = Objects.requireNonNull(schemaInfo);
        this.delayer = Objects.requireNonNull(delayer);
    }

    @Override
    public void create() {
        create(null);
    }

    @Override
    public void create(final Map<String, String> options) {
        final String statement =
                combine(
                        () -> Optional.of(String.format(GRAPH_STATEMENT, schemaInfo.getSchemaName())),
                        () -> getGraphOptions(options),
                        () -> Optional.of(CREATE_COMMAND));

        LOGGER.info("Creating schema {}", schemaInfo.getSchemaName());
        waitForSchemaAgreement(dseSession.executeGraph(statement));
    }

    @Override
    public void drop() {
        final String statement =
                combine(
                        () -> Optional.of(String.format(GRAPH_STATEMENT, schemaInfo.getSchemaName())),
                        () -> Optional.of(DROP_COMMAND));

        LOGGER.info("Dropping schema {}", schemaInfo.getSchemaName());
        waitForSchemaAgreement(dseSession.executeGraph(statement));
    }

    @Override
    public void execute(final String statement) {
        execute(new SimpleGraphStatement(statement));
    }

    @Override
    public GraphResultSet execute(final GraphStatement graphStatement) {
        final GraphResultSet resultSet =
                dseSession.executeGraph(graphStatement.setGraphName(schemaInfo.getSchemaName()));

        if (isSchemaChange(graphStatement)) {
            waitForSchemaAgreement(resultSet);
        }

        return resultSet;
    }

    @VisibleForTesting
    static boolean isSchemaChange(final GraphStatement statement) {
        if (!(statement instanceof SimpleGraphStatement)) {
            throw new IllegalArgumentException(
                    "Unsupported statement type: " + statement.getClass().getCanonicalName());
        }

        final String query = ((SimpleGraphStatement) statement).getQueryString();

        return query.startsWith("schema.");
    }

    private void waitForSchemaAgreement(final GraphResultSet resultSet) {
        if (resultSet.getExecutionInfo().isSchemaInAgreement()) {
            return;
        }

        for (int i = 0; i < SCHEMA_AGREEMENT_ATTEMPTS; i++) {
            if (dseSession.getCluster().getMetadata().checkSchemaAgreement()) {
                return;
            }
            LOGGER.info("Waiting for schema agreement.");
            delayer.delay(RETRY_DELAY_MILLIS);
        }
        throw new IllegalStateException("Failed to achieve schema agreement across the cluster.");
    }

    private String combine(final Supplier<Optional<String>>... supplier) {
        final String statement =
                Arrays.stream(supplier)
                        .map(Supplier::get)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.joining(COMMAND_SEPARATOR));
        return statement;
    }

    private Optional<String> getGraphOptions(final Map<String, String> options) {
        if (options == null) {
            return Optional.empty();
        }

        return Optional.of(
                options
                        .entrySet()
                        .stream()
                        .map(t -> mapOptionValue(t.getKey(), t.getValue()))
                        .collect(Collectors.joining(COMMAND_SEPARATOR)));
    }

    private static String mapOptionValue(final String key, final String value) {
        return String.format(OPTION_STATEMENT, key)
                + COMMAND_SEPARATOR
                + String.format(SET_STATEMENT, value);
    }

    public static final class SchemaInfo {
        private final String schemaName;

        public SchemaInfo(final String schemaName) {
            this.schemaName = Objects.requireNonNull(schemaName);
        }

        public String getSchemaName() {
            return schemaName;
        }
    }
}
