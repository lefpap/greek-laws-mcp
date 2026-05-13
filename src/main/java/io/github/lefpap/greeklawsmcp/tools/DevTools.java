package io.github.lefpap.greeklawsmcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class DevTools {

    @McpTool(
        name = "ping",
        description = "A simple tool to check if the system is responsive. It returns a 'pong' message along with the current timestamp."
    )
    public Map<String, Object> ping() {
        return Map.of(
            "message", "pong",
            "timestamp", Instant.now().toString()
        );
    }
}
