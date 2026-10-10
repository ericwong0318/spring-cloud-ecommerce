import '@testing-library/jest-dom';
import { vi } from 'vitest';
import React from 'react';

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock ResizeObserver
global.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

// Mock IntersectionObserver
global.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

// Mock scrollTo
window.scrollTo = vi.fn();

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock sessionStorage
const sessionStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};
Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock });

// Suppress console.error in tests (optional)
// const originalError = console.error;
// beforeAll(() => {
//   console.error = (...args) => {
//     if (args[0]?.includes?.('Warning: ReactDOM.render is no longer supported')) return;
//     originalError.call(console, ...args);
//   };
// });
// afterAll(() => {
//   console.error = originalError;
// });

// Mock React Query
vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(() => ({
    invalidateQueries: vi.fn(),
    setQueryData: vi.fn(),
    getQueryData: vi.fn(),
  })),
  QueryClient: vi.fn(),
  QueryClientProvider: ({ children }: { children: React.ReactNode }) => children,
}));

// Mock React Router
vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
  useSearchParams: () => [new URLSearchParams(), vi.fn()],
  useLocation: () => ({ pathname: '/' }),
  Routes: ({ children }: { children: React.ReactNode }) => children,
  Route: ({ children }: { children: React.ReactNode }) => children,
  Link: ({ children, to, ...props }: any) => <a href={to} {...props}>{children}</a>,
  Navigate: () => null,
  Outlet: () => null,
}));

// Mock MUI components that might cause issues in tests
vi.mock('@mui/material', async () => {
  const actual = await vi.importActual('@mui/material');
  return {
    ...actual,
    // Simplify components that might have complex rendering
  };
});

// Global test utilities
global.testUtils = {
  renderWithProviders: (ui: React.ReactElement) => {
    // This will be imported from @testing-library/react in actual tests
    return { ...ui };
  },
};

declare global {
  namespace Vi {
    interface Jest {
      testUtils: {
        renderWithProviders: (ui: React.ReactElement) => any;
      };
    }
  }
}