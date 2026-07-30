const dashboardState = {
  incidentStatus: "OPEN",
  activeIncident: null,
  refreshTimer: null,
};

const numberFormat = new Intl.NumberFormat();
const dateFormat = new Intl.DateTimeFormat(undefined, {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "UTC",
});

async function fetchJson(path, options = {}) {
  const response = await fetch(path, {
    headers: { Accept: "application/json", ...options.headers },
    ...options,
  });
  if (!response.ok) {
    let detail = `Request failed with status ${response.status}`;
    try {
      const problem = await response.json();
      detail = problem.detail || detail;
    } catch {
      // Some infrastructure responses do not contain a JSON problem body.
    }
    throw new Error(detail);
  }
  return response.json();
}

function clear(element) {
  element.replaceChildren();
}

function textElement(tag, className, value) {
  const element = document.createElement(tag);
  if (className) {
    element.className = className;
  }
  element.textContent = value;
  return element;
}

function badge(value) {
  return textElement("span", `badge badge-${value.toLowerCase()}`, value);
}

function formatInstant(value) {
  return dateFormat.format(new Date(value));
}

function compactRule(ruleCode) {
  return ruleCode.replaceAll("_", " ");
}

function setText(id, value) {
  document.getElementById(id).textContent = value;
}

async function loadHealth() {
  const indicator = document.getElementById("health-indicator");
  try {
    const health = await fetchJson("/actuator/health");
    indicator.className = "health healthy";
    indicator.lastChild.textContent = health.status === "UP" ? " Service healthy" : " Service status";
  } catch {
    indicator.className = "health unhealthy";
    indicator.lastChild.textContent = " Service unavailable";
  }
}

function renderTrend(points) {
  const chart = document.getElementById("trend-chart");
  clear(chart);
  const latest = points.slice(-12);
  const max = Math.max(1, ...latest.map((point) => point.count));
  if (latest.length === 0) {
    chart.append(textElement("p", "empty-state", "No event activity in this period."));
    return;
  }
  for (const point of latest) {
    const item = document.createElement("div");
    item.className = "trend-bar";
    item.title = `${formatInstant(point.bucketStart)}: ${numberFormat.format(point.count)} events`;
    const progress = document.createElement("progress");
    progress.max = max;
    progress.value = point.count;
    progress.setAttribute("aria-label", item.title);
    const label = textElement("span", "", new Date(point.bucketStart).getUTCHours().toString().padStart(2, "0"));
    item.append(progress, label);
    chart.append(item);
  }
}

function renderSeverities(items, total) {
  const list = document.getElementById("severity-list");
  clear(list);
  if (items.length === 0) {
    list.append(textElement("p", "empty-state", "No severity data available."));
    return;
  }
  for (const item of items) {
    const row = document.createElement("div");
    row.className = "severity-row";
    const progress = document.createElement("progress");
    progress.max = Math.max(total, 1);
    progress.value = item.count;
    progress.setAttribute("aria-label", `${item.name}: ${item.count}`);
    row.append(badge(item.name), progress, textElement("strong", "", numberFormat.format(item.count)));
    list.append(row);
  }
}

async function loadStatistics() {
  const data = await fetchJson("/api/v1/statistics?bucket=HOUR");
  setText("metric-events", numberFormat.format(data.totalEvents));
  setText("metric-incidents", numberFormat.format(data.openIncidents));
  setText("metric-range", `${formatInstant(data.from)} to ${formatInstant(data.to)}`);
  const errorCount = data.bySeverity
    .filter((item) => item.name === "ERROR" || item.name === "FATAL")
    .reduce((sum, item) => sum + item.count, 0);
  setText("metric-errors", numberFormat.format(errorCount));
  const topService = data.busiestServices[0];
  setText("metric-service", topService?.name || "—");
  setText(
    "metric-service-count",
    topService ? `${numberFormat.format(topService.count)} events` : "No events in range",
  );
  renderTrend(data.timeline);
  renderSeverities(data.bySeverity, data.totalEvents);
}

function appendCell(row, content, className = "") {
  const cell = document.createElement("td");
  cell.className = className;
  if (content instanceof Node) {
    cell.append(content);
  } else {
    cell.textContent = content;
  }
  row.append(cell);
}

