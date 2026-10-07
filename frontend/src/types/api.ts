export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: Record<string, string>;
}

export interface ValidationError {
  field: string;
  message: string;
}

export interface PageRequest {
  page: number;
  size: number;
  sort?: string[];
}

export interface SortRequest {
  property: string;
  direction: 'ASC' | 'DESC';
}