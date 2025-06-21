import React, { useState, useEffect } from 'react';
import apiService from '../services/api';
import { CreditCard, CreateCreditCardRequest, Bank } from '../types/creditCard';
import './CreditCards.css';

const CreditCards: React.FC = () => {
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [banks, setBanks] = useState<Bank[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingCard, setEditingCard] = useState<CreditCard | null>(null);

  const [formData, setFormData] = useState<CreateCreditCardRequest>({
    name: '',
    lastFourDigits: '',
    limit: 0,
    bankId: 0
  });

  useEffect(() => {
    loadCreditCards();
    loadBanks();
    
    const token = localStorage.getItem('token');
    if (!token) {
      setError('No authentication token found. Please login again.');
    }
  }, []);

  const loadCreditCards = async () => {
    try {
      setLoading(true);
      const response = await apiService.getCreditCards();
      setCreditCards(response.creditCards);
      setError(null);
    } catch (err) {
      setError('Failed to load credit cards');
      console.error('Error loading credit cards:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadBanks = async () => {
    try {
      const banksData = await apiService.getBanks();
      setBanks(banksData);
    } catch (err) {
      console.error('Error loading banks:', err);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'limit' ? parseFloat(value) || 0 : value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.name || !formData.lastFourDigits || formData.limit <= 0 || formData.bankId === 0) {
      setError('Please fill in all fields correctly');
      return;
    }

    try {
      if (editingCard) {
        await apiService.updateCreditCard(editingCard.id, formData);
      } else {
        await apiService.createCreditCard(formData);
      }
      
      setFormData({ name: '', lastFourDigits: '', limit: 0, bankId: 0 });
      setShowCreateForm(false);
      setEditingCard(null);
      await loadCreditCards();
      setError(null);
    } catch (err) {
      setError('Failed to save credit card');
      console.error('Error saving credit card:', err);
    }
  };

  const handleEdit = (card: CreditCard) => {
    setEditingCard(card);
    setFormData({
      name: card.name,
      lastFourDigits: card.lastFourDigits,
      limit: card.limit,
      bankId: banks.find(b => b.name === card.bankName)?.id || 0
    });
    setShowCreateForm(true);
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to deactivate this credit card?')) {
      try {
        await apiService.deleteCreditCard(id);
        await loadCreditCards();
        setError(null);
      } catch (err) {
        setError('Failed to deactivate credit card');
        console.error('Error deleting credit card:', err);
      }
    }
  };

  const handleActivate = async (id: number) => {
    if (window.confirm('Are you sure you want to activate this credit card?')) {
      try {
        await apiService.activateCreditCard(id);
        await loadCreditCards();
        setError(null);
      } catch (err) {
        setError('Failed to activate credit card');
        console.error('Error activating credit card:', err);
      }
    }
  };

  const handleCancel = () => {
    setShowCreateForm(false);
    setEditingCard(null);
    setFormData({ name: '', lastFourDigits: '', limit: 0, bankId: 0 });
    setError(null);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('pt-BR');
  };

  if (loading) {
    return <div className="loading">Loading credit cards...</div>;
  }

  return (
    <div className="credit-cards-container">
      <header className="credit-cards-header">
        <h1>Credit Cards</h1>
        <button 
          className="add-button"
          onClick={() => setShowCreateForm(true)}
        >
          Add New Card
        </button>
      </header>

      {error && <div className="error-message">{error}</div>}

      {showCreateForm && (
        <div className="form-overlay">
          <div className="form-container">
            <h2>{editingCard ? 'Edit Credit Card' : 'Add New Credit Card'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="name">Card Name</label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="e.g., Nubank, Itaú"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="lastFourDigits">Last 4 Digits</label>
                <input
                  type="text"
                  id="lastFourDigits"
                  name="lastFourDigits"
                  value={formData.lastFourDigits}
                  onChange={handleInputChange}
                  placeholder="1234"
                  maxLength={4}
                  pattern="[0-9]{4}"
                  required
                  disabled={!!editingCard}
                />
              </div>

              <div className="form-group">
                <label htmlFor="limit">Credit Limit</label>
                <input
                  type="number"
                  id="limit"
                  name="limit"
                  value={formData.limit}
                  onChange={handleInputChange}
                  placeholder="5000.00"
                  step="0.01"
                  min="0"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="bankId">Bank</label>
                <select
                  id="bankId"
                  name="bankId"
                  value={formData.bankId}
                  onChange={handleInputChange}
                  required
                >
                  <option value={0}>Select a bank</option>
                  {banks.map(bank => (
                    <option key={bank.id} value={bank.id}>
                      {bank.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-actions">
                <button type="submit" className="submit-button">
                  {editingCard ? 'Update Card' : 'Create Card'}
                </button>
                <button type="button" onClick={handleCancel} className="cancel-button">
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="credit-cards-grid">
        {creditCards.length === 0 ? (
          <div className="empty-state">
            <p>No credit cards found. Add your first credit card to get started!</p>
          </div>
        ) : (
          creditCards.map(card => (
            <div key={card.id} className={`credit-card ${!card.active ? 'inactive' : ''}`}>
              <div className="card-header">
                <h3>{card.name}</h3>
                <span className={`status ${card.active ? 'active' : 'inactive'}`}>
                  {card.active ? 'Active' : 'Inactive'}
                </span>
              </div>
              
              <div className="card-details">
                <div className="detail-item">
                  <span className="label">Last 4 digits:</span>
                  <span className="value">**** {card.lastFourDigits}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Bank:</span>
                  <span className="value">{card.bankName}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Limit:</span>
                  <span className="value limit">{formatCurrency(card.limit)}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Created:</span>
                  <span className="value">{formatDate(card.createdAt)}</span>
                </div>
              </div>

              <div className="card-actions">
                <button 
                  onClick={() => handleEdit(card)}
                  className="edit-button"
                >
                  Edit
                </button>
                {card.active ? (
                  <button 
                    onClick={() => handleDelete(card.id)}
                    className="delete-button"
                  >
                    Deactivate
                  </button>
                ) : (
                  <button 
                    onClick={() => handleActivate(card.id)}
                    className="activate-button"
                  >
                    Activate
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default CreditCards; 