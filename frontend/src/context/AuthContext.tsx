import { createContext, useContext } from 'react';
import type { RegistrationPayload, User } from '../services/auth.service';

export interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (payload: RegistrationPayload) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  getToken: () => Promise<string | undefined>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