function renderIncidents(data) {
  const body = document.getElementById("incident-rows");
  const empty = document.getElementById("incident-empty");
  clear(body);
  empty.hidden = data.items.length !== 0;
  for (const incident of data.items) {
    const row = document.createElement("tr");
    appendCell(row, badge(incident.severity));

    const title = document.createElement("div");
    title.append(
      textElement("span", "cell-title", incident.title),
      textElement("span", "cell-subtitle", incident.summary),
    );
    appendCell(row, title);
    appendCell(row, compactRule(incident.ruleCode));
    appendCell(row, `${formatInstant(incident.startedAt)} – ${formatInstant(incident.endedAt)}`);
    appendCell(row, numberFormat.format(incident.eventCount));

    const button = textElement("button", "table-action", "Investigate");
    button.type = "button";
    button.addEventListener("click", () => openIncident(incident.id));
    appendCell(row, button);
    body.append(row);
  }
}

async function loadIncidents() {
  const parameters = new URLSearchParams({ size: "50" });
  if (dashboardState.incidentStatus) {
    parameters.set("status", dashboardState.incidentStatus);
  }
  const data = await fetchJson(`/api/v1/incidents?${parameters}`);
  renderIncidents(data);
}

function eventSearchParameters() {
  const form = new FormData(document.getElementById("event-filter"));
  const parameters = new URLSearchParams({ size: "50" });
  for (const [key, value] of form.entries()) {
    if (value.toString().trim()) {
      parameters.set(key, value.toString().trim());
    }
  }
  return parameters;
}

function renderEvents(data) {
  const body = document.getElementById("event-rows");
  const empty = document.getElementById("event-empty");
  clear(body);
  empty.hidden = data.items.length !== 0;
  setText("event-count", `${numberFormat.format(data.totalElements)} matching events`);
  for (const event of data.items) {
    const row = document.createElement("tr");
    appendCell(row, formatInstant(event.occurredAt));
    appendCell(row, badge(event.severity));

    const service = document.createElement("div");
    service.append(
      textElement("span", "cell-title", event.service),
      textElement("span", "cell-subtitle", event.source),
    );
    appendCell(row, service);
    appendCell(row, event.message, "message-cell");
    appendCell(row, event.eventType || "—");
    body.append(row);
  }
}

async function loadEvents() {
  const data = await fetchJson(`/api/v1/log-events?${eventSearchParameters()}`);
  renderEvents(data);
}

function evidenceValue(value) {
  return typeof value === "object" ? JSON.stringify(value) : String(value);
}

async function openIncident(id) {
  const dialog = document.getElementById("incident-dialog");
  document.getElementById("assisted-summary").hidden = true;
  const [incident, timeline] = await Promise.all([
    fetchJson(`/api/v1/incidents/${id}`),
    fetchJson(`/api/v1/incidents/${id}/timeline`),
  ]);
  dashboardState.activeIncident = incident;
  setText("detail-title", incident.title);
  setText("detail-summary", incident.summary);
  setText("status-button", incident.status === "OPEN" ? "Mark resolved" : "Reopen incident");

  const metadata = document.getElementById("detail-meta");
  clear(metadata);
  metadata.append(
    badge(incident.severity),
    textElement("span", "meta-pill", incident.status),
    textElement("span", "meta-pill", compactRule(incident.ruleCode)),
    textElement("span", "meta-pill", `${incident.eventCount} linked events`),
  );

  const evidence = document.getElementById("detail-evidence");
  clear(evidence);
  for (const [key, value] of Object.entries(incident.evidence)) {
    const item = document.createElement("div");
    item.className = "evidence-item";
    const term = textElement("dt", "", compactRule(key));
    const description = textElement("dd", "", evidenceValue(value));
    item.append(term, description);
    evidence.append(item);
  }

  const timelineList = document.getElementById("detail-timeline");
  clear(timelineList);
  for (const event of timeline) {
    const item = document.createElement("li");
    item.append(
      textElement("time", "", formatInstant(event.occurredAt)),
      textElement("strong", "", `${event.severity} · ${event.service} · ${event.source}`),
      textElement("p", "", event.message),
    );
    timelineList.append(item);
  }
  dialog.showModal();
}

