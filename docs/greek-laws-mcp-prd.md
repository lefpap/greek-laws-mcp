# Product Requirements Document: Greek Parliament Laws MCP Server

## 1. Product Overview

### Product Name

**Greek Parliament Laws MCP Server**

### Summary

The Greek Parliament Laws MCP Server is a Dockerized Spring Boot application that exposes the Hellenic Parliament open-data API through the Model Context Protocol (MCP). It allows MCP-compatible clients and AI assistants to search Greek parliamentary law metadata, resolve law references, list ministries and law categories, and retrieve recent completed laws in a structured, LLM-friendly format.

The project focuses on providing a clean developer-facing MCP interface over the existing public API, hiding source-specific details such as GUID-based ministry filters, pagination behavior, date formatting, and raw response structures.

### Problem Statement

The Hellenic Parliament open-data API provides useful public metadata about laws, ministries, and law categories, but it is not optimized for natural-language interaction or MCP-based workflows. Users need to understand API parameters, ministry identifiers, date formats, and raw response fields before they can retrieve useful results.

AI assistants and developer tools need a structured, safe, source-linked MCP server that can translate user intent into reliable API calls and return normalized results.

### Goals

The project should:

- Provide an MCP server for Greek Parliament law metadata.
- Run easily through Docker.
- Expose useful MCP tools for law search, taxonomy lookup, and reference resolution.
- Normalize API responses into predictable structured objects.
- Include clear documentation and examples for other developers.
- Avoid making legal-advice claims or pretending to provide complete legal text.

### Non-Goals

The initial version will not:

- Provide legal advice.
- Guarantee legal completeness or current legal force.
- Perform full-text legal search.
- Scrape FEK or external legal databases.
- Parse PDFs.
- Provide a web frontend.
- Include embeddings, semantic search, or vector storage.
- Require user accounts or authentication.
- Store user queries.

---

## 2. Target Users

### Primary Users

#### Developers

Developers who want to connect an MCP-compatible client to Greek parliamentary law metadata.

Example needs:

- Run the server locally with Docker.
- Use it from an MCP client.
- Inspect and test exposed tools.
- Extend the project with additional sources later.

#### AI Assistant Users

Users working inside MCP-compatible environments who want to ask questions such as:

- “Find laws about taxation passed in 2024.”
- “What is Ν. 4412/2016?”
- “List recent laws from the Ministry of Finance.”
- “Show available law categories.”

### Secondary Users

#### Researchers and Journalists

People who need a simple way to discover recently voted laws or search public parliamentary metadata by title, date, ministry, or category.

#### Civic-Tech Contributors

Developers interested in Greek public-data tooling who may want to contribute improvements.

---

## 3. Product Scope

### Version 0.1.0 MVP

The first public release should expose a small, reliable set of MCP tools and resources.

#### MCP Tools

The server must expose the following tools:

1. `list_ministries`
2. `list_categories`
3. `search_laws`
4. `get_law_by_number`
5. `latest_laws`
6. `resolve_law_reference`
7. `resolve_ministry`

#### MCP Resources

The server should expose:

1. `parliament://taxonomies/ministries`
2. `parliament://taxonomies/categories`
3. `parliament://schemas/law-record`

#### Runtime and Distribution

The server must:

- Be implemented with Spring Boot.
- Use Spring AI MCP server support where appropriate.
- Run locally through Docker.
- Include a Dockerfile.
- Include a `docker-compose.yml` for local development.
- Include GitHub Actions CI.
- Publish a Docker image to GitHub Container Registry or equivalent.
- Provide clear README setup instructions.

---

## 4. Functional Requirements

### 4.1 Parliament API Client

The application must include a client for the Hellenic Parliament open-data API.

The client should support:

- Fetching ministries.
- Fetching law categories.
- Searching completed laws.
- Passing supported filters to the upstream API.
- Handling upstream timeouts.
- Handling empty responses.
- Handling upstream errors gracefully.

