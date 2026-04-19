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

const ACCESS_TOKEN_KEY = 'cc_access_token';
const REFRESH_TOKEN_KEY = 'cc_refresh_token';
let currentUser: User | null = null;

function getAccessToken(): string | undefined {
  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? undefined;
}

function getRefreshToken(): string | undefined {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? undefined;
}

function saveTokens(tokens: TokenResponse): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
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

async function parseResponse(response: Response): Promise<TokenResponse> {
  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Неверный логин или пароль');
    }
    if (response.status === 409) {
      throw new Error('Пользователь с таким email уже существует');
    }
    throw new Error('Ошибка авторизации. Попробуйте снова');
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
  const user = toUser(profile, token);
  currentUser = user;
  return user;
}

const AuthService = {
  async init(): Promise<User | null> {
    const token = getAccessToken();
    if (!token) {
      currentUser = null;
      return null;
    }

    try {
      return await fetchCurrentUserProfile(token);
    } catch {
      clearTokens();
      currentUser = null;
      return null;
    }
  },

  async login(username: string, password: string): Promise<User | null> {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const tokens = await parseResponse(response);
    saveTokens(tokens);

    try {
      return await fetchCurrentUserProfile(tokens.accessToken);
    } catch {
      clearTokens();
      currentUser = null;
      throw new Error('Не удалось загрузить профиль пользователя');
    }
  },

  async register(payload: RegistrationPayload): Promise<User | null> {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const tokens = await parseResponse(response);
    saveTokens(tokens);

    try {
      return await fetchCurrentUserProfile(tokens.accessToken);
    } catch {
      clearTokens();
      currentUser = null;
      throw new Error('Не удалось загрузить профиль пользователя');
    }
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
    currentUser = null;
  },

  getToken(): string | undefined {
    return getAccessToken();
  },

  async refreshToken(minValidity: number = 30): Promise<boolean> {
    void minValidity;
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      return false;
    }
    try {
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
      const tokens = await parseResponse(response);
      saveTokens(tokens);
      if (currentUser) {
        currentUser = {
          ...currentUser,
          token: tokens.accessToken,
        };
      }
      return true;
    } catch {
      clearTokens();
      currentUser = null;
      return false;
    }
  },

  getCurrentUser(): User | null {
    return currentUser;
  },

  isAuthenticated(): boolean {
    return !!getAccessToken();
  },
};

export default AuthService;
