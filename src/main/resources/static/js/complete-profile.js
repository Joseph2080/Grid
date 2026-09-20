(() => {
  'use strict';
  const auth = window.SessionAuth;
  const form = document.querySelector('#profileForm');
  const fields = document.querySelector('#profileFields');
  const message = document.querySelector('#profileMessage');
  const retry = document.querySelector('#retryProfile');
  const returnTo = (() => {
    const fromQuery = auth.currentReturnPath();
    if (fromQuery && fromQuery !== '/') return fromQuery;
    if (document.referrer) {
      try {
        const referrer = new URL(document.referrer);
        if (referrer.origin === window.location.origin) {
          return auth.safeReturnPath(referrer.pathname + referrer.search + referrer.hash);
        }
      } catch {
        // Ignore malformed referrer and use fallback.
      }
    }
    return fromQuery || '/';
  })();
  let profileState;

  document.querySelector('#backToStore').href = returnTo;
  auth.bindAccountControls();

  async function load() {
    fields.disabled = true;
    retry.hidden = true;
    message.textContent = 'Loading your profile...';
    profileState = await auth.refreshProfile();
    if (profileState.status === 'anonymous') {
      message.textContent = 'Sign in before completing your profile.';
      return;
    }
    if (profileState.status === 'error') {
      message.textContent = profileState.message || 'Unable to load your profile. Please retry.';
      retry.hidden = false;
      return;
    }
    const profile = profileState.profile || {};
    for (const name of ['firstName', 'lastName', 'middleName']) {
      form.elements[name].value = profile[name] || '';
    }
    fields.disabled = false;
    message.textContent = profileState.missing
      ? 'Add your name to start shopping. Your email comes from your secure sign-in.'
      : 'Confirm your first and last name to continue.';
  }

  retry.addEventListener('click', load);
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (!profileState || !['incomplete', 'complete'].includes(profileState.status)) return;
    const payload = {};
    for (const name of ['firstName', 'lastName', 'middleName']) {
      payload[name] = form.elements[name].value.trim();
    }
    if (!payload.firstName || !payload.lastName) {
      message.textContent = 'First and last name are required.';
      return;
    }
    fields.disabled = true;
    message.textContent = 'Saving your profile...';
    try {
      const response = await auth.request(
        profileState.missing ? '/api/user-profiles' : '/api/user-profiles/me',
        {
          method: profileState.missing ? 'POST' : 'PUT',
          headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
          body: JSON.stringify(payload)
        }
      );
      const body = await response.json();
      if (!response.ok) throw new Error(body.message || 'Unable to save your profile.');
      profileState = await auth.refreshProfile();
      if (profileState.status !== 'complete') {
        throw new Error('Your profile could not be verified. Reload it before trying again.');
      }
      window.location.assign(returnTo);
    } catch (error) {
      if (error.redirecting) return;
      message.textContent = error.message || 'Unable to save your profile.';
      // Reload after an uncertain save rather than replaying a create request.
      retry.hidden = false;
    }
  });

  load();
})();
