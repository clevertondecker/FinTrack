import { Invoice } from '../types/invoice';
import { consolidateInvoices } from './invoiceUtils';

const invoice = (overrides: Partial<Invoice>): Invoice => ({
  id: 1,
  creditCardId: 1,
  creditCardName: 'Card',
  dueDate: '2026-09-05',
  invoiceMonth: '2026-09',
  totalAmount: 100,
  paidAmount: 0,
  status: 'OPEN',
  createdAt: '2026-09-01T00:00:00Z',
  ...overrides,
});

it('keeps the user share when consolidating a bank statement total', () => {
  const consolidated = consolidateInvoices([
    invoice({
      id: 1,
      totalAmount: 100,
      userShare: 40,
      statementTotalAmount: 150,
      contactShares: [{ contactName: 'Sabrina', contactEmail: 'sabrina@example.com', totalAmount: 60 }],
    }),
    invoice({
      id: 2,
      creditCardId: 2,
      totalAmount: 50,
      userShare: 20,
      contactShares: [{ contactName: 'Sabrina', contactEmail: 'sabrina@example.com', totalAmount: 30 }],
    }),
  ]);

  expect(consolidated).toHaveLength(1);
  expect(consolidated[0]).toMatchObject({
    totalAmount: 150,
    userShare: 60,
    contactShares: [{ contactName: 'Sabrina', contactEmail: 'sabrina@example.com', totalAmount: 90 }],
  });
});