Configuration should include:

```yaml
parliament:
  base-url: https://www.hellenicparliament.gr
  timeout-seconds: 10
  cache:
    taxonomy-ttl-hours: 24
```

### 4.2 Data Normalization

The server must normalize raw API data before returning it from MCP tools.

Normalized law records should follow this shape:

```json
{
  "law_number": 5083,
  "title": "...",
  "ministry": {
    "id": "...",
    "name": "..."
  },
  "category": {
    "id": "...",
    "name": "..."
  },
  "date_inserted": "2024-01-15",
  "date_voted": "2024-01-25",
  "source": {
    "name": "Hellenic Parliament Open Data",
    "query": "...",
    "url": "..."
  }
}
```

Raw upstream fields may be retained internally, but the default MCP response should prioritize normalized fields.

### 4.3 `list_ministries`

Returns the available ministries from the Parliament API.

#### Input

No required input.

Optional input:

```json
{
  "refresh": false
}
```

#### Output

```json
{
  "items": [
    {
      "id": "...",
      "name": "..."
    }
  ],
  "source": {
    "name": "Hellenic Parliament Open Data",
    "url": "..."
  }
}
```

#### Acceptance Criteria

- Returns a structured list of ministries.
- Uses cache by default.
- Supports cache refresh if implemented.
- Does not expose stack traces on failure.

### 4.4 `list_categories`

Returns the available law categories from the Parliament API.

#### Input

No required input.

Optional input:

```json
{
  "refresh": false
}
```

#### Output

```json
{
  "items": [
    {
      "id": "...",
      "name": "..."
    }
  ],
  "source": {
    "name": "Hellenic Parliament Open Data",
    "url": "..."
  }
}
```

#### Acceptance Criteria

- Returns a structured list of law categories.
- Uses cache by default.
- Supports cache refresh if implemented.
- Does not expose stack traces on failure.

### 4.5 `search_laws`

Searches completed law records using structured filters.

#### Input

```json
{
  "title": "string optional",
  "law_number": "integer optional",
  "ministry_id": "string optional",
  "category_id": "string optional",
  "date_inserted": "yyyy-MM-dd optional",
  "date_voted": "yyyy-MM-dd optional",
  "page": 1,
  "page_size": 10
}
```

#### Output

```json
{
  "items": [
    {
      "law_number": 5083,
      "title": "...",
      "ministry": {
        "id": "...",
        "name": "..."
      },
      "category": {
        "id": "...",
        "name": "..."
      },
      "date_inserted": "2024-01-15",
      "date_voted": "2024-01-25",
      "source": {
        "name": "Hellenic Parliament Open Data",
        "query": "...",
        "url": "..."
      }
    }
  ],
  "page": 1,
  "page_size": 10,
  "has_more": true
}
```

#### Acceptance Criteria

- Supports all MVP filters.
- Validates page and page size.
- Enforces a maximum page size.
- Returns empty results clearly when no records match.
- Includes source/query metadata.
- Handles upstream errors gracefully.

### 4.6 `get_law_by_number`

Retrieves law records matching a law number.

#### Input

```json
{
  "law_number": 4412,
  "year": 2016
}
```

The `year` field is optional and may be used to narrow or validate results.

#### Output

```json
{
  "match_type": "single | multiple | none",
  "items": [
    {
      "law_number": 4412,
      "title": "...",
      "date_voted": "2016-08-08",
      "source": {
        "name": "Hellenic Parliament Open Data",
        "query": "...",
        "url": "..."
      }
    }
  ]
}
```

#### Acceptance Criteria

- Searches by law number.
- Returns a clear status when there is no match.
- Returns candidates when multiple records are found.
- Does not hallucinate details that are not present in the API.

### 4.7 `latest_laws`

Returns recently voted laws.

#### Input

