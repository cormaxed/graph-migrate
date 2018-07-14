package omahoco.migrate.graph;

import java.util.Map;

public interface Schema<T, V> {
    void create();

    void create(Map<String, String> options);

    void drop();

    void execute(String statement);

    T execute(V statement);
}
