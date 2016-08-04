package net.nemerosa.ontrack.neo4j.migration;

import java.util.Map;
import java.util.stream.Collectors;

public final class CypherUtils {

    public static String getCypherParameters(Map<String, ?> map) {
        return map.keySet().stream()
                .map(key -> String.format("%1$s: {%1$s}", key))
                .collect(Collectors.joining(", "));
    }
}
