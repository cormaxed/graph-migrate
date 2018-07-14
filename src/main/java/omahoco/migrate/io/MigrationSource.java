package omahoco.migrate.io;

import java.io.File;
import java.util.List;

public interface MigrationSource {
    List<File> getMigrations();
}
