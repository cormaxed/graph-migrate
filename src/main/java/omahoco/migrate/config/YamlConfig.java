package omahoco.migrate.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

public final class YamlConfig {
    private YamlConfig() {
    }

    public static Config load(final InputStream is) {
        final Constructor constructor = new Constructor(Config.class);
        final Yaml yaml = new Yaml(constructor);

        return yaml.load(is);
    }
}
