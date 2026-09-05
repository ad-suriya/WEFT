import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import Constants from 'expo-constants';
import { router } from 'expo-router';
import { GoogleSignin, isErrorWithCode } from '@react-native-google-signin/google-signin';
import { api, ApiError, setTokenProvider } from './api';
import { storage } from './storage';
import type { FocusPrefs, Profile } from './types';

type AuthValue = { loading: boolean; profile: Profile | null; signIn: () => Promise<void>; signOut: () => Promise<void>; acceptConsent: () => Promise<void>; updateFocusPrefs: (patch: Partial<FocusPrefs>) => Promise<void>; error: string | null };
const AuthContext = createContext<AuthValue | null>(null);

function signInErrorMessage(error: unknown) {
  if (isErrorWithCode(error) && error.code === '10') {
    return 'Google Sign-In is not enabled for this Android build yet. The app signing certificate must be added to the WEFT Android OAuth client.';
  }
  return error instanceof Error ? error.message : 'Google sign-in failed.';
}

async function includeLocalConsent(profile: Profile) {
  const consentUserId = await storage.getConsentUserId();
  return consentUserId === profile.id && !profile.consent_accepted_at
    ? { ...profile, consent_accepted_at: new Date().toISOString() }
    : profile;
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const extra = Constants.expoConfig?.extra || {};

  useEffect(() => { GoogleSignin.configure({ webClientId: extra.googleWebClientId }); }, [extra.googleWebClientId]);
  useEffect(() => { setTokenProvider(storage.getToken); storage.getToken().then(async token => { if (!token) return; try { setProfile(await includeLocalConsent(await api.profile())); } catch { await storage.clearToken(); } }).finally(() => setLoading(false)); }, []);
  const value = useMemo<AuthValue>(() => ({ loading, profile, error, signIn: async () => { setError(null); setLoading(true); try { await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true }); const response = await GoogleSignin.signIn(); if (response.type !== 'success') return; const token = response.data.idToken; if (!token) throw new Error('Google did not return an ID token. Check the web OAuth client configuration.'); await storage.setToken(token); setProfile(await includeLocalConsent(await api.upsertProfile())); } catch (e) { setError(signInErrorMessage(e)); } finally { setLoading(false); } }, signOut: async () => { await GoogleSignin.signOut().catch(() => null); await storage.clearAll(); setProfile(null); router.replace('/login'); }, acceptConsent: async () => { if (!profile) return; try { await api.acceptConsent(); } catch (e) { if (!(e instanceof ApiError && e.status === 404)) throw e; } await storage.setConsentUserId(profile.id); setProfile(await includeLocalConsent(await api.profile())); }, updateFocusPrefs: async patch => { const focus_prefs = await api.patchFocusPrefs(patch); setProfile(current => current ? { ...current, focus_prefs } : current); } }), [loading, profile, error]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() { const value = useContext(AuthContext); if (!value) throw new Error('AuthProvider missing'); return value; }
