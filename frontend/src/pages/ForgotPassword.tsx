import { useState } from 'react';
import { Box, Button, TextField, Typography, Alert, AlertTitle, Paper, Link } from '@mui/material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { useNotification } from '@context/NotificationContext';
import { authApi } from '@services/endpoints';
import { ROUTES } from '@utils/constants';

const schema = z.object({
  email: z.string().email('Invalid email address'),
});

type FormData = z.infer<typeof schema>;

export function ForgotPassword() {
  const navigate = useNavigate();
  const { showNotification } = useNotification();
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    try {
      setLoading(true);
      await authApi.requestPasswordReset(data.email);
      setSubmitted(true);
      showNotification({ type: 'success', message: 'If an account exists, a reset link has been sent.' });
    } catch {
      showNotification({ type: 'error', message: 'Failed to send reset email. Please try again.' });
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <Paper elevation={3} sx={{ p: 4, maxWidth: 440, mx: 'auto', mt: 8 }}>
        <Alert severity="success" sx={{ mb: 3 }}>
          <AlertTitle>Email Sent</AlertTitle>
          If an account with that email exists, you'll receive a password reset link shortly.
        </Alert>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Button variant="contained" fullWidth onClick={() => navigate(ROUTES.LOGIN)}>
            Back to Login
          </Button>
          <Button variant="outlined" fullWidth onClick={() => setSubmitted(false)}>
            Send Another Email
          </Button>
        </Box>
      </Paper>
    );
  }

  return (
    <Paper elevation={3} sx={{ p: 4, maxWidth: 440, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom align="center">
        Forgot Password
      </Typography>
      <Typography color="text.secondary" paragraph align="center">
        Enter your email and we'll send you a link to reset your password.
      </Typography>
      <Box component="form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 3 }}>
        <TextField
          fullWidth
          label="Email"
          type="email"
          autoComplete="email"
          {...register('email')}
          error={!!errors.email}
          helperText={errors.email?.message}
          sx={{ mb: 2 }}
        />
        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={loading}
        >
          {loading ? 'Sending...' : 'Send Reset Link'}
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