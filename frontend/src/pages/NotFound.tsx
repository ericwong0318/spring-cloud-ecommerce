import { Box, Button, Typography, Link } from '@mui/material';
import { Home, Search, ArrowBack } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@utils/constants';

export function NotFound() {
  const navigate = useNavigate();

  const handleGoHome = () => navigate(ROUTES.HOME);
  const handleGoBack = () => window.history.back();
  const handleSearch = () => navigate(ROUTES.PRODUCTS);

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
      <Typography variant="h1" fontSize="12rem" fontWeight={700} color="primary.main" lineHeight={1} sx={{ mb: 1 }}>
        404
      </Typography>
      <Typography variant="h3" fontWeight={600} gutterBottom>
        Page Not Found
      </Typography>
      <Typography color="text.secondary" paragraph sx={{ maxWidth: 500, mb: 4 }}>
        Sorry, we couldn't find the page you're looking for. It might have been moved,
        deleted, or never existed in the first place.
      </Typography>
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
        <Button
          variant="contained"
          size="large"
          startIcon={<Home />}
          onClick={handleGoHome}
        >
          Go Home
        </Button>
        <Button
          variant="outlined"
          size="large"
          startIcon={<Search />}
          onClick={handleSearch}
        >
          Browse Products
        </Button>
        <Button
          variant="text"
          startIcon={<ArrowBack />}
          onClick={handleGoBack}
        >
          Go Back
        </Button>
      </Box>
      <Box sx={{ mt: 6, display: 'flex', gap: 3, flexWrap: 'wrap', justifyContent: 'center' }}>
        <Link href={ROUTES.PRODUCTS} variant="body2">
          <Search sx={{ fontSize: 20, mr: 0.5, verticalAlign: 'middle' }} />
          Shop All Products
        </Link>
        <Link href={ROUTES.ACCOUNT} variant="body2">
          <Home sx={{ fontSize: 20, mr: 0.5, verticalAlign: 'middle' }} />
          My Account
        </Link>
        <Link href="/contact" variant="body2">
          Contact Support
        </Link>
      </Box>
    </Box>
  );
}