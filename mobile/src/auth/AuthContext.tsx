import * as SecureStore from 'expo-secure-store';
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

import { apiClient, TOKEN_STORAGE_KEY } from '../api/client';
import type { AuthResponse } from '../api/types';

const USER_STORAGE_KEY = 'smpn_auth_user';

interface AuthUser {
  userId: string;
  email: string;
  displayName: string;
}

interface AuthContextValue {
  isLoading: boolean;
  token: string | null;
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

async function persistSession(auth: AuthResponse) {
  await SecureStore.setItemAsync(TOKEN_STORAGE_KEY, auth.token);
  await SecureStore.setItemAsync(
    USER_STORAGE_KEY,
    JSON.stringify({ userId: auth.userId, email: auth.email, displayName: auth.displayName }),
  );
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isLoading, setIsLoading] = useState(true);
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    (async () => {
      const [storedToken, storedUser] = await Promise.all([
        SecureStore.getItemAsync(TOKEN_STORAGE_KEY),
        SecureStore.getItemAsync(USER_STORAGE_KEY),
      ]);
      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser) as AuthUser);
      }
      setIsLoading(false);
    })();
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isLoading,
      token,
      user,
      async login(email, password) {
        const response = await apiClient.post<AuthResponse>('/auth/login', { email, password });
        await persistSession(response.data);
        setToken(response.data.token);
        setUser({
          userId: response.data.userId,
          email: response.data.email,
          displayName: response.data.displayName,
        });
      },
      async register(email, password, displayName) {
        const response = await apiClient.post<AuthResponse>('/auth/register', {
          email,
          password,
          displayName,
        });
        await persistSession(response.data);
        setToken(response.data.token);
        setUser({
          userId: response.data.userId,
          email: response.data.email,
          displayName: response.data.displayName,
        });
      },
      async logout() {
        await SecureStore.deleteItemAsync(TOKEN_STORAGE_KEY);
        await SecureStore.deleteItemAsync(USER_STORAGE_KEY);
        setToken(null);
        setUser(null);
      },
    }),
    [isLoading, token, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
