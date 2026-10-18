const navigationLinks = document.querySelectorAll(".nav-link");
const pages = document.querySelectorAll(".page");

function showPage(pageId, moveFocus = false) {
    // Ignore unknown hashes and keep the dashboard as the safe default.
    const activePage = Array.from(pages).find((page) => page.id === pageId) || pages[0];

    pages.forEach((page) => {
        page.hidden = page !== activePage;
    });

    // Contextual screens stay within their parent workspace navigation area.
    const navigationPage = activePage.dataset.navParent || activePage.id;
    navigationLinks.forEach((link) => {
        const isActive = link.hash === `#${navigationPage}`;
        link.classList.toggle("is-active", isActive);
        if (isActive) {
            link.setAttribute("aria-current", navigationPage === activePage.id ? "page" : "location");
        } else {
            link.removeAttribute("aria-current");
        }
    });

    const title = activePage.querySelector("h2").textContent;
    const headerTitle = document.querySelector("#app-page-title");
    headerTitle.textContent = title;
    document.querySelector("#app-page-subtitle").textContent = activePage.querySelector(".page-heading p:last-child").textContent;
    document.querySelector("#header-sprint-board").hidden = activePage.id !== "dashboard";
    document.querySelectorAll(".header-action").forEach((action) => {
        action.hidden = action.dataset.page !== activePage.id;
    });
    document.querySelector("#demo-feedback").textContent = "";
    document.title = `${title} | AI Project Coach`;
    if (moveFocus) {
        headerTitle.focus({ preventScroll: true });
        window.scrollTo(0, 0);
    }
}

// Hash navigation works with keyboard activation, refresh, and browser history.
window.addEventListener("hashchange", () => {
    if (window.location.hash === "#main-content") return;
    showPage(window.location.hash.slice(1), true);
});
window.addEventListener("load", () => window.scrollTo(0, 0));

showPage(window.location.hash.slice(1));

// Demo service boundary: replace list/update with real API calls when available.
// Only requests for groups owned by the demo user are included. The future backend
// must enforce ownership and persist decisions; this preview has no authentication.
const joinRequestService = (() => {
    const requests = [
        { id: "jr-1", requesterName: "Alex Morgan", studyGroup: "Software Engineering", status: "pending" },
        { id: "jr-2", requesterName: "Sam Taylor", studyGroup: "Software Engineering", status: "pending" },
        { id: "jr-3", requesterName: "Jordan Lee", studyGroup: "Database Systems", status: "pending" }
    ];

    return {
        async list() {
            return requests.map((request) => ({ ...request }));
        },
        async update(id, status, { simulateFailure = false } = {}) {
            const request = requests.find((item) => item.id === id);
            if (simulateFailure) throw new Error("Simulated demo failure");
            if (!request || request.status !== "pending" || !["approved", "rejected"].includes(status)) {
                throw new Error("Request cannot be updated");
            }
            request.status = status;
            return { ...request };
        }
    };
})();

const requestList = document.querySelector("#request-list");
const requestTemplate = document.querySelector("#request-card-template");
const requestSummary = document.querySelector("#request-summary");
const requestSuccess = document.querySelector("#request-success");
const requestError = document.querySelector("#request-error");
const requestsEmpty = document.querySelector("#requests-empty");
const retryRequests = document.querySelector("#retry-requests");
const failNextRequest = document.querySelector("#fail-next-request");
let joinRequests = [];
const busyRequests = new Set();

function updateRequestSummary() {
    const pending = joinRequests.filter((request) => request.status === "pending").length;
    requestSummary.textContent = `${pending} pending · ${joinRequests.length - pending} reviewed`;
    requestsEmpty.hidden = pending !== 0;
}

function updateRequestCard(card, request) {
    const status = card.querySelector(".request-status");
    const busy = busyRequests.has(request.id);
    status.textContent = request.status[0].toUpperCase() + request.status.slice(1);
    status.dataset.status = request.status;
    card.setAttribute("aria-busy", String(busy));
    card.querySelectorAll("button").forEach((button) => {
        button.disabled = busy || request.status !== "pending";
    });
}

async function decideJoinRequest(card, id, decision) {
    const request = joinRequests.find((item) => item.id === id);
    if (!request || request.status !== "pending" || busyRequests.has(id)) return;

    requestSuccess.textContent = "";
    requestError.textContent = "";
    busyRequests.add(id);
    updateRequestCard(card, request);
    const simulateFailure = failNextRequest.checked;
    failNextRequest.checked = false;

    try {
        const updated = await joinRequestService.update(id, decision, { simulateFailure });
        Object.assign(request, updated);
        updateRequestCard(card, request);
        requestSuccess.textContent = `${request.requesterName}'s request to join ${request.studyGroup} was ${request.status}.`;
        updateRequestSummary();
        // Keep keyboard focus in the reviewed card when its actions become disabled.
        if (document.querySelector("#join-requests").hidden === false) {
            card.querySelector(".request-status").focus({ preventScroll: true });
        }
    } catch {
        const action = decision === "approved" ? "approve" : "reject";
        requestError.textContent = `We couldn't ${action} ${request.requesterName}'s request to join ${request.studyGroup}. The request is still pending. Please try again.`;
    } finally {
        busyRequests.delete(id);
        updateRequestCard(card, request);
    }
}

