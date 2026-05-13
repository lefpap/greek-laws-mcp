# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Spring Boot MCP (Model Context Protocol) server that exposes the Hellenic Parliament open-data API. Goal: let MCP clients search Greek parliamentary law metadata, resolve law references, and list ministries/categories without dealing with raw API parameters, GUIDs, or date formatting. Metadata-only — no legal advice, no full-text legal search.

See `docs/greek-laws-mcp-prd.md` for the full PRD, planned tool surface (`list_ministries`, `list_categories`, `search_laws`, `get_law_by_number`, `latest_laws`, `resolve_law_reference`, `resolve_ministry`), and the ordered implementation backlog. The current code is bootstrap-only: a single `heartbeat` tool plus the Spring Boot entrypoint.

## Stack

- **Java 25** (set in `pom.xml`; note the PRD mentions Java 21 — the POM is authoritative).
- **Spring Boot 4.0.6** + **Spring AI 2.0.0-M6** (`spring-ai-starter-mcp-server`).
- Lombok, Spring Boot Actuator, RestClient, Validation.
- Maven wrapper (`mvnw` / `mvnw.cmd`).

## Common commands

On Windows, use `mvnw.cmd`; on Unix, `./mvnw`.

```bash
./mvnw clean package              # build + run tests
./mvnw spring-boot:run            # run the MCP server (STDIO transport)
./mvnw test                       # run all tests
./mvnw test -Dtest=ClassName      # run a single test class
./mvnw test -Dtest=ClassName#method  # run a single test method
```

## Architecture notes

- **MCP transport is STDIO** (`spring.ai.mcp.server.stdio: true` in `application.yaml`). The server is a `SYNC` MCP server registered as `greek-laws-mcp`. Because STDIO is used, the application **must not write non-MCP output to stdout** — keep logs on stderr / files.
- **MCP tools are plain Spring `@Component` classes** with `@McpTool`-annotated methods. See `src/main/java/io/github/lefpap/greeklawsmcp/tools/HeartbeatTools.java` for the canonical shape: a public method returns a record, and Spring AI auto-registers it by the `name` attribute.
- **Planned layering** (per PRD §6.3): MCP tool layer → `LawService` / `TaxonomyService` → `ParliamentApiClient` → Hellenic Parliament Open Data API. Suggested package split: `config`, `mcp`, `parliament`, `laws`, `taxonomy`, `normalize`, `error` under `io.github.lefpap.greeklawsmcp`.
- **Data normalization is a hard requirement**: raw upstream DTOs stay internal; MCP responses use the normalized shape defined in PRD §4.2 (snake_case fields, embedded `source` object with name/query/url). Keep raw and normalized DTOs in separate types so upstream schema drift only touches the raw layer.
- **Taxonomies (ministries, categories) must be cached** (PRD §5.2). The upstream uses GUID-based ministry filters — `resolve_ministry` is the user-facing layer that maps free-text and Greek synonyms to those GUIDs.
- **Error model** (PRD §6.4): tools return structured `{ error: { code, message, retryable } }` with codes `INVALID_INPUT`, `UPSTREAM_TIMEOUT`, `UPSTREAM_ERROR`, `NO_RESULTS`, `NORMALIZATION_ERROR`, `INTERNAL_ERROR`. Never leak stack traces to MCP clients.

## Conventions

- `groupId` is `io.github.lefpap`; root package `io.github.lefpap.greeklawsmcp` (the PRD's older `com.lefpap.greeklawsmcp` example is out of date — match the existing code).
- MCP tool names use `snake_case` (e.g. `heartbeat`, `list_ministries`); Java methods can be camelCase — the `name` attribute on `@McpTool` is what clients see.
- Response DTOs are typically `record`s nested inside the tool class when small (see `HeartbeatResponse`).
