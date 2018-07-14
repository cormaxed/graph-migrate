package omahoco.migrate.config;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigTest {
    @Test
    public void testParseConfig() {
        final Config config = YamlConfig.load(ConfigTest.class.getResourceAsStream("graph-migrate.yaml"));

        assertThat(config.getSchema(), is("killrvideo"));
        assertThat(config.getMigrationPath(), is("../"));
        assertThat(config.getProfiles().keySet(), hasItems("local", "prod"));

        final Profile profile = config.getProfiles().get("local");

        assertThat(profile.getOptions().get("graph.replication_config"),
                is("{'class' : 'SimpleStrategy', 'replication_factor' : 1 }"));
        assertThat(profile.getOptions().get("graph.allow_scan"), is("true"));
    }
}
