import axios from 'axios';
import { AxiosError } from 'axios';

export interface LoginData {
  email: string;
  password: string;
}

export interface RegisterData {
  email: string;
  password: string;
  name: string;
}

export interface User {
  id: string;
  email: string;
  name: string;
  token: string;
}

// Тип для payload JWT (расширен для полей Keycloak)
interface JwtPayload {
  sub?: string;
  user_id?: string;
  email?: string;
  given_name?: string;
  family_name?: string;
  name?: string;
  preferred_username?: string;
  [key: string]: unknown; // для любых других полей
}

function parseJwt(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload) as JwtPayload;
  } catch (e) {
    console.warn('Failed to parse JWT', e);
    return null;
  }
}

const API_GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8083';

interface TokenResponse {
  access_token: string;
  refresh_token?: string;
  expires_in?: number;
  [key: string]: unknown;
}

const AuthService = {
  async register(data: RegisterData): Promise<User> {
    console.log('Register called with:', data);
    await new Promise(resolve => setTimeout(resolve, 1000));
    const fakeUser: User = {
      id: Math.random().toString(36).substr(2, 9),
      email: data.email,
      name: data.name,
      token: 'fake-jwt-token-' + Math.random()
    };
    localStorage.setItem('user', JSON.stringify(fakeUser));
    return fakeUser;
  },

  async login(data: LoginData): Promise<User> {
    console.log('Login called with:', data);

    try {
      const response = await axios.post<TokenResponse>(
        `${API_GATEWAY_URL}/api/auth/login`,
        {
          username: data.email,
          password: data.password
        }
      );

      const tokenData = response.data;
      const accessToken = tokenData.access_token;
      const payload = parseJwt(accessToken) || {};

      const user: User = {
        id: (payload.sub as string) || (payload.user_id as string) || 'unknown',
        email: (payload.email as string) || data.email,
        // Приоритет: given_name, затем preferred_username, затем name, затем часть email
        name: (payload.given_name as string) || 
              (payload.preferred_username as string) || 
              (payload.name as string) || 
              data.email.split('@')[0],
        token: accessToken
      };

      localStorage.setItem('user', JSON.stringify(user));
      return user;

    } catch (error: unknown) {
      console.error('Login error:', error);
      let message = 'Login failed';
      if (error instanceof AxiosError) {
        message = error.response?.data?.message || error.message || message;
      } else if (error instanceof Error) {
        message = error.message;
      }
      throw new Error(message);
    }
  },

  logout(): void {
    localStorage.removeItem('user');
  },

  getCurrentUser(): User | null {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      return JSON.parse(userStr) as User;
    }
    return null;
  }
};

export default AuthService;