'use strict';

// Dependency-free checks run with the existing Node runtime: node src\test\js\session.test.js
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const resources = path.resolve(__dirname, '..', '..', 'main', 'resources');
const sessionScript = fs.readFileSync(path.join(resources, 'static', 'js', 'session.js'), 'utf8');
const completeProfile = { id: 'profile-id', firstName: 'First', lastName: 'Last', middleName: '' };

function response(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

function harness(profileResponse = () => response({ data: completeProfile })) {
  const calls = [];
  const redirects = [];
  const alerts = [];
  const storage = new Map();
  let mutationResponse = () => response({ data: 'ok' });
  let tokenNumber = 0;
  const sandbox = {
    URL, URLSearchParams, Headers,
    CustomEvent: class { constructor(type, options) { this.type = type; this.detail = options.detail; } },
    document: { querySelectorAll: () => [] },
    sessionStorage: {
      getItem: key => storage.get(key), setItem: (key, value) => storage.set(key, value),
      removeItem: key => storage.delete(key)
    },
    window: {
      location: { origin: 'https://shop.example', pathname: '/demo', search: '?category=shirts', hash: '',
        assign: url => redirects.push(url) },
      dispatchEvent: () => {}, alert: text => alerts.push(text)
    },
    fetch: async (url, options) => {
      calls.push({ url, options });
      if (url === '/api/csrf') {
        return response({ data: { token: `token-${++tokenNumber}`, headerName: 'X-XSRF-TOKEN', parameterName: '_csrf' } });
      }
      if (url === '/api/user-profiles/me') return profileResponse();
      return mutationResponse();
    }
  };
  vm.createContext(sandbox);
  vm.runInContext(sessionScript, sandbox);
  return { auth: sandbox.window.SessionAuth, calls, redirects, alerts, storage, sandbox,
    setMutation: callback => { mutationResponse = callback; } };
}

async function run() {
  let checks = 0;
  const check = async (name, test) => {
    await test();
    checks++;
    console.log(`PASS ${name}`);
  };

  await check('safe same-origin returns reject external, encoded and auth endpoints', () => {
    const { auth } = harness();
    for (const unsafe of ['https://evil.example/', '//evil.example/', '/\\evil.example', '/%2Fevil.example',
      '/%5Cevil.example', '/\nevil.example', '/api/private', '/oauth2/authorization/cognito', '/logout',
      '/complete-profile', '/auth/continue']) {
      assert.equal(auth.safeReturnPath(unsafe), '/');
    }
    assert.equal(auth.safeReturnPath('/demo?category=shirts#shop'), '/demo?category=shirts#shop');
  });

  await check('loading blocks action until a complete profile is loaded', async () => {
    const { auth, redirects } = harness();
    assert.equal(auth.state.status, 'loading');
    assert.equal(await auth.requireCompleteProfile(), true);
    assert.equal(auth.state.status, 'complete');
    assert.equal(redirects.length, 0);
  });

  await check('anonymous action initiates login without replaying mutation', async () => {
    const h = harness(() => response({ errors: { code: 'AUTHENTICATION_REQUIRED' } }, 401));
    assert.equal(await h.auth.requireCompleteProfile(), false);
    assert.equal(h.auth.state.status, 'anonymous');
    assert.deepEqual(h.redirects, ['/oauth2/authorization/cognito']);
    assert.equal(h.storage.get('vektrlabs-return-to'), '/demo?category=shirts');
    assert.equal(h.calls.length, 2);
  });

  await check('missing profile goes to create onboarding', async () => {
    const h = harness(() => response({ errors: { code: 'PROFILE_NOT_FOUND' } }, 404));
    assert.equal(await h.auth.requireCompleteProfile(), false);
    assert.equal(h.auth.state.missing, true);
    assert.equal(h.redirects[0], '/complete-profile?returnTo=%2Fdemo%3Fcategory%3Dshirts');
  });

  await check('blank names require onboarding but optional middle name does not', async () => {
    for (const profile of [{ ...completeProfile, firstName: ' ' }, { ...completeProfile, lastName: '' }]) {
      const h = harness(() => response({ data: profile }));
      await h.auth.refreshProfile();
      assert.equal(h.auth.state.status, 'incomplete');
      assert.equal(h.auth.state.missing, false);
    }
    const h = harness();
    assert.equal((await h.auth.refreshProfile()).status, 'complete');
  });

  await check('network, server and unrelated not-found failures never count as signed in', async () => {
    for (const result of [
      () => { throw new Error('Network unavailable'); },
      () => response({ message: 'Server failure' }, 500),
      () => response({ message: 'Not found' }, 404),
      () => response({ data: null })
    ]) {
      const h = harness(result);
      assert.equal(await h.auth.requireCompleteProfile(), false);
      assert.equal(h.auth.state.status, 'error');
      assert.equal(h.redirects.length, 0);
      assert.equal(h.alerts.length, 1);
    }
  });

  await check('every mutation including anonymous chat sends a fresh CSRF header', async () => {
    const h = harness();
    for (const [index, method] of ['POST', 'PUT', 'DELETE'].entries()) {
      await h.auth.request('/api/v1/ai/chat', { method });
      const call = h.calls.at(-1);
      assert.equal(call.options.credentials, 'same-origin');
      assert.equal(call.options.headers.get('X-XSRF-TOKEN'), `token-${index + 1}`);
    }
    assert.equal(h.redirects.length, 0);
  });

  await check('no token-bearing cross-origin request is made', async () => {
    const h = harness();
    await assert.rejects(h.auth.request('https://evil.example/', { method: 'POST' }), /same-origin/);
    assert.equal(h.calls.length, 0);
  });

  await check('backend and outer AI profile errors redirect before normal 404 handling', async () => {
    for (const [code, status] of [['PROFILE_NOT_FOUND', 404], ['PROFILE_INCOMPLETE', 403],
      ['AUTHENTICATION_REQUIRED', 401]]) {
      const h = harness();
      h.setMutation(() => response({ errors: { code } }, status));
      await assert.rejects(h.auth.request('/api/v1/ai/chat', { method: 'POST' }), error => error.redirecting);
      assert.equal(h.redirects.length, 1);
      assert.equal(h.calls.length, 2);
    }
  });

  await check('generic access-denied, CSRF and not-found errors do not trigger onboarding', async () => {
    for (const [code, status] of [['ACCESS_DENIED', 403], ['CSRF_INVALID', 403], ['NOT_FOUND', 404]]) {
      const h = harness();
      h.setMutation(() => response({ errors: { code } }, status));
      assert.equal((await h.auth.request('/api/private', { method: 'DELETE' })).status, status);
      assert.equal(h.redirects.length, 0);
      assert.equal(h.calls.length, 2);
    }
  });

  await check('return storage is revalidated and consumed once', () => {
    const h = harness();
    h.storage.set('vektrlabs-return-to', '//evil.example/');
    assert.equal(h.auth.takeReturnPath(), '/');
    h.storage.set('vektrlabs-return-to', '/demo');
    assert.equal(h.auth.takeReturnPath(), '/demo');
    assert.equal(h.auth.takeReturnPath(), '/');
  });

  await check('completion form creates missing and updates existing profiles, then rechecks state', async () => {
    for (const missing of [true, false]) {
      let reads = 0;
      const h = harness(() => {
        reads++;
        return reads > 1 ? response({ data: completeProfile })
          : missing ? response({ errors: { code: 'PROFILE_NOT_FOUND' } }, 404)
            : response({ data: { ...completeProfile, firstName: '' } });
      });
      let submit;
      const form = {
        elements: { firstName: { value: '' }, lastName: { value: '' }, middleName: { value: '' } },
        addEventListener: (type, handler) => { submit = handler; }
      };
      const fields = { disabled: true };
      const nodes = {
        '#profileForm': form, '#profileFields': fields, '#profileMessage': {},
        '#retryProfile': { addEventListener: () => {} }, '#backToStore': {}
      };
      h.sandbox.document.querySelector = selector => nodes[selector];
      vm.runInContext(fs.readFileSync(path.join(resources, 'static', 'js', 'complete-profile.js'), 'utf8'), h.sandbox);
      await new Promise(setImmediate);
      assert.equal(fields.disabled, false);
      form.elements.firstName.value = ' First ';
      form.elements.lastName.value = 'Last ';
      form.elements.middleName.value = ' ';
      await submit({ preventDefault: () => {} });
      const mutation = h.calls.find(call => /^(POST|PUT)$/.test(call.options.method));
      assert.equal(mutation.options.method, missing ? 'POST' : 'PUT');
      assert.equal(mutation.url, 'https://shop.example/api/user-profiles' + (missing ? '' : '/me'));
      assert.deepEqual(JSON.parse(mutation.options.body), { firstName: 'First', lastName: 'Last', middleName: '' });
      assert.equal(reads, 2);
      assert.equal(h.auth.state.status, 'complete');
      assert.deepEqual(h.redirects, ['/demo?category=shirts']);
    }
  });

  await check('all frontend scripts parse and storefront no longer supplies email identity', () => {
    for (const file of fs.readdirSync(path.join(resources, 'static', 'js'))) {
      new vm.Script(fs.readFileSync(path.join(resources, 'static', 'js', file), 'utf8'), { filename: file });
    }
    const index = fs.readFileSync(path.join(resources, 'templates', 'index.html'), 'utf8');
    assert.doesNotMatch(index, /customerEmail|TEST_CUSTOMER_EMAIL|\bfetch\(/);
    for (const match of index.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)) {
      new vm.Script(match[1], { filename: 'index.html script' });
    }
  });

  console.log(`${checks} frontend checks passed.`);
}

run().catch(error => { console.error(error); process.exitCode = 1; });
