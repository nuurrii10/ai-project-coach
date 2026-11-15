# AI Project Coach

Team 11 – SE project @ FH Technikum Wien

Basically a Jira-lite for student teams with some AI on top. You create projects, write user stories, plan sprints, and Gemini helps you turn vague requirements into proper user stories (with Given-When-Then acceptance criteria and all that).

## Tech Stack

**Backend**
- Java 21 + Spring Boot 4 (Web MVC, Data JPA, Validation)
- PostgreSQL 17 (runs in Docker)
- H2 in-memory DB for tests
- Google Gemini API for the AI stuff
- JUnit 5 + Mockito + MockMvc

**Frontend**
- Plain HTML/CSS/JS, no framework, no build step
- Tests with `node --test`

## Run it locally

```bash
# 1. start the DB
docker compose up -d

# 2. start the backend (needs a Gemini key, otherwise it won't boot)
export GEMINI_API_KEY=your-key
cd backend && ./mvnw spring-boot:run

# 3. serve the frontend on port 8765 (CORS only allows that one)
cd frontend && python -m http.server 8765
```

Backend runs on `localhost:8080`, frontend on `localhost:8765`.

## Tests

```bash
cd backend && ./mvnw test                      # backend
node --test frontend/tests/*.test.js          # frontend
```
