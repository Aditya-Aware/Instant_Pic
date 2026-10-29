import { useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Text } from 'react-native';

import { apiClient } from '../../src/api/client';
import { extractErrorMessage } from '../../src/api/errors';
import type { EventResponse } from '../../src/api/types';
import { Button } from '../../src/components/Button';
import { Screen } from '../../src/components/Screen';
import { TextField } from '../../src/components/TextField';
import { colors, spacing } from '../../src/theme';

export default function NewEventScreen() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit() {
    setError(null);
    setSubmitting(true);
    try {
      const response = await apiClient.post<EventResponse>('/events', {
        name: name.trim(),
        description: description.trim() || null,
        startDate: null,
        endDate: null,
      });
      await queryClient.invalidateQueries({ queryKey: ['events'] });
      router.replace({ pathname: '/events/[eventId]', params: { eventId: response.data.id } });
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not create event'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Screen>
      <Text style={{ fontSize: 22, fontWeight: '700', color: colors.text, marginBottom: spacing.lg }}>
        Create a new event
      </Text>
      <TextField label="Event name" value={name} onChangeText={setName} placeholder="Goa Trip 2026" autoCapitalize="words" />
      <TextField
        label="Description (optional)"
        value={description}
        onChangeText={setDescription}
        placeholder="Friends trip to the beach"
        multiline
      />
      {error && <Text style={{ color: colors.danger, marginBottom: spacing.md }}>{error}</Text>}
      <Button label="Create Event" onPress={handleSubmit} loading={submitting} disabled={!name.trim()} />
    </Screen>
  );
}
