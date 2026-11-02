// Local Spring Boot API. No mock fallback: failed requests reject their Promise.
const BACKEND_BASE_URL = 'http://localhost:8080';

async function requestBackend(path, options = {}) {
    let response;
    try {
        response = await fetch(`${BACKEND_BASE_URL}${path}`, options);
    } catch (error) {
        console.error(`API request failed: ${BACKEND_BASE_URL}${path}`, error);
        throw new Error(`Cannot reach the backend at localhost:8080 (${error.message}). Check that it is running and allows this frontend origin (CORS), then retry.`);
    }
    if (!response.ok) {
        if (response.status === 400) throw new Error('The backend rejected the entered data (HTTP 400). Check the fields and try again.');
        if (response.status === 404) throw new Error('The project or story could not be found (HTTP 404). Reload and try again.');
        throw new Error(`The backend request failed (HTTP ${response.status}). Please try again.`);
    }
    try {
        return await response.json();
    } catch {
        throw new Error('The backend returned an unexpected response. Please try again.');
    }
}

const projectService = {
    list() { return requestBackend('/api/projects'); },
    getById(id) { return requestBackend(`/api/projects/${encodeURIComponent(id)}`); },
    create({ name, description = '' }) {
        return requestBackend('/api/projects', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: name.trim(), description: description.trim() })
        });
    }
};

const userStoryService = {
    list(projectId) {
        return requestBackend(`/api/projects/${encodeURIComponent(projectId)}/user-stories`);
    },
    create(projectId, story) {
        return requestBackend(`/api/projects/${encodeURIComponent(projectId)}/user-stories`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(story)
        });
    }
};
