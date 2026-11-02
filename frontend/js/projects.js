// Project UI is independent of the existing workspace and US-025 controllers.
(() => {
    const list = document.querySelector("#project-list");
    const selector = document.querySelector("#current-project");
    const count = document.querySelector("#project-count");
    const empty = document.querySelector("#projects-empty");
    const details = document.querySelector("#project-details");
    const success = document.querySelector("#project-success");
    const error = document.querySelector("#project-error");
    const dialog = document.querySelector("#project-dialog");
    const form = document.querySelector("#project-form");
    const fields = document.querySelector("#project-form-fields");
    const nameInput = document.querySelector("#project-name");
    const descriptionInput = document.querySelector("#project-description");
    const nameError = document.querySelector("#project-name-error");
    const formError = document.querySelector("#project-form-error");
    let projects = [];
    let selectedId = null;
    try { selectedId = localStorage.getItem("aipc.selectedProjectId"); } catch { /* Selection still works without storage. */ }
    let announcedProjectId;
    let busy = false;
    let hasLoaded = false;
    let sidebarErrorShown = false;

    function element(tag, text, className) {
        const node = document.createElement(tag);
        if (text !== undefined) node.textContent = text;
        if (className) node.className = className;
        return node;
    }

    function currentProject() {
        return projects.find((project) => project.id === selectedId);
    }

    function clearFeedback() {
        success.textContent = "";
        error.textContent = "";
        if (sidebarErrorShown) {
            document.querySelector("#demo-feedback").textContent = "";
            sidebarErrorShown = false;
        }
    }

    function setBusy(value) {
        busy = value;
        selector.disabled = value || projects.length === 0;
        list.setAttribute("aria-busy", String(value));
        document.querySelectorAll(".project-create-trigger, .project-select-button, #reload-projects").forEach((control) => {
            control.disabled = value;
        });
    }

    function renderContext() {
        const selected = currentProject();
        selector.replaceChildren();
        if (!selected) {
            const option = element("option", hasLoaded ? "Select a project" : "Projects unavailable");
            option.value = "";
            option.disabled = projects.length > 0;
            selector.append(option);
        }
        projects.forEach((project) => {
            const option = element("option", project.name);
            option.value = project.id;
            selector.append(option);
        });
        selector.value = selected ? selected.id : "";
        selector.disabled = busy || projects.length === 0;
        document.querySelector("#current-project-description").textContent = selected
            ? "Selected project" : "View all projects to choose or create one.";
        document.querySelector("#workspace-project-context").textContent = selected
            ? `Project: ${selected.name} · Dashboard and sprint tools show sample data` : "No project selected";
        details.hidden = !selected;
        document.querySelector("#project-details-title").textContent = selected ? selected.name : "";
        document.querySelector("#project-details-description").textContent = selected
            ? selected.description || "No description provided." : "";
        const projectId = selected ? selected.id : null;
        if (projectId !== announcedProjectId) {
            announcedProjectId = projectId;
            try {
                if (hasLoaded) {
                    if (projectId) localStorage.setItem("aipc.selectedProjectId", projectId);
                    else localStorage.removeItem("aipc.selectedProjectId");
                }
            } catch { /* Browser storage is optional. */ }
            window.dispatchEvent(new CustomEvent("project-selected", { detail: { projectId } }));
        }
    }

    function renderProjects() {
        list.replaceChildren();
        projects.forEach((project, index) => {
            const card = element("li", undefined, "panel project-card");
            const selected = project.id === selectedId;
            card.classList.toggle("is-selected", selected);
            const heading = element("h3", project.name);
            heading.id = `project-card-title-${index}`;
            card.setAttribute("aria-labelledby", heading.id);
            const top = element("div", undefined, "panel-heading");
            top.append(heading);
            if (selected) top.append(element("span", "Current project", "status-badge"));
            const description = element("p", project.description || "No description provided.", "panel-copy");
            const button = element("button", selected ? "Selected · View details" : "Select project", "request-button project-select-button");
            button.type = "button";
            button.disabled = busy;
            button.setAttribute("aria-label", `Select ${project.name} and view project details`);
            button.setAttribute("aria-pressed", String(selected));
            button.addEventListener("click", () => selectProject(project.id, true));
            card.append(top, description, button);
            list.append(card);
        });
        count.textContent = `${projects.length} ${projects.length === 1 ? "project" : "projects"}`;
        empty.hidden = !hasLoaded || projects.length !== 0;
        renderContext();
    }

    async function loadProjects() {
        if (busy) return;
        const trigger = document.activeElement;
        clearFeedback();
        count.textContent = "Loading projects…";
        setBusy(true);
        try {
            projects = (await projectService.list()).map((project) => ({ ...project, id: String(project.id) }));
            hasLoaded = true;
            if (!currentProject()) selectedId = projects.length ? projects[0].id : null;
            renderProjects();
        } catch (failure) {
            count.textContent = hasLoaded ? `${projects.length} projects · Last loaded data` : "Projects unavailable";
            error.textContent = hasLoaded
                ? "We couldn't reload your projects. Your last loaded projects are still shown. Please try Reload projects again."
                : "We couldn't load your projects. Please choose Reload projects to try again.";
            error.textContent += ` ${failure.message}`;
            if (document.querySelector("#projects").hidden) {
                document.querySelector("#demo-feedback").textContent = error.textContent;
                sidebarErrorShown = true;
            }
            renderContext();
        } finally {
            setBusy(false);
            if (trigger.matches("#reload-projects")) {
                trigger.focus({ preventScroll: true });
            }
        }
    }

    async function selectProject(id, focusDetails = false) {
        if (busy) return;
        const trigger = document.activeElement;
        let failed = false;
        clearFeedback();
        setBusy(true);
        try {
            const result = await projectService.getById(id);
            const selected = { ...result, id: String(result.id) };
            projects = projects.map((project) => project.id === id ? selected : project);
            selectedId = selected.id;
            renderProjects();
            success.textContent = `${selected.name} selected. Open its workspace to continue.`;
            if (focusDetails && !document.querySelector("#projects").hidden) {
                document.querySelector("#project-details-title").focus();
            }
        } catch (failure) {
            failed = true;
            selector.value = currentProject() ? selectedId : "";
            const message = `We couldn't select that project. Your current selection has not changed. ${failure.message}`;
            error.textContent = message;
            // A selector action can fail while the overview is hidden.
            if (document.querySelector("#projects").hidden) {
                document.querySelector("#demo-feedback").textContent = message;
                sidebarErrorShown = true;
            }
        } finally {
            setBusy(false);
            if ((!focusDetails || failed) && trigger.isConnected) {
                trigger.focus({ preventScroll: true });
            }
        }
    }

    function validateName() {
        const name = nameInput.value.trim();
        const message = !name ? "Please enter a project name." : name.length > 100 ? "Keep the project name within 100 characters." : "";
        nameError.textContent = message;
        nameInput.setAttribute("aria-invalid", String(Boolean(message)));
        return !message;
    }

    document.querySelectorAll(".project-create-trigger").forEach((button) => {
        button.addEventListener("click", () => {
            if (busy) return;
            form.reset();
            formError.textContent = "";
            nameError.textContent = "";
            nameInput.removeAttribute("aria-invalid");
            dialog.showModal();
            nameInput.focus();
        });
    });
    document.querySelector("#cancel-project").addEventListener("click", () => dialog.close());
    dialog.addEventListener("cancel", (event) => {
        if (busy) event.preventDefault();
    });
    nameInput.addEventListener("input", () => {
        if (nameInput.getAttribute("aria-invalid") === "true") validateName();
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (busy) return;
        formError.textContent = "";
        if (!validateName()) {
            nameInput.focus();
            return;
        }
        if (descriptionInput.value.trim().length > 500) {
            formError.textContent = "Keep the description within 500 characters.";
            descriptionInput.focus();
            return;
        }
        clearFeedback();
        setBusy(true);
        fields.disabled = true;
        form.setAttribute("aria-busy", "true");
        document.querySelector("#save-project").textContent = "Creating…";
        let created = false;
        try {
            const result = await projectService.create({ name: nameInput.value, description: descriptionInput.value });
            const project = { ...result, id: String(result.id) };
            projects.unshift(project);
            hasLoaded = true;
            renderProjects();
            dialog.close();
            created = true;
            success.textContent = `${project.name} was created. Select it below to open its workspace.`;
        } catch (failure) {
            formError.textContent = `We couldn't create your project. Your entries are still here. ${failure.message}`;
        } finally {
            fields.disabled = false;
            form.setAttribute("aria-busy", "false");
            document.querySelector("#save-project").textContent = "Create Project";
            setBusy(false);
            if (created) {
                list.querySelector(".project-select-button").focus({ preventScroll: true });
            } else {
                document.querySelector("#save-project").focus();
            }
        }
    });

    selector.addEventListener("change", () => selectProject(selector.value));
    document.querySelector("#reload-projects").addEventListener("click", loadProjects);
    loadProjects();
})();
