import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { TrustedContact } from '../types/trustedContact';
import { ItemShareResponse } from '../types/itemShare';
import './ShareItemModal.css';

const SEARCH_DEBOUNCE_MS = 250;
const PERCENTAGE_SCALE = 10000;

const CURRENCY_FORMAT = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

interface Participant {
  userId?: number;
  contactId?: number;
  name: string;
  email: string;
  amount: number;
}

interface UserSharePayload {
  userId?: number;
  contactId?: number;
  percentage: number;
  responsible: boolean;
}

interface ShareItemModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoiceId: number;
  itemId: number;
  itemDescription: string;
  itemAmount: number;
  onSharesUpdated: () => void;
}

function shareResponseToParticipant(share: ItemShareResponse): Participant {
  if (share.contactId != null) {
    return {
      contactId: share.contactId,
      name: share.contactName ?? '',
      email: share.contactEmail ?? '',
      amount: share.amount
    };
  }
  return {
    userId: share.userId ?? undefined,
    name: share.userName,
    email: share.userEmail,
    amount: share.amount
  };
}

function buildPercentagesSumToOne(amounts: number[], total: number): number[] {
  if (amounts.length === 0) return [];
  if (amounts.length === 1) return [1];
  const scale = PERCENTAGE_SCALE;
  const percentages: number[] = [];
  let sum = 0;
  for (let i = 0; i < amounts.length - 1; i++) {
    const pct = Math.round((amounts[i] / total) * scale) / scale;
    percentages.push(pct);
    sum += pct;
  }
  percentages.push(Math.round((1 - sum) * scale) / scale);
  return percentages;
}

function buildUserShares(participants: Participant[], percentages: number[]): UserSharePayload[] {
  return participants.map((p, i) => {
    const share: UserSharePayload = { percentage: percentages[i], responsible: false };
    if (p.userId != null) share.userId = p.userId;
    else if (p.contactId != null) share.contactId = p.contactId;
    return share;
  });
}

function getApiErrorMessage(err: unknown): string | undefined {
  if (err == null || typeof err !== 'object' || !('response' in err)) return undefined;
  const data = (err as { response?: { data?: { error?: string; message?: string } } }).response?.data;
  return data?.error ?? data?.message;
}

