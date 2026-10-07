import { createContext, useContext, useReducer, useEffect, useCallback, ReactNode } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { authApi } from '@services/endpoints';
import type { User, AuthTokens } from '../types/domain';
import { getAuthTokens, setAuthTokens, clearAuthTokens, isTokenExpired, getUserRolesFromToken } from '@utils/auth';
import { OAUTH_CONFIG } from '@utils/constants';

interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isInitializing: boolean;
}

type AuthAction =
  | { type: 'INIT_START' }
  | { type: 'INIT_SUCCESS'; payload: { user: User; tokens: AuthTokens } }
  | { type: 'INIT_FAILURE' }
  | { type: 'LOGIN_START' }
  | { type: 'LOGIN_SUCCESS'; payload: { user: User; tokens: AuthTokens } }
  | { type: 'LOGIN_FAILURE' }
  | { type: 'LOGOUT' }
  | { type: 'UPDATE_USER'; payload: Partial<User> }
  | { type: 'REFRESH_TOKENS'; payload: AuthTokens };

const initialState: AuthState = {
  user: null,
  tokens: null,
  isAuthenticated: false,
  isLoading: false,
  isInitializing: true,
};

function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'INIT_START':
      return { ...state, isInitializing: true };
    case 'INIT_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        tokens: action.payload.tokens,
        isAuthenticated: true,
        isInitializing: false,
        isLoading: false,
      };
    case 'INIT_FAILURE':
      return {
        ...state,
        user: null,
        tokens: null,
        isAuthenticated: false,
        isInitializing: false,
        isLoading: false,
      };
    case 'LOGIN_START':
      return { ...state, isLoading: true };
    case 'LOGIN_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        tokens: action.payload.tokens,
        isAuthenticated: true,
        isLoading: false,
      };
    case 'LOGIN_FAILURE':
      return {
        ...state,
        user: null,
        tokens: null,
        isAuthenticated: false,
        isLoading: false,
      };
    case 'LOGOUT':
      return {
        ...state,
        user: null,
        tokens: null,
        isAuthenticated: false,
        isLoading: false,
      };
    case 'UPDATE_USER':
      return {
        ...state,
        user: state.user ? { ...state.user, ...action.payload } : null,
      };
    case 'REFRESH_TOKENS':
      return {
        ...state,
        tokens: action.payload,
      };
    default:
      return state;
  }
}

