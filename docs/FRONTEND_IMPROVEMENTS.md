# Frontend Improvement Plan

## Overview
This document outlines the planned improvements for the frontend codebase to bring it to production-grade quality.

## Current State Assessment

### ✅ Strengths
- Comprehensive documentation (README.md)
- Modern tech stack (React 19, TypeScript, MUI v6, TanStack Query, React Hook Form + Zod)
- Clean architecture with proper separation of concerns
- Strong TypeScript usage
- Well-designed state management patterns
- Robust API layer with interceptors
- Robust Auth Context with PKCE
- Sophisticated Cart Context with optimistic updates
- Well-structured components with styled-components
- Feature-rich ProductList with filters, pagination, skeleton loading

### ❌ Gaps to Address

| Priority | Area | Description |
|----------|------|-------------|
| **High** | Placeholder Pages | ForgotPassword, ResetPassword, VerifyEmail, Addresses, Account Security pages are placeholders |
| **High** | Testing | No unit/integration tests visible |
| **High** | Error Boundaries | No React Error Boundary for graceful error handling |
| **Medium** | Accessibility | Good foundation but could add more ARIA attributes, skip links, focus management |
| **Medium** | Bundle Analysis | No bundle analyzer configured |
| **Medium** | Error Handling | No global error boundary, no 404 page, limited error UI |
| **Medium** | Loading States | Some components lack proper loading/error states |
| **Low** | Bundle Analysis | No bundle analyzer configured |
| **Low** | CI/CD | No visible CI/CD config for frontend |

## Improvement Plan

### Phase 1: Critical Fixes (Week 1)
- [ ] Implement Error Boundary component
- [ ] Implement 404/500 error pages
- [ ] Implement ForgotPassword page with proper form
- [ ] Implement ResetPassword page with token validation
- [ ] Implement VerifyEmail page
- [ ] Implement Addresses page with CRUD
- [ ] Implement Account Security page (change password, 2FA placeholder)

### Phase 2: Testing Infrastructure (Week 2)
- [ ] Set up Vitest + React Testing Library
- [ ] Add unit tests for hooks (useAuth, useCart, useDebounce, etc.)
- [ ] Add unit tests for utils (formatters, validators, auth)
- [ ] Add integration tests for contexts (AuthContext, CartContext)
- [ ] Add component tests for key components (ProductCard, ProductList, Cart)
- [ ] Add e2e tests with Playwright for critical flows

### Phase 3: Resilience & UX (Week 3)
- [ ] Add Error Boundary component with fallback UI
- [ ] Add 404 page (NotFound page)
- [ ] Add 500 page (ServerError page)
- [ ] Add global error logging (Sentry integration placeholder)
- [ ] Improve loading states across components
- [ ] Add skip-to-content link for accessibility
- [ ] Add focus management for modals/drawers
- [ ] Improve focus management in modals/drawers

### Phase 4: Performance & Monitoring (Week 4)
- [ ] Add bundle analyzer (vite-plugin-bundle-analyzer)
- [ ] Configure code splitting for routes
- [ ] Add React Query devtools in development
- [ ] Add Sentry integration placeholder
- [ ] Add performance monitoring hooks
- [ ] Optimize bundle size (tree shaking, dynamic imports)

### Phase 5: CI/CD & Quality Gates (Week 5)
- [ ] Add GitHub Actions workflow for frontend
- [ ] Add linting/typecheck in CI
- [ ] Add test coverage thresholds
- [ ] Add bundle size budget check
- [ ] Add accessibility audit in CI (axe-core)
- [ ] Add dependabot for dependency updates

## Technical Debt to Address

### Code Quality
- [ ] Fix duplicate plugin warnings in product/category pom.xml (not frontend but related)
- [ ] Remove duplicate spring-boot-maven-plugin in product pom.xml
- [ ] Fix duplicate postgresql dependency in product pom.xml
- [ ] Add proper JSDoc comments for public APIs
- [ ] Add more comprehensive TypeScript strict mode checks

