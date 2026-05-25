package com.github.siom79.japicmp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for the MCP stdio API.
 * Spawns the server JAR as a subprocess and communicates via the MCP protocol.
 * Run after: mvn package -DskipTests
 */
class CompareJapicmpVersionsTest {

    private static final Path JAR = Path.of("target/japicmp-mcp-server-1.0-SNAPSHOT.jar");

    @Test
    void compareJapicmpVersions() {
        assumeTrue(Files.exists(JAR), "JAR must exist — run: mvn package -DskipTests");

        var transport = new StdioClientTransport(
                ServerParameters.builder("java")
                        .args("-jar", JAR.toAbsolutePath().toString())
                        .build());

        try (McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(120))
                .build()) {

            client.initialize();

            var result = client.callTool(new McpSchema.CallToolRequest(
                    "compareJavaApi",
                    Map.of(
                            "oldGroupId", "com.github.siom79.japicmp",
                            "oldArtifactId", "japicmp",
                            "oldVersion", "0.25.0",
                            "newGroupId", "com.github.siom79.japicmp",
                            "newArtifactId", "japicmp",
                            "newVersion", "0.26.0",
                            "onlyModified", false,
                            "onlyBinaryIncompatible", false
                    )));

            assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
            assertThat(result.content()).isNotEmpty();

            String text = ((McpSchema.TextContent) result.content().getFirst()).text();
            assertThat(text)
                    .contains("Compatibility Report")
                    .contains("japicmp");
        }
    }

    @Test
    void compareLocalJars() {
        assumeTrue(Files.exists(JAR), "JAR must exist — run: mvn package -DskipTests");

        Path jar1 = Path.of("target/test-jars/japicmp-0.25.0.jar");
        Path jar2 = Path.of("target/test-jars/japicmp-0.26.0.jar");

        var transport = new StdioClientTransport(
                ServerParameters.builder("java")
                        .args("-jar", JAR.toAbsolutePath().toString())
                        .build());

        try (McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(120))
                .build()) {

            client.initialize();

            var result = client.callTool(new McpSchema.CallToolRequest(
                    "compareLocalJars",
                    Map.of(
                            "oldJarPath", jar1.toAbsolutePath().toString(),
                            "newJarPath", jar2.toAbsolutePath().toString(),
                            "onlyModified", false,
                            "onlyBinaryIncompatible", false
                    )));

            assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
            String text = ((McpSchema.TextContent) result.content().getFirst()).text();
            assertThat(text).contains("Compatibility Report");
        }
    }
}
