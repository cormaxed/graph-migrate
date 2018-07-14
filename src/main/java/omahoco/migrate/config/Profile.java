package omahoco.migrate.config;

import java.util.Map;

public final class Profile {
    private Map<String, String> options;

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(final Map<String, String> options) {
        this.options = options;
    }
}
