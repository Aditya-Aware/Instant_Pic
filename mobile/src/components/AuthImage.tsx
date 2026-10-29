import { Image } from 'react-native';
import type { ImageStyle, StyleProp } from 'react-native';

import { API_BASE_URL } from '../api/config';

export function AuthImage({
  photoId,
  token,
  style,
}: {
  photoId: string;
  token: string | null;
  style?: StyleProp<ImageStyle>;
}) {
  return (
    <Image
      source={{
        uri: `${API_BASE_URL}/photos/${photoId}/image`,
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      }}
      style={style}
      resizeMode="cover"
    />
  );
}
