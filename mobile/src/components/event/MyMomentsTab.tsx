import { useQuery } from '@tanstack/react-query';

import { apiClient } from '../../api/client';
import type { PhotoResponse } from '../../api/types';
import { PhotoGrid } from '../PhotoGrid';

function hasPendingProcessing(photos: PhotoResponse[] | undefined) {
  return !!photos?.some((p) => p.processingStatus !== 'RULES_EVALUATED' && p.processingStatus !== 'FAILED');
}

export function MyMomentsTab({ eventId }: { eventId: string }) {
  const { data, isLoading, isRefetching, refetch } = useQuery({
    queryKey: ['my-moments', eventId],
    queryFn: async () =>
      (await apiClient.get<PhotoResponse[]>(`/events/${eventId}/members/me/my-moments`)).data,
    refetchInterval: (query) => (hasPendingProcessing(query.state.data) ? 3000 : false),
  });

  return (
    <PhotoGrid
      photos={data ?? []}
      emptyMessage="Photos you appear in will show up here automatically — no need to search for them."
      refreshing={isLoading || isRefetching}
      onRefresh={refetch}
    />
  );
}
