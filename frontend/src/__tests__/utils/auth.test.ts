import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  getAuthTokens,
  setAuthTokens,
  clearAuthTokens,
  getAccessToken,
  isTokenExpired,
  parseJwt,
  getTokenClaims,
  getUserRolesFromToken,
  hasRole,
  hasAnyRole,
} from '@utils/auth';

const mockTokens = {
  accessToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiVVNFUiIsIkFETUlOIl19LCJzY29wZSI6InJlYWQgd3JpdGUifQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c',
  refreshToken: 'refresh-token-123',
  expiresIn: 3600,
  tokenType: 'Bearer',
};

describe('auth utils', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.resetAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('getAuthTokens', () => {
    it('should return null when no tokens stored', () => {
      expect(getAuthTokens()).toBeNull();
    });

    it('should return parsed tokens when valid JSON stored', () => {
      localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));
      expect(getAuthTokens()).toEqual(mockTokens);
    });

    it('should return null when invalid JSON stored', () => {
      localStorage.setItem('auth_tokens', 'invalid-json');
      expect(getAuthTokens()).toBeNull();
    });

    it('should handle localStorage errors gracefully', () => {
      vi.spyOn(localStorage, 'getItem').mockImplementationOnce(() => {
        throw new Error('Storage error');
      });
      expect(getAuthTokens()).toBeNull();
    });
  });

  describe('setAuthTokens', () => {
    it('should store tokens in localStorage', () => {
      setAuthTokens(mockTokens);
      expect(localStorage.getItem('auth_tokens')).toBe(JSON.stringify(mockTokens));
    });
  });

  describe('clearAuthTokens', () => {
    it('should remove tokens from localStorage', () => {
      localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));
      clearAuthTokens();
      expect(localStorage.getItem('auth_tokens')).toBeNull();
    });
  });

  describe('getAccessToken', () => {
    it('should return null when no tokens', () => {
      expect(getAccessToken()).toBeNull();
    });

    it('should return access token when tokens exist', () => {
      localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));
      expect(getAccessToken()).toBe(mockTokens.accessToken);
    });
  });

  describe('isTokenExpired', () => {
    it('should return true for expired token', () => {
      const expiredTokens = { ...mockTokens, expiresIn: -100 };
      expect(isTokenExpired(expiredTokens)).toBe(true);
    });

    it('should return false for valid token', () => {
      expect(isTokenExpired(mockTokens)).toBe(false);
    });

    it('should return true when token expires within 1 minute', () => {
      const nearExpiryTokens = { ...mockTokens, expiresIn: 30 }; // 30 seconds
      expect(isTokenExpired(nearExpiryTokens)).toBe(true);
    });
  });

  describe('parseJwt', () => {
    it('should parse valid JWT', () => {
      const claims = parseJwt(mockTokens.accessToken);
      expect(claims).not.toBeNull();
      expect(claims?.sub).toBe('1234567890');
      expect(claims?.name).toBe('John Doe');
    });

    it('should return null for invalid JWT', () => {
      expect(parseJwt('invalid.token')).toBeNull();
    });

    it('should return null for malformed JWT', () => {
      expect(parseJwt('not.a.jwt')).toBeNull();
    });
  });

  describe('getTokenClaims', () => {
    it('should return claims from token', () => {
      const claims = getTokenClaims(mockTokens.accessToken);
      expect(claims).not.toBeNull();
      expect(claims?.sub).toBe('1234567890');
    });
  });

  describe('getUserRolesFromToken', () => {
    it('should return empty array when no tokens', () => {
      expect(getUserRolesFromToken()).toEqual([]);
    });

    it('should return roles from realm_access', () => {
      localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));
      const roles = getUserRolesFromToken();
      expect(roles).toContain('USER');
      expect(roles).toContain('ADMIN');
    });
  });

  describe('hasRole', () => {
    it('should return false when no tokens', () => {
      expect(hasRole('USER')).toBe(false);
    });

    it('should return true when user has role', () => {
      localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));
      expect(hasRole('USER')).toBe(true);
      expect(hasRole('ADMIN')).toBe(true);
    });

    it('should return false when user does not have role', () => {
      localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));
      expect(hasRole('SUPER_ADMIN')).toBe(false);
    });
  });

  describe('hasAnyRole', () => {
    it('should return false when no tokens', () => {
      expect(hasAnyRole(['USER', 'ADMIN'])).toBe(false);
    });

    it('should return true when user has any of the roles', () => {
      localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));
      expect(hasAnyRole(['USER', 'SUPER_ADMIN'])).toBe(true);
      expect(hasAnyRole(['ADMIN', 'MODERATOR'])).toBe(true);
    });

    it('should return false when user has none of the roles', () => {
      localStorage.setItem('auth_tokens', JSON.stringify(mockTokens));
      expect(hasAnyRole(['SUPER_ADMIN', 'MODERATOR'])).toBe(false);
    });
  });
});