### Architecture
- [ ] Consider extracting common hooks to shared package
- [ ] Consider extracting common types to shared package
- [ ] Evaluate if CartContext should use Immer for immutable updates
- [ ] Evaluate if AuthContext should use React Query mutations instead of custom logic

### Security
- [ ] Add CSP headers configuration
- [ ] Add XSS protection for user-generated content
- [ ] Add rate limiting awareness in API client
- [ ] Add secure cookie flags for tokens (when deployed)

### Performance
- [ ] Implement virtualization for large product lists
- [ ] Add image optimization (lazy loading, WebP, responsive images)
- [ ] Add service worker for offline support
- [ ] Implement prefetching for navigation

## File Structure After Improvements

```
frontend/
├── src/
│   ├── components/
│   │   ├── ErrorBoundary.tsx          # NEW
│   │   ├── NotFound.tsx               # NEW
│   │   ├── ServerError.tsx            # NEW
│   │   ├── LoadingFallback.tsx        # NEW
│   │   ├── SkipLink.tsx               # NEW
│   │   └── ...
│   ├── pages/
│   │   ├── ForgotPassword.tsx         # IMPLEMENTED
│   │   ├── ResetPassword.tsx          # IMPLEMENTED
│   │   ├── VerifyEmail.tsx            # IMPLEMENTED
│   │   ├── NotFound.tsx               # NEW
│   │   ├── ServerError.tsx            # NEW
│   │   ├── Addresses.tsx              # IMPLEMENTED
│   │   ├── Security.tsx               # NEW
│   │   └── ...
│   ├── components/
│   │   ├── ErrorBoundary.tsx          # NEW
│   │   ├── SkipLink.tsx               # NEW
│   │   ├── LoadingFallback.tsx        # NEW
│   │   └── ...
│   ├── hooks/
│   │   ├── useMediaQuery.ts
│   │   ├── useDebounce.ts
│   │   └── ...
│   ├── __tests__/                     # NEW
│   │   ├── hooks/
│   │   ├── utils/
│   │   ├── context/
│   │   ├── components/
│   │   └── pages/
│   ├── e2e/                           # NEW
│   │   ├── auth.spec.ts
│   │   ├── cart.spec.ts
│   │   ├── checkout.spec.ts
│   │   └── product.spec.ts
│   └── ...
├── .github/
│   └── workflows/
│       ├── frontend-ci.yml            # NEW
│       └── frontend-cd.yml            # NEW
├── vite.config.ts                     # Updated with bundle analyzer
├── vitest.config.ts                   # NEW
├── playwright.config.ts               # NEW
├── .eslintrc.json                     # Updated
├── .prettierrc
├── package.json                       # Updated with test scripts
├── vitest.config.ts
├── playwright.config.ts
└── README.md                          # Updated
```

## Success Metrics

| Metric | Target |
|--------|--------|
| Test Coverage | >80% lines, >70% branches |
| Bundle Size (gzipped) | <200KB initial load |
| Lighthouse Score | >90 Performance, >95 Accessibility |
| Test Coverage in CI | Required to pass |
| Bundle Size Budget | <200KB initial, <50KB per route |
| Accessibility Audit | 0 violations (axe-core) |
| TypeScript Strict Mode | Enabled, 0 errors |

## Dependencies to Add

### Dev Dependencies
```json
{
  "vitest": "^1.0.0",
  "@testing-library/react": "^14.0.0",
  "@testing-library/user-event": "^14.0.0",
  "@testing-library/jest-dom": "^6.0.0",
  "vitest-coverage-v8": "^1.0.0",
  "playwright": "^1.40.0",
  "@playwright/test": "^1.40.0",
  "vite-plugin-bundle-analyzer": "^0.10.0",
  "vite-plugin-checker": "^0.6.0",
  "@axe-core/react": "^4.8.0",
  "eslint-plugin-jsx-a11y": "^6.8.0",
  "eslint-plugin-testing-library": "^6.0.0",
  "@vitest/coverage-v8": "^1.0.0"
}
```

