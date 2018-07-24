package omahoco.migrate.cli;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.DelimitedOptionHandler;
import org.kohsuke.args4j.spi.Setter;

public final class CommaSeparatedStringOptionHandler extends DelimitedOptionHandler<String> {
    public CommaSeparatedStringOptionHandler(final CmdLineParser parser, final OptionDef option,
                                             final Setter<? super String> setter) {
        super(parser, option, setter, ",", new SingleStringOptionHandler(parser, option, setter));
    }
}
