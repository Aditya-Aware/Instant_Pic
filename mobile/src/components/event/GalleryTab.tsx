import { useQuery } from '@tanstack/react-query';

import { apiClient } from '../../api/client';
import type { PhotoResponse } from '../../api/types';
import { PhotoGrid } from '../PhotoGrid';

function hasPendingProcessing(photos: PhotoResponse[] | undefined) {
  return !!photos?.some((p) => p.processingStatus !== 'RULES_EVALUATED' && p.processingStatus !== 'FAILED');
}

export function GalleryTab({ eventId }: { eventId: string }) {
  const { data, isLoading, isRefetching, refetch } = useQuery({
    queryKey: ['gallery', eventId],
    queryFn: async () => (await apiClient.get<PhotoResponse[]>(`/events/${eventId}/gallery`)).data,
    refetchInterval: (query) => (hasPendingProcessing(query.state.data) ? 3000 : false),
  });

  return (
    <PhotoGrid
      photos={data ?? []}
      emptyMessage="No group photos here yet — photos appear once 3+ people are recognized in them."
      refreshing={isLoading || isRefetching}
      onRefresh={refetch}
    />
  );
}
