import { useState, useEffect } from 'react';

export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    
    const media = window.matchMedia(query);
    if (media.matches !== matches) {
      setMatches(media.matches);
    }
    
    const listener = (event: MediaQueryListEvent) => {
      setMatches(event.matches);
    };
    
    media.addEventListener('change', listener);
    return () => media.removeEventListener('change', listener);
  }, [matches, query]);

  return matches;
}

export function useBreakpoint() {
  const isXs = useMediaQuery(`(max-width: 599px)`);
  const isSm = useMediaQuery(`(min-width: 600px) and (max-width: 899px)`);
  const isMd = useMediaQuery(`(min-width: 900px) and (max-width: 1199px)`);
  const isLg = useMediaQuery(`(min-width: 1200px) and (max-width: 1535px)`);
  const isXl = useMediaQuery(`(min-width: 1536px)`);
  
  const isMobile = isXs || isSm;
  const isTablet = isMd;
  const isDesktop = isLg || isXl;
  
  return {
    isXs,
    isSm,
    isMd,
    isLg,
    isXl,
    isMobile,
    isTablet,
    isDesktop,
  };
}