```json
{
  "from_date": "yyyy-MM-dd optional",
  "to_date": "yyyy-MM-dd optional",
  "ministry_id": "string optional",
  "category_id": "string optional",
  "limit": 10
}
```

#### Output

```json
{
  "items": [
    {
      "law_number": 5083,
      "title": "...",
      "date_voted": "2024-01-25",
      "source": {
        "name": "Hellenic Parliament Open Data",
        "query": "...",
        "url": "..."
      }
    }
  ],
  "filters_used": {
    "from_date": "...",
    "to_date": "...",
    "ministry_id": "...",
    "category_id": "..."
  }
}
```

#### Acceptance Criteria

- Returns recently voted laws.
- Supports date range filters.
- Enforces a maximum limit.
- Sorts results by voted date descending where possible.
- Includes filters used in the response.

### 4.8 `resolve_law_reference`

Parses messy law references into structured values.

#### Example Inputs

- `Ν. 4412/2016`
- `ν 4412`
- `Law 4412`
- `Νόμος 5083/2024`
- `n. 5027/2023`

#### Input

```json
{
  "reference": "Ν. 4412/2016"
}
```

#### Output

```json
{
  "input": "Ν. 4412/2016",
  "law_number": 4412,
  "year": 2016,
  "confidence": "high",
  "notes": []
}
```

#### Acceptance Criteria

- Handles common Greek and English law-reference formats.
- Extracts law number.
- Extracts year when present.
- Returns confidence.
- Returns clear failure when no law number can be parsed.

### 4.9 `resolve_ministry`

Maps free-text ministry input to the closest known ministry.

#### Example Inputs

- `Υπουργείο Οικονομικών`
- `Οικονομικών`
- `Finance ministry`
- `minfin`

#### Input

```json
{
  "query": "Finance ministry",
  "limit": 5
}
```

#### Output

```json
{
  "input": "Finance ministry",
  "matches": [
    {
      "id": "...",
      "name": "...",
      "confidence": "high"
    }
  ]
}
```

#### Acceptance Criteria

- Uses cached ministry taxonomy.
- Performs accent-insensitive matching for Greek.
- Supports a small synonym map.
- Returns multiple candidates if confidence is low.
- Does not guess a single ministry when ambiguous.

---

## 5. Non-Functional Requirements

### 5.1 Reliability

The server should:

- Use timeouts for all upstream HTTP calls.
- Return meaningful MCP errors.
- Avoid leaking stack traces.
- Continue running if taxonomy fetch fails at startup, unless strict mode is enabled.

### 5.2 Performance

The server should:

- Cache ministries and categories.
- Avoid repeated upstream calls for static taxonomies.
- Enforce maximum page size and result limits.

Suggested limits:

```yaml
mcp:
  limits:
    max-page-size: 50
    max-latest-limit: 50
```

### 5.3 Security

The server should:

- Validate all tool inputs.
- Avoid accepting arbitrary URLs.
- Use a fixed configured upstream base URL.
- Avoid logging sensitive MCP client payloads unnecessarily.
- Sanitize error messages returned to clients.

### 5.4 Observability

The server should include:

- Structured logs.
- Startup logs showing active transport/profile.
- Upstream API timing logs.
- Clear warnings when upstream responses cannot be normalized.

### 5.5 Portability

The server should:

- Run with Docker.
- Require no local database for v0.1.0.
- Require no external credentials.
- Work on Linux, macOS, and Windows through Docker.

---

## 6. Technical Design

### 6.1 Proposed Stack

- Java 21
- Spring Boot 3.x
- Spring AI MCP Server
- Maven or Gradle
- Jackson
- Caffeine or Spring Cache
- JUnit 5
- WireMock or MockWebServer
- Docker
- GitHub Actions

### 6.2 Package Structure

```text
com.lefpap.greeklawsmcp
  config
  mcp
  parliament
  laws
  taxonomy
  normalize
  error
```

### 6.3 Layered Architecture

