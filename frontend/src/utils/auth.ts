import type { AuthTokens } from '../types/domain';

const TOKEN_KEY = 'auth_tokens';

export function getAuthTokens(): AuthTokens | null {
  try {
    const stored = localStorage.getItem(TOKEN_KEY);
    if (stored) {
      return JSON.parse(stored) as AuthTokens;
    }
  } catch {
    // Ignore parse errors
  }
  return null;
}

export function setAuthTokens(tokens: AuthTokens): void {
  localStorage.setItem(TOKEN_KEY, JSON.stringify(tokens));
}

export function clearAuthTokens(): void {
  localStorage.removeItem(TOKEN_KEY);
  // Note: refresh token is in httpOnly cookie, cleared by server on logout
}

export function getAccessToken(): string | null {
  return getAuthTokens()?.accessToken ?? null;
}

export function isTokenExpired(tokens: AuthTokens): boolean {
  const expiryTime = Date.now() + tokens.expiresIn * 1000;
  return Date.now() >= expiryTime - 60000; // Consider expired 1 minute before actual expiry
}

export function parseJwt(token: string): Record<string, unknown> | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export function getTokenClaims(token: string): Record<string, unknown> | null {
  return parseJwt(token);
}

export function getUserRolesFromToken(): string[] {
  const tokens = getAuthTokens();
  if (!tokens?.accessToken) return [];
  
  const claims = getTokenClaims(tokens.accessToken);
  if (!claims) return [];
  
  // Handle both realm_access.roles and scope-based roles
  if (claims.realm_access && typeof claims.realm_access === 'object' && 'roles' in claims.realm_access) {
    return (claims.realm_access as { roles: string[] }).roles;
  }
  
  if (claims.scope && typeof claims.scope === 'string') {
    return claims.scope.split(' ');
  }
  
  return [];
}

export function hasRole(role: string): boolean {
  return getUserRolesFromToken().includes(role);
}

export function hasAnyRole(roles: string[]): boolean {
  const userRoles = getUserRolesFromToken();
  return roles.some((role) => userRoles.includes(role));
}