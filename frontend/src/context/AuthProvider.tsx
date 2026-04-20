import React, { useState, useEffect, useCallback } from 'react';
import type { ReactNode } from 'react';
import { AuthContext } from './AuthContext';
import AuthService from '../services/auth.service';
import type { RegistrationPayload, User } from '../services/auth.service';

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    const unsubscribe = AuthService.subscribe((nextUser) => {
      if (isMounted) {
        setUser(nextUser);
      }
    });

    const initAuth = async () => {
      try {
        await AuthService.init();
      } catch (err) {
        console.error('Failed to initialize auth', err);
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    void initAuth();

    return () => {
      isMounted = false;
      unsubscribe();
    };
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    await AuthService.login(username, password);
  }, []);

  const register = useCallback(async (payload: RegistrationPayload) => {
    await AuthService.register(payload);
  }, []);

  const logout = useCallback(async () => {
    await AuthService.logout();
  }, []);

  const getToken = useCallback(async (): Promise<string | undefined> => {
    return AuthService.getToken(30);
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