### Production Dependencies (if needed)
```json
{
  "@sentry/react": "^8.0.0",
  "@sentry/tracing": "^8.0.0",
  "react-error-boundary": "^4.0.0"
}
```

## Implementation Notes

### Error Boundary Implementation
```tsx
// components/ErrorBoundary.tsx
import { Component, ErrorInfo, ReactNode } from 'react';
import { Alert, Button, Box, Typography } from '@mui/material';
import { Refresh } from '@mui/icons-material';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { hasError: false, error: null };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
    // Send to Sentry/analytics here
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <Box sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h5" gutterBottom>Something went wrong</Typography>
          <Typography color="text.secondary" paragraph>
            {this.state.error?.message}
          </Typography>
          <Button variant="contained" startIcon={<Refresh />} onClick={() => window.location.reload()}>
            Reload Page
          </Button>
        </Box>
      );
    }
    return this.props.children;
  }
}
```

### ForgotPassword Page Implementation
```tsx
// pages/ForgotPassword.tsx
import { useState } from 'react';
import { Box, Button, TextField, Typography, Alert, AlertTitle, Paper } from '@mui/material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { useNotification } from '@context/NotificationContext';
import { authApi } from '@services/endpoints';

const schema = z.object({
  email: z.string().email('Invalid email address'),
});

export function ForgotPassword() {
  const navigate = useNavigate();
  const { showNotification } = useNotification();
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useResolver(schema);

  const onSubmit = async (data: { email: string }) => {
    try {
      setLoading(true);
      await authApi.requestPasswordReset(data.email);
      setSubmitted(true);
      showNotification('success', 'If an account exists, a reset link has been sent.');
    } catch {
      showNotification('error', 'Failed to send reset email');
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <Paper elevation={3} sx={{ p: 4, maxWidth: 440, mx: 'auto', mt: 8 }}>
        <Alert severity="success" sx={{ mb: 3 }}>
          <AlertTitle>Email Sent</AlertTitle>
          If an account with that email exists, you'll receive a password reset link shortly.
        </Alert>
        <Button variant="contained" fullWidth onClick={() => navigate('/login')}>
          Back to Login
        </Button>
      </Paper>
    );
  }

  return (
    <Paper elevation={3} sx={{ p: 4, maxWidth: 440, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom align="center">
        Forgot Password
      </Typography>
      <Typography color="text.secondary" paragraph align="center">
        Enter your email and we'll send you a link to reset your password.
      </Typography>
      <form onSubmit={handleSubmit(onSubmit)} sx={{ mt: 3 }}>
        <TextField
          fullWidth
          label="Email"
          type="email"
          {...register('email')}
          error={!!errors.email}
          helperText={errors.email?.message}
          sx={{ mb: 2 }}
        />
        <Button type="submit" variant="contained" fullWidth size="large" disabled={loading}>
          Send Reset Link
        </Button>
      </form>
    );
  }
}
```

## Next Steps

1. Start with Phase 1: Implement placeholder pages and error boundaries
2. Set up testing infrastructure
3. Implement missing pages
4. Add error boundaries and error pages
5. Set up testing infrastructure
6. Add CI/CD pipeline
6. Add bundle analyzer and performance monitoring
7. Add accessibility improvements
8. Document all changes

## Notes

- The category service is running locally on port 8082 with seed data (17 categories)
- The product service is running locally on port 8080 with seed data (33 products)
- The category service has a permissive security config for local development
- The product service needs the category service to be running for seeding
- Both services connect to local databases (PostgreSQL for category, MongoDB for product)
- Eureka server is running on port 8761
- Config server is running on port 8888
- Auth server is running on port 9000 (but unhealthy)