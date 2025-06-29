import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { User } from '../types/user';
import './ShareItemModal.css';

interface ShareItemModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoiceId: number;
  itemId: number;
  itemDescription: string;
  itemAmount: number;
  onSharesUpdated: () => void;
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
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<number[]>([]);
  const [userShares, setUserShares] = useState<{ [key: number]: number }>({});
  const [inputValues, setInputValues] = useState<{ [key: number]: string }>({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      loadUsers();
    }
  }, [isOpen, invoiceId, itemId]);

  useEffect(() => {
    if (isOpen && users.length > 0) {
      loadExistingShares();
    }
    // eslint-disable-next-line
  }, [users, isOpen]);

  useEffect(() => {
    if (!isOpen) {
      // Clear state when modal closes
      setUsers([]);
      setSelectedUsers([]);
      setUserShares({});
      setInputValues({});
      setError(null);
    }
  }, [isOpen]);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Load users first
      await loadUsers();
      
      // Then load existing shares
      await loadExistingShares();
    } catch (err) {
      setError(t('shares.errorLoadingData'));
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    try {
      const response = await apiService.getUsers();
      const loadedUsers = response.users.filter(user => user.id !== 1); // Exclude current user
      setUsers(loadedUsers);
    } catch (err) {
      console.error('Error loading users:', err);
      throw err;
    }
  };

  const loadExistingShares = async () => {
    try {
      const response = await apiService.getItemShares(invoiceId, itemId);
      
      if (response.shares && Array.isArray(response.shares)) {
        const shares = response.shares;
        const userIdsWithShares: number[] = [];
        const userSharesData: { [key: number]: number } = {};
        const newInputValues: { [key: number]: string } = {};

        shares.forEach(share => {
          const shareUserId = Number(share.userId);
          const userExists = users.some(user => user.id === shareUserId);
          
          if (userExists) {
            userIdsWithShares.push(shareUserId);
            userSharesData[shareUserId] = share.amount;
            newInputValues[shareUserId] = share.amount.toString();
          }
        });

        setSelectedUsers(userIdsWithShares);
        setUserShares(userSharesData);
        setInputValues(newInputValues);
      }
    } catch (err) {
      console.error('Error loading existing shares:', err);
      // Don't throw error here, just log it
    }
  };

  const handleUserToggle = (userId: number) => {
    setSelectedUsers(prev => {
      const newSelected = prev.includes(userId)
        ? prev.filter(id => id !== userId)
        : [...prev, userId];
      
      // Update input values
      setInputValues(prevValues => {
        const newValues = { ...prevValues };
        if (newSelected.includes(userId)) {
          if (!newValues[userId]) {
            newValues[userId] = '';
          }
        } else {
          delete newValues[userId];
        }
        return newValues;
      });
      
      return newSelected;
    });
  };

  const handleInputChange = (userId: number, value: string) => {
    setInputValues(prev => ({
      ...prev,
      [userId]: value
    }));
  };

  const calculateTotalShared = () => {
    return Object.values(inputValues).reduce((total, value) => {
      const numValue = parseFloat(value) || 0;
      return total + numValue;
    }, 0);
  };

  const getRemainingAmount = () => {
    return itemAmount - calculateTotalShared();
  };

  // NOVA FUNÇÃO: Dividir igualmente
  const handleDivideEqually = () => {
    if (selectedUsers.length === 0) return;
    const baseValue = Math.floor((itemAmount / selectedUsers.length) * 100) / 100; // valor base arredondado para baixo
    let total = 0;
    const newValues: { [key: number]: string } = {};

    selectedUsers.forEach((userId, idx) => {
      if (idx === selectedUsers.length - 1) {
        // O último recebe o restante para fechar o valor exato
        newValues[userId] = (itemAmount - total).toFixed(2);
      } else {
        newValues[userId] = baseValue.toFixed(2);
        total += baseValue;
      }
    });
    setInputValues(newValues);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const totalShared = calculateTotalShared();
    if (totalShared > itemAmount) {
      setError(t('shares.totalExceedsAmount'));
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const updatedShares = selectedUsers.map(userId => ({
        userId,
        percentage: parseFloat(inputValues[userId] || '0') / itemAmount,
        responsible: false
      })).filter(share => share.percentage > 0);

      const request = {
        userShares: updatedShares
      };

      await apiService.createItemShares(invoiceId, itemId, request);
      onSharesUpdated();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || t('shares.errorSavingShares'));
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
            <strong>{t('shares.itemAmount')}:</strong> {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(itemAmount)}
          </p>
        </div>
        <div className="share-controls">
          <button className="equal-shares-btn" onClick={handleDivideEqually} disabled={selectedUsers.length === 0}>
            {t('shares.divideEqually')}
          </button>
        </div>
        <div className="users-list">
          {users.map(user => (
            <div key={user.id} className="user-share-item">
              <div className="user-select">
                <input
                  type="checkbox"
                  checked={selectedUsers.includes(user.id)}
                  onChange={() => handleUserToggle(user.id)}
                />
                <label>
                  <strong>{user.name}</strong>
                  <span className="user-email">{user.email}</span>
                </label>
                {selectedUsers.includes(user.id) && (
                  <div className="percentage-input">
                    <label>{t('shares.amount')}</label>
                    <input
                      type="number"
                      value={inputValues[user.id] || ''}
                      onChange={e => handleInputChange(user.id, e.target.value)}
                      min="0"
                      max={itemAmount}
                      step="0.01"
                      placeholder="0.00"
                    />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
        <div className="distribution-status">
          <div className="status-info">
            <span>{t('shares.totalShared')}:</span>
            <span className="shared">{new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(calculateTotalShared())}</span>
          </div>
          <div className="status-info">
            <span>{t('shares.remaining')}:</span>
            <span className={`remaining ${getRemainingAmount() < 0 ? 'negative' : ''}`}>{new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(getRemainingAmount())}</span>
          </div>
        </div>
        {error && <div className="error-message">{error}</div>}
        <div className="modal-actions">
          <button className="save-shares-btn" onClick={handleSubmit} disabled={saving}>
            {saving ? t('shares.saving') : t('shares.saveShares')}
          </button>
          <button className="remove-shares-btn" onClick={onClose}>
            {t('common.cancel')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ShareItemModal; 