interface AuthContextType extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  register: (data: { email: string; password: string; firstName: string; lastName: string; phone?: string }) => Promise<void>;
  updateProfile: (data: Partial<User>) => Promise<void>;
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  requestPasswordReset: (email: string) => Promise<void>;
  resetPassword: (token: string, password: string) => Promise<void>;
  verifyEmail: (token: string) => Promise<void>;
  resendVerificationEmail: () => Promise<void>;
  initiateOAuthLogin: () => void;
  handleCallback: (code: string, state: string) => Promise<void>;
  hasRole: (role: string) => boolean;
  hasAnyRole: (roles: string[]) => boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialState);
  const navigate = useNavigate();
  const location = useLocation();

  const initializeAuth = useCallback(async () => {
    dispatch({ type: 'INIT_START' });
    
    const tokens = getAuthTokens();
    if (!tokens?.accessToken) {
      dispatch({ type: 'INIT_FAILURE' });
      return;
    }

    if (isTokenExpired(tokens)) {
      // Try to refresh token
      try {
        const response = await fetch(`${OAUTH_CONFIG.AUTH_SERVER_URL}/oauth2/token`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({
            grant_type: 'refresh_token',
            refresh_token: tokens.refreshToken || '',
            client_id: OAUTH_CONFIG.CLIENT_ID,
          }),
        });
        
        if (response.ok) {
          const newTokens = await response.json();
          const updatedTokens = { ...tokens, accessToken: newTokens.access_token, expiresIn: newTokens.expires_in };
          setAuthTokens(updatedTokens);
          
          // Fetch user profile
          const userResponse = await authApi.getProfile();
          dispatch({ type: 'INIT_SUCCESS', payload: { user: userResponse, tokens: updatedTokens } });
          return;
        }
      } catch {
        // Refresh failed, clear tokens
      }
      clearAuthTokens();
      dispatch({ type: 'INIT_FAILURE' });
      return;
    }

    try {
      const user = await authApi.getProfile();
      dispatch({ type: 'INIT_SUCCESS', payload: { user, tokens } });
    } catch {
      clearAuthTokens();
      dispatch({ type: 'INIT_FAILURE' });
    }
  }, []);

  useEffect(() => {
    initializeAuth();
  }, [initializeAuth]);

  const login = async (email: string, password: string) => {
    dispatch({ type: 'LOGIN_START' });
    try {
      const tokens = await authApi.login(email, password);
      setAuthTokens(tokens);
      const user = await authApi.getProfile();
      dispatch({ type: 'LOGIN_SUCCESS', payload: { user, tokens } });
      navigate('/account');
    } catch {
      dispatch({ type: 'LOGIN_FAILURE' });
      throw new Error('Invalid credentials');
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      // Ignore logout errors
    } finally {
      clearAuthTokens();
      dispatch({ type: 'LOGOUT' });
      navigate('/');
    }
  };

  const register = async (data: { email: string; password: string; firstName: string; lastName: string; phone?: string }) => {
    dispatch({ type: 'LOGIN_START' });
    try {
      await authApi.register(data);
      // After registration, log in automatically
      await login(data.email, data.password);
    } catch {
      dispatch({ type: 'LOGIN_FAILURE' });
      throw new Error('Registration failed');
    }
  };

  const updateProfile = async (data: Partial<User>) => {
    try {
      const updatedUser = await authApi.updateProfile(data);
      dispatch({ type: 'UPDATE_USER', payload: updatedUser });
    } catch {
      throw new Error('Failed to update profile');
    }
  };

  const changePassword = async (currentPassword: string, newPassword: string) => {
    await authApi.changePassword(currentPassword, newPassword);
  };

  const requestPasswordReset = async (email: string) => {
    await authApi.requestPasswordReset(email);
  };

  const resetPassword = async (token: string, password: string) => {
    await authApi.resetPassword(token, password);
  };

  const verifyEmail = async (token: string) => {
    await authApi.verifyEmail(token);
  };

  const resendVerificationEmail = async () => {
    await authApi.resendVerificationEmail();
  };

  const initiateOAuthLogin = async () => {
    const codeVerifier = generateCodeVerifier();
    const codeChallenge = await generateCodeChallenge(codeVerifier);
    const state = generateState();
    
    // Store PKCE values in sessionStorage
    sessionStorage.setItem('pkce_code_verifier', codeVerifier);
    sessionStorage.setItem('pkce_state', state);
    
    const params = new URLSearchParams({
      response_type: OAUTH_CONFIG.RESPONSE_TYPE,
      client_id: OAUTH_CONFIG.CLIENT_ID,
      redirect_uri: OAUTH_CONFIG.REDIRECT_URI,
      scope: OAUTH_CONFIG.SCOPE,
      code_challenge: codeChallenge,
      code_challenge_method: OAUTH_CONFIG.CODE_CHALLENGE_METHOD,
      state,
    });
    
    window.location.href = `${OAUTH_CONFIG.AUTH_SERVER_URL}/oauth2/authorize?${params.toString()}`;
  };

  const handleCallback = async (code: string, state: string) => {
    const storedState = sessionStorage.getItem('pkce_state');
    const codeVerifier = sessionStorage.getItem('pkce_code_verifier');
    
    if (state !== storedState || !codeVerifier) {
      throw new Error('Invalid state parameter');
    }
    
    try {
      const response = await fetch(`${OAUTH_CONFIG.AUTH_SERVER_URL}/oauth2/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          grant_type: 'authorization_code',
          code,
          redirect_uri: OAUTH_CONFIG.REDIRECT_URI,
          client_id: OAUTH_CONFIG.CLIENT_ID,
          code_verifier: codeVerifier,
        }),
      });
      
      if (!response.ok) {
        throw new Error('Token exchange failed');
      }
      
      const tokens = await response.json();
      const authTokens: AuthTokens = {
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        idToken: tokens.id_token,
        expiresIn: tokens.expires_in,
        tokenType: tokens.token_type,
        scope: tokens.scope,
      };
      
      setAuthTokens(authTokens);
      sessionStorage.removeItem('pkce_code_verifier');
      sessionStorage.removeItem('pkce_state');
      
      const user = await authApi.getProfile();
      dispatch({ type: 'LOGIN_SUCCESS', payload: { user, tokens: authTokens } });
      
      // Redirect to intended page or account
      const from = location.state?.from?.pathname || '/account';
      navigate(from, { replace: true });
    } catch {
      throw new Error('Authentication failed');
    }
  };

  const hasRole = (role: string): boolean => {
    return getUserRolesFromToken().includes(role);
  };

  const hasAnyRole = (roles: string[]): boolean => {
    const userRoles = getUserRolesFromToken();
    return roles.some((role) => userRoles.includes(role));
  };

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        logout,
        register,
        updateProfile,
        changePassword,
        requestPasswordReset,
        resetPassword,
        verifyEmail,
        resendVerificationEmail,
        initiateOAuthLogin,
        handleCallback,
        hasRole,
        hasAnyRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

// PKCE Helper Functions
function generateCodeVerifier(): string {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return btoa(String.fromCharCode(...array))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

async function generateCodeChallenge(codeVerifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(codeVerifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

function generateState(): string {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return btoa(String.fromCharCode(...array))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}