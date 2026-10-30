ai-project-coach
Team 11

Local setup
- Backend: `cd backend && ./mvnw spring-boot:run`
- Default local configuration uses an in-memory H2 database unless you provide `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- The AI integration falls back to a placeholder Gemini key so the app can start without a configured API key; set `GEMINI_API_KEY` to enable live generation.
