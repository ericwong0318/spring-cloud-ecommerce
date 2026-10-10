import { Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ThemeProvider, CssBaseline, Box, CircularProgress } from '@mui/material';
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
import { CheckoutSuccess } from '@pages/CheckoutSuccess';
import { Orders } from '@pages/Orders';
import { OrderDetail } from '@pages/OrderDetail';
import { Login } from '@pages/Login';
import { Register } from '@pages/Register';
import { Callback } from '@pages/Callback';
import { ForgotPassword } from '@pages/ForgotPassword';
import { ResetPassword } from '@pages/ResetPassword';
import { VerifyEmail } from '@pages/VerifyEmail';
import { NotFound } from '@pages/NotFound';
import { ServerError } from '@pages/ServerError';

// Components
import { Layout } from '@components/Layout';
import { SkipLink } from '@components/SkipLink';

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
        <Route path={ROUTES.CHECKOUT_SUCCESS} element={<CheckoutSuccess />} />
        <Route path={ROUTES.ACCOUNT_ORDERS} element={<Orders />} />
        <Route path={ROUTES.ACCOUNT_ORDER_DETAIL} element={<OrderDetail />} />
      </Route>

      {/* Error Pages */}
      <Route path="/404" element={<NotFound />} />
      <Route path="/500" element={<ServerError />} />

      {/* Default redirect */}
      <Route path="*" element={<Navigate to="/404" replace />} />
    </Routes>
  );
}

function NotificationContainer() {
  const { notifications, hideNotification } = useNotification();

  return (
    <Box sx={{ position: 'fixed', top: 80, right: 16, zIndex: 1400, display: 'flex', flexDirection: 'column', gap: 1, maxWidth: 400 }}>
      {notifications.map((notification) => (
        <Box key={notification.id}>
          {/* Notification rendered by NotificationProvider via portal or similar */}
        </Box>
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
              <SkipLink />
              <AppRoutes />
            </NotificationProvider>
          </CartProvider>
        </AuthProvider>
      </ThemeProvider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}