import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { Box, Typography, TextField, Button, Link, Alert, Paper, Stack, CircularProgress, Checkbox, FormControlLabel } from '@mui/material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerSchema, type RegisterFormData } from '../utils/validators';
import { useAuth } from '../hooks/useAuth';

export function Register() {
  const navigate = useNavigate();
  const { register, isAuthenticated, isInitializing } = useAuth();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { register: registerField, handleSubmit, formState: { errors } } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    setError('');
    setSuccess('');
    try {
      await register({
        email: data.email,
        password: data.password,
        firstName: data.firstName,
        lastName: data.lastName,
        phone: data.phone,
      });
      setSuccess('Account created successfully! Redirecting to login...');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError('Registration failed. Please try again.');
      setIsLoading(false);
    }
  };

  if (isAuthenticated) {
    navigate('/account');
    return null;
  }

  if (isInitializing) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 480, mx: 'auto', py: 6 }}>
      <Paper elevation={0} variant="outlined" sx={{ p: 4 }}>
        <Typography variant="h4" component="h1" fontWeight={700} gutterBottom textAlign="center">
          Create Account
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph sx={{ mb: 3, textAlign: 'center' }}>
          Join our community and enjoy exclusive benefits.
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>
            {success}
          </Alert>
        )}

        <form onSubmit={handleSubmit(onSubmit)}>
          <Stack spacing={2}>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                label="First Name"
                fullWidth
                required
                {...registerField('firstName')}
                error={!!errors.firstName}
                helperText={errors.firstName?.message}
                autoComplete="given-name"
              />
              <TextField
                label="Last Name"
                fullWidth
                required
                {...registerField('lastName')}
                error={!!errors.lastName}
                helperText={errors.lastName?.message}
                autoComplete="family-name"
              />
            </Box>

            <TextField
              label="Email"
              type="email"
              fullWidth
              required
              {...registerField('email')}
              error={!!errors.email}
              helperText={errors.email?.message}
              autoComplete="email"
            />

            <TextField
              label="Password"
              type="password"
              fullWidth
              required
              {...registerField('password')}
              error={!!errors.password}
              helperText={errors.password?.message}
              autoComplete="new-password"
            />

            <TextField
              label="Confirm Password"
              type="password"
              fullWidth
              required
              {...registerField('confirmPassword')}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword?.message}
              autoComplete="new-password"
            />

            <TextField
              label="Phone (optional)"
              type="tel"
              fullWidth
              {...registerField('phone')}
              error={!!errors.phone}
              helperText={errors.phone?.message}
              autoComplete="tel"
            />

            <FormControlLabel
              control={<Checkbox required />}
              label={<Typography variant="body2">I agree to the <Link href="/terms" target="_blank">Terms of Service</Link> and <Link href="/privacy" target="_blank">Privacy Policy</Link></Typography>}
            />

            <Button type="submit" variant="contained" size="large" fullWidth disabled={isLoading}>
              {isLoading ? <CircularProgress size={24} /> : 'Create Account'}
            </Button>
          </Stack>
        </form>

        <Typography variant="body2" align="center" sx={{ mt: 3 }}>
          Already have an account?{' '}
          <Link component={RouterLink} to="/login" variant="body2" fontWeight={500}>
            Sign In
          </Link>
        </Typography>
      </Paper>
    </Box>
  );
}