const ShareItemModal: React.FC<ShareItemModalProps> = ({
  isOpen,
  onClose,
  invoiceId,
  itemId,
  itemDescription,
  itemAmount,
  onSharesUpdated
}) => {
  const { t } = useTranslation();
  const { user: currentUser } = useAuth();
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [contactSearch, setContactSearch] = useState('');
  const [contactSuggestions, setContactSuggestions] = useState<TrustedContact[]>([]);
  const [suggestionsOpen, setSuggestionsOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadExistingShares = useCallback(async () => {
    try {
      const response = await apiService.getItemShares(invoiceId, itemId);
      if (response.shares && Array.isArray(response.shares)) {
        const list = response.shares.map(shareResponseToParticipant);
        setParticipants(list);
      }
    } catch (err) {
      console.error('Error loading existing shares:', err);
    }
  }, [invoiceId, itemId]);

  const loadContactSuggestions = useCallback(async (search: string) => {
    try {
      const list = await apiService.getTrustedContacts(search || undefined);
      setContactSuggestions(list);
    } catch (err) {
      console.error('Error loading trusted contacts:', err);
      setContactSuggestions([]);
    }
  }, []);

  useEffect(() => {
    if (isOpen) {
      loadExistingShares();
      loadContactSuggestions('');
      setContactSearch('');
      setSuggestionsOpen(false);
    }
  }, [isOpen, loadExistingShares, loadContactSuggestions]);

  useEffect(() => {
    if (!isOpen) {
      setParticipants([]);
      setContactSearch('');
      setContactSuggestions([]);
      setError(null);
    }
  }, [isOpen]);

  useEffect(() => {
    if (!suggestionsOpen) return;
    const close = () => setSuggestionsOpen(false);
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, [suggestionsOpen]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (isOpen) loadContactSuggestions(contactSearch);
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [contactSearch, isOpen, loadContactSuggestions]);

  const addParticipant = (contact: TrustedContact) => {
    const already = participants.some(p => p.contactId === contact.id);
    if (already) return;
    setParticipants(prev => [...prev, {
      contactId: contact.id,
      name: contact.name,
      email: contact.email,
      amount: 0
    }]);
    setContactSearch('');
    setSuggestionsOpen(false);
  };

  const addCurrentUser = () => {
    if (!currentUser) return;
    const already = participants.some(p => p.userId === currentUser.id);
    if (already) return;
    setParticipants(prev => [...prev, {
      userId: currentUser.id,
      name: currentUser.name,
      email: currentUser.email,
      amount: 0
    }]);
  };

  const removeParticipant = (index: number) => {
    setParticipants(prev => prev.filter((_, i) => i !== index));
  };

  const setParticipantAmount = (index: number, value: number) => {
    setParticipants(prev => prev.map((p, i) => i === index ? { ...p, amount: value } : p));
  };

  const calculateTotalShared = () => participants.reduce((sum, p) => sum + p.amount, 0);
  const getRemainingAmount = () => itemAmount - calculateTotalShared();

  const handleDivideEqually = () => {
    setParticipants(prev => {
      if (prev.length === 0) return prev;
      const n = prev.length;
      const base = Math.floor((itemAmount / n) * 100) / 100;
      const totalFromBase = base * (n - 1);
      const lastAmount = Math.round((itemAmount - totalFromBase) * 100) / 100;
      const amounts = prev.map((_, idx) => (idx === n - 1 ? lastAmount : base));
      return prev.map((p, i) => ({ ...p, amount: amounts[i] }));
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const totalShared = calculateTotalShared();
    if (totalShared > itemAmount) {
      setError(t('shares.totalExceedsAmount'));
      return;
    }
    const withAmount = participants.filter(p => p.amount > 0);
    if (withAmount.length === 0) {
      setError(t('shares.totalExceedsAmount'));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const total = withAmount.reduce((s, p) => s + p.amount, 0);
      const amounts = withAmount.map(p => p.amount);
      const percentages = buildPercentagesSumToOne(amounts, total);
      const userShares = buildUserShares(withAmount, percentages);
      await apiService.createItemShares(invoiceId, itemId, { userShares });
      onSharesUpdated();
      onClose();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err) || t('shares.errorSavingShares'));
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="share-modal-overlay">
      <div className="share-modal-container">
        <div className="modal-header">
          <h2>{t('shares.shareItem')}</h2>
          <button onClick={onClose} className="close-modal">&times;</button>
        </div>
        <div className="item-info">
          <h3>{itemDescription}</h3>
          <p>
            <strong>{t('shares.itemAmount')}:</strong>{' '}
            {CURRENCY_FORMAT.format(itemAmount)}
          </p>
        </div>
        <div className="share-controls">
          <button
            type="button"
            className="equal-shares-btn"
            onClick={handleDivideEqually}
            disabled={participants.length === 0}
          >
            {t('shares.divideEqually')}
          </button>
        </div>

        <div className="share-add-contact">
          <div className="share-add-row">
            <label>{t('people.addPerson')}</label>
            {currentUser && (
              <button
                type="button"
                className="share-add-me-btn"
                onClick={addCurrentUser}
                disabled={participants.some(p => p.userId === currentUser.id)}
              >
                {t('shares.addMe')}
              </button>
            )}
          </div>
          <div className="share-autocomplete-wrap" onClick={(e) => e.stopPropagation()}>
            <input
              type="text"
              value={contactSearch}
              onChange={(e) => { setContactSearch(e.target.value); setSuggestionsOpen(true); }}
              onFocus={() => setSuggestionsOpen(true)}
              placeholder={t('people.searchPlaceholder')}
              className="share-autocomplete-input"
            />
            {suggestionsOpen && (
              <ul className="share-autocomplete-list">
                {contactSuggestions.length === 0 ? (
                  <li className="share-autocomplete-empty">{t('common.noData')}</li>
                ) : (
                  contactSuggestions.map((c) => (
                    <li
                      key={c.id}
                      className="share-autocomplete-item"
                      onClick={() => addParticipant(c)}
                    >
                      <strong>{c.name}</strong>
                      <span className="user-email">{c.email}</span>
                    </li>
                  ))
                )}
              </ul>
            )}
          </div>
        </div>

        <div className="users-list">
          {participants.map((p, index) => (
            <div key={`${p.contactId ?? p.userId}-${index}`} className="user-share-item">
              <div className="user-select">
                <button
                  type="button"
                  className="share-remove-participant"
                  onClick={() => removeParticipant(index)}
                  aria-label={t('people.remove')}
                >
                  &times;
                </button>
                <label>
                  <strong>{p.name}</strong>
                  <span className="user-email">{p.email}</span>
                </label>
                <div className="percentage-input">
                  <label>{t('shares.amount')}</label>
                  <input
                    type="number"
                    value={typeof p.amount === 'number' && p.amount >= 0 ? p.amount : ''}
                    onChange={(e) => setParticipantAmount(index, parseFloat(e.target.value) || 0)}
                    min={0}
                    max={itemAmount}
                    step="0.01"
                    placeholder="0.00"
                  />
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="distribution-status">
          <div className="status-info">
            <span>{t('shares.totalShared')}:</span>
            <span className="shared">
              {CURRENCY_FORMAT.format(calculateTotalShared())}
            </span>
          </div>
          <div className="status-info">
            <span>{t('shares.remaining')}:</span>
            <span className={`remaining ${getRemainingAmount() < 0 ? 'negative' : ''}`}>
              {CURRENCY_FORMAT.format(getRemainingAmount())}
            </span>
          </div>
        </div>
        {error && <div className="error-message">{error}</div>}
        <div className="modal-actions">
          <button className="save-shares-btn" onClick={handleSubmit} disabled={saving}>
            {saving ? t('shares.saving') : t('shares.saveShares')}
          </button>
          <button type="button" className="remove-shares-btn" onClick={onClose}>
            {t('common.cancel')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ShareItemModal;
