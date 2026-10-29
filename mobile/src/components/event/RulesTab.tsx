import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Switch, Text, View } from 'react-native';
import { StyleSheet } from 'react-native';

import { apiClient } from '../../api/client';
import { extractErrorMessage } from '../../api/errors';
import type { SharingRuleResponse } from '../../api/types';
import { colors, radii, spacing } from '../../theme';

const RULE_INFO: Record<string, { title: string; description: string; editable: boolean }> = {
  DEFAULT_SELF: {
    title: 'Automatic delivery',
    description: 'Anyone recognized in a photo always receives it. This is always on.',
    editable: false,
  },
  GROUP_PHOTO: {
    title: 'Group photo → shared gallery',
    description: 'Photos with 3 or more recognized people also appear in the shared event gallery.',
    editable: true,
  },
  PRIVATE_PHOTO: {
    title: 'Private photo stays private',
    description: 'Photos with exactly 1 recognized person never appear in the shared gallery.',
    editable: true,
  },
  PAIR_RULE: { title: 'Pair rule', description: 'Coming soon.', editable: false },
  SUBGROUP: { title: 'Subgroup rule', description: 'Coming soon.', editable: false },
  PHOTOGRAPHER_MODE: { title: 'Photographer mode', description: 'Coming soon.', editable: false },
  APPROVAL_MODE: { title: 'Approval mode', description: 'Coming soon.', editable: false },
};

const DISPLAY_ORDER = [
  'DEFAULT_SELF',
  'GROUP_PHOTO',
  'PRIVATE_PHOTO',
  'PAIR_RULE',
  'SUBGROUP',
  'PHOTOGRAPHER_MODE',
  'APPROVAL_MODE',
];

export function RulesTab({ eventId, isOrganizer }: { eventId: string; isOrganizer: boolean }) {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['sharing-rules', eventId],
    queryFn: async () => (await apiClient.get<SharingRuleResponse[]>(`/events/${eventId}/sharing-rules`)).data,
  });

  async function toggle(ruleType: string, enabled: boolean) {
    try {
      await apiClient.put(`/events/${eventId}/sharing-rules`, { ruleType, enabled });
      await queryClient.invalidateQueries({ queryKey: ['sharing-rules', eventId] });
    } catch (err) {
      // Best-effort UI; a real toast/error surface would go here.
      console.warn(extractErrorMessage(err));
    }
  }

  if (isLoading || !data) {
    return null;
  }

  const byType = new Map(data.map((r) => [r.ruleType, r]));

  return (
    <View>
      {DISPLAY_ORDER.map((ruleType) => {
        const rule = byType.get(ruleType);
        const info = RULE_INFO[ruleType];
        if (!rule || !info) return null;
        const canToggle = info.editable && isOrganizer;
        return (
          <View key={ruleType} style={styles.row}>
            <View style={{ flex: 1, marginRight: spacing.md }}>
              <Text style={styles.title}>{info.title}</Text>
              <Text style={styles.description}>{info.description}</Text>
            </View>
            <Switch
              value={rule.enabled}
              disabled={!canToggle}
              onValueChange={(value) => toggle(ruleType, value)}
              trackColor={{ true: colors.primary }}
            />
          </View>
        );
      })}
      {!isOrganizer && (
        <Text style={styles.note}>Only the event organizer can change these settings.</Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  title: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.text,
  },
  description: {
    fontSize: 13,
    color: colors.textMuted,
    marginTop: 2,
  },
  note: {
    fontSize: 13,
    color: colors.textMuted,
    textAlign: 'center',
    marginTop: spacing.sm,
  },
});
