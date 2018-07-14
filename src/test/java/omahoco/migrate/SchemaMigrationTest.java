package omahoco.migrate;

import omahoco.migrate.config.Config;
import omahoco.migrate.config.Profile;
import omahoco.migrate.graph.Schema;
import omahoco.migrate.io.MigrationSource;
import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.graph.GraphResultSet;
import com.datastax.driver.dse.graph.GraphStatement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SchemaMigrationTest {
    @Mock
    private DseSession dseSession;
    @Mock
    private Schema<GraphResultSet, GraphStatement> schema;
    @Mock
    private MigrationSource migrationSource;
    @Mock
    private MigrationMetadata migrationMetadata;
    @Mock
    private DseCluster dseCluster;

    private final Config config = new Config();
    private SchemaMigration schemaMigration;

    @Before
    public void before() {
        when(dseSession.getCluster()).thenReturn(dseCluster);

        config.setSchema("testSchema");
        schemaMigration = new SchemaMigration(dseSession, new Profile(), schema,
                migrationMetadata, migrationSource);
    }

    @Test
    public void testMigrateAllNoExistingMigrations() {
        when(migrationSource.getMigrations()).thenReturn(getTestMigrations());

        schemaMigration.migrate();

        verify(migrationSource).getMigrations();
        verify(migrationMetadata).getMigrations();
        // 13 + 4 + 8 statements expected
        verify(schema, times(25)).execute(anyString());
        verify(migrationMetadata, times(3)).saveMigration(any());
    }

    @Test
    public void testMigrateUpToVersion() {
        when(migrationSource.getMigrations()).thenReturn(getTestMigrations());
        when(migrationMetadata.getMigrations()).thenReturn(Collections.emptyList());

        schemaMigration.setMaxVersion(2);
        schemaMigration.migrate();

        verify(migrationSource).getMigrations();
        verify(migrationMetadata).getMigrations();
        // Only v001 and v002 file should be applied
        // 13 + 4 statements expected
        verify(schema, times(17)).execute(anyString());
        verify(migrationMetadata, times(2)).saveMigration(any());
    }

    @Test
    public void testMigrateAllMigrationsAlreadyApplied() {
        when(migrationSource.getMigrations()).thenReturn(getTestMigrations());
        when(migrationMetadata.getMigrations()).thenReturn(getAppliedMigrations());

        schemaMigration.migrate();

        verify(migrationSource).getMigrations();
        verify(migrationMetadata).getMigrations();
        verify(schema, never()).execute(anyString());
        verify(migrationMetadata, never()).saveMigration(any());
    }

    @Test
    public void testMigrateOneNotAlreadyApplied() {
        when(migrationSource.getMigrations()).thenReturn(getTestMigrations());
        when(migrationMetadata.getMigrations()).thenReturn(getAppliedMigrations().subList(0, 2));

        schemaMigration.migrate();

        verify(migrationSource).getMigrations();
        verify(migrationMetadata).getMigrations();
        // Only the third migration file should be applied
        verify(schema, times(8)).execute(anyString());
        verify(migrationMetadata).saveMigration(any());
    }

    @Test(expected = IllegalStateException.class)
    public void testMigrateChecksumDisagreement() {
        when(migrationSource.getMigrations()).thenReturn(getTestMigrations());

        final List<Migration> incorrectChecksums = getAppliedMigrations().stream()
                .map(t -> new Migration(t.getFileName(), t.getCheckSum() + "1")).collect(Collectors.toList());

        when(migrationMetadata.getMigrations()).thenReturn(incorrectChecksums);

        schemaMigration.migrate();
    }

    private List<File> getTestMigrations() {
        return Arrays.asList(
                // Contains 13 statements
                new File(SchemaMigrationTest.class.getResource("v001_killrvideo_properties.gremlin").getFile()),
                // Contains 4 statements
                new File(SchemaMigrationTest.class.getResource("v002_killrvideo_vertex_label.gremlin").getFile()),
                // Contains 8 statements
                new File(SchemaMigrationTest.class.getResource("v003_killrvideo_edge_labels.gremlin").getFile()));
    }

    private List<Migration> getAppliedMigrations() {
        final ArrayList<Migration> appliedMigrations = new ArrayList<>();

        return Arrays.asList(
                new Migration("v001_killrvideo_properties.gremlin", "725b63a6"),
                new Migration("v002_killrvideo_vertex_label.gremlin", "051a0c30"),
                new Migration("v003_killrvideo_edge_labels.gremlin", "bde508de")
        );
    }
}