async function loadJoinRequests() {
    retryRequests.hidden = true;
    requestError.textContent = "";
    requestSummary.textContent = "Loading requests…";
    requestList.setAttribute("aria-busy", "true");
    try {
        joinRequests = await joinRequestService.list();
        requestList.replaceChildren();
        joinRequests.forEach((request) => {
            const card = requestTemplate.content.firstElementChild.cloneNode(true);
            card.querySelector(".requester-name").textContent = request.requesterName;
            card.querySelector(".request-group").textContent = request.studyGroup;
            card.querySelectorAll("button").forEach((button) => {
                const action = button.dataset.decision === "approved" ? "Approve" : "Reject";
                button.setAttribute("aria-label", `${action} ${request.requesterName}'s request to join ${request.studyGroup}`);
                button.addEventListener("click", () => decideJoinRequest(card, request.id, button.dataset.decision));
            });
            updateRequestCard(card, request);
            requestList.append(card);
        });
        updateRequestSummary();
    } catch {
        requestSummary.textContent = "Requests unavailable";
        requestError.textContent = "We couldn't load the study group requests. Please try loading them again.";
        retryRequests.hidden = false;
    } finally {
        requestList.setAttribute("aria-busy", "false");
    }
}

retryRequests.addEventListener("click", loadJoinRequests);
loadJoinRequests();

// The remaining mockup screens use local preview data only, separate from US-025.
const previewStories = [
    { id: 'US-031', title: 'As a student, I want to save a study group', priority: 'High', points: 5, status: 'Ready' },
    { id: 'US-030', title: 'As a moderator, I want to report inappropriate posts', priority: 'High', points: 8, status: 'Draft' },
    { id: 'US-029', title: 'As a student, I want to edit my profile interests', priority: 'Medium', points: 3, status: 'Ready' },
    { id: 'US-028', title: 'As a tutor, I want to publish available office hours', priority: 'Medium', points: 5, status: 'Refine' },
    { id: 'US-027', title: 'As a student, I want a schedule conflict warning', priority: 'High', points: 2, status: 'In Sprint' },
    { id: 'US-026', title: 'As a student, I want to request access to a public study group', priority: 'Medium', points: 5, status: 'Ready' },
    { id: 'US-025', title: 'As a group owner, I want to approve join requests', priority: 'Low', points: 2, status: 'In Sprint' },
    { id: 'US-024', title: 'As a user, I want to reset a forgotten password', priority: 'High', points: 3, status: 'In Sprint' }
];

function previewMessage(message) {
    document.querySelector('#demo-feedback').textContent = message;
}

function makeElement(tag, text, className) {
    const element = document.createElement(tag);
    if (text !== undefined) element.textContent = text;
    if (className) element.className = className;
    return element;
}

function renderBacklog() {
    const query = document.querySelector('#backlog-search').value.trim().toLowerCase();
    const priority = document.querySelector('#backlog-priority').value;
    const status = document.querySelector('#backlog-status').value;
    const filtered = previewStories.filter((story) =>
        `${story.id} ${story.title}`.toLowerCase().includes(query) &&
        (!priority || story.priority === priority) && (!status || story.status === status)
    );
    const rows = document.querySelector('#backlog-rows');
    rows.replaceChildren();
    filtered.forEach((story) => {
        const row = makeElement('tr');
        [story.id, story.title, story.priority, story.points].forEach((value) => row.append(makeElement('td', value)));
        const statusCell = makeElement('td');
        statusCell.append(makeElement('span', story.status, 'status-badge'));
        row.append(statusCell);
        const action = makeElement('td');
        if (story.id === 'US-025') {
            const link = makeElement('a', 'Review requests', 'table-link');
            link.href = '#join-requests';
            action.append(link);
        }
        row.append(action);
        rows.append(row);
    });
    document.querySelector('#backlog-count').textContent = `${filtered.length} demo stories · ${filtered.reduce((sum, story) => sum + story.points, 0)} total points`;
    document.querySelector('#backlog-empty').hidden = filtered.length !== 0;
}
['#backlog-search', '#backlog-priority', '#backlog-status'].forEach((selector) => {
    document.querySelector(selector).addEventListener('input', renderBacklog);
});
renderBacklog();

