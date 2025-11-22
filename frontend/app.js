// Change this if your daemon runs on a different host/port
const API_BASE = "http://127.0.0.1:8080";

const healthIndicator = document.getElementById("health-indicator");
const healthText = document.getElementById("health-text");

const form = document.getElementById("assign-form");
const resultTreatment = document.getElementById("result-treatment");
const resultDecision = document.getElementById("result-decision");
const resultVersion = document.getElementById("result-version");
const resultTs = document.getElementById("result-ts");
const resultRaw = document.getElementById("result-raw");
const historyList = document.getElementById("history-list");

const history = [];

async function checkHealth() {
    try {
        const res = await fetch(`${API_BASE}/health`);
        if (!res.ok) throw new Error("HTTP " + res.status);
        const data = await res.json();
        if (data.status === "SERVING") {
            healthIndicator.className = "health health-ok";
            healthText.textContent = "SERVING";
        } else {
            healthIndicator.className = "health health-bad";
            healthText.textContent = data.status || "UNKNOWN";
        }
    } catch (err) {
        console.error("Health check failed:", err);
        healthIndicator.className = "health health-bad";
        healthText.textContent = "DOWN";
    }
}

function buildQuery(params) {
    const usp = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && String(v).trim() !== "") {
            usp.append(k, v);
        }
    });
    return usp.toString();
}

async function requestAssignment(event) {
    event.preventDefault();

    const exp = document.getElementById("exp").value.trim();
    const user = document.getElementById("user").value.trim();
    const device = document.getElementById("device").value.trim();
    const req = document.getElementById("req").value.trim();
    const country = document.getElementById("country").value.trim();
    const appVer = document.getElementById("app_ver").value.trim();

    const qs = buildQuery({
        exp,
        user,
        device,
        req,
        country,
        app_ver: appVer,
    });

    try {
        const res = await fetch(`${API_BASE}/assign?` + qs);
        const data = await res.json();

        // update main view
        resultTreatment.textContent = data.treatment ?? "–";
        resultDecision.textContent = data.decision ?? "–";
        resultVersion.textContent = data.configVersion ?? "–";
        resultTs.textContent = data.ts ?? new Date().toISOString();
        resultRaw.textContent = JSON.stringify(data, null, 2);

        // push to history
        const entry = {
            ts: data.ts ?? new Date().toISOString(),
            exp: data.experiment,
            user: user || device || req || "(anon)",
            treatment: data.treatment,
            decision: data.decision,
        };
        history.unshift(entry);
        if (history.length > 10) history.pop();
        renderHistory();
    } catch (err) {
        console.error("Assignment request failed:", err);
        resultRaw.textContent = "Error: " + err.message;
    }
}

function renderHistory() {
    historyList.innerHTML = "";
    history.forEach((h) => {
        const li = document.createElement("li");
        li.textContent = `[${h.ts}] exp=${h.exp}, user=${h.user}, treatment=${h.treatment}, decision=${h.decision}`;
        historyList.appendChild(li);
    });
}

// initial wiring
form.addEventListener("submit", requestAssignment);
checkHealth();
setInterval(checkHealth, 5000);