```text
MCP Tool Layer
  ↓
LawService / TaxonomyService
  ↓
ParliamentApiClient
  ↓
Hellenic Parliament Open Data API
```

### 6.4 Error Model

Errors should be returned in a predictable structure:

```json
{
  "error": {
    "code": "UPSTREAM_TIMEOUT",
    "message": "The Hellenic Parliament API did not respond in time.",
    "retryable": true
  }
}
```

Suggested error codes:

```text
INVALID_INPUT
UPSTREAM_TIMEOUT
UPSTREAM_ERROR
NO_RESULTS
NORMALIZATION_ERROR
INTERNAL_ERROR
```

---

## 7. Docker Requirements

### 7.1 Dockerfile

The project must include a production-ready Dockerfile.

Requirements:

- Uses a slim JRE runtime image.
- Does not run as root if reasonably practical.
- Exposes only necessary ports for HTTP mode.
- Supports STDIO mode for local MCP clients.
- Supports configuration through environment variables.

### 7.2 Docker Compose

The project should include a `docker-compose.yml` for local development.

Example usage:

```bash
docker compose up --build
```

### 7.3 Published Image

The project should publish an image such as:

```text
ghcr.io/<owner>/greek-laws-mcp:latest
ghcr.io/<owner>/greek-laws-mcp:v0.1.0
```

---

## 8. Documentation Requirements

The README must include:

- Project description.
- What the server can do.
- What the server cannot do.
- Requirements.
- Local run instructions.
- Docker run instructions.
- MCP client configuration example.
- Tool reference.
- Resource reference.
- Example prompts/questions.
- Known limitations.
- Legal/data disclaimer.
- Contribution instructions.

### Example User Questions

```text
Find laws about taxation passed in 2024.

List available Greek Parliament law categories.

What laws were recently voted by the Ministry of Finance?

Resolve this law reference: Ν. 4412/2016.

Search laws with title containing "ενέργεια".
```

### Required Disclaimer

The README and tool descriptions should include a disclaimer:

```text
This MCP server retrieves public parliamentary metadata. It does not provide legal advice and may not include the full text, amendments, codification status, or later legal effects of a law.
```

---

## 9. Milestones

### Milestone 1: Project Bootstrap

Estimated effort: 8–12 hours

Deliverables:

- Spring Boot project created.
- MCP server dependency added.
- Basic server starts.
- Dockerfile added.
- App runs in Docker.

### Milestone 2: Parliament API Integration

Estimated effort: 12–18 hours

Deliverables:

- API client implemented.
- Ministries can be fetched.
- Categories can be fetched.
- Laws can be searched.
- DTOs and normalization implemented.
- API client tests added.

### Milestone 3: Core MCP Tools

Estimated effort: 16–24 hours

Deliverables:

- `list_ministries`
- `list_categories`
- `search_laws`
- `get_law_by_number`
- `latest_laws`
- Validation and friendly errors.

### Milestone 4: LLM-Friendly Helpers

Estimated effort: 10–16 hours

Deliverables:

- `resolve_law_reference`
- `resolve_ministry`
- Taxonomy caching.
- MCP resources.

### Milestone 5: Release Readiness

Estimated effort: 10–16 hours

Deliverables:

- Integration tests.
- README.
- Example MCP config.
- GitHub Actions CI.
- Docker image publishing.
- v0.1.0 release.

---

## 10. Ordered Implementation Backlog

### Phase 1: Foundation

1. Create GitHub repository.
2. Bootstrap Spring Boot project.
3. Add Spring AI MCP server dependency.
4. Configure initial STDIO transport.
5. Add `application.yml`.
6. Add Dockerfile.
7. Add `docker-compose.yml`.
8. Add basic startup test.

### Phase 2: API Client

9. Implement `ParliamentApiClient`.
10. Add raw response DTOs.
11. Add normalized domain DTOs.
12. Add date normalization helpers.
13. Add query-building helpers.
14. Add mocked API client tests.

