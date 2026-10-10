import { Box, Button, Typography, Alert, AlertTitle, Collapse } from '@mui/material';
import { Home, Refresh, BugReport, ExpandMore } from '@mui/icons-material';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@utils/constants';

interface ServerErrorProps {
  error?: Error | null;
  resetError?: () => void;
}

export function ServerError({ error, resetError }: ServerErrorProps) {
  const navigate = useNavigate();
  const [showDetails, setShowDetails] = useState(false);

  const handleReload = () => {
    if (resetError) {
      resetError();
    } else {
      window.location.reload();
    }
  };

  const handleGoHome = () => navigate(ROUTES.HOME);

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '70vh',
        padding: 4,
        textAlign: 'center',
      }}
    >
      <BugReport sx={{ fontSize: 80, color: 'error.main', mb: 2 }} />
      <Typography variant="h3" fontWeight={600} gutterBottom>
        Something Went Wrong
      </Typography>
      <Typography color="text.secondary" paragraph sx={{ maxWidth: 500, mb: 4 }}>
        We've encountered an unexpected error. Our team has been notified and is working on a fix.
      </Typography>

      <Alert severity="error" variant="outlined" sx={{ width: '100%', maxWidth: 600, mb: 3, textAlign: 'left' }}>
        <AlertTitle>Error Details</AlertTitle>
        <Typography variant="body2" fontFamily="monospace" sx={{ whiteSpace: 'pre-wrap' }}>
          {error?.message || 'An unknown error occurred'}
        </Typography>
        <Box sx={{ mt: 1 }}>
          <Button
            size="small"
            onClick={() => setShowDetails(!showDetails)}
            startIcon={showDetails ? <ExpandMore /> : <ExpandMore />}
          >
            {showDetails ? 'Hide Details' : 'Show Details'}
          </Button>
        </Box>
        <Collapse in={showDetails}>
          <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.100', borderRadius: 1, fontSize: '0.75rem', fontFamily: 'monospace', textAlign: 'left', maxHeight: 300, overflow: 'auto' }}>
            {error?.stack || 'No stack trace available'}
          </Box>
        </Collapse>
      </Alert>

      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
        <Button
          variant="contained"
          size="large"
          startIcon={<Refresh />}
          onClick={handleReload}
        >
          Reload Page
        </Button>
        <Button
          variant="outlined"
          size="large"
          startIcon={<Home />}
          onClick={handleGoHome}
        >
          Go Home
        </Button>
      </Box>

      <Box sx={{ mt: 6, color: 'text.secondary', fontSize: '0.875rem' }}>
        <Typography variant="caption">
          If this problem persists, please{' '}
          <a href="/contact" style={{ color: 'inherit' }}>contact support</a>
          {' '}with the error details above.
        </Typography>
      </Box>
    </Box>
  );
}