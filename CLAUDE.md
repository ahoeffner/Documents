# Document Handler

Spring Boot + Vue 3 TypeScript document management system with RAG/AI chat.

## Structure

- `Backend/` — Spring Boot Java backend (JdbcTemplate, PostgreSQL/pgvector, Gemini API)
- `Frontend/` — Vue 3 TypeScript SPA (Pinia, Vite)

## Coding style

Before editing any Java file, read [.claude/coding-style.md](.claude/coding-style.md).
Before editing any TypeScript/Vue file, read [.claude/coding-style-frontend.md](.claude/coding-style-frontend.md).

Key rules (both):
- Allman braces (opening brace on its own line)
- `return(value)` — always parentheses
- Imports sorted by line length, shortest first, no grouping
- Two blank lines between methods/functions and after fields
