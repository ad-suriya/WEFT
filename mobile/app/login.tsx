import { Redirect } from 'expo-router';
import { SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { useAuth } from '@/auth';
import { Button, Card, s } from '@/ui';
import { colors } from '@/theme';

export default function Login() {
  const { profile, signIn, loading, error } = useAuth(); if (profile) return <Redirect href={profile.consent_accepted_at ? '/(tabs)/today' : '/consent'} />;
  return <SafeAreaView style={styles.page}><View style={styles.hero}><Text style={styles.mark}>W</Text><Text style={styles.name}>WEFT</Text><Text style={styles.line}>One goal. Every device.{`\n`}One continuous workflow.</Text></View><Card style={styles.card}><Text style={s.h2}>Pick up where you left off.</Text><Text style={s.body}>Sign in with the same Google account you use on the WEFT website. Your tasks, workflows and active work stay connected.</Text>{error && <Text style={s.error}>{error}</Text>}<Button onPress={signIn} busy={loading}>Continue with Google</Button><Text style={s.muted}>WEFT only captures browser title and URL when you explicitly save a reference or while an active work session permits context sharing.</Text></Card></SafeAreaView>;
}
const styles = StyleSheet.create({ page: { flex: 1, backgroundColor: colors.paper, padding: 24, justifyContent: 'space-between' }, hero: { paddingTop: 70 }, mark: { width: 58, height: 58, textAlign: 'center', paddingTop: 8, backgroundColor: colors.ink, color: colors.white, fontFamily: 'serif', fontSize: 32, fontWeight: '900' }, name: { marginTop: 22, fontSize: 54, letterSpacing: 9, fontWeight: '900', color: colors.ink }, line: { marginTop: 14, fontFamily: 'serif', fontSize: 25, lineHeight: 34, color: colors.ink }, card: { marginBottom: 18 } });