const storyDialog = document.querySelector('#story-dialog');
document.querySelector('#create-story').addEventListener('click', () => storyDialog.showModal());
document.querySelector('#cancel-story').addEventListener('click', () => storyDialog.close());
function addPreviewStory(title, priority, points) {
    const nextId = Math.max(...previewStories.map((story) => Number(story.id.slice(3)))) + 1;
    previewStories.unshift({ id: `US-${String(nextId).padStart(3, '0')}`, title, priority, points, status: 'Draft' });
    document.querySelector('#backlog-search').value = '';
    document.querySelector('#backlog-priority').value = '';
    document.querySelector('#backlog-status').value = '';
    renderBacklog();
    return `US-${String(nextId).padStart(3, '0')}`;
}
document.querySelector('#story-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const title = document.querySelector('#new-story-title');
    if (!title.value.trim()) {
        title.setCustomValidity('Please enter a user story.');
        title.reportValidity();
        return;
    }
    const id = addPreviewStory(title.value.trim(), document.querySelector('#new-story-priority').value, Number(document.querySelector('#new-story-points').value));
    storyDialog.close();
    event.target.reset();
    previewMessage(`${id} was added to the demo backlog. It will reset on refresh.`);
});
document.querySelector('#new-story-title').addEventListener('input', (event) => event.target.setCustomValidity(''));

const requirement = document.querySelector('#story-requirement');
function updateRequirementCount() {
    document.querySelector('#requirement-count').textContent = `${requirement.value.length} / 1,000`;
}
requirement.addEventListener('input', updateRequirementCount);
updateRequirementCount();
document.querySelector('#generate-story').addEventListener('click', () => {
    previewMessage(requirement.value.trim()
        ? 'Sample study-group draft is ready to review and edit. This demo does not generate new AI content.'
        : 'Describe the requirement first, then review the sample draft.');
});
document.querySelector('#add-draft').addEventListener('click', () => {
    const title = document.querySelector('#suggested-story').value.trim();
    if (!title) return previewMessage('Enter a user story before adding the draft to the backlog.');
    const id = addPreviewStory(title, 'Medium', 5);
    // Update the route before the message so the hash handler does not clear feedback.
    window.location.hash = 'backlog';
    showPage('backlog', true);
    window.setTimeout(() => previewMessage(`${id} was added to the demo backlog. No AI service was called.`), 0);
});

const participants = [
    ['SG', 'Serhat Goektas', 5], ['AM', 'Aladam Mishref', 5], ['SL', 'Stephan Lottes', 3],
    ['EMM', 'Enrique Morales Mesino', 8], ['NA', 'Nuri Ali', 5], ['MR', 'Markus Rizkalla', 5]
];
participants.forEach(([initials, name, vote]) => {
    const card = makeElement('div', undefined, 'estimate-card');
    card.append(makeElement('span', initials, 'avatar'), makeElement('p', name), makeElement('span', 'Developer', 'muted'), makeElement('strong', vote));
    document.querySelector('#team-estimates').append(card);
});
let selectedVote = '5';
let votingClosed = false;
function selectVote(value) {
    selectedVote = value;
    document.querySelectorAll('.vote-button').forEach((button) => button.setAttribute('aria-pressed', String(button.textContent === value)));
    document.querySelector('#selected-estimate').textContent = value === '?' ? 'Selected: unsure' : `Selected: ${value} points`;
}
['1', '2', '3', '5', '8', '13', '?'].forEach((value) => {
    const button = makeElement('button', value, 'request-button vote-button');
    button.type = 'button';
    button.setAttribute('aria-label', value === '?' ? 'Unsure of estimate' : `Estimate ${value} points`);
    button.addEventListener('click', () => selectVote(value));
    document.querySelector('#vote-options').append(button);
});
selectVote(selectedVote);
document.querySelector('#use-estimate').addEventListener('click', () => {
    selectVote('5');
    previewMessage('Final estimate set to 5 story points in this demo.');
});
document.querySelector('#end-voting').addEventListener('click', (event) => {
    votingClosed = !votingClosed;
    event.target.textContent = votingClosed ? 'Reopen Voting' : 'End Voting';
    document.querySelectorAll('.vote-button').forEach((button) => { button.disabled = votingClosed; });
    document.querySelector('#voting-description').textContent = votingClosed ? 'Voting is closed. Discuss the revealed sample votes and agree on a final estimate.' : 'Votes are revealed. Discuss the difference before agreeing a final estimate.';
});
document.querySelector('#add-to-sprint').addEventListener('click', (event) => {
    if (selectedVote === '?') return previewMessage('Choose a numeric estimate before adding the story to Sprint 5.');
    event.target.disabled = true;
    event.target.textContent = 'Added to Sprint 5';
    previewMessage(`US-026 was added to the demo Sprint 5 with ${selectedVote} points. No server changes were made.`);
});

