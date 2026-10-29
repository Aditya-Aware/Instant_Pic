import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { Share, StyleSheet, Text, View } from 'react-native';

import { apiClient } from '../../api/client';
import { extractErrorMessage } from '../../api/errors';
import type { InvitationResponse } from '../../api/types';
import { Button } from '../Button';
import { colors, radii, spacing } from '../../theme';

export function InviteTab({ eventId, isOrganizer }: { eventId: string; isOrganizer: boolean }) {
  const [error, setError] = useState<string | null>(null);
  const [invitation, setInvitation] = useState<InvitationResponse | null>(null);

  const { mutate, isPending } = useMutation({
    mutationFn: async () =>
      (await apiClient.post<InvitationResponse>(`/events/${eventId}/invitations`)).data,
    onSuccess: (data) => {
      setInvitation(data);
      setError(null);
    },
    onError: (err) => setError(extractErrorMessage(err, 'Could not create invite')),
  });

  if (!isOrganizer) {
    return <Text style={styles.note}>Only the event organizer can create invites.</Text>;
  }

  return (
    <View>
      {invitation ? (
        <View style={styles.codeCard}>
          <Text style={styles.codeLabel}>Invite code</Text>
          <Text style={styles.code}>{invitation.code}</Text>
          <Text style={styles.expiry}>
            Expires {new Date(invitation.expiresAt).toLocaleDateString()}
          </Text>
          <View style={{ height: spacing.md }} />
          <Button
            label="Share Invite"
            onPress={() =>
              Share.share({
                message: `Join my event on Shared Memory! Use code ${invitation.code} or open the app and enter it.`,
              })
            }
          />
        </View>
      ) : (
        <Text style={styles.note}>Generate a code your friends can enter to join this event.</Text>
      )}
      {error && <Text style={{ color: colors.danger, marginTop: spacing.sm }}>{error}</Text>}
      <View style={{ height: spacing.md }} />
      <Button
        label={invitation ? 'Generate New Code' : 'Generate Invite Code'}
        variant="secondary"
        onPress={() => mutate()}
        loading={isPending}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  codeCard: {
    backgroundColor: colors.chipBackground,
    borderRadius: radii.lg,
    padding: spacing.lg,
    alignItems: 'center',
  },
  codeLabel: {
    fontSize: 13,
    color: colors.textMuted,
    fontWeight: '600',
  },
  code: {
    fontSize: 36,
    fontWeight: '800',
    letterSpacing: 4,
    color: colors.primary,
    marginTop: spacing.xs,
  },
  expiry: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: spacing.sm,
  },
  note: {
    color: colors.textMuted,
    marginBottom: spacing.md,
  },
});
