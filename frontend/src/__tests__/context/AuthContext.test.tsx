import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '@context/AuthContext';
import { ThemeProvider } from '@mui/material';
import { theme } from '@styles/theme';

const mockUser = {
  id: 'user-1',
  email: 'test@example.com',
  firstName: 'John',
  lastName: 'Doe',
  roles: ['USER'],
  emailVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
};

const mockTokens = {
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  expiresIn: 3600,
  tokenType: 'Bearer',
};

const renderWithProviders = (component: React.ReactElement) => {
  return render(
    <ThemeProvider theme={theme}>
      <AuthProvider>
        {component}
      </AuthProvider>
    </ThemeProvider>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('provides auth state and actions', () => {
    const TestComponent = () => {
      const { isAuthenticated, user, login } = useAuth();
      return (
        <div>
          <span data-testid="authenticated">{isAuthenticated.toString()}</span>
          <span data-testid="user-email">{user?.email ?? 'none'}</span>
          <button onClick={() => login('test@example.com', 'password')} data-testid="login-btn">
            Login
          </button>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);
    expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    expect(screen.getByTestId('user-email')).toHaveTextContent('none');
  });

  it('initializes from localStorage on mount', async () => {
    localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockUser,
    } as Response);

    const TestComponent = () => {
      const { isAuthenticated, isInitializing, user } = useAuth();
      return (
        <div>
          <span data-testid="initializing">{isInitializing.toString()}</span>
          <span data-testid="authenticated">{isAuthenticated.toString()}</span>
          <span data-testid="user-email">{user?.email ?? 'none'}</span>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);

    await waitFor(() => {
      expect(screen.getByTestId('initializing')).toHaveTextContent('false');
    });

    expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    expect(screen.getByTestId('user-email')).toHaveTextContent('test@example.com');
  });

  it('logs in successfully', async () => {
    const TestComponent = () => {
      const { login, isLoading, error } = useAuth();
      return (
        <div>
          <span data-testid="loading">{isLoading.toString()}</span>
          <span data-testid="error">{error ?? 'none'}</span>
          <button
            onClick={() => login('test@example.com', 'password')}
            data-testid="login-btn"
            disabled={isLoading}
          >
            Login
          </button>
        </div>
      );
    };

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({ ...mockTokens, accessToken: 'new-access-token' }),
    } as Response);

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockUser,
    } as Response);

    renderWithProviders(<TestComponent />);

    await act(async () => {
      fireEvent.click(screen.getByTestId('login-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    expect(screen.getByTestId('error')).toHaveTextContent('none');
  });

  it('handles login error', async () => {
    const TestComponent = () => {
      const { login, isLoading, error } = useAuth();
      return (
        <div>
          <span data-testid="loading">{isLoading.toString()}</span>
          <span data-testid="error">{error ?? 'none'}</span>
          <button
            onClick={() => login('test@example.com', 'wrong-password')}
            data-testid="login-btn"
            disabled={isLoading}
          >
            Login
          </button>
        </div>
      );
    };

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: async () => ({ message: 'Invalid credentials' }),
    } as Response);

    renderWithProviders(<TestComponent />);

    await act(async () => {
      fireEvent.click(screen.getByTestId('login-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    expect(screen.getByTestId('error')).toHaveTextContent('Invalid credentials');
  });

  it('logs out successfully', async () => {
    localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockUser,
    } as Response);

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({}),
    } as Response);

    const TestComponent = () => {
      const { isAuthenticated, logout, isLoading } = useAuth();
      return (
        <div>
          <span data-testid="authenticated">{isAuthenticated.toString()}</span>
          <span data-testid="loading">{isLoading.toString()}</span>
          <button onClick={() => logout()} data-testid="logout-btn" disabled={isLoading}>
            Logout
          </button>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('logout-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    expect(localStorage.getItem('auth_tokens')).toBeNull();
  });

  it('checks user roles correctly', async () => {
    localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockUser,
    } as Response);

    const TestComponent = () => {
      const { hasRole, hasAnyRole } = useAuth();
      return (
        <div>
          <span data-testid="has-user">{hasRole('USER').toString()}</span>
          <span data-testid="has-admin">{hasRole('ADMIN').toString()}</span>
          <span data-testid="has-any">{hasAnyRole(['USER', 'MODERATOR']).toString()}</span>
          <span data-testid="has-none">{hasAnyRole(['ADMIN', 'SUPER_ADMIN']).toString()}</span>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);

    await waitFor(() => {
      expect(screen.getByTestId('has-user')).toHaveTextContent('true');
    });

    expect(screen.getByTestId('has-admin')).toHaveTextContent('false');
    expect(screen.getByTestId('has-any')).toHaveTextContent('true');
    expect(screen.getByTestId('has-none')).toHaveTextContent('false');
  });

  it('updates user profile', async () => {
    localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockUser,
    } as Response);

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({ ...mockUser, firstName: 'Jane' }),
    } as Response);

    const TestComponent = () => {
      const { user, updateUser, isLoading } = useAuth();
      return (
        <div>
          <span data-testid="first-name">{user?.firstName ?? 'none'}</span>
          <span data-testid="loading">{isLoading.toString()}</span>
          <button
            onClick={() => updateUser({ firstName: 'Jane' })}
            data-testid="update-btn"
            disabled={isLoading}
          >
            Update
          </button>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);

    await waitFor(() => {
      expect(screen.getByTestId('first-name')).toHaveTextContent('John');
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('update-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    expect(screen.getByTestId('first-name')).toHaveTextContent('Jane');
  });
});

import { fireEvent } from '@testing-library/react';