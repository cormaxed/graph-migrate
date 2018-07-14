package omahoco.migrate.graph;

import omahoco.migrate.Migration;
import omahoco.migrate.MigrationMetadata;
import omahoco.migrate.parse.SimpleStatementParser;
import omahoco.migrate.parse.StatementParser;
import com.datastax.driver.dse.graph.GraphNode;
import com.datastax.driver.dse.graph.GraphResultSet;
import com.datastax.driver.dse.graph.GraphStatement;
import com.datastax.driver.dse.graph.SimpleGraphStatement;
import com.datastax.driver.dse.graph.Vertex;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GraphMigrationMetadata implements MigrationMetadata {
    private static final String DATABASE_MIGRATION_LABEL = "databaseMigration";
    private static final String CREATE_METADATA_RESOURCE = "migration_metadata.gremlin";
    private final Schema<GraphResultSet, GraphStatement> graphSchema;
    private final StatementParser parser = new SimpleStatementParser();
    private boolean metadataExists = false;

    @Inject
    public GraphMigrationMetadata(final Schema<GraphResultSet, GraphStatement> graphSchema) {
        this.graphSchema = Objects.requireNonNull(graphSchema);
    }

    @Override
    public List<Migration> getMigrations() {
        final GraphStatement statement =
                new SimpleGraphStatement(
                        MessageFormat.format(
                                Statements.GET_MIGRATION_TEMPLATE,
                                DATABASE_MIGRATION_LABEL,
                                DATABASE_MIGRATION_LABEL));

        return graphSchema
                .execute(statement)
                .all()
                .stream()
                .map(GraphNode::asVertex)
                .map(MigrationMapper::toMigration)
                .sorted(Comparator.comparing(Migration::getFileName))
                .collect(Collectors.toList());
    }

    @Override
    public void saveMigration(final Migration migration) {
        init();

        final String statement =
                MessageFormat.format(
                        Statements.SAVE_MIGRATION_TEMPLATE,
                        Properties.MIGRATION_LABEL,
                        Properties.MIGRATION_FILENAME,
                        Properties.MIGRATION_VERSION,
                        Properties.MIGRATION_CHECKSUM,
                        Properties.MIGRATION_CONTENTS,
                        Properties.MIGRATION_APPLIED_AT);

        final GraphStatement graphStatement =
                new SimpleGraphStatement(
                        statement,
                        ImmutableMap.<String, Object>builder()
                                .put("vertexLabel", DATABASE_MIGRATION_LABEL)
                                .put("filename", migration.getFileName())
                                .put("version", migration.getVersion())
                                .put("checksum", migration.getCheckSum())
                                .put("contents", migration.getContents())
                                .put("appliedAt", Timestamp.from(Instant.now()))
                                .build());

        graphSchema.execute(graphStatement);
    }

    private void init() {
        if (!metadataExists()) {
            createMigrationMetadataVertex();
        }
    }

    private boolean metadataExists() {
        if (metadataExists) {
            return true;
        }

        return graphSchema.execute(Statements.METADATA_EXISTS).one().asBoolean();
    }

    private void createMigrationMetadataVertex() {
        final InputStream inputStream = getClass().getResourceAsStream(CREATE_METADATA_RESOURCE);

        try {
            final List<String> statements =
                    parser.parse(readLines(inputStream));
            statements.forEach(graphSchema::execute);
            metadataExists = true;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> readLines(final InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.toList());
        }
    }

    private static class MigrationMapper {
        private static Migration toMigration(final Vertex vertex) {
            final Migration migration =
                    new Migration(vertex.getProperty(Properties.MIGRATION_FILENAME).getValue().asString());
            migration.setVersion(vertex.getProperty(Properties.MIGRATION_VERSION).getValue().asInt());
            migration.setCheckSum(
                    vertex.getProperty(Properties.MIGRATION_CHECKSUM).getValue().asString());
            migration.setStatements(
                    Arrays.asList(
                            vertex
                                    .getProperty(Properties.MIGRATION_CONTENTS)
                                    .getValue()
                                    .asString()
                                    .split(System.lineSeparator())));
            migration.setAppliedAt(
                    vertex
                            .getProperty(Properties.MIGRATION_APPLIED_AT)
                            .getValue()
                            .as(Timestamp.class)
                            .toInstant());

            return migration;
        }
    }

    private static class Properties {
        private static final String MIGRATION_LABEL = "migrationLabel";
        private static final String MIGRATION_FILENAME = "migrationFilename";
        private static final String MIGRATION_VERSION = "migrationVersion";
        private static final String MIGRATION_CHECKSUM = "migrationChecksum";
        private static final String MIGRATION_CONTENTS = "migrationContents";
        private static final String MIGRATION_APPLIED_AT = "migrationAppliedAt";
    }

    private static class Statements {
        private static final GraphStatement METADATA_EXISTS =
                new SimpleGraphStatement(
                        String.format("schema.vertexLabel('%s').exists()", DATABASE_MIGRATION_LABEL));
        private static final String GET_MIGRATION_TEMPLATE =
                "g.V().hasLabel(''{0}'').has(''migrationLabel'', ''{1}'')";
        private static final String SAVE_MIGRATION_TEMPLATE =
                "graph.addVertex(label, vertexLabel, ''{0}'', vertexLabel,''{1}'', filename, ''{2}'', version, ''{3}''"
                        + ", checksum, ''{4}'', contents, ''{5}'', appliedAt)";
    }
}
