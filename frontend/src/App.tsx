import { Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ThemeProvider, CssBaseline, Alert, Button, Box, CircularProgress, Typography } from '@mui/material';
import { AuthProvider, useAuth } from '@context/AuthContext';
import { CartProvider } from '@context/CartContext';
import { NotificationProvider, useNotification } from '@context/NotificationContext';
import { theme } from '@styles/theme';
import { ROUTES } from '@utils/constants';

// Pages
import { Home } from '@pages/Home';
import { ProductList } from '@pages/ProductList';
import { ProductDetail } from '@pages/ProductDetail';
import { Cart } from '@pages/Cart';
import { Checkout } from '@pages/Checkout';
import { CheckoutSuccess } from '@pages/CheckoutSuccess';
import { Orders } from '@pages/Orders';
import { OrderDetail } from '@pages/OrderDetail';
import { Account } from '@pages/Account';
import { Login } from '@pages/Login';
import { Register } from '@pages/Register';
import { Callback } from '@pages/Callback';

// Components
import { Layout } from '@components/Layout';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

// Protected route wrapper
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isInitializing } = useAuth();

  if (isInitializing) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

// Public route wrapper (redirect if already authenticated)
function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isInitializing } = useAuth();

  if (isInitializing) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/account" replace />;
  }

  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      {/* Public Routes */}
      <Route element={<Layout />}>
        <Route path={ROUTES.HOME} element={<Home />} />
        <Route path={ROUTES.PRODUCTS} element={<ProductList />} />
        <Route path={ROUTES.PRODUCT_DETAIL} element={<ProductDetail />} />
        <Route path={ROUTES.CART} element={<Cart />} />
        <Route path={ROUTES.LOGIN} element={<PublicRoute><Login /></PublicRoute>} />
        <Route path={ROUTES.REGISTER} element={<PublicRoute><Register /></PublicRoute>} />
        <Route path={ROUTES.CALLBACK} element={<Callback />} />
        <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPassword />} />
        <Route path={ROUTES.RESET_PASSWORD} element={<ResetPassword />} />
        <Route path={ROUTES.VERIFY_EMAIL} element={<VerifyEmail />} />
      </Route>

      {/* Protected Routes */}
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path={ROUTES.CHECKOUT} element={<Checkout />} />
        <Route path={ROUTES.CHECKOUT_SUCCESS} element={<CheckoutSuccess />} />
        <Route path={ROUTES.ACCOUNT_ORDERS} element={<Orders />} />
        <Route path={ROUTES.ACCOUNT_ORDER_DETAIL} element={<OrderDetail />} />
        <Route path={ROUTES.ACCOUNT_ADDRESSES} element={<Addresses />} />
        <Route path={ROUTES.ACCOUNT_PROFILE} element={<Account />} />
        <Route path={ROUTES.ACCOUNT_SECURITY} element={<Account />} />
        <Route path={ROUTES.ACCOUNT} element={<Account />} />
      </Route>

      {/* Default redirect */}
      <Route path="*" element={<Navigate to={ROUTES.HOME} replace />} />
    </Routes>
  );
}

// Placeholder pages (to be implemented)
function ForgotPassword() {
  return (
    <Box sx={{ textAlign: 'center', py: 6 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>Forgot Password</Typography>
      <Typography color="text.secondary">Password reset functionality coming soon.</Typography>
    </Box>
  );
}

function ResetPassword() {
  return (
    <Box sx={{ textAlign: 'center', py: 6 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>Reset Password</Typography>
      <Typography color="text.secondary">Password reset functionality coming soon.</Typography>
    </Box>
  );
}

function VerifyEmail() {
  return (
    <Box sx={{ textAlign: 'center', py: 6 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>Verify Email</Typography>
      <Typography color="text.secondary">Email verification functionality coming soon.</Typography>
    </Box>
  );
}

function Addresses() {
  return (
    <Box sx={{ textAlign: 'center', py: 6 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>Addresses</Typography>
      <Typography color="text.secondary">Address management functionality coming soon.</Typography>
    </Box>
  );
}

function NotificationContainer() {
  const { notifications, hideNotification } = useNotification();

  return (
    <Box sx={{ position: 'fixed', top: 80, right: 16, zIndex: 1400, display: 'flex', flexDirection: 'column', gap: 1, maxWidth: 400 }}>
      {notifications.map((notification) => (
        <Alert
          key={notification.id}
          severity={notification.type}
          onClose={() => hideNotification(notification.id)}
          action={
            notification.action ? (
              <Button color="inherit" size="small" onClick={notification.action.onClick}>
                {notification.action.label}
              </Button>
            ) : undefined
          }
          sx={{ boxShadow: 3 }}
        >
          {notification.message}
        </Alert>
      ))}
    </Box>
  );
}

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <AuthProvider>
          <CartProvider>
            <NotificationProvider>
              <AppRoutes />
              <NotificationContainer />
            </NotificationProvider>
          </CartProvider>
        </AuthProvider>
      </ThemeProvider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}