const boardStories = [
    ['US-027', 'Show schedule conflict warning', 'High', 2, 'SG', 'To Do'],
    ['US-025', 'Approve study group join requests', 'Medium', 2, 'AM', 'To Do'],
    ['US-024', 'Reset forgotten password', 'High', 3, 'SL', 'In Progress', true],
    ['US-022', 'Notify owner of new join request', 'Medium', 1, 'EMM', 'In Progress'],
    ['US-023', 'Review public study group access request', 'High', 5, 'NA', 'Review / Test', true],
    ['US-014', 'Create a study group', 'High', 3, 'MR', 'Done'],
    ['US-015', 'Search study groups', 'High', 3, 'SG', 'Done'],
    ['US-016', 'View group details', 'Medium', 3, 'AM', 'Done'],
    ['US-017', 'Leave a study group', 'Low', 2, 'SL', 'Done'],
    ['US-018', 'View study group member list', 'Medium', 2, 'EMM', 'Done'],
    ['US-019', 'Filter groups by faculty', 'Low', 2, 'NA', 'Done'],
    ['US-020', 'Receive group notifications', 'Medium', 3, 'MR', 'Done'],
    ['US-021', 'Update study group profile', 'Low', 3, 'SG', 'Done']
];
function renderBoard() {
    const assignee = document.querySelector('#board-assignee').value;
    const priority = document.querySelector('#board-priority').value;
    const filtered = boardStories.filter((story) => (!assignee || story[4] === assignee) && (!priority || story[2] === priority));
    const board = document.querySelector('#board-columns');
    board.replaceChildren();
    ['To Do', 'In Progress', 'Review / Test', 'Done'].forEach((status, index) => {
        const column = makeElement('section', undefined, 'board-column');
        const stories = filtered.filter((story) => story[5] === status);
        const heading = makeElement('h3', `${status} `);
        heading.id = `board-column-${index}`;
        heading.append(makeElement('span', stories.length, 'column-count'));
        column.setAttribute('aria-labelledby', heading.id);
        column.append(heading);
        if (['In Progress', 'Review / Test'].includes(status)) column.append(makeElement('p', `WIP ${stories.length} / 3`, 'field-help'));
        stories.forEach(([id, title, storyPriority, points, initials, , blocked]) => {
            const card = makeElement('article', undefined, 'board-card');
            const top = makeElement('div', undefined, 'board-card-meta');
            top.append(makeElement('span', id), makeElement('span', blocked ? 'Blocked' : storyPriority, blocked ? 'risk-badge' : ''));
            const name = id === 'US-025' ? makeElement('a', title) : makeElement('h4', title);
            if (id === 'US-025') name.href = '#join-requests';
            const bottom = makeElement('div', undefined, 'board-card-meta');
            const person = participants.find((participant) => participant[0] === initials);
            const avatar = makeElement('span', initials, 'avatar');
            avatar.setAttribute('aria-label', person[1]);
            bottom.append(makeElement('span', `${points} pts`), avatar);
            card.append(top, name, bottom);
            column.append(card);
        });
        if (!stories.length) column.append(makeElement('p', 'No matching stories.', 'field-help'));
        const add = makeElement('a', '+ Add story from backlog', 'column-add');
        add.href = '#backlog';
        column.append(add);
        board.append(column);
    });
    document.querySelector('#board-count').textContent = `${filtered.length} stories · ${filtered.filter((story) => story[6]).length} blocked`;
}
document.querySelector('#board-assignee').addEventListener('change', renderBoard);
document.querySelector('#board-priority').addEventListener('change', renderBoard);
renderBoard();

document.querySelector('#start-retro').addEventListener('click', () => {
    document.querySelector('#team-retrospective').hidden = false;
    document.querySelector('#retro-notes').focus();
});
document.querySelector('#ask-coach').addEventListener('click', () => {
    document.querySelector('#coach-followup').hidden = false;
    document.querySelector('#coach-question').focus();
});
document.querySelector('#create-actions').addEventListener('click', (event) => {
    event.target.disabled = true;
    event.target.textContent = '3 Demo Action Items Created';
    previewMessage('The three suggested improvements are marked as Sprint 5 demo action items. They reset on refresh.');
});
document.querySelector('#export-summary').addEventListener('click', () => {
    const text = 'AI Project Coach — Sprint 4 demo retrospective\nPlanned: 34 points. Completed: 21 points (8/13 stories). Carry-over: 13 points.\n\nSuggested actions:\n1. Confirm authentication API ownership and test data.\n2. Set a daily review handoff.\n3. Protect sprint scope.\n\nTeam notes:\n' + document.querySelector('#retro-notes').value;
    const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }));
    const link = makeElement('a');
    link.href = url;
    link.download = 'sprint-4-demo-retrospective.txt';
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    previewMessage('Demo retrospective summary exported as a text file.');
});
