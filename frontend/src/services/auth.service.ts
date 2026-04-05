import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'credit-calculator',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'credit-calculator-app',
});

export interface User {
  id: string;
  email: string;
  name: string;
  token: string;
  roles: string[];
}

function extractUser(): User | null {
  if (!keycloak.authenticated || !keycloak.tokenParsed) {
    return null;
  }

  const tp = keycloak.tokenParsed;
  return {
    id: tp.sub || 'unknown',
    email: (tp.email as string) || (tp.preferred_username as string) || '',
    name: (tp.given_name as string) ||
          (tp.name as string) ||
          (tp.preferred_username as string) ||
          '',
    token: keycloak.token || '',
    roles: (tp.realm_access?.roles as string[]) || [],
  };
}

let initPromise: Promise<boolean> | null = null;

const AuthService = {
  keycloak,

  async init(): Promise<User | null> {
    if (!initPromise) {
      initPromise = keycloak.init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        checkLoginIframe: false,
        pkceMethod: 'S256',
      });
    }

    try {
      await initPromise;
    } catch (err) {
      console.error('Keycloak init failed:', err);
    }

    return extractUser();
  },

  async login(): Promise<void> {
    await keycloak.login({
      redirectUri: window.location.origin + '/',
    });
  },

  async register(): Promise<void> {
    await keycloak.register({
      redirectUri: window.location.origin + '/',
    });
  },

  async logout(): Promise<void> {
    await keycloak.logout({
      redirectUri: window.location.origin + '/',
    });
  },

  getToken(): string | undefined {
    return keycloak.token;
  },

  async refreshToken(minValidity: number = 30): Promise<boolean> {
    try {
      const refreshed = await keycloak.updateToken(minValidity);
      return refreshed;
    } catch {
      return false;
    }
  },

  getCurrentUser(): User | null {
    return extractUser();
  },

  isAuthenticated(): boolean {
    return !!keycloak.authenticated;
  },

  onTokenExpired(callback: () => void): void {
    keycloak.onTokenExpired = callback;
  },
};

export default AuthService;
