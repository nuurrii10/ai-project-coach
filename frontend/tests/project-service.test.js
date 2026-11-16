// Run: node --test frontend/tests/project-service.test.js (no extra dependencies).
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');
const source = readFileSync(join(__dirname, '../js/project-service.js'), 'utf8');

function services(fetch) {
    const context = vm.createContext({ fetch, console: { error() {} } });
    vm.runInContext(source, context);
    return vm.runInContext('({ projectService, userStoryService, sprintService })', context);
}
function response(data, status = 200) {
    return { ok: status >= 200 && status < 300, status, json: async () => data };
}

test('loads the backend project list, including an actual empty list', async () => {
    const api = services(async (url) => {
        assert.equal(url, 'http://localhost:8080/api/projects');
        return response([]);
    });
    assert.deepEqual(await api.projectService.list(), []);
});

test('loads a project by its real ID', async () => {
    const api = services(async (url) => {
        assert.equal(url, 'http://localhost:8080/api/projects/42');
        return response({ id: 42, name: 'Backend project' });
    });
    assert.equal((await api.projectService.getById(42)).id, 42);
});

test('creates projects as JSON and returns the backend record', async () => {
    const api = services(async (url, options) => {
        assert.equal(url, 'http://localhost:8080/api/projects');
        assert.equal(options.method, 'POST');
        assert.equal(options.headers['Content-Type'], 'application/json');
        assert.deepEqual(JSON.parse(options.body), { name: 'New project', description: 'Description' });
        return response({ id: 9, name: 'New project' }, 201);
    });
    assert.equal((await api.projectService.create({ name: ' New project ', description: ' Description ' })).id, 9);
});

test('loads only the selected project stories', async () => {
    const api = services(async (url) => {
        assert.equal(url, 'http://localhost:8080/api/projects/7/user-stories');
        return response([{ id: 3, projectId: 7, title: 'Saved story' }]);
    });
    assert.equal((await api.userStoryService.list(7))[0].projectId, 7);
});

test('sends the backend story field names, text criteria, enum priority, and numeric points', async () => {
    const story = { title: 'Title', description: 'Description', acceptanceCriteria: 'First\nSecond', priority: 'HIGH', storyPoints: 8 };
    const api = services(async (url, options) => {
        assert.equal(url, 'http://localhost:8080/api/projects/7/user-stories');
        assert.equal(options.method, 'POST');
        assert.equal(options.headers['Content-Type'], 'application/json');
        assert.deepEqual(JSON.parse(options.body), story);
        return response({ ...story, id: 11, projectId: 7, status: 'BACKLOG' }, 201);
    });
    assert.equal((await api.userStoryService.create(7, story)).id, 11);
});

test('keeps omitted story points null instead of inventing a value', async () => {
    const api = services(async (_url, options) => {
        assert.equal(JSON.parse(options.body).storyPoints, null);
        return response({ id: 1 }, 201);
    });
    await api.userStoryService.create(1, { title: 'Title', priority: 'MEDIUM', storyPoints: null });
});

test('rejects HTTP failures without returning mock records', async () => {
    for (const status of [400, 404, 500]) {
        const api = services(async () => response(null, status));
        await assert.rejects(api.projectService.list(), new RegExp(`HTTP ${status}`));
        await assert.rejects(api.userStoryService.create(1, {}), new RegExp(`HTTP ${status}`));
    }
});

test('network/CORS failures reject clearly, and retry calls the server again', async () => {
    let attempts = 0;
    const api = services(async () => {
        if (++attempts === 1) throw new TypeError('Load failed');
        return response([{ id: 1, name: 'Real result' }]);
    });
    await assert.rejects(api.projectService.list(), /Cannot reach.*CORS/);
    assert.equal((await api.projectService.list())[0].name, 'Real result');
    assert.equal(attempts, 2);
});

test('invalid JSON is reported as an unexpected server response', async () => {
    const api = services(async () => ({ ok: true, json: async () => { throw new SyntaxError('Invalid JSON'); } }));
    await assert.rejects(api.projectService.list(), /unexpected response/);
});

test('loads project sprints, sprint details, and saved sprint assignments', async () => {
    const urls = [];
    const api = services(async (url) => {
        urls.push(url);
        return response([]);
    });
    await api.sprintService.list(7);
    await api.sprintService.getById(42);
    await api.sprintService.stories(42);
    assert.deepEqual(urls, [
        'http://localhost:8080/api/projects/7/sprints',
        'http://localhost:8080/api/sprints/42',
        'http://localhost:8080/api/sprints/42/user-stories'
    ]);
});

test('creates a sprint with a goal and YYYY-MM-DD dates as JSON', async () => {
    const sprint = { name: 'Sprint 3', goal: 'Complete sprint planning', startDate: '2026-11-16', endDate: '2026-11-22' };
    const api = services(async (url, options) => {
        assert.equal(url, 'http://localhost:8080/api/projects/7/sprints');
        assert.equal(options.method, 'POST');
        assert.equal(options.headers['Content-Type'], 'application/json');
        assert.deepEqual(JSON.parse(options.body), sprint);
        return response({ ...sprint, id: 42, projectId: 7 }, 201);
    });
    assert.equal((await api.sprintService.create(7, sprint)).goal, sprint.goal);
});

test('assigns and removes stories using real IDs and PUT/DELETE without a request body', async () => {
    const methods = [];
    const api = services(async (url, options) => {
        assert.equal(url, 'http://localhost:8080/api/sprints/42/user-stories/19');
        assert.equal(options.body, undefined);
        methods.push(options.method);
        return response({ id: 19, projectId: 7, sprintId: options.method === 'PUT' ? 42 : null });
    });
    assert.equal((await api.sprintService.assign(42, 19)).sprintId, 42);
    assert.equal((await api.sprintService.remove(42, 19)).sprintId, null);
    assert.deepEqual(methods, ['PUT', 'DELETE']);
});

test('sprint operations reject HTTP and network errors without a mock fallback', async () => {
    const unavailable = services(async () => { throw new TypeError('Failed to fetch'); });
    await assert.rejects(unavailable.sprintService.list(7), /Cannot reach.*CORS/);
    for (const status of [400, 404, 409, 500]) {
        const api = services(async () => response(null, status));
        await assert.rejects(api.sprintService.create(7, {}), new RegExp(`HTTP ${status}`));
        await assert.rejects(api.sprintService.assign(42, 19), new RegExp(`HTTP ${status}`));
        await assert.rejects(api.sprintService.remove(42, 19), new RegExp(`HTTP ${status}`));
    }
});
