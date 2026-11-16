// Real sprint planning is separate from the existing Planning Poker demo.
(() => {
    const panel = document.querySelector('#sprint-management');
    const form = document.querySelector('#sprint-form');
    const selector = document.querySelector('#planning-sprint');
    const success = document.querySelector('#sprint-success');
    const error = document.querySelector('#sprint-error');
    const nameInput = document.querySelector('#sprint-name');
    const startInput = document.querySelector('#sprint-start');
    const endInput = document.querySelector('#sprint-end');
    let projectId = null;
    let sprints = [];
    let selectedSprint = null;
    let projectStories = [];
    let assignedStories = [];
    let loaded = false;
    let busy = false;
    // Ignore responses belonging to a previous project or selection.
    let requestVersion = 0;

    function render() {
        panel.setAttribute('aria-busy', String(busy));
        panel.querySelectorAll('input, textarea, select, button').forEach((control) => {
            control.disabled = busy || !projectId;
        });
        selector.replaceChildren();
        if (!selectedSprint) {
            const option = makeElement('option', 'Select a sprint');
            option.value = '';
            selector.append(option);
        }
        sprints.forEach((sprint) => {
            const option = makeElement('option', sprint.name);
            option.value = String(sprint.id);
            selector.append(option);
        });
        selector.value = selectedSprint ? String(selectedSprint.id) : '';
        selector.disabled = busy || !loaded || !sprints.length;
        document.querySelector('#sprint-load-status').textContent = !projectId
            ? 'Select a project to plan its sprints.' : busy ? 'Loading sprint planning…'
            : loaded ? `${sprints.length} sprints in this project` : 'Planning unavailable. Choose Reload planning to retry.';
        document.querySelector('#sprints-empty').hidden = !loaded || sprints.length > 0;
        document.querySelector('#planning-sprint-details').hidden = !selectedSprint;
        document.querySelector('#planning-sprint-name').textContent = selectedSprint ? selectedSprint.name : '';
        document.querySelector('#planning-sprint-dates').textContent = selectedSprint
            ? `${selectedSprint.startDate} – ${selectedSprint.endDate}` : '';
        document.querySelector('#planning-sprint-goal').textContent = selectedSprint
            ? selectedSprint.goal || 'No sprint goal provided.' : '';

        const rows = document.querySelector('#sprint-story-rows');
        rows.replaceChildren();
        if (selectedSprint && loaded) {
            const assignedIds = new Set(assignedStories.map((story) => String(story.id)));
            projectStories.forEach((story) => {
                const assigned = assignedIds.has(String(story.id));
                const elsewhere = !assigned && story.sprintId != null;
                const otherSprint = sprints.find((sprint) => String(sprint.id) === String(story.sprintId));
                const assignment = assigned ? 'Assigned to this sprint' : elsewhere
                    ? `Assigned to ${otherSprint ? otherSprint.name : `sprint ${story.sprintId}`}` : 'Unassigned';
                const row = makeElement('tr');
                row.append(makeElement('td', `US-${String(story.id).padStart(3, '0')}`), makeElement('td', story.title), makeElement('td', assignment));
                const action = makeElement('td');
                const button = makeElement('button', assigned ? 'Remove' : 'Assign', 'request-button');
                button.type = 'button';
                button.disabled = busy || elsewhere;
                button.setAttribute('aria-label', `${assigned ? 'Remove' : 'Assign'} ${story.title} ${assigned ? 'from' : 'to'} ${selectedSprint.name}`);
                button.addEventListener('click', () => updateAssignment(story.id, assigned));
                action.append(button);
                row.append(action);
                rows.append(row);
            });
        }
        document.querySelector('#sprint-story-summary').textContent = selectedSprint && loaded
            ? `${assignedStories.length} assigned · ${projectStories.length} project stories`
            : 'Select a project and sprint to view its stories.';
        document.querySelector('#sprint-stories-empty').hidden = !selectedSprint || !loaded || projectStories.length > 0;
    }

    async function loadPlanning(preferredId = selectedSprint?.id, message = '') {
        const version = ++requestVersion;
        const loadingProjectId = projectId;
        success.textContent = message;
        error.textContent = '';
        selectedSprint = null;
        projectStories = [];
        assignedStories = [];
        sprints = [];
        loaded = false;
        busy = Boolean(projectId);
        render();
        if (!projectId) return;
        try {
            const [sprintList, stories] = await Promise.all([
                sprintService.list(loadingProjectId), userStoryService.list(loadingProjectId)
            ]);
            if (version !== requestVersion) return;
            // Only display records belonging to the selected project.
            sprints = sprintList.filter((sprint) => String(sprint.projectId) === loadingProjectId);
            projectStories = stories.filter((story) => String(story.projectId) === loadingProjectId);
            if (!preferredId) {
                try { preferredId = localStorage.getItem(`aipc.selectedSprint.${loadingProjectId}`); } catch { /* Storage is optional. */ }
            }
            const choice = sprints.find((sprint) => String(sprint.id) === String(preferredId)) || sprints[0];
            if (choice) {
                const [sprint, assignments] = await Promise.all([
                    sprintService.getById(choice.id), sprintService.stories(choice.id)
                ]);
                if (version !== requestVersion) return;
                if (String(sprint.projectId) !== loadingProjectId) throw new Error('This sprint does not belong to the selected project.');
                selectedSprint = sprint;
                assignedStories = assignments.filter((story) => String(story.projectId) === loadingProjectId);
                try { localStorage.setItem(`aipc.selectedSprint.${loadingProjectId}`, String(sprint.id)); } catch { /* Storage is optional. */ }
            }
            loaded = true;
        } catch (failure) {
            if (version !== requestVersion) return;
            error.textContent = `We couldn't load sprint planning. ${failure.message} Choose Reload planning to retry.`;
        } finally {
            if (version === requestVersion) {
                busy = false;
                render();
            }
        }
    }

    function validateForm() {
        const nameError = !nameInput.value.trim() ? 'Please enter a sprint name.'
            : nameInput.value.trim().length > 100 ? 'Keep the sprint name within 100 characters.' : '';
        const startError = !startInput.value || !startInput.checkValidity() ? 'Please enter a valid start date.' : '';
        const endError = !endInput.value || !endInput.checkValidity() ? 'Please enter a valid end date.'
            : !startError && endInput.value < startInput.value ? 'End date cannot be before start date.' : '';
        const fields = [[nameInput, 'sprint-name-error', nameError], [startInput, 'sprint-start-error', startError], [endInput, 'sprint-end-error', endError]];
        fields.forEach(([input, id, message]) => {
            document.getElementById(id).textContent = message;
            input.setAttribute('aria-invalid', String(Boolean(message)));
        });
        const invalid = fields.find(([, , message]) => message);
        if (invalid) invalid[0].focus();
        return !invalid;
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (busy || !projectId || !validateForm()) return;
        const version = ++requestVersion;
        const savingProjectId = projectId;
        const sprint = {
            name: nameInput.value.trim(),
            goal: document.querySelector('#sprint-goal').value.trim(),
            startDate: startInput.value,
            endDate: endInput.value
        };
        success.textContent = '';
        error.textContent = '';
        busy = true;
        render();
        try {
            const created = await sprintService.create(savingProjectId, sprint);
            if (version !== requestVersion) return;
            form.reset();
            form.querySelectorAll('[aria-invalid]').forEach((input) => input.removeAttribute('aria-invalid'));
            await loadPlanning(created.id, `${created.name} was created and saved, including its sprint goal.`);
            if (projectId === savingProjectId && selectedSprint?.id === created.id) {
                document.querySelector('#planning-sprint-name').focus();
            }
        } catch (failure) {
            if (version !== requestVersion) return;
            error.textContent = `We couldn't confirm that the sprint was saved. Your entries are still here. ${failure.message} Reload planning before retrying to check for a saved sprint.`;
            error.focus();
        } finally {
            if (version === requestVersion) {
                busy = false;
                render();
            }
        }
    });

    async function updateAssignment(storyId, remove) {
        if (busy || !projectId || !selectedSprint) return;
        const story = projectStories.find((item) => String(item.id) === String(storyId));
        if (!story || String(story.projectId) !== projectId || String(selectedSprint.projectId) !== projectId) return;
        const assigned = assignedStories.some((item) => String(item.id) === String(storyId));
        if (remove !== assigned || (!remove && story.sprintId != null)) return;
        const version = ++requestVersion;
        const sprintId = selectedSprint.id;
        const savingProjectId = projectId;
        success.textContent = '';
        error.textContent = '';
        busy = true;
        render();
        try {
            if (remove) await sprintService.remove(sprintId, storyId);
            else await sprintService.assign(sprintId, storyId);
            if (version !== requestVersion) return;
            await loadPlanning(sprintId, `${story.title} was ${remove ? 'removed from' : 'assigned to'} the sprint and saved.`);
            if (projectId === savingProjectId && selectedSprint?.id === sprintId) {
                document.querySelector('#planning-sprint-name').focus();
            }
        } catch (failure) {
            if (version !== requestVersion) return;
            error.textContent = `We couldn't confirm the assignment change. ${failure.message} Choose Reload planning to check the saved assignments before retrying.`;
            error.focus();
        } finally {
            if (version === requestVersion) {
                busy = false;
                render();
            }
        }
    }

    window.addEventListener('project-selected', (event) => {
        projectId = event.detail.projectId == null ? null : String(event.detail.projectId);
        form.reset();
        form.querySelectorAll('[aria-invalid]').forEach((input) => input.removeAttribute('aria-invalid'));
        form.querySelectorAll('.story-field-error').forEach((message) => { message.textContent = ''; });
        loadPlanning(null);
    });
    selector.addEventListener('change', () => loadPlanning(selector.value));
    document.querySelector('#reload-sprints').addEventListener('click', () => loadPlanning());
    window.addEventListener('hashchange', () => {
        if (window.location.hash === '#sprint-planning' && !busy) loadPlanning();
    });
    render();
})();
