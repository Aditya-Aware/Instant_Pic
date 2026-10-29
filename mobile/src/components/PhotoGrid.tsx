import { FlatList, StyleSheet, Text, View } from 'react-native';
import type { ListRenderItem } from 'react-native';

import type { PhotoResponse } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { colors, radii, spacing } from '../theme';
import { AuthImage } from './AuthImage';

const GAP = spacing.xs;

export function PhotoGrid({
  photos,
  emptyMessage,
  refreshing,
  onRefresh,
}: {
  photos: PhotoResponse[];
  emptyMessage: string;
  refreshing?: boolean;
  onRefresh?: () => void;
}) {
  const { token } = useAuth();

  const renderItem: ListRenderItem<PhotoResponse> = ({ item }) => (
    <View style={styles.cell}>
      <AuthImage photoId={item.id} token={token} style={styles.image} />
      {item.processingStatus !== 'RULES_EVALUATED' && (
        <View style={styles.statusBadge}>
          <Text style={styles.statusText}>{statusLabel(item.processingStatus)}</Text>
        </View>
      )}
    </View>
  );

  return (
    <FlatList
      data={photos}
      renderItem={renderItem}
      keyExtractor={(item) => item.id}
      numColumns={3}
      refreshing={refreshing}
      onRefresh={onRefresh}
      columnWrapperStyle={{ gap: GAP }}
      contentContainerStyle={{ gap: GAP }}
      ListEmptyComponent={<Text style={styles.empty}>{emptyMessage}</Text>}
    />
  );
}

function statusLabel(status: PhotoResponse['processingStatus']) {
  switch (status) {
    case 'PENDING':
      return 'Waiting…';
    case 'DETECTING':
      return 'Detecting…';
    case 'MATCHING':
      return 'Matching…';
    case 'FAILED':
      return 'Failed';
    default:
      return '';
  }
}

const styles = StyleSheet.create({
  cell: {
    flex: 1 / 3,
    aspectRatio: 1,
    borderRadius: radii.sm,
    overflow: 'hidden',
    backgroundColor: colors.border,
  },
  image: {
    width: '100%',
    height: '100%',
  },
  statusBadge: {
    position: 'absolute',
    bottom: 4,
    left: 4,
    right: 4,
    backgroundColor: 'rgba(0,0,0,0.6)',
    borderRadius: 4,
    paddingVertical: 2,
    paddingHorizontal: 4,
  },
  statusText: {
    color: '#fff',
    fontSize: 10,
    textAlign: 'center',
  },
  empty: {
    color: colors.textMuted,
    textAlign: 'center',
    marginTop: spacing.xl,
    width: '100%',
  },
});
