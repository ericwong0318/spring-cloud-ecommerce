import { Box, Link as MuiLink } from '@mui/material';
import { useRef, useEffect } from 'react';

interface SkipLinkProps {
  targets?: Array<{ id: string; label: string }>;
}

export function SkipLink({ targets = [
  { id: 'main-content', label: 'Skip to main content' },
  { id: 'navigation', label: 'Skip to navigation' },
  { id: 'search', label: 'Skip to search' },
] }: SkipLinkProps) {
  const linkRefs = useRef<HTMLAnchorElement[]>([]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Tab' && !event.shiftKey) {
        const activeElement = document.activeElement;
        if (activeElement === linkRefs.current[0]) {
          event.preventDefault();
          const mainContent = document.getElementById('main-content');
          if (mainContent) {
            mainContent.focus();
            mainContent.scrollIntoView({ behavior: 'smooth' });
          }
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <Box sx={{ position: 'fixed', top: 0, left: 0, zIndex: 9999 }}>
      {targets.map((target, index) => (
        <MuiLink
          key={target.id}
          ref={(el) => { linkRefs.current[index] = el as HTMLAnchorElement; }}
          href={`#${target.id}`}
          sx={{
            display: 'block',
            padding: '8px 16px',
            backgroundColor: 'primary.main',
            color: 'primary.contrastText',
            textDecoration: 'none',
            fontWeight: 600,
            transform: 'translateY(-100%)',
            transition: 'transform 0.2s ease-in-out',
            '&:focus': {
              transform: 'translateY(0)',
              outline: '3px solid',
              outlineOffset: 2,
              zIndex: 10000,
            },
          }}
        >
          {target.label}
        </MuiLink>
      ))}
    </Box>
  );
}

export default SkipLink;