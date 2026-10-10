import { useState, useEffect } from 'react';
import { Box, Button, TextField, Typography, Alert, AlertTitle, Paper, Link } from '@mui/material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useNotification } from '@context/NotificationContext';
import { authApi } from '@services/endpoints';
import { ROUTES } from '@utils/constants';

const schema = z.object({
  password: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

type FormData = z.infer<typeof schema>;

export function ResetPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { showNotification } = useNotification();
  const [loading, setLoading] = useState(false);
  const [tokenValid, setTokenValid] = useState<boolean | null>(null);
  const token = searchParams.get('token');

  const { register, handleSubmit, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const password = watch('password');

  useEffect(() => {
    if (!token) {
      setTokenValid(false);
    } else {
      setTokenValid(true);
    }
  }, [token]);

  const onSubmit = async (data: FormData) => {
    if (!token) return;

    try {
      setLoading(true);
      await authApi.resetPassword(token, data.password);
      showNotification({ type: 'success', message: 'Password has been reset successfully.' });
      navigate(ROUTES.LOGIN);
    } catch {
      showNotification({ type: 'error', message: 'Failed to reset password. The link may have expired.' });
    } finally {
      setLoading(false);
    }
  };

  if (tokenValid === false) {
    return (
      <Paper elevation={3} sx={{ p: 4, maxWidth: 440, mx: 'auto', mt: 8 }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          <AlertTitle>Invalid Reset Link</AlertTitle>
          This password reset link is invalid or has expired.
        </Alert>
        <Button variant="contained" fullWidth onClick={() => navigate(ROUTES.FORGOT_PASSWORD)}>
          Request New Reset Link
        </Button>
      </Paper>
    );
  }

  return (
    <Paper elevation={3} sx={{ p: 4, maxWidth: 440, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom align="center">
        Reset Password
      </Typography>
      <Typography color="text.secondary" paragraph align="center">
        Enter your new password below.
      </Typography>
      <Box component="form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 3 }}>
        <TextField
          fullWidth
          label="New Password"
          type="password"
          autoComplete="new-password"
          {...register('password')}
          error={!!errors.password}
          helperText={errors.password?.message}
          sx={{ mb: 2 }}
        />
        <TextField
          fullWidth
          label="Confirm Password"
          type="password"
          autoComplete="new-password"
          {...register('confirmPassword')}
          error={!!errors.confirmPassword}
          helperText={errors.confirmPassword?.message}
          sx={{ mb: 2 }}
        />
        {password && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="caption" color="text.secondary" display="block">
              Strength: {getPasswordStrength(password)}
            </Typography>
          </Box>
        )}
        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={loading}
        >
          {loading ? 'Resetting...' : 'Reset Password'}
        </Button>
      </Box>
      <Box sx={{ mt: 3, textAlign: 'center' }}>
        <Link href={ROUTES.LOGIN} variant="body2">
          Back to Login
        </Link>
      </Box>
    </Paper>
  );
}

function getPasswordStrength(password: string): string {
  let strength = 0;
  if (password.length >= 8) strength++;
  if (/[A-Z]/.test(password)) strength++;
  if (/[a-z]/.test(password)) strength++;
  if (/[0-9]/.test(password)) strength++;
  if (/[^A-Za-z0-9]/.test(password)) strength++;

  switch (strength) {
    case 5:
    case 4:
      return 'Strong';
    case 3:
      return 'Medium';
    case 2:
      return 'Weak';
    default:
      return 'Very Weak';
  }
}