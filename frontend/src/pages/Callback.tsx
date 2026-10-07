import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, useLocation } from 'react-router-dom';
import { Box, Typography, CircularProgress, Alert, Button, Paper } from '@mui/material';
import { useAuth } from '../hooks/useAuth';

export function Callback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { handleCallback, isAuthenticated } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const code = searchParams.get('code');
    const state = searchParams.get('state');
    const errorParam = searchParams.get('error');
    const errorDescription = searchParams.get('error_description');

    if (errorParam) {
      setError(errorDescription || `OAuth error: ${errorParam}`);
      setIsLoading(false);
      return;
    }

    if (!code || !state) {
      setError('Invalid callback parameters. Missing code or state.');
      setIsLoading(false);
      return;
    }

    handleCallback(code, state)
      .then(() => {
        setIsLoading(false);
        // Navigate to intended page or account
        const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/account';
        navigate(from, { replace: true });
      })
      .catch((err) => {
        setError(err.message || 'Authentication failed. Please try again.');
        setIsLoading(false);
      });
  }, [searchParams, handleCallback, navigate]);

  if (isAuthenticated) {
    navigate('/account', { replace: true });
    return null;
  }

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
      <Paper elevation={0} variant="outlined" sx={{ p: 4, maxWidth: 480, width: '100%', textAlign: 'center' }}>
        {isLoading ? (
          <>
            <CircularProgress sx={{ mb: 2 }} />
            <Typography variant="h6" fontWeight={600} gutterBottom>Completing Sign In</Typography>
            <Typography variant="body2" color="text.secondary">Please wait while we authenticate you...</Typography>
          </>
        ) : error ? (
          <>
            <Alert severity="error" sx={{ mb: 3 }}>
              <Typography variant="h6" gutterBottom>Authentication Failed</Typography>
              <Typography variant="body2">{error}</Typography>
            </Alert>
            <Button variant="contained" onClick={() => navigate('/login')}>
              Back to Login
            </Button>
          </>
        ) : null}
      </Paper>
    </Box>
  );
}