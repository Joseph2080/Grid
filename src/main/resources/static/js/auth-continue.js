(() => {
  'use strict';
  const auth = window.SessionAuth;
  const message = document.querySelector('#continueMessage');
  const retry = document.querySelector('#retryContinue');
  const returnTo = auth.takeReturnPath();
  auth.bindAccountControls(returnTo);

  async function continueAfterLogin() {
    retry.hidden = true;
    const state = await auth.refreshProfile();
    if (state.status === 'complete') window.location.replace(returnTo);
    else if (state.status === 'incomplete') auth.completeProfile(returnTo);
    else {
      message.textContent = state.status === 'anonymous'
        ? 'Sign-in did not complete. Please sign in again.'
        : 'Unable to check your profile. You can retry or continue browsing.';
      retry.hidden = state.status !== 'error';
    }
  }

  document.querySelector('#backToStore').href = returnTo;
  retry.addEventListener('click', continueAfterLogin);
  continueAfterLogin();
})();
