import { Link } from 'expo-router';
import { useState } from 'react';
import { Text, View } from 'react-native';

import { extractErrorMessage } from '../src/api/errors';
import { useAuth } from '../src/auth/AuthContext';
import { Button } from '../src/components/Button';
import { Screen } from '../src/components/Screen';
import { TextField } from '../src/components/TextField';
import { colors, spacing } from '../src/theme';

export default function RegisterScreen() {
  const { register } = useAuth();
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit() {
    setError(null);
    setSubmitting(true);
    try {
      await register(email.trim(), password, displayName.trim());
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not create account'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Screen>
      <Text style={{ fontSize: 28, fontWeight: '700', color: colors.text, marginBottom: spacing.lg }}>
        Create your account
      </Text>
      <TextField label="Name" value={displayName} onChangeText={setDisplayName} placeholder="Aditya" />
      <TextField
        label="Email"
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        placeholder="you@example.com"
      />
      <TextField
        label="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        placeholder="At least 8 characters"
      />
      {error && <Text style={{ color: colors.danger, marginBottom: spacing.md }}>{error}</Text>}
      <Button
        label="Create Account"
        onPress={handleSubmit}
        loading={submitting}
        disabled={!email || !password || !displayName}
      />
      <View style={{ marginTop: spacing.lg, alignItems: 'center' }}>
        <Link href="/login">
          <Text style={{ color: colors.primary, fontWeight: '600' }}>Already have an account? Log in</Text>
        </Link>
      </View>
    </Screen>
  );
}
