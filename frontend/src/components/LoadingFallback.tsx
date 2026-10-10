import { Box, CircularProgress, Typography, Skeleton, Stack, Paper } from '@mui/material';

interface LoadingFallbackProps {
  variant?: 'spinner' | 'skeleton' | 'page';
  message?: string;
  rows?: number;
}

export function LoadingFallback({ variant = 'spinner', message = 'Loading...', rows = 3 }: LoadingFallbackProps) {
  switch (variant) {
    case 'skeleton':
      return (
        <Box sx={{ p: 2 }}>
          <Skeleton variant="rectangular" width="40%" height={40} sx={{ mb: 2 }} />
          {Array.from({ length: rows }).map((_, i) => (
            <Skeleton key={i} variant="rectangular" width={i === rows - 1 ? '60%' : '100%'} height={16} sx={{ mb: 1 }} />
          ))}
        </Box>
      );

    case 'page':
      return (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '50vh',
            padding: 4,
            textAlign: 'center',
          }}
        >
          <CircularProgress size={48} sx={{ mb: 3 }} />
          <Typography variant="h6" color="text.secondary">
            {message}
          </Typography>
        </Box>
      );

    case 'spinner':
    default:
      return (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 4,
            textAlign: 'center',
          }}
        >
          <CircularProgress sx={{ mb: 2 }} />
          <Typography variant="body2" color="text.secondary">
            {message}
          </Typography>
        </Box>
      );
  }
}

export function CardSkeleton({ lines = 3, image = true }: { lines?: number; image?: boolean }) {
  return (
    <Paper elevation={1} sx={{ p: 0, overflow: 'hidden', height: '100%', display: 'flex', flexDirection: 'column' }}>
      {image && (
        <Skeleton variant="rectangular" width="100%" height={200} />
      )}
      <Box sx={{ p: 2, flex: 1, display: 'flex', flexDirection: 'column' }}>
        <Skeleton variant="rectangular" width="60%" height={24} sx={{ mb: 1 }} />
        <Skeleton variant="rectangular" width="100%" height={20} sx={{ mb: 1 }} />
        {Array.from({ length: lines }).map((_, i) => (
          <Skeleton key={i} variant="rectangular" width={i === lines - 1 ? '70%' : '100%'} height={12} sx={{ mb: 0.5 }} />
        ))}
        <Box sx={{ mt: 'auto', pt: 2 }}>
          <Skeleton variant="rectangular" width="30%" height={36} />
        </Box>
      </Box>
    </Paper>
  );
}

export function TableSkeleton({ rows = 5, columns = 5 }: { rows?: number; columns?: number }) {
  return (
    <Box>
      <Box sx={{ display: 'flex', mb: 1 }}>
        {Array.from({ length: columns }).map((_, i) => (
          <Skeleton key={`header-${i}`} variant="rectangular" width={`100%`} height={40} sx={{ flex: 1, mr: i < columns - 1 ? 2 : 0 }} />
        ))}
      </Box>
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <Box key={rowIndex} sx={{ display: 'flex', mb: 1 }}>
          {Array.from({ length: columns }).map((_, colIndex) => (
            <Skeleton
              key={`cell-${rowIndex}-${colIndex}`}
              variant="rectangular"
              width={`100%`}
              height={48}
              sx={{ flex: 1, mr: colIndex < columns - 1 ? 2 : 0 }}
            />
          ))}
        </Box>
      ))}
    </Box>
  );
}

export function ListSkeleton({ items = 5, hasAvatar = true, lines = 2 }: { items?: number; hasAvatar?: boolean; lines?: number }) {
  return (
    <Box>
      {Array.from({ length: items }).map((_, i) => (
        <Paper key={i} elevation={0} variant="outlined" sx={{ mb: 1, p: 2 }}>
          <Box sx={{ display: 'flex', gap: 2 }}>
            {hasAvatar && (
              <Skeleton variant="circular" width={48} height={48} sx={{ flexShrink: 0 }} />
            )}
            <Box sx={{ flex: 1 }}>
              <Skeleton variant="rectangular" width="40%" height={20} sx={{ mb: 1 }} />
              {Array.from({ length: lines }).map((_, j) => (
                <Skeleton key={j} variant="rectangular" width={j === lines - 1 ? '60%' : '80%'} height={14} sx={{ mb: 0.5 }} />
              ))}
            </Box>
          </Box>
        </Paper>
      ))}
    </Box>
  );
}

export default LoadingFallback;