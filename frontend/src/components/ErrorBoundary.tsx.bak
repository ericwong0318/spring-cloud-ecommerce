import { Component, ErrorInfo, ReactNode } from 'react';
import { Alert, Button, Box, Typography } from '@mui/material';
import { Refresh, BugReport } from '@mui/icons-material';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<{ children: ReactNode; fallback?: ReactNode }, State> {
  state: State = { hasError: false, error: null, errorInfo: null };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error, errorInfo: null };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
    // In production, send to Sentry/analytics here
    // Sentry.captureException(error, { extra: errorInfo });
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
    // Force re-render by updating key or using window.location.reload()
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '60vh',
            padding: 4,
            textAlign: 'center',
          }}
        >
          <BugReport sx={{ fontSize: 64, color: 'error.main', mb: 2 }} />
          <Typography variant="h4" gutterBottom>
            Something went wrong
          </Typography>
          <Typography color="text.secondary" paragraph sx={{ maxWidth: 600 }}>
            We've encountered an unexpected error. Our team has been notified.
          </Typography>
          {this.state.error && (
            <Box sx={{ mt: 3, p: 2, bgcolor: 'error.light', borderRadius: 2, maxWidth: 600, textAlign: 'left' }}>
              <Typography variant="body2" color="error.dark" fontFamily="monospace" sx={{ whiteSpace: 'pre-wrap' }}>
                {this.state.error?.message}
              </Typography>
            </Box>
          )}
          <Box sx={{ mt: 3, display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
            <Button
              variant="contained"
              startIcon={<Refresh />}
              onClick={this.handleRetry}
              size="large"
            >
              Reload Page
            </Button>
            <Button
              variant="outlined"
              onClick={() => window.location.href = '/'}
            >
              Go Home
            </Button>
          </Box>
          {process.env.NODE_ENV === 'development' && this.state.errorInfo && (
            <details sx={{ mt: 4, maxWidth: 800, textAlign: 'left' }}>
              <summary>Error Details (Development)</summary>
              <pre sx={{ mt: 2, p: 2, bgcolor: 'grey.100', overflow: 'auto', maxHeight: 300, fontSize: '0.75rem' }}>
                {this.state.error?.stack}
                {'\n\n'}
                {this.state.errorInfo?.componentStack}
              </pre>
            </details>
          )}
        </Box>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;