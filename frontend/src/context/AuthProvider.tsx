import React, { useState, useEffect, useCallback } from 'react';
import type { ReactNode } from 'react';
import { AuthContext } from './AuthContext';
import AuthService from '../services/auth.service';
import type { User } from '../services/auth.service';

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      try {
        const currentUser = await AuthService.init();
        setUser(currentUser);
      } catch (err) {
        console.error('Failed to initialize auth', err);
      } finally {
        setLoading(false);
      }
    };
    initAuth();
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const currentUser = await AuthService.login(username, password);
    setUser(currentUser);
  }, []);

  const register = useCallback(async (
    email: string,
    password: string,
    firstName: string,
    lastName: string,
    middleName?: string,
  ) => {
    const currentUser = await AuthService.register(email, password, firstName, lastName, middleName);
    setUser(currentUser);
  }, []);

  const logout = useCallback(async () => {
    await AuthService.logout();
    setUser(null);
  }, []);

  const getToken = useCallback(async (): Promise<string | undefined> => {
    await AuthService.refreshToken(30);
    return AuthService.getToken();
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        register,
        logout,
        isAuthenticated: !!user,
        getToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
