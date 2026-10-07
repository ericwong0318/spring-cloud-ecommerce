import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { Box, Typography, TextField, Button, Link, Alert, Paper, IconButton, InputAdornment, FormControlLabel, Checkbox, Divider, Stack, CircularProgress } from '@mui/material';
import { Visibility, VisibilityOff, Google, Facebook, Apple } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, type LoginFormData } from '../utils/validators';
import { useAuth } from '../hooks/useAuth';

export function Login() {
  const navigate = useNavigate();
  const { login, initiateOAuthLogin, isAuthenticated, isInitializing } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    setError('');
    try {
      await login(data.email, data.password);
    } catch (err) {
      setError('Invalid email or password. Please try again.');
      setIsLoading(false);
    }
  };

  const handleOAuthLogin = () => {
    // OAuth login via PKCE - will be implemented with social login provider
    initiateOAuthLogin();
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
          Sign In
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph sx={{ mb: 3, textAlign: 'center' }}>
          Welcome back! Please sign in to your account.
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit(onSubmit)}>
          <Stack spacing={2}>
            <TextField
              label="Email"
              type="email"
              fullWidth
              required
              {...register('email')}
              error={!!errors.email}
              helperText={errors.email?.message}
              autoComplete="email"
            />
            <TextField
              label="Password"
              type={showPassword ? 'text' : 'password'}
              fullWidth
              required
              {...register('password')}
              error={!!errors.password}
              helperText={errors.password?.message}
              autoComplete="current-password"
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setShowPassword(!showPassword)} edge="end">
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <FormControlLabel control={<Checkbox />} label="Remember me" />
              <Link component={RouterLink} to="/forgot-password" variant="body2">
                Forgot password?
              </Link>
            </Box>

            <Button type="submit" variant="contained" size="large" fullWidth disabled={isLoading}>
              {isLoading ? <CircularProgress size={24} /> : 'Sign In'}
            </Button>
          </Stack>
        </form>

        <Divider sx={{ my: 3 }}>
          <Typography variant="body2" color="text.secondary">OR</Typography>
        </Divider>

        {/* Social Login Buttons */}
        <Stack spacing={1.5}>
          <Button variant="outlined" fullWidth startIcon={<Google />} onClick={() => handleOAuthLogin()}>
            Continue with Google
          </Button>
          <Button variant="outlined" fullWidth startIcon={<Facebook />} onClick={() => handleOAuthLogin()}>
            Continue with Facebook
          </Button>
          <Button variant="outlined" fullWidth startIcon={<Apple />} onClick={() => handleOAuthLogin()}>
            Continue with Apple
          </Button>
        </Stack>

        <Typography variant="body2" align="center" sx={{ mt: 3 }}>
          Don't have an account?{' '}
          <Link component={RouterLink} to="/register" variant="body2" fontWeight={500}>
            Create Account
          </Link>
        </Typography>

        <Typography variant="caption" color="text.secondary" align="center" display="block" sx={{ mt: 2 }}>
          By signing in, you agree to our Terms of Service and Privacy Policy.
        </Typography>
      </Paper>
    </Box>
  );
}