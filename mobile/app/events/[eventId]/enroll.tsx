import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as ImagePicker from 'expo-image-picker';
import { useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Alert, Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { apiClient } from '../../../src/api/client';
import { extractErrorMessage } from '../../../src/api/errors';
import type { FaceEnrollmentResponse } from '../../../src/api/types';
import { Button } from '../../../src/components/Button';
import { Screen } from '../../../src/components/Screen';
import { colors, radii, spacing } from '../../../src/theme';

interface PickedPhoto {
  uri: string;
  fileName: string;
  mimeType: string;
}

const MAX_SELFIES = 5;

export default function EnrollFaceScreen() {
  const { eventId } = useLocalSearchParams<{ eventId: string }>();
  const queryClient = useQueryClient();
  const [photos, setPhotos] = useState<PickedPhoto[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const { data: status, isLoading } = useQuery({
    queryKey: ['face-enrollment', eventId],
    queryFn: async () =>
      (await apiClient.get<FaceEnrollmentResponse>(`/events/${eventId}/members/me/face-enrollment`)).data,
  });

  async function addPhoto(fromCamera: boolean) {
    const permission = fromCamera
      ? await ImagePicker.requestCameraPermissionsAsync()
      : await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('Permission needed', 'Please allow access to continue.');
      return;
    }

    const result = fromCamera
      ? await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 0.8 })
      : await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.8 });

    if (result.canceled || !result.assets[0]) return;
    const asset = result.assets[0];
    setPhotos((prev) =>
      [...prev, { uri: asset.uri, fileName: asset.fileName ?? `selfie-${Date.now()}.jpg`, mimeType: asset.mimeType ?? 'image/jpeg' }].slice(
        0,
        MAX_SELFIES,
      ),
    );
  }

  function removePhoto(index: number) {
    setPhotos((prev) => prev.filter((_, i) => i !== index));
  }

  async function submit() {
    setError(null);
    setSubmitting(true);
    try {
      const formData = new FormData();
      photos.forEach((photo) => {
        formData.append('selfies', {
          uri: photo.uri,
          name: photo.fileName,
          type: photo.mimeType,
        } as unknown as Blob);
      });
      await apiClient.post(`/events/${eventId}/members/me/face-enrollment`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setPhotos([]);
      await queryClient.invalidateQueries({ queryKey: ['face-enrollment', eventId] });
      Alert.alert('Done', 'Your face has been enrolled for this event.');
    } catch (err) {
      setError(extractErrorMessage(err, 'Enrollment failed — make sure each selfie shows exactly one face'));
    } finally {
      setSubmitting(false);
    }
  }

  async function deleteEnrollment() {
    setSubmitting(true);
    try {
      await apiClient.delete(`/events/${eventId}/members/me/face-enrollment`);
      await queryClient.invalidateQueries({ queryKey: ['face-enrollment', eventId] });
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not delete face profile'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Screen>
      <Text style={styles.title}>Face enrollment</Text>
      <Text style={styles.subtitle}>
        Add {MAX_SELFIES > 1 ? '3–5' : '1'} clear selfies, one face per photo. We use these to recognize you in
        event photos — the selfies themselves are never stored, only the resulting match data.
      </Text>

      {!isLoading && status && status.status === 'ACTIVE' && (
        <View style={styles.statusCard}>
          <Text style={styles.statusText}>✓ Enrolled with {status.sampleCount} sample photo(s)</Text>
          <Pressable onPress={deleteEnrollment} disabled={submitting}>
            <Text style={styles.deleteLink}>Delete my face data</Text>
          </Pressable>
        </View>
      )}

      <ScrollView horizontal style={styles.thumbRow} showsHorizontalScrollIndicator={false}>
        {photos.map((photo, index) => (
          <Pressable key={photo.uri} onPress={() => removePhoto(index)} style={styles.thumbWrapper}>
            <Image source={{ uri: photo.uri }} style={styles.thumb} />
            <View style={styles.removeBadge}>
              <Text style={styles.removeBadgeText}>✕</Text>
            </View>
          </Pressable>
        ))}
        {photos.length < MAX_SELFIES && (
          <View style={styles.addButtons}>
            <Pressable style={styles.addButton} onPress={() => addPhoto(true)}>
              <Text style={styles.addButtonText}>📷{'\n'}Camera</Text>
            </Pressable>
            <Pressable style={styles.addButton} onPress={() => addPhoto(false)}>
              <Text style={styles.addButtonText}>🖼️{'\n'}Library</Text>
            </Pressable>
          </View>
        )}
      </ScrollView>

      {error && <Text style={{ color: colors.danger, marginVertical: spacing.md }}>{error}</Text>}

      <View style={{ marginTop: spacing.lg }}>
        <Button
          label={`Submit ${photos.length} Photo${photos.length === 1 ? '' : 's'}`}
          onPress={submit}
          loading={submitting}
          disabled={photos.length === 0}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 22,
    fontWeight: '700',
    color: colors.text,
    marginBottom: spacing.xs,
  },
  subtitle: {
    fontSize: 14,
    color: colors.textMuted,
    marginBottom: spacing.lg,
    lineHeight: 20,
  },
  statusCard: {
    backgroundColor: colors.chipBackground,
    borderRadius: radii.md,
    padding: spacing.md,
    marginBottom: spacing.lg,
  },
  statusText: {
    color: colors.text,
    fontWeight: '600',
  },
  deleteLink: {
    color: colors.danger,
    marginTop: spacing.sm,
    fontSize: 13,
  },
  thumbRow: {
    flexGrow: 0,
  },
  thumbWrapper: {
    marginRight: spacing.sm,
  },
  thumb: {
    width: 90,
    height: 90,
    borderRadius: radii.sm,
  },
  removeBadge: {
    position: 'absolute',
    top: -6,
    right: -6,
    backgroundColor: colors.danger,
    width: 22,
    height: 22,
    borderRadius: 11,
    alignItems: 'center',
    justifyContent: 'center',
  },
  removeBadgeText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '700',
  },
  addButtons: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  addButton: {
    width: 90,
    height: 90,
    borderRadius: radii.sm,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addButtonText: {
    textAlign: 'center',
    color: colors.textMuted,
    fontSize: 12,
  },
});
