import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useDebounce } from '@hooks/useDebounce';

describe('useDebounce', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should return initial value immediately', () => {
    const { result } = renderHook(() => useDebounce('initial', 500));
    expect(result.current).toBe('initial');
  });

  it('should debounce value changes', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'initial', delay: 500 } }
    );

    expect(result.current).toBe('initial');

    rerender({ value: 'updated', delay: 500 });
    expect(result.current).toBe('initial'); // Still initial during debounce

    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(result.current).toBe('initial'); // Still initial

    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(result.current).toBe('updated'); // Now updated
  });

  it('should reset timer on rapid changes', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'initial', delay: 500 } }
    );

    rerender({ value: 'first', delay: 500 });
    act(() => {
      vi.advanceTimersByTime(300);
    });

    rerender({ value: 'second', delay: 500 });
    act(() => {
      vi.advanceTimersByTime(300);
    });

    expect(result.current).toBe('initial'); // Still initial because timer reset

    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(result.current).toBe('second'); // Now updated
  });
});