import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import InvoiceImport from '../InvoiceImport';
import apiService from '../../services/api';
import { ImportSource, ImportStatus } from '../../types/invoiceImport';

// Mock the API service
jest.mock('../../services/api');
const mockedApiService = apiService as jest.Mocked<typeof apiService>;

// Mock file
const createMockFile = (name: string, size: number, type: string): File => {
  const file = new File(['mock content'], name, { type });
  Object.defineProperty(file, 'size', { value: size });
  return file;
};

describe('InvoiceImport Component', () => {
  const mockCreditCards = [
    { id: 1, name: 'Nubank', lastFourDigits: '1234' },
    { id: 2, name: 'Itaú', lastFourDigits: '5678' }
  ];

  const mockImports = [
    {
      id: 1,
      userId: 1,
      creditCardId: 1,
      source: ImportSource.PDF,
      originalFileName: 'test-invoice.pdf',
      filePath: '/uploads/test-invoice.pdf',
      status: ImportStatus.COMPLETED,
      importedAt: '2024-01-01T10:00:00Z',
      processedAt: '2024-01-01T10:05:00Z',
      totalAmount: 1500.00,
      dueDate: '2024-02-01T00:00:00Z'
    }
  ];

  beforeEach(() => {
    // Reset all mocks
    jest.clearAllMocks();

    // Mock API responses
    mockedApiService.getCreditCards.mockResolvedValue({
      message: 'Success',
      creditCards: mockCreditCards,
      count: 2
    });

    mockedApiService.getInvoiceImports.mockResolvedValue({
      message: 'Success',
      imports: mockImports,
      count: 1
    });

    mockedApiService.importInvoice.mockResolvedValue({
      id: 1,
      message: 'Import started successfully',
      status: 'PENDING'
    });
  });

  const renderComponent = () => {
    return render(
      <BrowserRouter>
        <InvoiceImport />
      </BrowserRouter>
    );
  };

  it('renders the component', () => {
    renderComponent();
    expect(screen.getByText('Importar Faturas')).toBeInTheDocument();
  });

  it('loads credit cards on mount', async () => {
    renderComponent();

    await waitFor(() => {
      expect(mockedApiService.getCreditCards).toHaveBeenCalled();
    });
  });

  it('loads imports on mount', async () => {
    renderComponent();

    await waitFor(() => {
      expect(mockedApiService.getInvoiceImports).toHaveBeenCalled();
    });
  });

  it('allows file selection', () => {
    renderComponent();

    const fileInput = screen.getByLabelText('Selecionar Arquivo:');
    const file = createMockFile('test-invoice.pdf', 1024 * 1024, 'application/pdf');

    fireEvent.change(fileInput, { target: { files: [file] } });

    expect(screen.getByText('test-invoice.pdf')).toBeInTheDocument();
    expect(screen.getByText('1.00 MB')).toBeInTheDocument();
  });

  it('displays imports list', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('test-invoice.pdf')).toBeInTheDocument();
    });
  });
}); 