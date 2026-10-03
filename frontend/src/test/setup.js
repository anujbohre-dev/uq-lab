import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Remove the rendered components after every test so tests cannot affect each other.
afterEach(() => cleanup());
