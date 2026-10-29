import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Stack, useRouter, useSegments } from 'expo-router';
import { useEffect } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AuthProvider, useAuth } from '../src/auth/AuthContext';
import { colors } from '../src/theme';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
    },
  },
});

// "index" is deliberately excluded here — app/index.tsx handles its own redirect on mount.
const PUBLIC_ROUTES = new Set(['login', 'register', 'join']);

function AuthGate() {
  const { isLoading, token } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (isLoading) return;

    const firstSegment = segments[0] as string | undefined;
    if (firstSegment === undefined) return;

    if (!token && !PUBLIC_ROUTES.has(firstSegment)) {
      router.replace('/login');
    } else if (token && (firstSegment === 'login' || firstSegment === 'register')) {
      router.replace('/events');
    }
  }, [isLoading, token, segments, router]);

  if (isLoading) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <Stack screenOptions={{ headerStyle: { backgroundColor: colors.surface }, headerTintColor: colors.text }}>
      <Stack.Screen name="index" options={{ headerShown: false }} />
      <Stack.Screen name="login" options={{ title: 'Log In' }} />
      <Stack.Screen name="register" options={{ title: 'Create Account' }} />
      <Stack.Screen name="join/[code]" options={{ title: 'Join Event' }} />
      <Stack.Screen name="events/index" options={{ title: 'My Events' }} />
      <Stack.Screen name="events/new" options={{ title: 'New Event' }} />
      <Stack.Screen name="events/[eventId]/index" options={{ title: 'Event' }} />
      <Stack.Screen name="events/[eventId]/enroll" options={{ title: 'Enroll Face' }} />
      <Stack.Screen name="events/[eventId]/upload" options={{ title: 'Upload Photo' }} />
      <Stack.Screen name="events/[eventId]/privacy" options={{ title: 'Privacy' }} />
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <AuthGate />
        </AuthProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}
