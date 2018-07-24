package omahoco.migrate.cli;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

public final class SingleStringOptionHandler extends OneArgumentOptionHandler<String> {
    public SingleStringOptionHandler(final CmdLineParser parser, final OptionDef option,
                                     final Setter<? super String> setter) {
        super(parser, option, setter);
    }

    @Override
    protected String parse(final String s) throws NumberFormatException {
        return s;
    }
}
