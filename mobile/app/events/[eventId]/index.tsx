import { useQuery } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { apiClient } from '../../../src/api/client';
import type { EventMemberResponse, EventResponse } from '../../../src/api/types';
import { useAuth } from '../../../src/auth/AuthContext';
import { Button } from '../../../src/components/Button';
import { Screen } from '../../../src/components/Screen';
import { GalleryTab } from '../../../src/components/event/GalleryTab';
import { InviteTab } from '../../../src/components/event/InviteTab';
import { MembersTab } from '../../../src/components/event/MembersTab';
import { MyMomentsTab } from '../../../src/components/event/MyMomentsTab';
import { RulesTab } from '../../../src/components/event/RulesTab';
import { colors, radii, spacing } from '../../../src/theme';

type TabKey = 'gallery' | 'my-moments' | 'members' | 'rules' | 'invite';

const TABS: { key: TabKey; label: string }[] = [
  { key: 'my-moments', label: 'My Moments' },
  { key: 'gallery', label: 'Gallery' },
  { key: 'members', label: 'Members' },
  { key: 'rules', label: 'Rules' },
  { key: 'invite', label: 'Invite' },
];

export default function EventDashboardScreen() {
  const { eventId } = useLocalSearchParams<{ eventId: string }>();
  const { user } = useAuth();
  const router = useRouter();
  const [tab, setTab] = useState<TabKey>('my-moments');

  const { data: event } = useQuery({
    queryKey: ['event', eventId],
    queryFn: async () => (await apiClient.get<EventResponse>(`/events/${eventId}`)).data,
  });

  const { data: members } = useQuery({
    queryKey: ['members', eventId],
    queryFn: async () => (await apiClient.get<EventMemberResponse[]>(`/events/${eventId}/members`)).data,
  });

  const isOrganizer = useMemo(
    () => members?.some((m) => m.userId === user?.userId && m.role === 'ORGANIZER') ?? false,
    [members, user?.userId],
  );

  return (
    <Screen scroll={false}>
      <Text style={styles.title}>{event?.name ?? '…'}</Text>

      <View style={styles.actionsRow}>
        <View style={{ flex: 1 }}>
          <Button
            label="📷 Upload Photo"
            onPress={() => router.push({ pathname: '/events/[eventId]/upload', params: { eventId } })}
          />
        </View>
        <View style={{ flex: 1 }}>
          <Button
            label="🙂 Enroll Face"
            variant="secondary"
            onPress={() => router.push({ pathname: '/events/[eventId]/enroll', params: { eventId } })}
          />
        </View>
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.tabBar}>
        {TABS.map((t) => (
          <Pressable key={t.key} onPress={() => setTab(t.key)} style={[styles.tab, tab === t.key && styles.tabActive]}>
            <Text style={[styles.tabLabel, tab === t.key && styles.tabLabelActive]}>{t.label}</Text>
          </Pressable>
        ))}
      </ScrollView>

      <View style={styles.tabContent}>
        {tab === 'gallery' && <GalleryTab eventId={eventId} />}
        {tab === 'my-moments' && <MyMomentsTab eventId={eventId} />}
        {tab === 'members' && <MembersTab eventId={eventId} />}
        {tab === 'rules' && <RulesTab eventId={eventId} isOrganizer={isOrganizer} />}
        {tab === 'invite' && <InviteTab eventId={eventId} isOrganizer={isOrganizer} />}
      </View>

      <Pressable
        onPress={() => router.push({ pathname: '/events/[eventId]/privacy', params: { eventId } })}
        style={styles.privacyLink}
      >
        <Text style={styles.privacyLinkText}>Privacy settings</Text>
      </Pressable>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: colors.text,
    marginBottom: spacing.md,
  },
  actionsRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginBottom: spacing.md,
  },
  tabBar: {
    flexGrow: 0,
    marginBottom: spacing.md,
  },
  tab: {
    paddingVertical: spacing.xs + 2,
    paddingHorizontal: spacing.md,
    borderRadius: radii.lg,
    marginRight: spacing.xs,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  tabActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  tabLabel: {
    color: colors.textMuted,
    fontWeight: '600',
    fontSize: 13,
  },
  tabLabelActive: {
    color: colors.primaryText,
  },
  tabContent: {
    flex: 1,
  },
  privacyLink: {
    paddingVertical: spacing.sm,
    alignItems: 'center',
  },
  privacyLinkText: {
    color: colors.textMuted,
    fontSize: 13,
    textDecorationLine: 'underline',
  },
});
