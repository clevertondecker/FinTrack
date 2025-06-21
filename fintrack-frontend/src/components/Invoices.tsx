import React, { useState, useEffect } from 'react';
import apiService from '../services/api';
import { Invoice, CreateInvoiceRequest } from '../types/invoice';
import { CreditCard } from '../types/creditCard';
import './Invoices.css';

const Invoices: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);

  const [formData, setFormData] = useState<CreateInvoiceRequest>({
    creditCardId: 0,
    dueDate: ''
  });

  useEffect(() => {
    loadInvoices();
    loadCreditCards();
    
    const token = localStorage.getItem('token');
    if (!token) {
      setError('No authentication token found. Please login again.');
    }
  }, []);

  const loadInvoices = async () => {
    try {
      setLoading(true);
      const response = await apiService.getInvoices();
      setInvoices(response.invoices);
      setError(null);
    } catch (err) {
      setError('Failed to load invoices');
      console.error('Error loading invoices:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadCreditCards = async () => {
    try {
      const response = await apiService.getCreditCards();
      setCreditCards(response.creditCards);
    } catch (err) {
      console.error('Error loading credit cards:', err);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'creditCardId' ? parseInt(value) || 0 : value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.creditCardId || !formData.dueDate) {
      setError('Please fill in all fields correctly');
      return;
    }

    try {
      await apiService.createInvoice(formData);
      
      setFormData({ creditCardId: 0, dueDate: '' });
      setShowCreateForm(false);
      await loadInvoices();
      setError(null);
    } catch (err) {
      setError('Failed to create invoice');
      console.error('Error creating invoice:', err);
    }
  };

  const handleCancel = () => {
    setShowCreateForm(false);
    setFormData({ creditCardId: 0, dueDate: '' });
    setError(null);
  };

  const formatCurrency = (amount: number | null | undefined) => {
    if (amount === null || amount === undefined || isNaN(amount)) {
      return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
      }).format(0);
    }
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('pt-BR');
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PAID':
        return 'status-paid';
      case 'OVERDUE':
        return 'status-overdue';
      case 'PARTIAL':
        return 'status-partial';
      default:
        return 'status-open';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'PAID':
        return 'Paga';
      case 'OVERDUE':
        return 'Vencida';
      case 'PARTIAL':
        return 'Parcial';
      default:
        return 'Aberta';
    }
  };

  if (loading) {
    return <div className="loading">Loading invoices...</div>;
  }

  return (
    <div className="invoices-container">
      <header className="invoices-header">
        <h1>Invoices</h1>
        <button 
          className="add-button"
          onClick={() => setShowCreateForm(true)}
        >
          Add New Invoice
        </button>
      </header>

      {error && <div className="error-message">{error}</div>}

      {showCreateForm && (
        <div className="form-overlay">
          <div className="form-container">
            <h2>Add New Invoice</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="creditCardId">Credit Card</label>
                <select
                  id="creditCardId"
                  name="creditCardId"
                  value={formData.creditCardId}
                  onChange={handleInputChange}
                  required
                >
                  <option value={0}>Select a credit card</option>
                  {creditCards.map(card => (
                    <option key={card.id} value={card.id}>
                      {card.name} - **** {card.lastFourDigits}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="dueDate">Due Date</label>
                <input
                  type="date"
                  id="dueDate"
                  name="dueDate"
                  value={formData.dueDate}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="submit-button">
                  Create Invoice
                </button>
                <button type="button" onClick={handleCancel} className="cancel-button">
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="invoices-grid">
        {invoices.length === 0 ? (
          <div className="empty-state">
            <p>No invoices found. Add your first invoice to get started!</p>
          </div>
        ) : (
          invoices.map(invoice => (
            <div key={invoice.id} className="invoice-card">
              <div className="invoice-header">
                <h3>{invoice.creditCardName}</h3>
                <span className={`status ${getStatusColor(invoice.status)}`}>
                  {getStatusText(invoice.status)}
                </span>
              </div>
              
              <div className="invoice-details">
                <div className="detail-item">
                  <span className="label">Due Date:</span>
                  <span className="value">{formatDate(invoice.dueDate)}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Total Amount:</span>
                  <span className="value total">{formatCurrency(invoice.totalAmount)}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Paid Amount:</span>
                  <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Remaining:</span>
                  <span className="value remaining">{formatCurrency((invoice.totalAmount || 0) - (invoice.paidAmount || 0))}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Created:</span>
                  <span className="value">{formatDate(invoice.createdAt)}</span>
                </div>
              </div>

              <div className="invoice-actions">
                <button 
                  onClick={() => setSelectedInvoice(invoice)}
                  className="view-button"
                >
                  View Details
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default Invoices; 