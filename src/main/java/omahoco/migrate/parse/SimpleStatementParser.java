package omahoco.migrate.parse;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple parser to process graph statements. The parser requires all statements to be terminated with a semicolon.
 * It can handle multiline indented statements, comment lines and inline comments.
 * For simplicity multiline comment blocks are not supported.
 */
public final class SimpleStatementParser implements StatementParser {
    private static final Pattern LINE_COMMENT = Pattern.compile("^(//|\\/\\*.+?\\*\\/)");
    private static final Pattern LEFT_TRIM = Pattern.compile("^(\\s|\\t)+");
    private static final Pattern RIGHT_TRIM = Pattern.compile("(\\s|\\t)+$");
    private static final Pattern STATEMENT = Pattern.compile("^(.*?)(:?\\s\\/\\/.*|\\s\\/\\*.*\\*\\/)?$");
    private static final String STATEMENT_TERMINATOR = ";";

    @Override
    public List<String> parse(final List<String> lines) {
        final ArrayList<String> statements = new ArrayList<>();
        StringBuffer currentStatement = new StringBuffer();

        for (final String line : lines) {
            if (LINE_COMMENT.matcher(line).find()) {
                continue;
            }

            final Matcher m = STATEMENT.matcher(line);
            if (m.find()) {
                final String statement = trim(m.group(1));
                currentStatement.append(statement);

                if (statement.endsWith(STATEMENT_TERMINATOR)) {
                    statements.add(
                            currentStatement.toString());
                    currentStatement = new StringBuffer();
                }
            }
        }

        if (currentStatement.length() > 0) {
            statements.add(currentStatement.toString());
        }

        return statements;
    }

    @VisibleForTesting
    String trim(final String trim) {
        return RIGHT_TRIM.matcher(LEFT_TRIM.matcher(trim).replaceAll(""))
                .replaceAll("");
    }
}
