import { useQuery } from '@tanstack/react-query';
import { FlatList, StyleSheet, Text, View } from 'react-native';

import { apiClient } from '../../api/client';
import type { EventMemberResponse } from '../../api/types';
import { colors, radii, spacing } from '../../theme';

export function MembersTab({ eventId }: { eventId: string }) {
  const { data, isLoading, isRefetching, refetch } = useQuery({
    queryKey: ['members', eventId],
    queryFn: async () => (await apiClient.get<EventMemberResponse[]>(`/events/${eventId}/members`)).data,
  });

  return (
    <FlatList
      data={data ?? []}
      keyExtractor={(item) => item.id}
      refreshing={isLoading || isRefetching}
      onRefresh={refetch}
      ListEmptyComponent={<Text style={styles.empty}>No members yet.</Text>}
      renderItem={({ item }) => (
        <View style={styles.row}>
          <View>
            <Text style={styles.name}>{item.displayName}</Text>
            <Text style={styles.meta}>
              {item.role === 'ORGANIZER' ? 'Organizer' : item.role === 'PHOTOGRAPHER' ? 'Photographer' : 'Member'}
            </Text>
          </View>
          {item.status !== 'ACTIVE' && (
            <View style={styles.badge}>
              <Text style={styles.badgeText}>{item.status === 'LEFT' ? 'Left' : 'Removed'}</Text>
            </View>
          )}
        </View>
      )}
    />
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  name: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.text,
  },
  meta: {
    fontSize: 13,
    color: colors.textMuted,
    marginTop: 2,
  },
  badge: {
    backgroundColor: colors.chipBackground,
    borderRadius: radii.sm,
    paddingHorizontal: spacing.sm,
    paddingVertical: 4,
  },
  badgeText: {
    fontSize: 12,
    color: colors.textMuted,
  },
  empty: {
    color: colors.textMuted,
    textAlign: 'center',
    marginTop: spacing.xl,
  },
});
