/* Shared helpers for all portal pages. */
const CATEGORIES = ["ID Card", "Phone", "Wallet", "Book", "Bag", "Keys", "Bottle", "Umbrella", "Electronics", "Other"];
const LOCATIONS = ["Library", "Canteen", "Main Gate", "Lab Block", "Hostel", "Playground", "Admin Block", "Auditorium", "Parking"];

let currentUser = null;

async function api(url, options = {}) {
  const res = await fetch(url, { headers: { "Content-Type": "application/json" }, ...options });
  if (res.status === 401) throw new Error("LOGIN_REQUIRED");
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { /* not json */ }
  if (!res.ok) throw new Error((data && data.error) || ("Request failed: " + res.status));
  return data;
}
const apiGet = (url) => api(url);
const apiPost = (url, body) => api(url, { method: "POST", body: body ? JSON.stringify(body) : undefined });

async function loadCurrentUser() {
  try { currentUser = await apiGet("/api/auth/me"); }
  catch (e) { currentUser = null; }
  return currentUser;
}

function requireAuth() {
  if (!currentUser) { window.location.href = "/login.html"; return false; }
  return true;
}

function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function fmtDate(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  return d.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })
       + ", " + d.toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit" });
}

function itemCard(item, extra = "") {
  const img = item.image
    ? `<img src="${esc(item.image)}" class="card-img-top" alt="${esc(item.title)}">`
    : `<img src="" class="card-img-top" alt="">`;
  return `
  <div class="col-md-4 mb-4">
    <div class="card item-card">
      ${img}
      <div class="card-body">
        <div class="d-flex justify-content-between mb-2">
          <span class="badge ${item.type === "LOST" ? "badge-lost" : "badge-found"}">${item.type}</span>
          <span class="badge badge-status-${item.status}">${item.status}</span>
        </div>
        <h5 class="card-title">${esc(item.title)}</h5>
        <p class="card-text text-muted mb-1">${esc(item.category)} &middot; ${esc(item.location || "-")}</p>
        <p class="card-text">${esc((item.description || "").slice(0, 90))}${item.description && item.description.length > 90 ? "..." : ""}</p>
        ${extra}
        <a href="/item.html?id=${item.itemId}" class="btn btn-brand btn-sm">View details</a>
      </div>
      <div class="card-footer text-muted" style="font-size:.82rem">
        Reported by ${esc(item.reportedByName)} &middot; ${fmtDate(item.createdAt)}
      </div>
    </div>
  </div>`;
}

/* Builds the top navigation bar; highlights unread notification count. */
async function renderNavbar(active) {
  const el = document.getElementById("app-navbar");
  if (!el) return;
  await loadCurrentUser();
  let unread = 0;
  if (currentUser) {
    try { unread = (await apiGet("/api/notifications/unread-count")).count; } catch (e) {}
  }
  const link = (href, label, key, extra = "") =>
    `<li class="nav-item"><a class="nav-link ${active === key ? "active fw-semibold" : ""}" href="${href}">${label}${extra}</a></li>`;
  el.innerHTML = `
  <nav class="navbar navbar-expand-lg navbar-dark bg-brand">
    <div class="container">
      <a class="navbar-brand" href="/index.html">&#128269; Lost &amp; Found</a>
      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navMenu">
        <ul class="navbar-nav me-auto">
          ${link("/index.html", "Home", "home")}
          ${link("/browse.html", "Browse &amp; Search", "browse")}
          ${link("/report.html", "Report Item", "report")}
          ${link("/map.html", "Campus Map", "map")}
          ${currentUser ? link("/my-reports.html", "My Reports", "myreports") : ""}
          ${currentUser ? link("/my-claims.html", "My Claims", "myclaims") : ""}
          ${currentUser ? link("/verify.html", "Verify Handover", "verify") : ""}
          ${currentUser ? link("/notifications.html", "Notifications", "notifications",
              unread > 0 ? ` <span class="badge bg-warning text-dark">${unread}</span>` : "") : ""}
          ${currentUser && currentUser.role === "ADMIN" ? link("/admin.html", "Admin", "admin") : ""}
        </ul>
        <ul class="navbar-nav">
          ${currentUser
            ? `<li class="nav-item"><span class="nav-link">Hi, ${esc(currentUser.name)}</span></li>
               <li class="nav-item">
                 <form method="post" action="/logout" class="d-inline">
                   <button class="btn btn-outline-light btn-sm mt-1" type="submit">Logout</button>
                 </form>
               </li>`
            : `<li class="nav-item"><a class="nav-link" href="/login.html">Login</a></li>
               <li class="nav-item"><a class="btn btn-accent btn-sm mt-1" href="/register.html">Register</a></li>`}
        </ul>
      </div>
    </div>
  </nav>`;
}

function pageFooter() {
  const el = document.getElementById("app-footer");
  if (el) el.innerHTML = `
  <footer class="container mt-5 mb-4">
    <hr>
    <div class="d-flex justify-content-between flex-wrap">
      <span>Lost &amp; Found - Smart Campus Asset Recovery Platform</span>
      <span>Team Four Founders &middot; GL Bajaj Institute of Technology and Management, Greater Noida</span>
    </div>
  </footer>`;
}
