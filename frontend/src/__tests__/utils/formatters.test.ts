import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  formatCurrency,
  formatPrice,
  formatDate,
  formatDateTime,
  formatRelativeTime,
  formatOrderStatus,
  formatPaymentStatus,
  truncateText,
  slugify,
  generateOrderNumber,
  debounce,
  classNames,
  getInitials,
  validateEmail,
  validatePhone,
  validatePostalCode,
} from '@utils/formatters';

describe('formatters', () => {
  describe('formatCurrency', () => {
    it('should format USD currency correctly', () => {
      expect(formatCurrency(1000, 'USD')).toBe('$1,000.00');
    });

    it('should format EUR currency correctly', () => {
      expect(formatCurrency(1000, 'EUR', 'de-DE')).toBe('1.000,00 €');
    });

    it('should handle zero amount', () => {
      expect(formatCurrency(0, 'USD')).toBe('$0.00');
    });

    it('should handle negative amounts', () => {
      expect(formatCurrency(-100, 'USD')).toBe('-$100.00');
    });
  });

  describe('formatPrice', () => {
    it('should convert cents to dollars and format', () => {
      expect(formatPrice(10000)).toBe('$100.00');
    });

    it('should handle zero cents', () => {
      expect(formatPrice(0)).toBe('$0.00');
    });

    it('should handle odd cents', () => {
      expect(formatPrice(999)).toBe('$9.99');
    });
  });

  describe('formatDate', () => {
    it('should format date with default format', () => {
      expect(formatDate('2024-01-15')).toBe('Jan 15, 2024');
    });

    it('should format date with custom format', () => {
      expect(formatDate('2024-01-15', 'YYYY-MM-DD')).toBe('2024-01-15');
    });

    it('should handle Date objects', () => {
      expect(formatDate(new Date('2024-01-15'))).toBe('Jan 15, 2024');
    });
  });

  describe('formatDateTime', () => {
    it('should format date and time with default format', () => {
      expect(formatDateTime('2024-01-15T14:30:00')).toBe('Jan 15, 2024 2:30 PM');
    });

    it('should format with custom format', () => {
      expect(formatDateTime('2024-01-15T14:30:00', 'YYYY/MM/DD HH:mm')).toBe('2024/01/15 14:30');
    });
  });

  describe('formatRelativeTime', () => {
    it('should return relative time string', () => {
      const now = new Date();
      const result = formatRelativeTime(now);
      expect(result).toContain('ago');
    });
  });

  describe('formatOrderStatus', () => {
    it('should return correct label and color for known statuses', () => {
      expect(formatOrderStatus('PENDING')).toEqual({ label: 'Pending', color: 'warning' });
      expect(formatOrderStatus('CONFIRMED')).toEqual({ label: 'Confirmed', color: 'info' });
      expect(formatOrderStatus('PROCESSING')).toEqual({ label: 'Processing', color: 'primary' });
      expect(formatOrderStatus('SHIPPED')).toEqual({ label: 'Shipped', color: 'primary' });
      expect(formatOrderStatus('DELIVERED')).toEqual({ label: 'Delivered', color: 'success' });
      expect(formatOrderStatus('CANCELLED')).toEqual({ label: 'Cancelled', color: 'error' });
      expect(formatOrderStatus('RETURNED')).toEqual({ label: 'Returned', color: 'warning' });
      expect(formatOrderStatus('REFUNDED')).toEqual({ label: 'Refunded', color: 'info' });
    });

    it('should return default for unknown status', () => {
      expect(formatOrderStatus('UNKNOWN')).toEqual({ label: 'UNKNOWN', color: 'default' });
    });
  });

  describe('formatPaymentStatus', () => {
    it('should return correct label and color for known statuses', () => {
      expect(formatPaymentStatus('PENDING')).toEqual({ label: 'Pending', color: 'warning' });
      expect(formatPaymentStatus('AUTHORIZED')).toEqual({ label: 'Authorized', color: 'info' });
      expect(formatPaymentStatus('CAPTURED')).toEqual({ label: 'Captured', color: 'success' });
      expect(formatPaymentStatus('FAILED')).toEqual({ label: 'Failed', color: 'error' });
      expect(formatPaymentStatus('REFUNDED')).toEqual({ label: 'Refunded', color: 'info' });
      expect(formatPaymentStatus('PARTIALLY_REFUNDED')).toEqual({ label: 'Partially Refunded', color: 'warning' });
    });

    it('should return default for unknown status', () => {
      expect(formatPaymentStatus('UNKNOWN')).toEqual({ label: 'UNKNOWN', color: 'default' });
    });
  });

  describe('truncateText', () => {
    it('should return original text if shorter than maxLength', () => {
      expect(truncateText('Hello', 10)).toBe('Hello');
    });

    it('should truncate and add ellipsis if longer than maxLength', () => {
      expect(truncateText('Hello World', 8)).toBe('Hello...');
    });

    it('should handle exact length', () => {
      expect(truncateText('Hello', 5)).toBe('Hello');
    });
  });

  describe('slugify', () => {
    it('should convert text to slug', () => {
      expect(slugify('Hello World')).toBe('hello-world');
    });

    it('should remove special characters', () => {
      expect(slugify('Hello @World!')).toBe('hello-world');
    });

    it('should handle multiple spaces', () => {
      expect(slugify('Hello   World')).toBe('hello-world');
    });

    it('should handle leading/trailing spaces', () => {
      expect(slugify('  Hello World  ')).toBe('hello-world');
    });

    it('should handle empty string', () => {
      expect(slugify('')).toBe('');
    });
  });

  describe('generateOrderNumber', () => {
    it('should generate order number with correct prefix', () => {
      const orderNumber = generateOrderNumber();
      expect(orderNumber).toMatch(/^ORD-[A-Z0-9]+-[A-Z0-9]+$/);
    });

    it('should generate unique numbers', () => {
      const numbers = new Set();
      for (let i = 0; i < 100; i++) {
        numbers.add(generateOrderNumber());
      }
      expect(numbers.size).toBe(100);
    });
  });

  describe('debounce', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('should delay function execution', () => {
      const fn = vi.fn();
      const debouncedFn = debounce(fn, 100);

      debouncedFn('arg1');
      expect(fn).not.toHaveBeenCalled();

      vi.advanceTimersByTime(100);
      expect(fn).toHaveBeenCalledWith('arg1');
    });

    it('should cancel previous timeout on rapid calls', () => {
      const fn = vi.fn();
      const debouncedFn = debounce(fn, 100);

      debouncedFn('arg1');
      debouncedFn('arg2');
      debouncedFn('arg3');

      vi.advanceTimersByTime(100);
      expect(fn).toHaveBeenCalledTimes(1);
      expect(fn).toHaveBeenCalledWith('arg3');
    });
  });

  describe('classNames', () => {
    it('should join class names', () => {
      expect(classNames('a', 'b', 'c')).toBe('a b c');
    });

    it('should filter out falsy values', () => {
      expect(classNames('a', false, 'b', null, undefined, 'c')).toBe('a b c');
    });

    it('should handle empty input', () => {
      expect(classNames()).toBe('');
    });
  });

  describe('getInitials', () => {
    it('should return initials from first and last name', () => {
      expect(getInitials('John', 'Doe')).toBe('JD');
    });

    it('should handle lowercase names', () => {
      expect(getInitials('john', 'doe')).toBe('JD');
    });

    it('should handle empty names', () => {
      expect(getInitials('', '')).toBe('');
    });
  });

  describe('validateEmail', () => {
    it('should return true for valid emails', () => {
      expect(validateEmail('test@example.com')).toBe(true);
      expect(validateEmail('user.name@domain.org')).toBe(true);
      expect(validateEmail('user+tag@example.co.uk')).toBe(true);
    });

    it('should return false for invalid emails', () => {
      expect(validateEmail('invalid')).toBe(false);
      expect(validateEmail('invalid@')).toBe(false);
      expect(validateEmail('@domain.com')).toBe(false);
      expect(validateEmail('')).toBe(false);
    });
  });

  describe('validatePhone', () => {
    it('should return true for valid US phone numbers', () => {
      expect(validatePhone('(555) 123-4567')).toBe(true);
      expect(validatePhone('555-123-4567')).toBe(true);
      expect(validatePhone('5551234567')).toBe(true);
      expect(validatePhone('+1 555 123 4567')).toBe(true);
    });

    it('should return false for invalid phone numbers', () => {
      expect(validatePhone('123')).toBe(false);
      expect(validatePhone('abc-def-ghij')).toBe(false);
      expect(validatePhone('')).toBe(false);
    });
  });

  describe('validatePostalCode', () => {
    it('should validate US postal codes', () => {
      expect(validatePostalCode('12345', 'US')).toBe(true);
      expect(validatePostalCode('12345-6789', 'US')).toBe(true);
      expect(validatePostalCode('1234', 'US')).toBe(false);
      expect(validatePostalCode('123456', 'US')).toBe(false);
    });

    it('should validate other countries with basic check', () => {
      expect(validatePostalCode('SW1A 1AA', 'GB')).toBe(true);
      expect(validatePostalCode('V5K 0A1', 'CA')).toBe(true);
      expect(validatePostalCode('AB', 'XX')).toBe(false);
      expect(validatePostalCode('12345678901', 'XX')).toBe(false);
    });
  });
});