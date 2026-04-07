export interface User {
  id: string;
  email: string;
  name: string;
  token: string;
  roles: string[];
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

const ACCESS_TOKEN_KEY = 'cc_access_token';
const REFRESH_TOKEN_KEY = 'cc_refresh_token';

function parseJwt(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) {
      return null;
    }
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join(''),
    );
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function extractUser(token?: string): User | null {
  if (!token) {
    return null;
  }
  const payload = parseJwt(token);
  if (!payload) {
    return null;
  }
  const rolesRaw = payload.realm_access as { roles?: string[] } | undefined;
  const email = (payload.email as string) || (payload.preferred_username as string) || '';
  const name =
    (payload.given_name as string) ||
    (payload.name as string) ||
    (payload.preferred_username as string) ||
    '';
  return {
    id: (payload.sub as string) || 'unknown',
    email,
    name,
    token,
    roles: rolesRaw?.roles || [],
  };
}

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

const AuthService = {
  async init(): Promise<User | null> {
    const token = getAccessToken();
    return extractUser(token);
  },

  async login(username: string, password: string): Promise<User | null> {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const tokens = await parseResponse(response);
    saveTokens(tokens);
    return extractUser(tokens.accessToken);
  },

  async register(email: string, password: string, firstName: string, lastName: string): Promise<User | null> {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, firstName, lastName }),
    });
    const tokens = await parseResponse(response);
    saveTokens(tokens);
    return extractUser(tokens.accessToken);
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
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
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
      return true;
    } catch {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      return false;
    }
  },

  getCurrentUser(): User | null {
    return extractUser(getAccessToken());
  },

  isAuthenticated(): boolean {
    return !!getAccessToken();
  },
};

export default AuthService;
