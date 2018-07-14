package omahoco.migrate.io;

import omahoco.migrate.Migration;
import omahoco.migrate.parse.StatementParser;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MigrationFileLoader {
    private static final Pattern VERSION_FILE_PATTERN =
            Pattern.compile("^v(\\d{3})_.*.(gremlin|groovy)$", Pattern.CASE_INSENSITIVE);
    private final StatementParser parser;

    public MigrationFileLoader(final StatementParser parser) {
        this.parser = Objects.requireNonNull(parser);
    }

    public Migration load(final File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + file.getPath());
        }

        try {
            final Matcher m = VERSION_FILE_PATTERN.matcher(file.getName());

            if (!m.find()) {
                throw new IllegalArgumentException(
                        "Invalid filename, name must have the form: "
                                + "v{version_number:03d}_{description}.{extension:(groovy|gremlin)} "
                                + "e.g v001_migration.gremlin");
            }

            final Migration migration = new Migration(file.getName());
            migration.setVersion(Integer.valueOf(m.group(1)));

            final HashCode hashCode = Files.asByteSource(file).hash(Hashing.crc32());
            migration.setCheckSum(hashCode.toString());

            final List<String> lines = Files.readLines(file, Charset.defaultCharset());
            migration.setStatements(parser.parse(lines));

            return migration;
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
