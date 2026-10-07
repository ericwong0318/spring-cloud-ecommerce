import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/en';

dayjs.extend(relativeTime);
dayjs.locale('en');

export function formatCurrency(amount: number, currency = 'USD', locale = 'en-US'): string {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function formatPrice(amount: number, currency = 'USD'): string {
  return formatCurrency(amount / 100, currency);
}

export function formatDate(date: string | Date, format = 'MMM D, YYYY'): string {
  return dayjs(date).format(format);
}

export function formatDateTime(date: string | Date, format = 'MMM D, YYYY h:mm A'): string {
  return dayjs(date).format(format);
}

export function formatRelativeTime(date: string | Date): string {
  return dayjs(date).fromNow();
}

export function formatOrderStatus(status: string): { label: string; color: 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' } {
  const statusMap: Record<string, { label: string; color: 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' }> = {
    PENDING: { label: 'Pending', color: 'warning' },
    CONFIRMED: { label: 'Confirmed', color: 'info' },
    PROCESSING: { label: 'Processing', color: 'primary' },
    SHIPPED: { label: 'Shipped', color: 'primary' },
    DELIVERED: { label: 'Delivered', color: 'success' },
    CANCELLED: { label: 'Cancelled', color: 'error' },
    RETURNED: { label: 'Returned', color: 'warning' },
    REFUNDED: { label: 'Refunded', color: 'info' },
  };
  return statusMap[status] || { label: status, color: 'default' };
}

export function formatPaymentStatus(status: string): { label: string; color: 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' } {
  const statusMap: Record<string, { label: string; color: 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' }> = {
    PENDING: { label: 'Pending', color: 'warning' },
    AUTHORIZED: { label: 'Authorized', color: 'info' },
    CAPTURED: { label: 'Captured', color: 'success' },
    FAILED: { label: 'Failed', color: 'error' },
    REFUNDED: { label: 'Refunded', color: 'info' },
    PARTIALLY_REFUNDED: { label: 'Partially Refunded', color: 'warning' },
  };
  return statusMap[status] || { label: status, color: 'default' };
}

export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength).trim() + '...';
}

export function slugify(text: string): string {
  return text
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, '')
    .replace(/[\s_-]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export function generateOrderNumber(): string {
  const timestamp = Date.now().toString(36).toUpperCase();
  const random = Math.random().toString(36).substring(2, 8).toUpperCase();
  return `ORD-${timestamp}-${random}`;
}

export function debounce<T extends (...args: unknown[]) => unknown>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;
  return (...args: Parameters<T>) => {
    if (timeoutId) clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func(...args), wait);
  };
}

export function classNames(...classes: (string | boolean | undefined | null)[]): string {
  return classes.filter(Boolean).join(' ');
}

export function getInitials(firstName: string, lastName: string): string {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
}

export function validateEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

export function validatePhone(phone: string): boolean {
  const phoneRegex = /^[\+]?[(]?[0-9]{3}[)]?[-\s\.]?[0-9]{3}[-\s\.]?[0-9]{4,6}$/;
  return phoneRegex.test(phone);
}

export function validatePostalCode(postalCode: string, country = 'US'): boolean {
  if (country === 'US') {
    return /^\d{5}(-\d{4})?$/.test(postalCode);
  }
  // Basic validation for other countries
  return postalCode.length >= 3 && postalCode.length <= 10;
}