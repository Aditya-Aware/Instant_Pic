import { useQuery } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, Text, View } from 'react-native';

import { apiClient } from '../../src/api/client';
import { extractErrorMessage } from '../../src/api/errors';
import type { EventMemberResponse, InvitationPreviewResponse } from '../../src/api/types';
import { useAuth } from '../../src/auth/AuthContext';
import { Button } from '../../src/components/Button';
import { Screen } from '../../src/components/Screen';
import { TextField } from '../../src/components/TextField';
import { colors, radii, spacing } from '../../src/theme';

export default function JoinScreen() {
  const { code } = useLocalSearchParams<{ code: string }>();
  const { token, user } = useAuth();
  const router = useRouter();
  const [displayName, setDisplayName] = useState(user?.displayName ?? '');
  const [error, setError] = useState<string | null>(null);
  const [joining, setJoining] = useState(false);

  const { data: preview, isLoading, isError } = useQuery({
    queryKey: ['invitation-preview', code],
    queryFn: async () => (await apiClient.get<InvitationPreviewResponse>(`/invitations/${code}`)).data,
    enabled: !!code,
  });

  async function handleJoin() {
    setError(null);
    setJoining(true);
    try {
      const response = await apiClient.post<EventMemberResponse>('/events/join', {
        code,
        displayName: displayName.trim(),
      });
      router.replace({ pathname: '/events/[eventId]', params: { eventId: response.data.eventId } });
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not join this event'));
    } finally {
      setJoining(false);
    }
  }

  if (isLoading) {
    return (
      <Screen>
        <ActivityIndicator color={colors.primary} />
      </Screen>
    );
  }

  if (isError || !preview) {
    return (
      <Screen>
        <Text style={{ color: colors.danger }}>This invite link is invalid or has expired.</Text>
      </Screen>
    );
  }

  return (
    <Screen>
      <View style={{ backgroundColor: colors.chipBackground, borderRadius: radii.md, padding: spacing.md, marginBottom: spacing.lg }}>
        <Text style={{ fontSize: 22, fontWeight: '700', color: colors.text }}>{preview.eventName}</Text>
        <Text style={{ color: colors.textMuted, marginTop: spacing.xs }}>
          Organized by {preview.organizerDisplayName} · {preview.memberCount} member
          {preview.memberCount === 1 ? '' : 's'}
        </Text>
      </View>

      {!token ? (
        <View>
          <Text style={{ color: colors.text, marginBottom: spacing.md }}>
            Log in or create an account to join this event.
          </Text>
          <Button label="Log In" onPress={() => router.push('/login')} />
          <View style={{ height: spacing.sm }} />
          <Button label="Create Account" variant="secondary" onPress={() => router.push('/register')} />
        </View>
      ) : (
        <View>
          <TextField label="Your name in this event" value={displayName} onChangeText={setDisplayName} />
          {error && <Text style={{ color: colors.danger, marginBottom: spacing.md }}>{error}</Text>}
          <Button label="Join Event" onPress={handleJoin} loading={joining} disabled={!displayName.trim()} />
        </View>
      )}
    </Screen>
  );
}
