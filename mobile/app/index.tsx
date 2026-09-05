import { Redirect } from 'expo-router';
import { ActivityIndicator, View } from 'react-native';
import { useAuth } from '@/auth';
import { colors } from '@/theme';

export default function Index() { const { loading, profile } = useAuth(); if (loading) return <View style={{ flex: 1, justifyContent: 'center', backgroundColor: colors.paper }}><ActivityIndicator color={colors.ink} /></View>; return <Redirect href={profile ? profile.consent_accepted_at ? '/(tabs)/today' : '/consent' : '/login'} />; }
