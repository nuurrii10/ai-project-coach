// Controller unit tests with small DOM/service stubs, not browser or backend tests.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');
const source = readFileSync(join(__dirname, '../js/sprint-planning.js'), 'utf8');
const html = readFileSync(join(__dirname, '../index.html'), 'utf8');

function setup({ sprints = [], stories = [], storage = new Map() } = {}) {
    const nodes = new Map();
    let focused;
    function element(tag, text = '') {
        const attributes = new Map();
        return {
            tag, textContent: text, value: '', children: [], events: {}, disabled: false, hidden: false,
            append(...children) { this.children.push(...children); },
            replaceChildren(...children) { this.children = children; },
            addEventListener(type, handler) { this.events[type] = handler; },
            setAttribute(name, value) { attributes.set(name, value); },
            removeAttribute(name) { attributes.delete(name); },
            focus() { focused = this; },
            checkValidity() { return Boolean(this.value); }
        };
    }
    for (const [, id] of html.matchAll(/id="([^"]+)"/g)) nodes.set(id, element('div'));
    const fieldIds = ['sprint-name', 'sprint-start', 'sprint-end', 'sprint-goal'];
    const fields = fieldIds.map((id) => nodes.get(id));
    const errorIds = ['sprint-name-error', 'sprint-start-error', 'sprint-end-error'];
    nodes.get('sprint-form').reset = () => fields.forEach((field) => { field.value = ''; });
    nodes.get('sprint-form').querySelectorAll = (selector) => selector === '.story-field-error'
        ? errorIds.map((id) => nodes.get(id)) : fields;
    nodes.get('sprint-management').querySelectorAll = () => [
        ...fields, nodes.get('create-sprint'), nodes.get('planning-sprint'), nodes.get('reload-sprints')
    ];
    const events = {};
    const calls = [];
    const sprintService = {
        async list(projectId) { calls.push(['list', projectId]); return sprints.map((sprint) => ({ ...sprint })); },
        async getById(id) { calls.push(['get', id]); return { ...sprints.find((sprint) => sprint.id === id) }; },
        async stories(id) { return stories.filter((story) => story.sprintId === id).map((story) => ({ ...story })); },
        async create(projectId, data) {
            calls.push(['create', projectId, data]);
            const sprint = { ...data, id: 42, projectId: Number(projectId) };
            sprints.push(sprint);
            return sprint;
        },
        async assign(sprintId, storyId) {
            calls.push(['assign', sprintId, storyId]);
            stories.find((story) => story.id === storyId).sprintId = sprintId;
        },
        async remove(sprintId, storyId) {
            calls.push(['remove', sprintId, storyId]);
            stories.find((story) => story.id === storyId).sprintId = null;
        }
    };
    const context = vm.createContext({
        document: { querySelector: (selector) => nodes.get(selector.slice(1)), getElementById: (id) => nodes.get(id) },
        window: { addEventListener: (type, handler) => { events[type] = handler; }, location: { hash: '' } },
        localStorage: { getItem: (key) => storage.get(key), setItem: (key, value) => storage.set(key, value) },
        makeElement: element, sprintService,
        userStoryService: { list: async () => stories.map((story) => ({ ...story })) }
    });
    vm.runInContext(source, context);
    return {
        nodes, sprints, stories, storage, calls, sprintService,
        get focused() { return focused; },
        async selectProject(projectId) {
            events['project-selected']({ detail: { projectId } });
            await new Promise(setImmediate);
        },
        async submit() { await nodes.get('sprint-form').events.submit({ preventDefault() {} }); },
        async reload() { await nodes.get('reload-sprints').events.click(); },
        async clickStory(index = 0) { await nodes.get('sprint-story-rows').children[index].children[3].children[0].events.click(); }
    };
}

function fillForm(ui) {
    ui.nodes.get('sprint-name').value = ' Sprint 3 ';
    ui.nodes.get('sprint-start').value = '2026-11-16';
    ui.nodes.get('sprint-end').value = '2026-11-22';
    ui.nodes.get('sprint-goal').value = 'Complete sprint planning';
}
const sprint = { id: 42, projectId: 7, name: 'Sprint 3', goal: 'Saved goal', startDate: '2026-11-16', endDate: '2026-11-22' };

test('shows no-project and empty states without attempting to save', async () => {
    const ui = setup();
    assert.equal(ui.nodes.get('create-sprint').disabled, true);
    await ui.submit();
    assert.equal(ui.calls.length, 0);
    await ui.selectProject('7');
    assert.equal(ui.nodes.get('sprints-empty').hidden, false);
    assert.equal(ui.nodes.get('create-sprint').disabled, false);
});

test('requires name and dates, rejects reversed dates, and permits equal dates', async () => {
    const ui = setup();
    await ui.selectProject('7');
    await ui.submit();
    for (const id of ['sprint-name-error', 'sprint-start-error', 'sprint-end-error']) assert.ok(ui.nodes.get(id).textContent);
    assert.equal(ui.focused, ui.nodes.get('sprint-name'));
    fillForm(ui);
    ui.nodes.get('sprint-end').value = '2026-11-15';
    await ui.submit();
    assert.match(ui.nodes.get('sprint-end-error').textContent, /before/);
    assert.equal(ui.calls.some(([method]) => method === 'create'), false);
    ui.nodes.get('sprint-end').value = '2026-11-16';
    await ui.submit();
    assert.equal(ui.sprints.length, 1);
});

