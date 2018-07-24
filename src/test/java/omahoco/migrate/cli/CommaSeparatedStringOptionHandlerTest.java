package omahoco.migrate.cli;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.List;

import static org.junit.Assert.*;

public class CommaSeparatedStringOptionHandlerTest {
    @Test
    public void testParseCommaSeparatedOption() throws CmdLineException {
        final TestArgs testArgs = new TestArgs();
        final CmdLineParser parser = new CmdLineParser(testArgs);

        parser.parseArgument("-H","a,b,c,d");

        assertThat(testArgs.getListArgs(),IsCollectionContaining.hasItems("a","b","c","d"));
    }

    @Test
    public void testParseSpaceSeparatedOption() throws CmdLineException {
        final TestArgs testArgs = new TestArgs();
        final CmdLineParser parser = new CmdLineParser(testArgs);

        parser.parseArgument("-H","a b c d");

        assertThat(testArgs.getListArgs(),IsCollectionContaining.hasItems("a b c d"));
    }

    static final class TestArgs {
        @Option(name = "-H", handler = CommaSeparatedStringOptionHandler.class,
                usage = "Comma-separated list",
                required = true)
        List<String> listArgs;

        public List<String> getListArgs() {
            return listArgs;
        }

        public void setListArgs(List<String> listArgs) {
            this.listArgs = listArgs;
        }
    }
}
