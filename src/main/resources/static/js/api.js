// Small shared helpers for every page: session storage, authenticated fetch, formatting.

const Session = {
  get token() { return localStorage.getItem('token'); },
  get role() { return localStorage.getItem('role'); },
  get name() { return localStorage.getItem('name'); },
  save(data) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('role', data.role);
    localStorage.setItem('name', data.name);
  },
  clear() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('name');
  },
  /** Send the user to login unless they are signed in with the expected role. */
  require(role) {
    if (!this.token || this.role !== role) {
      window.location.replace('login.html');
      return false;
    }
    return true;
  },
  logout() {
    this.clear();
    window.location.replace('login.html');
  }
};

/**
 * fetch() wrapper: adds the JWT, parses JSON, and turns API errors into
 * thrown Error objects carrying the server's message.
 */
async function api(path, { method = 'GET', body, raw = false } = {}) {
  const headers = {};
  if (Session.token) headers['Authorization'] = 'Bearer ' + Session.token;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  });

  if (res.status === 401 && !path.startsWith('/api/auth/')) {
    Session.logout(); // token expired
    throw new Error('Your session expired. Please log in again.');
  }

  if (!res.ok) {
    let message = 'Request failed (' + res.status + ')';
    try {
      const data = await res.json();
      if (data && data.error) message = data.error;
    } catch (_) { /* non-JSON error */ }
    throw new Error(message);
  }

  if (raw) return res;
  if (res.status === 204) return null;
  return res.json();
}

function showMsg(el, text, kind = 'error') {
  el.textContent = text;
  el.className = 'msg show ' + kind;
}

function hideMsg(el) {
  el.className = 'msg';
  el.textContent = '';
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}

const fmt = {
  day: iso => new Date(iso).getDate(),
  month: iso => new Date(iso).toLocaleString('en-IN', { month: 'short' }),
  dateTime: iso => new Date(iso).toLocaleString('en-IN', {
    weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit'
  }),
  time: iso => new Date(iso).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' })
};

function fillTopbar() {
  const nameEl = document.getElementById('who-name');
  if (nameEl) nameEl.textContent = Session.name || '';
  const out = document.getElementById('logout');
  if (out) out.addEventListener('click', () => Session.logout());
}
