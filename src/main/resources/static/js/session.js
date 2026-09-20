(() => {
  'use strict';

  const RETURN_KEY = 'vektrlabs-return-to';
  let state = { status: 'loading', profile: null, missing: false };
  let csrf = null;
  let csrfLoading = null;
  let profileLoading = null;

  function safeReturnPath(value, fallback = '/') {
    if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')
        || /[\u0000-\u0020\\]/.test(value) || /%(?:2f|5c|00|0a|0d)/i.test(value)) {
      return fallback;
    }
    try {
      const url = new URL(value, window.location.origin);
      if (url.origin !== window.location.origin
          || /^\/(?:api|oauth2|login|logout|auth|complete-profile)(?:\/|$)/.test(url.pathname)) {
        return fallback;
      }
      return url.pathname + url.search + url.hash;
    } catch {
      return fallback;
    }
  }

  function currentReturnPath() {
    if (window.location.pathname === '/complete-profile') {
      return safeReturnPath(new URLSearchParams(window.location.search).get('returnTo'));
    }
    return safeReturnPath(window.location.pathname + window.location.search + window.location.hash);
  }

  function rememberReturnPath(value) {
    try { sessionStorage.setItem(RETURN_KEY, safeReturnPath(value)); } catch { /* Storage may be disabled. */ }
  }

  function takeReturnPath() {
    try {
      const path = safeReturnPath(sessionStorage.getItem(RETURN_KEY));
      sessionStorage.removeItem(RETURN_KEY);
      return path;
    } catch {
      return '/';
    }
  }

  function setState(next) {
    state = next;
    renderAccount();
    window.dispatchEvent(new CustomEvent('profile-state-changed', { detail: state }));
    return state;
  }

  function codeOf(body) {
    return body?.errors?.code || body?.code;
  }

  async function bootstrapCsrf() {
    if (csrfLoading) return csrfLoading;
    csrfLoading = (async () => {
      const response = await fetch('/api/csrf', {
        credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' }
      });
      if (!response.ok) throw new Error('Unable to secure this request. Refresh the page and try again.');
      csrf = (await response.json()).data;
      if (!csrf?.token || csrf.headerName !== 'X-XSRF-TOKEN') {
        throw new Error('Invalid CSRF bootstrap response.');
      }
      return csrf;
    })();
    try { return await csrfLoading; } finally { csrfLoading = null; }
  }

  async function refreshProfile() {
    if (profileLoading) return profileLoading;
    setState({ status: 'loading', profile: null, missing: false });
    profileLoading = (async () => {
      try {
        await bootstrapCsrf();
        const response = await fetch('/api/user-profiles/me', {
          credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' }
        });
        const body = await response.json();
        if (response.status === 401 && codeOf(body) === 'AUTHENTICATION_REQUIRED') {
          return setState({ status: 'anonymous', profile: null, missing: false });
        }
        if (response.status === 404 && codeOf(body) === 'PROFILE_NOT_FOUND') {
          return setState({ status: 'incomplete', profile: null, missing: true });
        }
        if (!response.ok || !body.data || typeof body.data !== 'object') {
          throw new Error(body.message || 'Unable to load your profile.');
        }
        const profile = body.data;
        const complete = typeof profile.firstName === 'string' && profile.firstName.trim()
          && typeof profile.lastName === 'string' && profile.lastName.trim();
        return setState({ status: complete ? 'complete' : 'incomplete', profile, missing: false });
      } catch (error) {
        return setState({ status: 'error', profile: null, missing: false, message: error.message });
      }
    })();
    try { return await profileLoading; } finally { profileLoading = null; }
  }

  function login(returnTo = currentReturnPath()) {
    rememberReturnPath(returnTo);
    window.location.assign('/oauth2/authorization/cognito');
  }

  function completeProfile(returnTo = currentReturnPath()) {
    window.location.assign('/complete-profile?returnTo=' + encodeURIComponent(safeReturnPath(returnTo)));
  }

  async function requireCompleteProfile() {
    if (state.status === 'loading' || state.status === 'error') await refreshProfile();
    if (state.status === 'complete') return true;
    if (state.status === 'anonymous') login();
    else if (state.status === 'incomplete') completeProfile();
    else window.alert('Unable to verify your profile. Please retry; you can still browse the catalogue.');
    return false;
  }

  function handleSecurityError(body) {
    const code = codeOf(body);
    if (code === 'AUTHENTICATION_REQUIRED') {
      setState({ status: 'anonymous', profile: null, missing: false });
      login();
    } else if (code === 'PROFILE_NOT_FOUND' || code === 'PROFILE_INCOMPLETE') {
      setState({ status: 'incomplete', profile: null, missing: code === 'PROFILE_NOT_FOUND' });
      completeProfile();
    } else {
      return false;
    }
    return true;
  }

  async function request(path, options = {}) {
    const url = new URL(path, window.location.origin);
    if (url.origin !== window.location.origin) throw new Error('Only same-origin API requests are allowed.');
    const headers = new Headers(options.headers);
    if (!/^(GET|HEAD|OPTIONS)$/i.test(options.method || 'GET')) {
      // Bootstrap for each mutation also handles tokens rotated in another tab.
      const token = await bootstrapCsrf();
      headers.set(token.headerName, token.token);
    }
    const response = await fetch(url.href, { ...options, headers, credentials: 'same-origin' });
    if (!response.ok) {
      const body = await response.clone().json().catch(() => null);
      if (handleSecurityError(body)) {
        const error = new Error(body.message || 'Sign in and complete your profile to continue.');
        error.redirecting = true;
        throw error;
      }
    }
    return response;
  }

  async function logout() {
    try {
      const token = await bootstrapCsrf();
      localStorage.removeItem('vektrlabs-cart-id');
      sessionStorage.removeItem('vektrlabs-pending-order');
      sessionStorage.removeItem(RETURN_KEY);
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = '/logout';
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = token.parameterName;
      input.value = token.token;
      form.appendChild(input);
      document.body.appendChild(form);
      form.submit();
    } catch (error) {
      window.alert(error.message || 'Unable to sign out. Please try again.');
    }
  }

  function themeIconMarkup(lightMode) {
    return `
      <svg viewBox="0 0 24 24" aria-hidden="true" class="theme-icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        ${lightMode ? `
          <path d="M13.2 2.9L6.6 12h4.7l-1 9.1 7.1-10h-4.8l.6-8.2Z"></path>
        ` : `
          <circle cx="12" cy="12" r="9" fill="#2a3139" stroke="none"></circle>
          <circle cx="10.8" cy="13.2" r="5.9" fill="#f5f7fb" stroke="none"></circle>
          <circle cx="13.9" cy="10.7" r="6.2" fill="#2a3139" stroke="none"></circle>
        `}
      </svg>
    `;
  }

  function applyTheme(themeName) {
    const nextTheme = themeName === 'dark' ? 'dark' : 'light';
    document.body.dataset.theme = nextTheme;
    document.documentElement.dataset.theme = nextTheme;
    document.querySelectorAll('[data-theme-toggle]').forEach(button => {
      button.setAttribute('aria-label', nextTheme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode');
      button.innerHTML = themeIconMarkup(nextTheme === 'light');
      button.style.setProperty('--toggle-knob-position', nextTheme === 'dark' ? '18px' : '0px');
    });
    try { localStorage.setItem('vektrlabs-theme', nextTheme); } catch { /* Ignore storage failure. */ }
  }

  function renderAccount() {
    const labels = {
      loading: 'Sign in',
      anonymous: 'Sign in',
      incomplete: 'Sign in',
      complete: 'Sign in',
      error: 'Sign in'
    };
    const profileLabels = {
      loading: 'Login',
      anonymous: 'Login',
      error: 'Login',
      incomplete: 'Profile',
      complete: 'Profile'
    };
    const allowLogout = document.body.dataset.allowLogout === 'true';
    document.querySelectorAll('[data-account-action]').forEach(button => {
      const label = labels[state.status] || labels.complete;
      button.textContent = label;
      button.title = label;
      button.classList.toggle('is-loading', state.status === 'loading');
      if (button instanceof HTMLAnchorElement) {
        button.href = '#';
        button.setAttribute('aria-disabled', state.status === 'loading' ? 'true' : 'false');
      } else {
        button.disabled = state.status === 'loading';
      }
      button.setAttribute('aria-label', label);
    });
    document.querySelectorAll('[data-profile-action]').forEach(button => {
      const label = profileLabels[state.status] || profileLabels.complete;
      button.textContent = label;
      button.title = label;
      button.classList.toggle('is-loading', state.status === 'loading');
      button.setAttribute('aria-label', label);
    });
    document.querySelectorAll('[data-auth-status]').forEach(element => {
      element.textContent = '';
      element.hidden = true;
    });
    document.querySelectorAll('[data-logout]').forEach(button => {
      button.textContent = 'Sign out';
      button.setAttribute('aria-label', 'Sign out');
      button.hidden = !(allowLogout && ['complete', 'incomplete'].includes(state.status));
    });
  }

  function initThemeToggle() {
    const savedTheme = localStorage.getItem('vektrlabs-theme') === 'dark' ? 'dark' : 'light';
    applyTheme(savedTheme);
    document.querySelectorAll('[data-theme-toggle]').forEach(button => {
      button.addEventListener('click', () => {
        const nextTheme = document.body.dataset.theme === 'dark' ? 'light' : 'dark';
        applyTheme(nextTheme);
      });
    });
  }

  function bindAccountControls(returnTo) {
    document.querySelectorAll('[data-account-action]').forEach(button => {
      button.addEventListener('click', event => {
        event.preventDefault();
        if (state.status === 'anonymous') login(returnTo);
        else if (state.status === 'error') refreshProfile();
        else if (state.status !== 'loading') completeProfile(returnTo);
      });
    });
    document.querySelectorAll('[data-profile-action]').forEach(button => {
      button.addEventListener('click', event => {
        event.preventDefault();
        if (state.status === 'loading') return;
        if (state.status === 'anonymous' || state.status === 'error') login(returnTo);
        else completeProfile(returnTo);
      });
    });
    document.querySelectorAll('[data-logout]').forEach(button => button.addEventListener('click', logout));
    renderAccount();
  }

  initThemeToggle();

  window.SessionAuth = {
    get state() { return state; },
    safeReturnPath, currentReturnPath, takeReturnPath, login, completeProfile,
    refreshProfile, requireCompleteProfile, request, handleSecurityError, logout,
    bindAccountControls, applyTheme, initThemeToggle
  };
})();
