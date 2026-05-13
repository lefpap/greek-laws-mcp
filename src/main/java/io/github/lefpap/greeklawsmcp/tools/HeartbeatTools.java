package io.github.lefpap.greeklawsmcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class HeartbeatTools {

    @McpTool(
        name = "heartbeat",
        description = "A simple tool to check if the system is alive and responsive."
    )
    public HeartbeatResponse beat() {
        return new HeartbeatResponse("Beating..", Instant.now());
    }

    public record HeartbeatResponse(
        String message,
        Instant timestamp
    ) {    }

}
