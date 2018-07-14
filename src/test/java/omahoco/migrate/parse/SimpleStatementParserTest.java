package omahoco.migrate.parse;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SimpleStatementParserTest {
    private static final SimpleStatementParser parser = new SimpleStatementParser();

    @Test
    public void testTrim() {
        assertThat(parser.trim("    piglet    "), is("piglet"));
        assertThat(parser.trim("\t\t    piglet    \t\t"), is("piglet"));
        assertThat(parser.trim("piglet    \t\t"), is("piglet"));
        assertThat(parser.trim("\t\t    piglet"), is("piglet"));
    }

    @Test
    public void testIgnoreComments() {
        final ArrayList lines = new ArrayList();
        lines.add("// Line comment");
        lines.add("/* Single line block comment.*/");
        lines.add("statement() // With a comment");

        final List<String> actual = parser.parse(lines);

        assertThat(actual.get(0), is("statement()"));
    }

    @Test
    public void testIgnoreCommentsMultiline() {
        final ArrayList lines = new ArrayList();
        lines.add("step1(). // Inline comment");
        lines.add("step2(). /* Inline block comment */");
        lines.add("step3(); // Inline comment");

        final List<String> actual = parser.parse(lines);

        assertThat(actual.get(0), is("step1().step2().step3();"));
    }

    @Test
    public void testIgnoreFormattingChars() {
        final ArrayList lines = new ArrayList();
        lines.add("step1().\r");
        lines.add("\tstep2().\r\n");
        lines.add("  step3();");

        final List<String> actual = parser.parse(lines);

        assertThat(actual.get(0), is("step1().step2().step3();"));
    }

    @Test
    public void testParseStatementsWithUrls() {
        final ArrayList lines = new ArrayList();
        lines.add("graph.addVertex(label,'aLabel','websiteUrl', 'https://www.google.com.au');");
        lines.add("graph.addVertex(label,'aLabel','websiteUrl', 'https://www.apple.com.');");

        final List<String> actual = parser.parse(lines);
        assertThat(actual.size(), is(2));
        assertThat(actual.get(0), is(lines.get(0)));
        assertThat(actual.get(1), is(lines.get(1)));
    }
}
