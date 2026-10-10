import { Box, Container, CssBaseline } from '@mui/material';
import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { Footer } from './Footer';

export function Layout() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <CssBaseline />
      <Header />
      <Container id="main-content" maxWidth="xl" sx={{ flexGrow: 1, py: 3, width: '100%' }} tabIndex={-1}>
        <Outlet />
      </Container>
      <Footer />
    </Box>
  );
}

export function AuthLayout() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <CssBaseline />
      <Container maxWidth="md" sx={{ flexGrow: 1, py: 6, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Outlet />
      </Container>
    </Box>
  );
}

export function BlankLayout() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <CssBaseline />
      <Outlet />
    </Box>
  );
}