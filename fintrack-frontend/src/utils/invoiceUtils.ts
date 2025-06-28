import { useTranslation } from 'react-i18next';

/**
 * Utility functions for invoice-related operations
 */

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

export const getStatusText = (status: string, t: (key: string) => string): string => {
  switch (status) {
    case 'PAID': return t('invoices.status.paid');
    case 'OVERDUE': return t('invoices.status.overdue');
    case 'PARTIAL': return t('invoices.status.partial');
    case 'CLOSED': return t('invoices.status.closed');
    default: return t('invoices.status.open');
  }
};

export const getUrgencyText = (dueDate: string, status: string, t: (key: string, opts?: any) => string): string => {
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