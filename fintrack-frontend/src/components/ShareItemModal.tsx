import React, { useState, useEffect } from 'react';
import { CreateItemShareRequest, UserShare, ItemShareResponse } from '../types/itemShare';
import { apiService } from '../services/api';
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

interface User {
  id: number;
  name: string;
  email: string;
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
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<number[]>([]);
  const [userShares, setUserShares] = useState<UserShare[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [existingShares, setExistingShares] = useState<ItemShareResponse[]>([]);

  useEffect(() => {
    if (isOpen) {
      loadUsers();
      loadExistingShares();
    }
  }, [isOpen, invoiceId, itemId]);

  const loadUsers = async () => {
    try {
      // For now, we'll use a mock list of users
      // In a real app, you'd fetch this from an API
      const mockUsers: User[] = [
        { id: 1, name: 'João Silva', email: 'joao@example.com' },
        { id: 2, name: 'Maria Santos', email: 'maria@example.com' },
        { id: 3, name: 'Pedro Costa', email: 'pedro@example.com' },
        { id: 4, name: 'Ana Oliveira', email: 'ana@example.com' }
      ];
      setUsers(mockUsers);
    } catch (err) {
      setError('Failed to load users');
    }
  };

  const loadExistingShares = async () => {
    try {
      const response = await apiService.getItemShares(invoiceId, itemId);
      setExistingShares(response.shares);
      
      // Pre-populate form with existing shares
      const shares: UserShare[] = response.shares.map(share => ({
        userId: share.userId,
        percentage: share.percentage,
        responsible: share.responsible
      }));
      setUserShares(shares);
      setSelectedUsers(shares.map(s => s.userId));
    } catch (err) {
      console.error('Failed to load existing shares:', err);
    }
  };

  const handleUserToggle = (userId: number) => {
    setSelectedUsers(prev => {
      const newSelected = prev.includes(userId)
        ? prev.filter(id => id !== userId)
        : [...prev, userId];
      
      // Update userShares accordingly
      setUserShares(prevShares => {
        if (prevShares.some(s => s.userId === userId)) {
          return prevShares.filter(s => s.userId !== userId);
        } else {
          const newShare: UserShare = {
            userId,
            percentage: 0,
            responsible: false
          };
          return [...prevShares, newShare];
        }
      });
      
      return newSelected;
    });
  };

  const handlePercentageChange = (userId: number, percentage: number) => {
    setUserShares(prev => 
      prev.map(share => 
        share.userId === userId 
          ? { ...share, percentage }
          : share
      )
    );
  };

  const handleResponsibleChange = (userId: number, responsible: boolean) => {
    setUserShares(prev => 
      prev.map(share => 
        share.userId === userId 
          ? { ...share, responsible }
          : share
      )
    );
  };

  const calculateEqualShares = () => {
    if (selectedUsers.length === 0) return;
    
    const equalPercentage = 1 / selectedUsers.length;
    const shares: UserShare[] = selectedUsers.map(userId => ({
      userId,
      percentage: equalPercentage,
      responsible: false
    }));
    
    setUserShares(shares);
  };

  const validateShares = (): boolean => {
    if (userShares.length === 0) {
      setError('Please select at least one user to share with');
      return false;
    }

    const totalPercentage = userShares.reduce((sum, share) => sum + share.percentage, 0);
    if (Math.abs(totalPercentage - 1) > 0.01) {
      setError('Total percentage must equal 100%');
      return false;
    }

    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!validateShares()) {
      return;
    }

    setLoading(true);
    try {
      const request: CreateItemShareRequest = {
        userShares: userShares
      };

      await apiService.createItemShares(invoiceId, itemId, request);
      onSharesUpdated();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create shares');
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveShares = async () => {
    if (!window.confirm('Are you sure you want to remove all shares for this item?')) {
      return;
    }

    setLoading(true);
    try {
      await apiService.removeItemShares(invoiceId, itemId);
      onSharesUpdated();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to remove shares');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(amount);
  };

  const formatPercentage = (percentage: number) => {
    return `${(percentage * 100).toFixed(1)}%`;
  };

  if (!isOpen) return null;

  return (
    <div className="share-modal-overlay">
      <div className="share-modal-container">
        <button className="close-modal" onClick={onClose}>&times;</button>
        
        <h2>Dividir Item</h2>
        <div className="item-info">
          <p><strong>Item:</strong> {itemDescription}</p>
          <p><strong>Valor:</strong> {formatCurrency(itemAmount)}</p>
        </div>

        {error && <div className="error-message">{error}</div>}

        <div className="share-controls">
          <button 
            type="button" 
            onClick={calculateEqualShares}
            className="equal-shares-btn"
          >
            Dividir Igualmente
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="users-list">
            {users.map((user: User) => (
              <div key={user.id} className="user-share-item">
                <div className="user-select">
                  <input
                    type="checkbox"
                    id={`user-${user.id}`}
                    checked={selectedUsers.includes(user.id)}
                    onChange={() => handleUserToggle(user.id)}
                  />
                  <label htmlFor={`user-${user.id}`}>
                    <strong>{user.name}</strong>
                    <span className="user-email">{user.email}</span>
                  </label>
                </div>

                {selectedUsers.includes(user.id) && (
                  <div className="share-details">
                    <div className="percentage-input">
                      <label>Percentual:</label>
                      <input
                        type="number"
                        min="0"
                        max="100"
                        step="0.1"
                        value={(userShares.find(s => s.userId === user.id)?.percentage || 0) * 100}
                        onChange={(e) => handlePercentageChange(user.id, parseFloat(e.target.value) / 100)}
                        placeholder="0.0"
                      />
                      <span>%</span>
                    </div>

                    <div className="amount-display">
                      <label>Valor:</label>
                      <span>
                        {formatCurrency((userShares.find(s => s.userId === user.id)?.percentage || 0) * itemAmount)}
                      </span>
                    </div>

                    <div className="responsible-checkbox">
                      <input
                        type="checkbox"
                        id={`responsible-${user.id}`}
                        checked={userShares.find(s => s.userId === user.id)?.responsible || false}
                        onChange={(e) => handleResponsibleChange(user.id, e.target.checked)}
                      />
                      <label htmlFor={`responsible-${user.id}`}>Responsável pelo pagamento</label>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>

          <div className="total-info">
            <p>
              <strong>Total Selecionado:</strong> {formatPercentage(userShares.reduce((sum, s) => sum + s.percentage, 0))}
            </p>
            <p>
              <strong>Valor Total:</strong> {formatCurrency(userShares.reduce((sum, s) => sum + (s.percentage * itemAmount), 0))}
            </p>
          </div>

          <div className="modal-actions">
            {existingShares.length > 0 && (
              <button
                type="button"
                onClick={handleRemoveShares}
                className="remove-shares-btn"
                disabled={loading}
              >
                {loading ? 'Removendo...' : 'Remover Todas as Divisões'}
              </button>
            )}
            
            <button
              type="submit"
              className="save-shares-btn"
              disabled={loading || userShares.length === 0}
            >
              {loading ? 'Salvando...' : 'Salvar Divisão'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ShareItemModal; 