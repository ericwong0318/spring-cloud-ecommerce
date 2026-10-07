import { TextField, TextFieldProps, InputLabel, InputLabelProps, FormHelperText, FormHelperTextProps } from '@mui/material';
import { styled } from '@mui/material/styles';
import { forwardRef } from 'react';

export const Input = styled(TextField)<TextFieldProps>`
  &.MuiTextField-root {
    & .MuiOutlinedInput-root {
      border-radius: 8px;
      background-color: transparent;
      transition: all 0.2s ease;
      
      &:hover .MuiOutlinedInput-notchedOutline {
        border-color: #1976d2;
      }
      
      &.Mui-focused .MuiOutlinedInput-notchedOutline {
        border-color: #1976d2;
        border-width: 2px;
      }
      
      &.Mui-error .MuiOutlinedInput-notchedOutline {
        border-color: #d32f2f;
      }
    }
    
    & .MuiInputLabel-root {
      font-size: 0.875rem;
      color: #666;
      
      &.Mui-focused {
        color: #1976d2;
      }
      
      &.Mui-error {
        color: #d32f2f;
      }
    }
    
    & .MuiFormHelperText-root {
      font-size: 0.75rem;
      margin-top: 4px;
    }
  }
`;

export const Label = styled(InputLabel)<InputLabelProps>`
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  display: block;
`;

export const HelperText = styled(FormHelperText)<FormHelperTextProps>`
  font-size: 0.75rem;
  margin-top: 4px;
`;

export interface FormFieldProps extends Omit<TextFieldProps, 'label' | 'error' | 'helperText'> {
  label: string;
  error?: boolean;
  helperText?: string;
  required?: boolean;
}

export const FormField = forwardRef<HTMLInputElement, FormFieldProps>(
  ({ label, error, helperText, required, id, ...props }, ref) => {
    const inputId = id || label.toLowerCase().replace(/\s+/g, '-');
    return (
      <div style={{ width: '100%' }}>
        <Label htmlFor={inputId} required={required}>
          {label}
          {required && <span style={{ color: '#d32f2f', marginLeft: 4 }}>*</span>}
        </Label>
        <Input
          ref={ref}
          id={inputId}
          error={error}
          helperText={error ? helperText : undefined}
          {...props}
        />
      </div>
    );
  }
);

FormField.displayName = 'FormField';