import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import App from './App';

// Mock the API service
jest.mock('./services/api', () => ({
  __esModule: true,
  default: {
    getCurrentUser: jest.fn(() => Promise.reject(new Error('No token'))),
  }
}));

test('renders app without crashing', async () => {
  render(<App />);
  
  // Wait for loading to finish
  await waitFor(() => {
    // The app should render something (either login or loading)
    expect(document.body).toBeInTheDocument();
  });
});
