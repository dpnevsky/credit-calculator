import assert from 'node:assert/strict';
import { beforeEach, test } from 'node:test';
import AuthService from '../src/services/auth.service.ts';

const tokens = { accessToken: 'access', refreshToken: 'refresh', expiresIn: 300, tokenType: 'Bearer' };
const profile = { id: 'user', email: 'owner@example.com', firstName: 'Ivan', lastName: 'Ivanov', roles: [] };
const json = (body) => new Response(JSON.stringify(body), { status: 200 });

beforeEach(() => {
  const values = new Map();
  globalThis.localStorage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: (key) => values.delete(key),
  };
});

async function login() {
  globalThis.fetch = async (url) => json(url.endsWith('/me') ? profile : tokens);
  await AuthService.login('owner@example.com', 'password');
}

test('logout clears tokens and personal drafts when the server is unreachable', async () => {
  await login();
  localStorage.setItem('cc_create_application_form_draft', '{"passportNumber":"123456"}');
  globalThis.fetch = async () => { throw new TypeError('Network unavailable'); };
  const states = [];
  const unsubscribe = AuthService.subscribe((user) => states.push(user));
  try {
    await AuthService.logout();
    assert.equal(AuthService.isAuthenticated(), false);
    assert.equal(localStorage.getItem('cc_refresh_token'), null);
    assert.equal(localStorage.getItem('cc_create_application_form_draft'), null);
    assert.deepEqual(states, [null]);
  } finally {
    unsubscribe();
  }
});

test('an in-flight refresh cannot restore a session after logout', async () => {
  await login();
  localStorage.setItem('cc_access_token_expires_at', '0');
  let finishRefresh;
  const response = new Promise((resolve) => { finishRefresh = resolve; });
  globalThis.fetch = async (url) => url.endsWith('/refresh') ? response : json(profile);
  const refreshing = AuthService.getToken();
  await AuthService.logout();
  finishRefresh(json(tokens));
  assert.equal(await refreshing, undefined);
  assert.equal(AuthService.isAuthenticated(), false);
  assert.equal(localStorage.getItem('cc_refresh_token'), null);
});

test('parallel protected requests share a single token refresh', async () => {
  await login();
  localStorage.setItem('cc_access_token_expires_at', '0');
  let refreshCount = 0;
  globalThis.fetch = async (url) => {
    if (url.endsWith('/refresh')) {
      refreshCount += 1;
      return json(tokens);
    }
    return json(profile);
  };
  const results = await Promise.all([AuthService.getToken(), AuthService.getToken(), AuthService.getToken()]);
  assert.deepEqual(results, ['access', 'access', 'access']);
  assert.equal(refreshCount, 1);
});
