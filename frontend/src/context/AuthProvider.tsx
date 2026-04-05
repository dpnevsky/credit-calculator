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

        AuthService.onTokenExpired(async () => {
          const refreshed = await AuthService.refreshToken();
          if (refreshed) {
            setUser(AuthService.getCurrentUser());
          } else {
            setUser(null);
          }
        });
      } catch (err) {
        console.error('Failed to initialize auth', err);
      } finally {
        setLoading(false);
      }
    };
    initAuth();
  }, []);

  const login = useCallback(async () => {
    await AuthService.login();
  }, []);

  const register = useCallback(async () => {
    await AuthService.register();
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
