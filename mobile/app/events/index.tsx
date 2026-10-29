import { useQuery } from '@tanstack/react-query';
import { Link, useRouter } from 'expo-router';
import { useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { apiClient } from '../../src/api/client';
import type { EventResponse } from '../../src/api/types';
import { useAuth } from '../../src/auth/AuthContext';
import { Button } from '../../src/components/Button';
import { Screen } from '../../src/components/Screen';
import { TextField } from '../../src/components/TextField';
import { colors, radii, spacing } from '../../src/theme';

export default function EventsListScreen() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [joinCode, setJoinCode] = useState('');

  const { data: events, isLoading, refetch, isRefetching } = useQuery({
    queryKey: ['events'],
    queryFn: async () => (await apiClient.get<EventResponse[]>('/events')).data,
  });

  return (
    <Screen scroll={false}>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>My Events</Text>
          <Text style={styles.subtitle}>Hi {user?.displayName}</Text>
        </View>
        <Pressable onPress={logout}>
          <Text style={styles.logout}>Log out</Text>
        </Pressable>
      </View>

      <View style={styles.joinRow}>
        <View style={{ flex: 1 }}>
          <TextField
            label="Join with a code"
            value={joinCode}
            onChangeText={(text) => setJoinCode(text.toUpperCase())}
            placeholder="ABCD1234"
            autoCapitalize="characters"
          />
        </View>
        <Pressable
          style={[styles.joinButton, !joinCode && styles.joinButtonDisabled]}
          disabled={!joinCode}
          onPress={() => router.push({ pathname: '/join/[code]', params: { code: joinCode.trim() } })}
        >
          <Text style={styles.joinButtonText}>Go</Text>
        </Pressable>
      </View>

      <Button label="+ Create Event" onPress={() => router.push('/events/new')} />

      <FlatList
        style={{ marginTop: spacing.lg }}
        data={events ?? []}
        keyExtractor={(item) => item.id}
        refreshing={isLoading || isRefetching}
        onRefresh={refetch}
        ListEmptyComponent={
          !isLoading ? (
            <Text style={styles.empty}>No events yet. Create one, or join with an invite code.</Text>
          ) : null
        }
        renderItem={({ item }) => (
          <Link href={{ pathname: '/events/[eventId]', params: { eventId: item.id } }} asChild>
            <Pressable style={styles.card}>
              <Text style={styles.cardTitle}>{item.name}</Text>
              {item.description ? <Text style={styles.cardSubtitle}>{item.description}</Text> : null}
            </Pressable>
          </Link>
        )}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: spacing.lg,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.text,
  },
  subtitle: {
    fontSize: 14,
    color: colors.textMuted,
    marginTop: spacing.xs,
  },
  logout: {
    color: colors.danger,
    fontWeight: '600',
    marginTop: spacing.xs,
  },
  joinRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: spacing.sm,
  },
  joinButton: {
    backgroundColor: colors.text,
    borderRadius: radii.sm,
    paddingHorizontal: spacing.md,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  joinButtonDisabled: {
    opacity: 0.4,
  },
  joinButtonText: {
    color: colors.surface,
    fontWeight: '700',
  },
  card: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  cardTitle: {
    fontSize: 17,
    fontWeight: '600',
    color: colors.text,
  },
  cardSubtitle: {
    fontSize: 14,
    color: colors.textMuted,
    marginTop: spacing.xs,
  },
  empty: {
    textAlign: 'center',
    color: colors.textMuted,
    marginTop: spacing.xl,
  },
});