test('creates, selects, and reloads a sprint goal from service responses', async () => {
    const ui = setup();
    await ui.selectProject('7');
    fillForm(ui);
    await ui.submit();
    assert.equal(ui.sprints[0].name, 'Sprint 3');
    assert.equal(ui.nodes.get('sprint-name').value, '');
    assert.match(ui.nodes.get('sprint-success').textContent, /created and saved/);
    assert.equal(ui.nodes.get('planning-sprint').value, '42');
    const reloaded = setup(ui);
    await reloaded.selectProject('7');
    assert.equal(reloaded.nodes.get('planning-sprint-goal').textContent, 'Complete sprint planning');
});

test('assigns and removes a project story, then reloads service assignments', async () => {
    const ui = setup({ sprints: [{ ...sprint }], stories: [{ id: 19, projectId: 7, title: 'Story', sprintId: null }] });
    await ui.selectProject('7');
    await ui.clickStory();
    assert.deepEqual(ui.calls.find(([method]) => method === 'assign'), ['assign', 42, 19]);
    const reloaded = setup(ui);
    await reloaded.selectProject('7');
    assert.equal(reloaded.nodes.get('sprint-story-rows').children[0].children[3].children[0].textContent, 'Remove');
    await reloaded.clickStory();
    assert.deepEqual(reloaded.calls.find(([method]) => method === 'remove'), ['remove', 42, 19]);
    await reloaded.reload();
    assert.equal(reloaded.nodes.get('sprint-story-rows').children[0].children[3].children[0].textContent, 'Assign');
});

test('filters other projects and prevents moving a story out of another sprint', async () => {
    const ui = setup({ sprints: [{ ...sprint }, { ...sprint, id: 43, projectId: 8 }], stories: [
        { id: 19, projectId: 7, title: 'Already assigned elsewhere', sprintId: 44 },
        { id: 20, projectId: 8, title: 'Other project', sprintId: null }
    ] });
    await ui.selectProject('7');
    const rows = ui.nodes.get('sprint-story-rows').children;
    assert.equal(rows.length, 1);
    assert.equal(rows[0].children[3].children[0].disabled, true);
    await ui.clickStory();
    assert.equal(ui.calls.some(([method]) => method === 'assign'), false);
    await ui.selectProject('8');
    assert.equal(ui.nodes.get('planning-sprint').value, '43');
    assert.equal(ui.nodes.get('sprint-story-rows').children[0].children[1].textContent, 'Other project');
});

test('selects another sprint, shows its saved goal, and remembers that selection', async () => {
    const ui = setup({ sprints: [{ ...sprint }, { ...sprint, id: 43, name: 'Sprint 4', goal: 'Second goal' }], stories: [
        { id: 19, projectId: 7, title: 'Assigned story', sprintId: 43 }
    ] });
    await ui.selectProject('7');
    assert.equal(ui.nodes.get('sprint-story-rows').children[0].children[3].children[0].disabled, true);
    ui.nodes.get('planning-sprint').value = '43';
    await ui.nodes.get('planning-sprint').events.change();
    assert.equal(ui.nodes.get('planning-sprint-goal').textContent, 'Second goal');
    assert.equal(ui.nodes.get('sprint-story-rows').children[0].children[3].children[0].textContent, 'Remove');
    await ui.clickStory();
    const reloaded = setup(ui);
    await reloaded.selectProject('7');
    assert.equal(reloaded.nodes.get('planning-sprint').value, '43');
    assert.equal(reloaded.nodes.get('sprint-story-rows').children[0].children[3].children[0].textContent, 'Assign');
});

test('retains form data on save failure and shows a reloadable loading error', async () => {
    const ui = setup();
    await ui.selectProject('7');
    fillForm(ui);
    ui.sprintService.create = async () => { throw new Error('HTTP 500'); };
    await ui.submit();
    assert.equal(ui.nodes.get('sprint-name').value, ' Sprint 3 ');
    assert.match(ui.nodes.get('sprint-error').textContent, /HTTP 500/);
    assert.equal(ui.nodes.get('sprint-success').textContent, '');
    ui.sprintService.list = async () => { throw new Error('Backend unavailable'); };
    await ui.reload();
    assert.match(ui.nodes.get('sprint-error').textContent, /Reload planning/);
    assert.equal(ui.nodes.get('reload-sprints').disabled, false);
});

test('ignores an older project response after project selection changes', async () => {
    const ui = setup({ sprints: [{ ...sprint }] });
    let finishOldRequest;
    ui.sprintService.list = (projectId) => projectId === '7'
        ? new Promise((resolve) => { finishOldRequest = resolve; }) : Promise.resolve([]);
    await ui.selectProject('7');
    await ui.selectProject('8');
    finishOldRequest([{ ...sprint }]);
    await new Promise(setImmediate);
    assert.equal(ui.nodes.get('planning-sprint-details').hidden, true);
    assert.equal(ui.nodes.get('planning-sprint').value, '');
    assert.equal(ui.nodes.get('sprints-empty').hidden, false);
});
