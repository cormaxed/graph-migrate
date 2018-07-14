package omahoco.migrate.graph;

import omahoco.migrate.util.Delayer;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.graph.GraphResultSet;
import com.datastax.driver.dse.graph.SimpleGraphStatement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GraphSchemaTest {
    @Mock
    private DseSession dseSession;
    @Mock
    private DseCluster dseCluster;
    @Mock
    private GraphResultSet graphResultSet;
    @Mock
    private ExecutionInfo executionInfo;
    @Mock
    private Metadata metadata;
    @Mock
    private Delayer delayer;
    private GraphSchema graphSchema;

    private final GraphSchema.SchemaInfo schemaInfo = new GraphSchema.SchemaInfo("killrvideo");

    @Before
    public void before() {
        when(graphResultSet.getExecutionInfo()).thenReturn(executionInfo);
        when(dseSession.executeGraph(anyString())).thenReturn(graphResultSet);
        when(dseSession.getCluster()).thenReturn(dseCluster);
        when(dseCluster.getMetadata()).thenReturn(metadata);

        graphSchema = new GraphSchema(delayer, schemaInfo, dseSession);
    }

    @Test
    public void testCreateSchema() {
        when(graphResultSet.getExecutionInfo().isSchemaInAgreement()).thenReturn(true);

        graphSchema.create();

        verify(dseSession).executeGraph(eq("system.graph('killrvideo').ifNotExists().create()"));
        verify(metadata, never()).checkSchemaAgreement();
    }

    @Test
    public void testDropSchema() {
        when(graphResultSet.getExecutionInfo().isSchemaInAgreement()).thenReturn(true);

        graphSchema.drop();

        verify(dseSession).executeGraph(eq("system.graph('killrvideo').drop()"));
        verify(metadata, never()).checkSchemaAgreement();
    }

    @Test
    public void testCreateSchemaWithOptions() {
        final Map<String, String> options = new HashMap<>();
        options.put(
                "graph.replication_config", "{'class' : 'SimpleStrategy', 'replication_factor' : 1 }");
        options.put("graph.allow_scan", "false");

        when(graphResultSet.getExecutionInfo().isSchemaInAgreement()).thenReturn(true);

        graphSchema.create(options);

        verify(dseSession)
                .executeGraph(
                        eq("system.graph('killrvideo')"
                                + ".option(\"graph.replication_config\").set(\"{'class' : 'SimpleStrategy'," +
                                " 'replication_factor' : 1 }\")"
                                + ".option(\"graph.allow_scan\").set(\"false\").ifNotExists().create()"));
        verify(metadata, never()).checkSchemaAgreement();
    }

    @Test
    public void testIsSchemaChange() {
        Assert.assertThat(GraphSchema.isSchemaChange(
                new SimpleGraphStatement("")), is(false));
        Assert.assertThat(GraphSchema.isSchemaChange(
                new SimpleGraphStatement("schema.propertyKey('propertyName')")), is(true));
        Assert.assertThat(GraphSchema.isSchemaChange(
                new SimpleGraphStatement("schema.vertexLabel('label')")), is(true));
        Assert.assertThat(GraphSchema.isSchemaChange(
                new SimpleGraphStatement("schema.edgeLabel('label')")), is(true));
        Assert.assertThat(GraphSchema.isSchemaChange(
                new SimpleGraphStatement("graph.addVertex()")), is(false));
        Assert.assertThat(GraphSchema.isSchemaChange(
                new SimpleGraphStatement("g.V().hasLabel()")), is(false));
    }

    @Test
    public void testWaitForSchemaAgreement() {
        when(graphResultSet.getExecutionInfo().isSchemaInAgreement()).thenReturn(false);
        when(metadata.checkSchemaAgreement()).thenReturn(false).thenReturn(false).thenReturn(true);

        graphSchema.create();

        verify(delayer, times(2)).delay(anyInt());
    }

    @Test(expected = IllegalStateException.class)
    public void testSchemaAgreementFailed() {
        when(graphResultSet.getExecutionInfo().isSchemaInAgreement()).thenReturn(false);
        when(metadata.checkSchemaAgreement()).thenReturn(false);

        graphSchema.create();
    }
}
