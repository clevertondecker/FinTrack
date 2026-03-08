import { Invoice, InvoiceSummary, GroupedInvoices, ContactShareSummary } from '../types/invoice';

/**
 * Utility functions for invoice-related operations
 */

const STATUS_PRIORITY: Record<string, number> = {
  OVERDUE: 1, OPEN: 2, PARTIAL: 3, PAID: 4, CLOSED: 5
};

export const sortInvoices = (invoices: Invoice[]): Invoice[] => {
  return [...invoices].sort((a, b) => {
    const statusDiff = (STATUS_PRIORITY[a.status] || 6) - (STATUS_PRIORITY[b.status] || 6);
    if (statusDiff !== 0) return statusDiff;
    return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
  });
};

export const groupInvoices = (invoices: Invoice[]): GroupedInvoices => ({
  overdue: invoices.filter(i => i.status === 'OVERDUE'),
  open: invoices.filter(i => i.status === 'OPEN'),
  partial: invoices.filter(i => i.status === 'PARTIAL'),
  paid: invoices.filter(i => i.status === 'PAID'),
  closed: invoices.filter(i => i.status === 'CLOSED'),
});

export const resolveGroupStatus = (group: Invoice[]): string => {
  let worst = 'CLOSED';
  group.forEach(inv => {
    if ((STATUS_PRIORITY[inv.status] || 6) < (STATUS_PRIORITY[worst] || 6)) {
      worst = inv.status;
    }
  });
  return worst;
};

export const consolidateInvoices = (invoiceList: Invoice[]): Invoice[] => {
  const grouped = new Map<string, Invoice[]>();
  const standalone: Invoice[] = [];

  invoiceList.forEach(inv => {
    if (inv.importGroupId) {
      const list = grouped.get(inv.importGroupId) || [];
      list.push(inv);
      grouped.set(inv.importGroupId, list);
    } else {
      standalone.push(inv);
    }
  });

  const consolidated: Invoice[] = [...standalone];

  grouped.forEach((group, groupId) => {
    if (group.length === 1) {
      consolidated.push(group[0]);
      return;
    }

    const totalAmount = group.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);
    const paidAmount = group.reduce((sum, inv) => sum + (inv.paidAmount || 0), 0);
    const userShare = group.reduce((sum, inv) => sum + (inv.userShare ?? inv.totalAmount ?? 0), 0);
    const worstStatus = resolveGroupStatus(group);
    const cardNames = group.map(inv => inv.creditCardName).join(' + ');

    const mergedShares: ContactShareSummary[] = [];
    group.forEach(inv => {
      (inv.contactShares || []).forEach(cs => {
        const existing = mergedShares.find(s => s.contactEmail === cs.contactEmail);
        if (existing) {
          existing.totalAmount += cs.totalAmount;
        } else {
          mergedShares.push({ ...cs });
        }
      });
    });

    consolidated.push({
      id: group[0].id,
      creditCardId: group[0].creditCardId,
      creditCardName: cardNames,
      dueDate: group[0].dueDate,
      totalAmount,
      paidAmount,
      status: worstStatus,
      createdAt: group[0].createdAt,
      updatedAt: group[0].updatedAt,
      userShare: userShare !== totalAmount ? userShare : undefined,
      contactShares: mergedShares.length > 0 ? mergedShares : undefined,
      importGroupId: groupId,
      _consolidatedCards: group,
    });
  });

  return consolidated;
};

