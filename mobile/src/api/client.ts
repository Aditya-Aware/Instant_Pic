import axios from 'axios';
import * as SecureStore from 'expo-secure-store';

import { API_BASE_URL } from './config';

export const TOKEN_STORAGE_KEY = 'smpn_auth_token';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

apiClient.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function authHeader(token: string | null) {
  return token ? { Authorization: `Bearer ${token}` } : undefined;
}
