import { useQueryClient } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { Alert, Text, View } from 'react-native';

import { apiClient } from '../../../src/api/client';
import { extractErrorMessage } from '../../../src/api/errors';
import { Button } from '../../../src/components/Button';
import { Screen } from '../../../src/components/Screen';
import { colors, spacing } from '../../../src/theme';

export default function PrivacyScreen() {
  const { eventId } = useLocalSearchParams<{ eventId: string }>();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState<string | null>(null);

  async function deleteFaceData() {
    setError(null);
    setSubmitting('face');
    try {
      await apiClient.delete(`/events/${eventId}/members/me/face-enrollment`);
      await queryClient.invalidateQueries({ queryKey: ['face-enrollment', eventId] });
      Alert.alert('Done', 'Your face data has been deleted for this event.');
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not delete face data'));
    } finally {
      setSubmitting(null);
    }
  }

  function confirmLeaveEvent() {
    Alert.alert('Leave event?', 'You will stop being matched in future photos and lose access to this event.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Leave', style: 'destructive', onPress: leaveEvent },
    ]);
  }

  async function leaveEvent() {
    setError(null);
    setSubmitting('leave');
    try {
      await apiClient.post(`/events/${eventId}/leave`);
      await queryClient.invalidateQueries({ queryKey: ['events'] });
      router.replace('/events');
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not leave event'));
      setSubmitting(null);
    }
  }

  return (
    <Screen>
      <Text style={styles.title}>Privacy</Text>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Your face data</Text>
        <Text style={styles.sectionBody}>
          Delete your enrolled face for this event. You won't be automatically matched in future
          photos until you enroll again.
        </Text>
        <Button
          label="Delete My Face Data"
          variant="secondary"
          onPress={deleteFaceData}
          loading={submitting === 'face'}
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Leave this event</Text>
        <Text style={styles.sectionBody}>
          You'll stop appearing in future matches and lose access to this event's photos.
        </Text>
        <Button label="Leave Event" variant="danger" onPress={confirmLeaveEvent} loading={submitting === 'leave'} />
      </View>

      {error && <Text style={{ color: colors.danger, marginTop: spacing.md }}>{error}</Text>}
    </Screen>
  );
}

const styles = {
  title: {
    fontSize: 22,
    fontWeight: '700' as const,
    color: colors.text,
    marginBottom: spacing.lg,
  },
  section: {
    marginBottom: spacing.xl,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600' as const,
    color: colors.text,
    marginBottom: spacing.xs,
  },
  sectionBody: {
    fontSize: 14,
    color: colors.textMuted,
    marginBottom: spacing.md,
    lineHeight: 20,
  },
};