async function updateIncidentStatus() {
  const incident = dashboardState.activeIncident;
  if (!incident) {
    return;
  }
  const nextStatus = incident.status === "OPEN" ? "RESOLVED" : "OPEN";
  const updated = await fetchJson(`/api/v1/incidents/${incident.id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status: nextStatus }),
  });
  dashboardState.activeIncident = updated;
  setText("status-button", updated.status === "OPEN" ? "Mark resolved" : "Reopen incident");
  showToast(`Incident marked ${updated.status.toLowerCase()}.`);
  await Promise.all([loadIncidents(), loadStatistics()]);
}

async function generateSummary() {
  const incident = dashboardState.activeIncident;
  if (!incident) {
    return;
  }
  const button = document.getElementById("summary-button");
  button.disabled = true;
  try {
    const result = await fetchJson(`/api/v1/incidents/${incident.id}/summary`, { method: "POST" });
    const output = document.getElementById("assisted-summary");
    output.textContent =
      result.mode === "assisted"
        ? result.summary
        : `${result.summary} (${compactRule(result.fallbackReason)})`;
    output.hidden = false;
  } finally {
    button.disabled = false;
  }
}

async function pollImport(id) {
  const status = document.getElementById("import-status");
  for (let attempt = 0; attempt < 40; attempt += 1) {
    const current = await fetchJson(`/api/v1/log-imports/${id}`);
    status.textContent =
      `${current.status}: ${current.acceptedLines} accepted, ${current.rejectedLines} rejected`;
    if (current.status === "COMPLETED") {
      showToast("Log import completed.");
      await refreshDashboard();
      return;
    }
    if (current.status === "FAILED") {
      throw new Error(current.failureReason || "Log import failed.");
    }
    await new Promise((resolve) => window.setTimeout(resolve, 750));
  }
  status.textContent = "Import is still running. Refresh to check its latest status.";
}

async function submitImport(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const button = form.querySelector("button");
  const status = document.getElementById("import-status");
  button.disabled = true;
  status.textContent = "Uploading log file…";
  try {
    const response = await fetch("/api/v1/log-imports", {
      method: "POST",
      body: new FormData(form),
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      const problem = await response.json();
      throw new Error(problem.detail || "Import submission failed.");
    }
    const submitted = await response.json();
    status.textContent = `Import ${submitted.id} queued.`;
    await pollImport(submitted.id);
    form.reset();
  } catch (error) {
    status.textContent = error.message;
    showToast(error.message);
  } finally {
    button.disabled = false;
  }
}

function showToast(message) {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.hidden = false;
  window.clearTimeout(dashboardState.refreshTimer);
  dashboardState.refreshTimer = window.setTimeout(() => {
    toast.hidden = true;
  }, 4000);
}

async function refreshDashboard() {
  const refresh = document.getElementById("refresh-button");
  refresh.disabled = true;
  try {
    await Promise.all([loadHealth(), loadStatistics(), loadIncidents(), loadEvents()]);
    setText("last-updated", dateFormat.format(new Date()));
  } catch (error) {
    showToast(error.message);
  } finally {
    refresh.disabled = false;
  }
}

document.getElementById("refresh-button").addEventListener("click", refreshDashboard);
document.getElementById("event-filter").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await loadEvents();
  } catch (error) {
    showToast(error.message);
  }
});
document.getElementById("import-form").addEventListener("submit", submitImport);
document.getElementById("close-dialog").addEventListener("click", () => {
  document.getElementById("incident-dialog").close();
});
document.getElementById("status-button").addEventListener("click", updateIncidentStatus);
document.getElementById("summary-button").addEventListener("click", generateSummary);
for (const button of document.querySelectorAll("[data-incident-status]")) {
  button.addEventListener("click", async () => {
    for (const segment of document.querySelectorAll("[data-incident-status]")) {
      segment.classList.remove("active");
    }
    button.classList.add("active");
    dashboardState.incidentStatus = button.dataset.incidentStatus;
    try {
      await loadIncidents();
    } catch (error) {
      showToast(error.message);
    }
  });
}

refreshDashboard();
