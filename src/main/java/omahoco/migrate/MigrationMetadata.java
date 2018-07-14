package omahoco.migrate;

import java.util.List;

public interface MigrationMetadata {
    List<Migration> getMigrations();

    void saveMigration(Migration migration);
}
