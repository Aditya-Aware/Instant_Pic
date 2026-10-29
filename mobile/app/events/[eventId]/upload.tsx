import { useQueryClient } from '@tanstack/react-query';
import * as ImagePicker from 'expo-image-picker';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { Alert, Image, StyleSheet, Text, View } from 'react-native';

import { apiClient } from '../../../src/api/client';
import { extractErrorMessage } from '../../../src/api/errors';
import { Button } from '../../../src/components/Button';
import { Screen } from '../../../src/components/Screen';
import { colors, radii, spacing } from '../../../src/theme';

export default function UploadPhotoScreen() {
  const { eventId } = useLocalSearchParams<{ eventId: string }>();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [picked, setPicked] = useState<{ uri: string; fileName: string; mimeType: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function pick(fromCamera: boolean) {
    const permission = fromCamera
      ? await ImagePicker.requestCameraPermissionsAsync()
      : await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('Permission needed', 'Please allow access to continue.');
      return;
    }
    const result = fromCamera
      ? await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 0.85 })
      : await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.85 });
    if (result.canceled || !result.assets[0]) return;
    const asset = result.assets[0];
    setPicked({ uri: asset.uri, fileName: asset.fileName ?? `photo-${Date.now()}.jpg`, mimeType: asset.mimeType ?? 'image/jpeg' });
  }

  async function submit() {
    if (!picked) return;
    setError(null);
    setSubmitting(true);
    try {
      const formData = new FormData();
      formData.append('photo', {
        uri: picked.uri,
        name: picked.fileName,
        type: picked.mimeType,
      } as unknown as Blob);
      await apiClient.post(`/events/${eventId}/photos`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['my-moments', eventId] }),
        queryClient.invalidateQueries({ queryKey: ['gallery', eventId] }),
      ]);
      router.back();
    } catch (err) {
      setError(extractErrorMessage(err, 'Upload failed'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Screen>
      <Text style={styles.title}>Upload a photo</Text>
      <Text style={styles.subtitle}>
        We'll detect who's in it and deliver it to them automatically — no need to tag anyone.
      </Text>

      {picked ? (
        <Image source={{ uri: picked.uri }} style={styles.preview} />
      ) : (
        <View style={styles.placeholder}>
          <Text style={styles.placeholderText}>No photo selected</Text>
        </View>
      )}

      <View style={styles.pickRow}>
        <View style={{ flex: 1 }}>
          <Button label="📷 Take Photo" variant="secondary" onPress={() => pick(true)} />
        </View>
        <View style={{ flex: 1 }}>
          <Button label="🖼️ Choose Photo" variant="secondary" onPress={() => pick(false)} />
        </View>
      </View>

      {error && <Text style={{ color: colors.danger, marginVertical: spacing.md }}>{error}</Text>}

      <View style={{ marginTop: spacing.lg }}>
        <Button label="Upload" onPress={submit} loading={submitting} disabled={!picked} />
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
  preview: {
    width: '100%',
    aspectRatio: 1,
    borderRadius: radii.md,
    marginBottom: spacing.md,
  },
  placeholder: {
    width: '100%',
    aspectRatio: 1,
    borderRadius: radii.md,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  placeholderText: {
    color: colors.textMuted,
  },
  pickRow: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
});
