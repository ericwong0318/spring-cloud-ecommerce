import { Button as MuiButton, ButtonProps as MuiButtonProps } from '@mui/material';
import { styled } from '@mui/material/styles';

export const Button = styled(MuiButton)<MuiButtonProps>`
  &.MuiButton-root {
    text-transform: none;
    font-weight: 500;
    border-radius: 8px;
    padding: 8px 16px;
    font-size: 0.875rem;
    transition: all 0.2s ease;
  }
  
  &.MuiButton-contained {
    box-shadow: none;
    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }
  }
  
  &.MuiButton-outlined {
    border-width: 1.5px;
    &:hover {
      border-width: 1.5px;
    }
  }
`;

export const PrimaryButton = styled(MuiButton)`
  background-color: #1976d2;
  color: white;
  &:hover {
    background-color: #1565c0;
  }
`;

export const SecondaryButton = styled(MuiButton)`
  border-color: #9c27b0;
  color: #9c27b0;
  &:hover {
    background-color: rgba(156, 39, 176, 0.04);
    border-color: #7b1fa2;
  }
`;

export const DangerButton = styled(MuiButton)`
  color: #d32f2f;
  border-color: #d32f2f;
  &:hover {
    background-color: rgba(211, 47, 47, 0.04);
    border-color: #c62828;
  }
`;

export const GhostButton = styled(MuiButton)`
  color: #1976d2;
  &:hover {
    background-color: rgba(25, 118, 210, 0.04);
  }
`;