import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import * as Google from 'expo-auth-session/providers/google';
import Constants from 'expo-constants';
import { api, setTokenProvider } from './api';
import { storage } from './storage';
import type { FocusPrefs, Profile } from './types';

type AuthValue = { loading: boolean; profile: Profile | null; signIn: () => void; signOut: () => Promise<void>; acceptConsent: () => Promise<void>; updateFocusPrefs: (patch: Partial<FocusPrefs>) => Promise<void>; error: string | null };
const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const extra = Constants.expoConfig?.extra || {};
  const [request, response, promptAsync] = Google.useIdTokenAuthRequest({
    webClientId: extra.googleWebClientId,
    androidClientId: extra.googleAndroidClientId === 'REPLACE_WITH_ANDROID_OAUTH_CLIENT_ID' ? undefined : extra.googleAndroidClientId,
    redirectUri: extra.googleAndroidRedirectUri,
    scopes: ['openid', 'profile', 'email'],
  });

  useEffect(() => { setTokenProvider(storage.getToken); storage.getToken().then(async token => { if (!token) return; try { setProfile(await api.profile()); } catch { await storage.clearToken(); } }).finally(() => setLoading(false)); }, []);
  useEffect(() => {
    if (response?.type !== 'success') return;
    const token = response.authentication?.idToken || response.params.id_token;
    if (!token) { setError('Google did not return an ID token. Check the OAuth client configuration.'); return; }
    setLoading(true); storage.setToken(token).then(() => api.upsertProfile()).then(setProfile).catch(e => setError(e.message)).finally(() => setLoading(false));
  }, [response]);
  const value = useMemo<AuthValue>(() => ({ loading, profile, error, signIn: () => { setError(null); promptAsync(); }, signOut: async () => { await storage.clearAll(); setProfile(null); }, acceptConsent: async () => { await api.acceptConsent(); setProfile(await api.profile()); }, updateFocusPrefs: async patch => { const focus_prefs = await api.patchFocusPrefs(patch); setProfile(current => current ? { ...current, focus_prefs } : current); } }), [loading, profile, error, request]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() { const value = useContext(AuthContext); if (!value) throw new Error('AuthProvider missing'); return value; }
