export interface User {
  id: string;
  email: string;
  name: string;
  firstName?: string;
  lastName?: string;
  middleName?: string;
  birthDate?: string;
  token: string;
  roles: string[];
}

interface CurrentUserProfileResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  middleName: string;
  birthDate: string;
  roles: string[];
}

export interface RegistrationPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  middleName?: string;
  birthDate?: string;
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

type AuthAction = 'login' | 'register' | 'refresh';
type ProfileLoadAction = 'login' | 'register';
type AuthStateListener = (user: User | null) => void;

const ACCESS_TOKEN_KEY = 'cc_access_token';
const REFRESH_TOKEN_KEY = 'cc_refresh_token';
const EXPIRES_AT_KEY = 'cc_access_token_expires_at';
const authStateListeners = new Set<AuthStateListener>();
let refreshPromise: Promise<User | null> | null = null;

function getAccessToken(): string | undefined {
  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? undefined;
}

function getRefreshToken(): string | undefined {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? undefined;
}

function getExpiresAt(): number | undefined {
  const rawValue = localStorage.getItem(EXPIRES_AT_KEY);
  if (!rawValue) {
    return undefined;
  }

  const expiresAt = Number(rawValue);
  return Number.isFinite(expiresAt) ? expiresAt : undefined;
}

function saveTokens(tokens: TokenResponse): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  localStorage.setItem(EXPIRES_AT_KEY, String(Date.now() + tokens.expiresIn * 1000));
}

function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(EXPIRES_AT_KEY);
}

function emitAuthState(user: User | null): void {
  authStateListeners.forEach((listener) => listener(user));
}

function isTokenExpiringSoon(minValidity: number): boolean {
  const expiresAt = getExpiresAt();
  if (!expiresAt) {
    return true;
  }

  return expiresAt - Date.now() <= minValidity * 1000;
}

function toUser(profile: CurrentUserProfileResponse, token: string): User {
  const name = [profile.firstName, profile.lastName].filter(Boolean).join(' ').trim();

  return {
    id: profile.id,
    email: profile.email,
    name: name || profile.email,
    firstName: profile.firstName || '',
    lastName: profile.lastName || '',
    middleName: profile.middleName || '',
    birthDate: profile.birthDate || '',
    token,
    roles: profile.roles || [],
  };
}

async function parseResponse(response: Response, action: AuthAction): Promise<TokenResponse> {
  if (!response.ok) {
    if (action === 'login' && response.status === 401) {
      throw new Error('Неверный логин или пароль');
    }
    if (action === 'register' && response.status === 409) {
      throw new Error('Пользователь с таким email уже существует');
    }
    if (action === 'register' && response.status === 400) {
      throw new Error('Проверьте корректность регистрационных данных');
    }
    if (action === 'refresh' && response.status === 401) {
      throw new Error('Не удалось обновить сессию');
    }

    throw new Error(
      action === 'register'
        ? 'Ошибка регистрации. Попробуйте снова'
        : 'Ошибка авторизации. Попробуйте снова',
    );
  }

  return response.json() as Promise<TokenResponse>;
}

async function parseProfileResponse(response: Response): Promise<CurrentUserProfileResponse> {
  if (!response.ok) {
    throw new Error('PROFILE_FETCH_FAILED');
  }

  return response.json() as Promise<CurrentUserProfileResponse>;
}

async function fetchCurrentUserProfile(token: string): Promise<User> {
  const response = await fetch('/api/auth/me', {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  const profile = await parseProfileResponse(response);
  return toUser(profile, token);
}

function getProfileLoadErrorMessage(action: ProfileLoadAction): string {
  return action === 'register'
    ? 'Аккаунт создан, но профиль не удалось загрузить. Попробуйте войти.'
    : 'Вход выполнен, но профиль не удалось загрузить. Попробуйте войти снова.';
}

async function loadCurrentUserAfterAuth(
  tokens: TokenResponse,
  action: ProfileLoadAction,
): Promise<User> {
  saveTokens(tokens);

  try {
    const user = await fetchCurrentUserProfile(tokens.accessToken);
    emitAuthState(user);
    return user;
  } catch {
    clearTokens();
    emitAuthState(null);
    throw new Error(getProfileLoadErrorMessage(action));
  }
}

async function refreshSession(): Promise<User | null> {
  if (refreshPromise) {
    return refreshPromise;
  }

  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    clearTokens();
    emitAuthState(null);
    return null;
  }

  refreshPromise = (async () => {
    try {
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
      const tokens = await parseResponse(response, 'refresh');
      saveTokens(tokens);

      const user = await fetchCurrentUserProfile(tokens.accessToken);
      emitAuthState(user);
      return user;
    } catch {
      clearTokens();
      emitAuthState(null);
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

const AuthService = {
  async init(): Promise<User | null> {
    const token = getAccessToken();
    if (!token && !getRefreshToken()) {
      emitAuthState(null);
      return null;
    }

    if (!token || isTokenExpiringSoon(30)) {
      return refreshSession();
    }

    try {
      const user = await fetchCurrentUserProfile(token);
      emitAuthState(user);
      return user;
    } catch {
      return refreshSession();
    }
  },

  async login(username: string, password: string): Promise<User | null> {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const tokens = await parseResponse(response, 'login');
    return loadCurrentUserAfterAuth(tokens, 'login');
  },

  async register(payload: RegistrationPayload): Promise<User | null> {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const tokens = await parseResponse(response, 'register');
    return loadCurrentUserAfterAuth(tokens, 'register');
  },

  async logout(): Promise<void> {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      await fetch('/api/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
    }

    clearTokens();
    emitAuthState(null);
  },

  async getToken(minValidity: number = 30): Promise<string | undefined> {
    const token = getAccessToken();
    if (token && !isTokenExpiringSoon(minValidity)) {
      return token;
    }

    const refreshedUser = await refreshSession();
    return refreshedUser?.token;
  },

  async refreshToken(minValidity: number = 30): Promise<boolean> {
    const token = getAccessToken();
    if (token && !isTokenExpiringSoon(minValidity)) {
      return true;
    }

    return (await refreshSession()) !== null;
  },

  isAuthenticated(): boolean {
    return !!getAccessToken();
  },

  subscribe(listener: AuthStateListener): () => void {
    authStateListeners.add(listener);

    return () => {
      authStateListeners.delete(listener);
    };
  },
};

export default AuthService;
