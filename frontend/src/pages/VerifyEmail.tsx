import { useState, useEffect } from 'react';
import { Box, Button, Typography, Alert, AlertTitle, Paper, CircularProgress } from '@mui/material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useNotification } from '@context/NotificationContext';
import { authApi } from '@services/endpoints';
import { ROUTES } from '@utils/constants';
import { Email as EmailIcon, CheckCircle, Error as ErrorIcon, Info } from '@mui/icons-material';

export function VerifyEmail() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { showNotification } = useNotification();
  const [status, setStatus] = useState<'loading' | 'success' | 'error' | 'info'>('loading');
  const [message, setMessage] = useState<string>('');
  const token = searchParams.get('token');

  useEffect(() => {
    const verifyEmail = async () => {
      if (!token) {
        setStatus('error');
        setMessage('Invalid verification link. No token provided.');
        return;
      }

      try {
        await authApi.verifyEmail(token);
        setStatus('success');
        setMessage('Your email has been verified successfully!');
        showNotification({ type: 'success', message: 'Email verified successfully!' });
      } catch (error: any) {
        setStatus('error');
        if (error.response?.status === 400) {
          setMessage('This verification link is invalid or has expired.');
        } else {
          setMessage('Failed to verify email. Please try again later.');
        }
        showNotification({ type: 'error', message: 'Failed to verify email.' });
      }
    };

    verifyEmail();
  }, [token, showNotification]);

  const getIcon = () => {
    switch (status) {
      case 'success':
        return <CheckCircle sx={{ fontSize: 64, color: 'success.main', mb: 2 }} />;
      case 'error':
        return <ErrorIcon sx={{ fontSize: 64, color: 'error.main', mb: 2 }} />;
      case 'loading':
        return <CircularProgress size={64} sx={{ mb: 2 }} />;
      default:
        return <Info sx={{ fontSize: 64, color: 'info.main', mb: 2 }} />;
    }
  };

  const getTitle = () => {
    switch (status) {
      case 'success':
        return 'Email Verified';
      case 'error':
        return 'Verification Failed';
      case 'loading':
        return 'Verifying Email...';
      default:
        return 'Email Verification';
    }
  };

  return (
    <Paper elevation={3} sx={{ p: 4, maxWidth: 440, mx: 'auto', mt: 8, textAlign: 'center' }}>
      {getIcon()}
      <Typography variant="h5" fontWeight={600} gutterBottom>
        {getTitle()}
      </Typography>
      <Typography color="text.secondary" paragraph>
        {message}
      </Typography>
      {status === 'success' && (
        <Box sx={{ mt: 3, display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Button variant="contained" fullWidth onClick={() => navigate(ROUTES.LOGIN)}>
            Go to Login
          </Button>
          <Button variant="outlined" fullWidth onClick={() => navigate(ROUTES.HOME)}>
            Continue Shopping
          </Button>
        </Box>
      )}
      {status === 'error' && (
        <Box sx={{ mt: 3, display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Button variant="contained" fullWidth onClick={() => navigate(ROUTES.FORGOT_PASSWORD)}>
            Request New Verification Email
          </Button>
          <Button variant="outlined" fullWidth onClick={() => navigate(ROUTES.LOGIN)}>
            Back to Login
          </Button>
        </Box>
      )}
      {status === 'loading' && (
        <Box sx={{ mt: 3 }}>
          <Typography variant="body2" color="text.secondary">
            Please wait while we verify your email...
          </Typography>
        </Box>
      )}
    </Paper>
  );
}