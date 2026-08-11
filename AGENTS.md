# Repository Guidelines

## Project Structure & Module Organization

This repository combines a standalone Spring Boot backend and Vue chat UI.

- `pom.xml` aggregates the Maven modules `aichat` and `common`.
- `aichat/src/main/java/com/industry/aichat` owns the AI chat/RAG service; resources include config, prompts, and Flyway SQL under `db/migration`.
- `common/src/main/java/com/industry/kevin` holds shared result, exception, and constant classes.
- `frontend-rag-chat/src` is the Vite + Vue 3 app; API clients live in `src/api`, views in `src/views`.
- `ant-design-x-vue-main` is a standalone UI component package/reference; edit it only for component-library work.

Do not edit generated `target/`, `node_modules/`, `dist/`, or IDE metadata.

## Build, Test, and Development Commands

- `.\mvnw.cmd clean test` builds Maven modules and runs Java tests.
- `.\mvnw.cmd -pl aichat -am spring-boot:run` runs the AI chat service, normally on port `8051`.
- `cd frontend-rag-chat; npm install` installs frontend dependencies.
- `cd frontend-rag-chat; npm run dev` starts Vite on port `5173` with `/ai` proxied to `8051`.
- `cd frontend-rag-chat; npm run build` runs `vue-tsc` and creates a production build.

For `ant-design-x-vue-main`, use `pnpm test`, `pnpm build`, or `pnpm docs:dev`.

## Coding Style & Naming Conventions

Use Java 17 and Spring Boot conventions. Keep classes in existing package roots: Java classes use `PascalCase`, methods and fields use `lowerCamelCase`, constants use `UPPER_SNAKE_CASE`. Prefer service-layer logic over controller-heavy code.

Frontend code uses TypeScript, Vue 3 `<script setup>`, strict `tsconfig` checks, and the `@/*` alias. Use two-space indentation and keep HTTP clients in `src/api`.

## Testing Guidelines

Backend tests use JUnit 5 with Spring Boot Test. Put tests under `src/test/java` and name files `*Test.java`. Prefer unit tests; reserve `@SpringBootTest` for database, S3, Nacos, or full-context checks.

`frontend-rag-chat` has no test script, so `npm run build` is the minimum frontend validation.

## Commit & Pull Request Guidelines

Recent root history uses short summaries and merge commits, not strict Conventional Commits. Keep commits concise and action-oriented, optionally prefixed by module, for example `aichat: add file parsing test`.

Pull requests should include purpose, affected modules, commands run, linked issue if any, screenshots for UI changes, and notes for config or migrations.

## Security & Configuration Tips

Do not commit API keys, S3 credentials, database passwords, or private Nacos details. Keep local overrides out of version control and document Flyway assumptions in `aichat/src/main/resources/db/migration`.