export const calculateSummary = (invoices: Invoice[]): InvoiceSummary => {
  const summary: InvoiceSummary = {
    totalInvoices: invoices.length,
    totalAmount: 0, totalPaid: 0, totalRemaining: 0,
    overdueCount: 0, overdueAmount: 0,
    openCount: 0, openAmount: 0,
    partialCount: 0, partialAmount: 0,
    paidCount: 0, paidAmount: 0,
  };

  invoices.forEach(invoice => {
    const userAmount = invoice.userShare !== null && invoice.userShare !== undefined
      ? invoice.userShare
      : (invoice.totalAmount || 0);
    const totalInvoiceAmount = invoice.totalAmount || 0;
    const totalPaid = invoice.paidAmount || 0;

    let userPaid: number;
    if (totalInvoiceAmount > 0 && userAmount !== totalInvoiceAmount) {
      const paidPercentage = Math.min(totalPaid / totalInvoiceAmount, 1);
      userPaid = userAmount * paidPercentage;
    } else {
      userPaid = Math.min(totalPaid, userAmount);
    }

    summary.totalAmount += userAmount;
    summary.totalPaid += userPaid;
    summary.totalRemaining += Math.max(0, userAmount - userPaid);

    switch (invoice.status) {
      case 'OVERDUE': summary.overdueCount++; summary.overdueAmount += userAmount; break;
      case 'OPEN': summary.openCount++; summary.openAmount += userAmount; break;
      case 'PARTIAL': summary.partialCount++; summary.partialAmount += userAmount; break;
      case 'PAID': summary.paidCount++; summary.paidAmount += userAmount; break;
    }
  });

  return summary;
};

export const getStatusColor = (status: string): string => {
  switch (status) {
    case 'PAID':
      return 'status-paid';
    case 'OVERDUE':
      return 'status-overdue';
    case 'PARTIAL':
      return 'status-partial';
    case 'CLOSED':
      return 'status-closed';
    default:
      return 'status-open';
  }
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type TranslateFunction = (key: string, opts?: any) => any;

export const getStatusText = (status: string, t: TranslateFunction): string => {
  switch (status) {
    case 'PAID': return t('invoices.status.paid');
    case 'OVERDUE': return t('invoices.status.overdue');
    case 'PARTIAL': return t('invoices.status.partial');
    case 'CLOSED': return t('invoices.status.closed');
    default: return t('invoices.status.open');
  }
};

export const getUrgencyText = (dueDate: string, status: string, t: TranslateFunction): string => {
  if (status === 'PAID' || status === 'CLOSED') return '';
  const today = new Date();
  const due = new Date(dueDate);
  const diffTime = due.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  if (status === 'OVERDUE') return t('invoices.urgency.overdue', { days: Math.abs(diffDays) });
  if (diffDays === 0) return t('invoices.urgency.dueToday');
  if (diffDays === 1) return t('invoices.urgency.dueTomorrow');
  if (diffDays > 0) return t('invoices.urgency.dueInDays', { days: diffDays });
  return '';
};

export const formatCurrency = (amount: number | null | undefined): string => {
  if (amount === null || amount === undefined) return 'R$ 0,00';
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(amount);
};

/**
 * Formats a "yyyy-MM" month string into a localized label.
 * @param monthStr the month string in "yyyy-MM" format
 * @param language the i18n language code (e.g. 'pt', 'en')
 * @param style the month display style ('long' for full name, 'short' for abbreviation)
 * @returns the formatted month label (e.g. "mar. 2026" or "March 2026")
 */
export const formatMonthLabel = (
  monthStr: string,
  language: string,
  style: 'long' | 'short' = 'short'
): string => {
  const [year, month] = monthStr.split('-');
  const date = new Date(parseInt(year), parseInt(month) - 1);
  const locale = language === 'pt' ? 'pt-BR' : 'en-US';
  return date.toLocaleDateString(locale, { month: style, year: 'numeric' });
};

export const formatDate = (dateString: string): string => {
  if (!dateString || dateString === 'null' || dateString === 'undefined') return '-';
  
  try {
    const cleanDateString = dateString.trim();
    
    // Se já é uma string ISO válida
    if (cleanDateString.includes('T')) {
      const date = new Date(cleanDateString);
      if (!isNaN(date.getTime())) {
        return date.toLocaleDateString('pt-BR');
      }
    }
    
    // Se é apenas uma data (yyyy-MM-dd)
    if (cleanDateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
      const date = new Date(cleanDateString + 'T00:00:00');
      if (!isNaN(date.getTime())) {
        return date.toLocaleDateString('pt-BR');
      }
    }
    
    // Fallback: retorna a string original se não conseguir parsear
    return cleanDateString;
  } catch (error) {
    return dateString;
  }
}; 