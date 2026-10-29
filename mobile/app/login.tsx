import { Link } from 'expo-router';
import { useState } from 'react';
import { Text, View } from 'react-native';

import { extractErrorMessage } from '../src/api/errors';
import { useAuth } from '../src/auth/AuthContext';
import { Button } from '../src/components/Button';
import { Screen } from '../src/components/Screen';
import { TextField } from '../src/components/TextField';
import { colors, spacing } from '../src/theme';

export default function LoginScreen() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit() {
    setError(null);
    setSubmitting(true);
    try {
      await login(email.trim(), password);
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not log in'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Screen>
      <Text style={{ fontSize: 28, fontWeight: '700', color: colors.text, marginBottom: spacing.lg }}>
        Welcome back
      </Text>
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
        placeholder="••••••••"
      />
      {error && <Text style={{ color: colors.danger, marginBottom: spacing.md }}>{error}</Text>}
      <Button label="Log In" onPress={handleSubmit} loading={submitting} disabled={!email || !password} />
      <View style={{ marginTop: spacing.lg, alignItems: 'center' }}>
        <Link href="/register">
          <Text style={{ color: colors.primary, fontWeight: '600' }}>Don't have an account? Sign up</Text>
        </Link>
      </View>
    </Screen>
  );
}
