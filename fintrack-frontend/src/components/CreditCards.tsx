import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { CreditCard, CreateCreditCardRequest, Bank } from '../types/creditCard';
import './CreditCards.css';

const CreditCards: React.FC = () => {
  const { t } = useTranslation();
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
    bankId: 0,
    cardType: 'PHYSICAL',
    parentCardId: undefined,
    cardholderName: ''
  });

  useEffect(() => {
    loadCreditCards();
    loadBanks();
    
    const token = localStorage.getItem('token');
    if (!token) {
      setError(t('common.noAuthToken'));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [t]);

  const loadCreditCards = async () => {
    try {
      setLoading(true);
      const response = await apiService.getCreditCards();
      setCreditCards(response.creditCards);
      setError(null);
    } catch (err) {
      setError(t('creditCards.failedToLoad'));
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
      setError(t('creditCards.pleaseFillFields'));
      return;
    }

    try {
      if (editingCard) {
        await apiService.updateCreditCard(editingCard.id, formData);
      } else {
        await apiService.createCreditCard(formData);
      }
      
      setFormData({ name: '', lastFourDigits: '', limit: 0, bankId: 0, cardType: 'PHYSICAL', parentCardId: undefined, cardholderName: '' });
      setShowCreateForm(false);
      setEditingCard(null);
      await loadCreditCards();
      setError(null);
    } catch (err) {
      setError(t('creditCards.failedToSave'));
      console.error('Error saving credit card:', err);
    }
  };

  const handleEdit = (card: CreditCard) => {
    setEditingCard(card);
    setFormData({
      name: card.name,
      lastFourDigits: card.lastFourDigits,
      limit: card.limit,
      bankId: banks.find(b => b.name === card.bankName)?.id || 0,
      cardType: card.cardType,
      parentCardId: card.parentCardId,
      cardholderName: card.cardholderName || ''
    });
    setShowCreateForm(true);
  };

  const handleDelete = async (id: number) => {
    if (window.confirm(t('creditCards.confirmDeactivate'))) {
      try {
        await apiService.deleteCreditCard(id);
        await loadCreditCards();
        setError(null);
      } catch (err) {
        setError(t('creditCards.failedToDeactivate'));
        console.error('Error deleting credit card:', err);
      }
    }
  };

  const handleActivate = async (id: number) => {
    if (window.confirm(t('creditCards.confirmActivate'))) {
      try {
        await apiService.activateCreditCard(id);
        await loadCreditCards();
        setError(null);
      } catch (err) {
        setError(t('creditCards.failedToActivate'));
        console.error('Error activating credit card:', err);
      }
    }
  };

  const handleCancel = () => {
    setShowCreateForm(false);
    setEditingCard(null);
    setFormData({ name: '', lastFourDigits: '', limit: 0, bankId: 0, cardType: 'PHYSICAL', parentCardId: undefined, cardholderName: '' });
    setError(null);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(amount);
  };

  if (loading) {
    return <div className="loading">{t('creditCards.loading')}</div>;
  }

  return (
    <div className="credit-cards-container">
      <header className="credit-cards-header">
        <h1>{t('creditCards.title')}</h1>
        <button 
          className="add-button"
          onClick={() => setShowCreateForm(true)}
        >
          {t('creditCards.addCard')}
        </button>
      </header>

      {error && <div className="error-message">{error}</div>}

      {showCreateForm && (
        <div className="form-overlay">
          <div className="form-container">
            <h2>{editingCard ? t('creditCards.editCard') : t('creditCards.addNewCard')}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="name">{t('creditCards.cardName')}</label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder={t('creditCards.cardNamePlaceholder')}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="lastFourDigits">{t('creditCards.lastFourDigits')}</label>
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
                <label htmlFor="limit">{t('creditCards.creditLimit')}</label>
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
                <label htmlFor="bankId">{t('creditCards.bank')}</label>
                <select
                  id="bankId"
                  name="bankId"
                  value={formData.bankId}
                  onChange={handleInputChange}
                  required
                >
                  <option value={0}>{t('creditCards.selectBank')}</option>
                  {banks.map(bank => (
                    <option key={bank.id} value={bank.id}>
                      {bank.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="cardType">{t('creditCards.cardType')}</label>
                <select
                  id="cardType"
                  name="cardType"
                  value={formData.cardType}
                  onChange={handleInputChange}
                  required
                >
                  <option value="PHYSICAL">{t('creditCards.cardTypePhysical')}</option>
                  <option value="VIRTUAL">{t('creditCards.cardTypeVirtual')}</option>
                  <option value="ADDITIONAL">{t('creditCards.cardTypeAdditional')}</option>
                </select>
              </div>

              {formData.cardType === 'ADDITIONAL' && (
                <div className="form-group">
                  <label htmlFor="parentCardId">{t('creditCards.parentCard')}</label>
                  <select
                    id="parentCardId"
                    name="parentCardId"
                    value={formData.parentCardId || ''}
                    onChange={handleInputChange}
                    required
                  >
                    <option value="">{t('creditCards.selectParentCard')}</option>
                    {creditCards
                      .filter(card => card.cardType !== 'ADDITIONAL')
                      .map(card => (
                        <option key={card.id} value={card.id}>
                          {card.name} (**** {card.lastFourDigits})
                        </option>
                      ))}
                  </select>
                </div>
              )}

              <div className="form-group">
                <label htmlFor="cardholderName">{t('creditCards.cardholderName')}</label>
                <input
                  type="text"
                  id="cardholderName"
                  name="cardholderName"
                  value={formData.cardholderName}
                  onChange={handleInputChange}
                  placeholder={t('creditCards.cardholderNamePlaceholder')}
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="submit-button">
                  {editingCard ? t('creditCards.updateCard') : t('creditCards.createCard')}
                </button>
                <button type="button" onClick={handleCancel} className="cancel-button">
                  {t('common.cancel')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="credit-cards-grid">
        {creditCards.length === 0 ? (
          <div className="empty-state">
            <p>{t('creditCards.emptyState')}</p>
          </div>
        ) : (
          creditCards.map(card => (
            <div key={card.id} className={`credit-card ${!card.active ? 'inactive' : ''}`} data-card-type={card.cardType}>
              <div className="card-header">
                <h3>{card.name}</h3>
                <span className={`status ${card.active ? 'active' : 'inactive'}`}>
                  {card.active ? t('creditCards.status.active') : t('creditCards.status.inactive')}
                </span>
              </div>
              
              <div className="card-details">
                <div className="detail-item">
                  <span className="label">{t('creditCards.lastFourDigitsLabel')}:</span>
                  <span className="value">**** {card.lastFourDigits}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">{t('creditCards.bankLabel')}:</span>
                  <span className="value">{card.bankName}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">{t('creditCards.cardTypeLabel')}:</span>
                  <span className="value card-type">
                    {card.cardType === 'PHYSICAL' && t('creditCards.cardTypePhysical')}
                    {card.cardType === 'VIRTUAL' && t('creditCards.cardTypeVirtual')}
                    {card.cardType === 'ADDITIONAL' && t('creditCards.cardTypeAdditional')}
                  </span>
                </div>

                {card.parentCardName && (
                  <div className="detail-item">
                    <span className="label">{t('creditCards.parentCardLabel')}:</span>
                    <span className="value">{card.parentCardName}</span>
                  </div>
                )}

                {card.cardholderName && (
                  <div className="detail-item">
                    <span className="label">{t('creditCards.cardholderNameLabel')}:</span>
                    <span className="value">{card.cardholderName}</span>
                  </div>
                )}
                
                <div className="detail-item">
                  <span className="label">{t('creditCards.limitLabel')}:</span>
                  <span className="value limit">{formatCurrency(card.limit)}</span>
                </div>
              </div>

              <div className="card-actions">
                <button 
                  onClick={() => handleEdit(card)}
                  className="edit-button"
                >
                  {t('common.edit')}
                </button>
                {card.active ? (
                  <button 
                    onClick={() => handleDelete(card.id)}
                    className="delete-button"
                  >
                    {t('creditCards.deactivate')}
                  </button>
                ) : (
                  <button 
                    onClick={() => handleActivate(card.id)}
                    className="activate-button"
                  >
                    {t('creditCards.activate')}
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