### Phase 3: First MCP Tools

15. Implement `list_ministries`.
16. Implement `list_categories`.
17. Test tools with an MCP client or inspector.
18. Implement `search_laws`.
19. Add input validation and page-size limits.

### Phase 4: Useful Law Workflows

20. Implement `get_law_by_number`.
21. Implement `latest_laws`.
22. Add source/query metadata to all law results.
23. Add friendly error handling.
24. Add taxonomy caching.

### Phase 5: Natural-Language Helpers

25. Implement `resolve_law_reference`.
26. Implement `resolve_ministry`.
27. Add ministry synonym map.
28. Add accent-insensitive Greek matching.
29. Add MCP resources for taxonomies.
30. Add law-record schema resource.

### Phase 6: Release

31. Add integration/smoke tests.
32. Add GitHub Actions test workflow.
33. Add Docker publish workflow.
34. Write README.
35. Add example MCP client configuration.
36. Add limitations and legal disclaimer.
37. Run clean-machine Docker test.
38. Tag `v0.1.0`.

---

## 11. Acceptance Criteria for v0.1.0

The release is complete when:

- A developer can clone the repo and run tests.
- A developer can build the Docker image locally.
- A developer can run the MCP server through Docker.
- An MCP client can list available tools.
- `list_ministries` returns structured data.
- `list_categories` returns structured data.
- `search_laws` returns normalized law records.
- `get_law_by_number` returns matches or clear no-result output.
- `latest_laws` returns recent completed laws.
- `resolve_law_reference` parses common Greek law references.
- `resolve_ministry` returns likely ministry matches.
- Tool outputs include source/query information.
- Bad inputs return friendly errors.
- README includes setup, examples, and limitations.
- A Docker image is published.
- A `v0.1.0` release is tagged.

---

## 12. Risks and Mitigations

### Risk: Upstream API shape changes

Mitigation:

- Keep raw DTOs isolated.
- Add normalization tests.
- Return graceful upstream errors.

### Risk: Source API lacks full legal text

Mitigation:

- Clearly document that v0.1.0 provides metadata search only.
- Avoid claims about full legal interpretation.

### Risk: Ministry/category identifiers are hard to use

Mitigation:

- Add `resolve_ministry`.
- Cache taxonomies.
- Include names in normalized results.

### Risk: Ambiguous law references

Mitigation:

- Return confidence.
- Return candidates instead of guessing.
- Include notes when year is missing.

### Risk: MCP transport confusion

Mitigation:

- Start with one supported transport.
- Document exactly how to run it.
- Add HTTP profile later if needed.

---

## 13. Future Enhancements

Potential post-MVP features:

- Streamable HTTP transport profile.
- Local SQLite cache of law records.
- Scheduled ingestion.
- Full-text search if a reliable source is added.
- FEK linking.
- PDF ingestion.
- Semantic search with embeddings.
- Topic tracking.
- Change detection and notifications.
- Greek/English translation helpers.
- Better ministry alias database.
- More MCP prompts for research workflows.
- Additional Greek legal/public-data sources.

---

## 14. Open Questions

Before implementation, decide:

1. Should v0.1.0 support only STDIO, or also HTTP?
2. Should the Docker image default to STDIO or HTTP mode?
3. Which Java version should be required?
4. Should the project use Maven or Gradle?
5. Should raw upstream responses be optionally returned for debugging?
6. Should taxonomy cache refresh happen on startup?
7. What should the maximum `page_size` be?
8. Should the project name use `greek-laws-mcp` or `hellenic-parliament-laws-mcp`?

---

## 15. Recommended v0.1.0 Positioning

Recommended project description:

```text
A Dockerized Spring Boot MCP server for searching public Greek Parliament law metadata through the Hellenic Parliament Open Data API.
```

Recommended short README tagline:

```text
Search Greek Parliament law metadata from MCP clients without dealing with raw API parameters, ministry GUIDs, or date formatting.
```
