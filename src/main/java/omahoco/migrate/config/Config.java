package omahoco.migrate.config;

import java.util.Map;

public final class Config {
    private String schema;
    private Map<String, Profile> profiles;
    private String migrationPath;
    private String configPath;

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public Map<String, Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(final Map<String, Profile> profiles) {
        this.profiles = profiles;
    }

    public String getMigrationPath() {
        return migrationPath;
    }

    public void setMigrationPath(final String migrationPath) {
        this.migrationPath = migrationPath;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(final String configPath) {
        this.configPath = configPath;
    }
}
