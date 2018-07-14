package omahoco.migrate.parse;

import java.util.List;

public interface StatementParser {
    List<String> parse(List<String> lines);
}
