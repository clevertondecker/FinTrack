import React, { useState, useEffect, useRef } from 'react';
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
  const [inputValues, setInputValues] = useState<{ [userId: number]: string }>({});
  const [success, setSuccess] = useState<string | null>(null);

  // Monitor state changes
  useEffect(() => {
    console.log('userShares state changed:', userShares);
  }, [userShares]);

  useEffect(() => {
    console.log('selectedUsers state changed:', selectedUsers);
  }, [selectedUsers]);

  useEffect(() => {
    console.log('inputValues state changed:', inputValues);
  }, [inputValues]);

  // Log rendering data
  useEffect(() => {
    console.log('Rendering data - users:', users);
    console.log('Rendering data - selectedUsers:', selectedUsers);
    console.log('Rendering data - userShares:', userShares);
    console.log('Rendering data - inputValues:', inputValues);
  }, [users, selectedUsers, userShares, inputValues]);

  useEffect(() => {
    if (isOpen) {
      console.log('Modal opened - starting data loading sequence');
      console.log('Modal props:', { invoiceId, itemId, itemDescription, itemAmount });
      loadUsers();
    }
  }, [isOpen]);

  // Carregar shares apenas depois que os usuários foram carregados
  useEffect(() => {
    if (isOpen && users.length > 0) {
      console.log('Users loaded, now loading shares. Users count:', users.length);
      // Pequeno delay para garantir que os usuários foram processados
      setTimeout(() => {
        loadExistingShares();
      }, 100);
    }
  }, [isOpen, users]);

  // Limpar estado quando o modal é fechado
  useEffect(() => {
    if (!isOpen) {
      console.log('Modal closed - clearing state');
      setSelectedUsers([]);
      setUserShares([]);
      setError(null);
      setExistingShares([]);
      setInputValues({});
    }
  }, [isOpen]);

  const loadUsers = async () => {
    try {
      console.log('Loading users...');
      
      // TESTE: Usar dados mock que correspondem aos shares
      const mockUsers = [
        { id: 1, name: 'John Doe', email: 'john@example.com' },
        { id: 2, name: 'Cleverton Decker', email: 'cleverton_decker@hotmail.com' }
      ];
      
      console.log('Using MOCK users:', mockUsers);
      setUsers(mockUsers);
      
      // const response = await apiService.getUsers(); // Comment out real API call
      // console.log('Users API response:', response);
      // const loadedUsers = response.users || [];
      // console.log('Loaded users:', loadedUsers);
      // setUsers(loadedUsers);
    } catch (err) {
      console.error('Failed to load users:', err);
      // Fallback para lista mockada se a API não estiver disponível
      const mockUsers: User[] = [
        { id: 1, name: 'João Silva', email: 'joao@example.com' },
        { id: 2, name: 'Maria Santos', email: 'maria@example.com' },
        { id: 3, name: 'Pedro Costa', email: 'pedro@example.com' }
      ];
      console.log('Using fallback mock users:', mockUsers);
      setUsers(mockUsers);
      setError('Erro ao carregar usuários. Usando lista de exemplo.');
    }
  };

  const loadExistingShares = async () => {
    try {
      console.log('Loading existing shares for invoiceId:', invoiceId, 'itemId:', itemId);
      const response = await apiService.getItemShares(invoiceId, itemId);
      console.log('Full API response:', JSON.stringify(response, null, 2));
      console.log('Response shares:', response.shares);
      console.log('Response shares length:', response.shares?.length);
      console.log('Response type:', typeof response);
      console.log('Response keys:', Object.keys(response));
      
      if (response.shares && Array.isArray(response.shares)) {
        console.log('Current users state:', users);
        console.log('Users length:', users.length);
        console.log('Users IDs:', users.map(u => u.id));
        
        // Filtrar shares apenas para usuários que existem na lista
        const shares = response.shares.filter((share: ItemShareResponse) => {
          const shareUserId = Number(share.userId);
          const userExists = users.some(user => user.id === shareUserId);
          console.log(`Checking share for user ${share.userId} (converted to ${shareUserId}): user exists = ${!!userExists}`);
          console.log(`Available user IDs: ${users.map(u => `${u.id} (${typeof u.id})`).join(', ')}`);
          
          if (!userExists) {
            console.log(`WARNING: User ${share.userId} not found in users list!`);
          }
          
          return userExists;
        });
        
        console.log('Filtered shares:', shares);
        console.log('Available users:', users);
        
        // Definir shares e usuários selecionados
        setExistingShares(shares);
        
        // Converter para o formato UserShare
        const userSharesData: UserShare[] = shares.map((share: ItemShareResponse) => ({
          userId: Number(share.userId),
          percentage: share.percentage,
          responsible: share.responsible
        }));
        
        setUserShares(userSharesData);
        
        // Selecionar automaticamente os usuários que têm shares
        const userIdsWithShares = shares.map((share: ItemShareResponse) => Number(share.userId));
        setSelectedUsers(userIdsWithShares);
        
        // Definir valores dos inputs baseados nos shares existentes
        const newInputValues: { [key: number]: string } = {};
        shares.forEach((share: ItemShareResponse) => {
          const userId = Number(share.userId);
          newInputValues[userId] = (share.percentage * 100).toFixed(1);
        });
        
        console.log('Setting inputValues:', newInputValues);
        setInputValues(newInputValues);
        
        // Log final state after processing
        console.log('=== FINAL STATE AFTER PROCESSING ===');
        console.log('selectedUsers:', userIdsWithShares);
        console.log('userShares:', userSharesData);
        console.log('inputValues:', newInputValues);
        console.log('existingShares:', shares);
        console.log('=====================================');
        
        // Forçar re-render após um pequeno delay
        setTimeout(() => {
          console.log('=== FORCED RE-RENDER CHECK ===');
          console.log('selectedUsers after delay:', selectedUsers);
          console.log('userShares after delay:', userShares);
          console.log('inputValues after delay:', inputValues);
          console.log('==============================');
        }, 200);
        
      } else {
        console.log('No shares found or invalid response format');
        setExistingShares([]);
        setUserShares([]);
        setSelectedUsers([]);
        setInputValues({});
      }
    } catch (err) {
      console.error('Failed to load existing shares:', err);
      setError('Erro ao carregar divisões existentes.');
      setExistingShares([]);
      setUserShares([]);
      setSelectedUsers([]);
      setInputValues({});
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
          // Remove user from shares
          const updatedShares = prevShares.filter(s => s.userId !== userId);
          
          // If only one user remains, set their percentage to 100%
          if (updatedShares.length === 1) {
            const remainingShare = updatedShares[0];
            updatedShares[0] = { ...remainingShare, percentage: 1 };
            // Update inputValues for the remaining user
            setInputValues(prev => ({ ...prev, [remainingShare.userId]: '100.0' }));
          }
          
          return updatedShares;
        } else {
          // Add new user
          const newShare: UserShare = {
            userId,
            percentage: newSelected.length === 1 ? 1 : 0, // Se é o único usuário, define como 100%
            responsible: false
          };
          
          // Update inputValues to match the new share
          setInputValues(prev => ({ 
            ...prev, 
            [userId]: newSelected.length === 1 ? '100.0' : '0.0' 
          }));
          
          return [...prevShares, newShare];
        }
      });
      
      return newSelected;
    });
  };

  const handlePercentageChange = (userId: number, percentage: number) => {
    // Garantir que o valor seja um número válido
    if (isNaN(percentage) || percentage < 0) {
      percentage = 0;
    }
    
    setUserShares(prev => 
      prev.map(share => 
        share.userId === userId 
          ? { ...share, percentage }
          : share
      )
    );
    
    // Update inputValues to match the new percentage
    setInputValues(prev => ({ 
      ...prev, 
      [userId]: (percentage * 100).toFixed(1) 
    }));
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
    
    // Update inputValues to match the equal shares
    const newInputValues: { [userId: number]: string } = {};
    selectedUsers.forEach(userId => {
      newInputValues[userId] = (equalPercentage * 100).toFixed(1);
    });
    setInputValues(prev => ({ ...prev, ...newInputValues }));
    
    setUserShares(shares);
  };

  const validateShares = (): boolean => {
    if (userShares.length === 0) {
      setError('Selecione pelo menos um usuário para compartilhar');
      return false;
    }

    // Verificar se todos os usuários selecionados existem na lista de usuários
    const invalidUsers = selectedUsers.filter(userId => !users.find(u => u.id === userId));
    if (invalidUsers.length > 0) {
      setError(`Usuários não encontrados: ${invalidUsers.join(', ')}. Recarregue a página.`);
      return false;
    }

    // Calcular total considerando todos os usuários selecionados (mesmo cálculo do indicador)
    const totalPercentage = selectedUsers.reduce((sum, selectedUserId) => {
      const share = userShares.find(s => s.userId === selectedUserId);
      const inputValue = parseFloat(inputValues[selectedUserId] || '0');
      const userPercentage = !isNaN(inputValue) ? inputValue / 100 : (share ? share.percentage : 0);
      return sum + userPercentage;
    }, 0);
    
    if (Math.abs(totalPercentage - 1) > 0.01) {
      setError(`O percentual total deve ser 100%. Atual: ${(totalPercentage * 100).toFixed(1)}%`);
      return false;
    }

    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Atualizar userShares com os valores dos inputs antes de validar
    const updatedShares = selectedUsers.map(userId => {
      const existingShare = userShares.find(s => s.userId === userId);
      const inputValue = parseFloat(inputValues[userId] || '0');
      const percentage = !isNaN(inputValue) ? inputValue / 100 : (existingShare ? existingShare.percentage : 0);
      
      return {
        userId,
        percentage,
        responsible: existingShare ? existingShare.responsible : false
      };
    });
    
    console.log('Submitting shares:', updatedShares);
    console.log('Current inputValues:', inputValues);
    
    setUserShares(updatedShares);

    if (!validateShares()) {
      return;
    }

    setLoading(true);
    try {
      const request: CreateItemShareRequest = {
        userShares: updatedShares
      };

      console.log('Sending request to API:', request);
      await apiService.createItemShares(invoiceId, itemId, request);
      console.log('Shares saved successfully');
      
      // Exibe mensagem de sucesso
      setSuccess('Divisão salva com sucesso!');
      
      // Recarrega os shares do backend
      await loadExistingShares();
      
      // (Opcional) Chame onSharesUpdated() se precisar atualizar a lista fora do modal
      if (onSharesUpdated) onSharesUpdated();
    } catch (err: any) {
      console.error('Error saving shares:', err);
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

        <div className="share-controls">
          <button 
            type="button" 
            onClick={calculateEqualShares}
            className="equal-shares-btn"
          >
            Dividir Igualmente
          </button>
        </div>

        {error && <div className="error-message">{error}</div>}
        {success && <div className="success-message">{success}</div>}

        {selectedUsers.length > 0 && (
          <div className="distribution-status">
            {(() => {
              // Calcular total considerando todos os usuários selecionados
              const totalUsed = selectedUsers.reduce((sum, selectedUserId) => {
                const share = userShares.find(s => s.userId === selectedUserId);
                const inputValue = parseFloat(inputValues[selectedUserId] || '0');
                const userPercentage = !isNaN(inputValue) ? inputValue / 100 : (share ? share.percentage : 0);
                return sum + userPercentage;
              }, 0);
              
              const remaining = Math.max(0, 1 - totalUsed);
              return remaining > 0 ? (
                <div className="status-info">
                  <span>Distribuição: {(totalUsed * 100).toFixed(1)}% / 100%</span>
                  <span className="remaining">Restante: {(remaining * 100).toFixed(1)}%</span>
                </div>
              ) : totalUsed === 1 ? (
                <div className="status-complete">
                  <span>✓ Distribuição completa (100%)</span>
                </div>
              ) : (
                <div className="status-exceeded">
                  <span>⚠ Excedido: {((totalUsed - 1) * 100).toFixed(1)}% acima do limite</span>
                </div>
              );
            })()}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="users-list">
            {(() => {
              console.log('=== RENDERING USERS ===');
              console.log('users:', users);
              console.log('selectedUsers:', selectedUsers);
              console.log('userShares:', userShares);
              console.log('inputValues:', inputValues);
              console.log('======================');
              return null;
            })()}
            {users.map((user: User) => {
              return (
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
                          key={`percentage-${user.id}`}
                          type="text"
                          value={inputValues[user.id] ?? '0.0'}
                          onChange={(e) => {
                            setInputValues(prev => ({ ...prev, [user.id]: e.target.value }));
                          }}
                          onBlur={(e) => {
                            const value = parseFloat(e.target.value);
                            if (!isNaN(value) && value >= 0 && value <= 100) {
                              handlePercentageChange(user.id, value / 100);
                            } else if (e.target.value === '') {
                              setInputValues(prev => ({ ...prev, [user.id]: '0.0' }));
                              handlePercentageChange(user.id, 0);
                            }
                          }}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              const value = parseFloat((e.target as HTMLInputElement).value);
                              if (!isNaN(value) && value >= 0 && value <= 100) {
                                handlePercentageChange(user.id, value / 100);
                              }
                            }
                          }}
                          placeholder="0.0"
                          style={{
                            width: '80px',
                            padding: '8px',
                            border: '1px solid #ddd',
                            borderRadius: '4px',
                            textAlign: 'center',
                            fontSize: '14px',
                            position: 'relative',
                            zIndex: 1000,
                            pointerEvents: 'auto',
                            backgroundColor: 'white'
                          }}
                        />
                        <span>%</span>
                        {selectedUsers.length > 1 && (() => {
                          const currentShare = userShares.find(s => s.userId === user.id);
                          const currentInputValue = parseFloat(inputValues[user.id] || '0');
                          const currentPercentage = !isNaN(currentInputValue) ? currentInputValue / 100 : (currentShare ? currentShare.percentage : 0);
                          
                          // Calcular total considerando todos os usuários selecionados
                          const totalUsed = selectedUsers.reduce((sum, selectedUserId) => {
                            if (selectedUserId === user.id) {
                              return sum + currentPercentage; // Usar valor atual do input para este usuário
                            }
                            const share = userShares.find(s => s.userId === selectedUserId);
                            const inputValue = parseFloat(inputValues[selectedUserId] || '0');
                            const userPercentage = !isNaN(inputValue) ? inputValue / 100 : (share ? share.percentage : 0);
                            return sum + userPercentage;
                          }, 0);
                          
                          const remaining = Math.max(0, 1 - totalUsed);
                          return remaining > 0 ? (
                            <small>
                              Restante: {(remaining * 100).toFixed(1)}%
                            </small>
                          ) : totalUsed === 1 ? (
                            <small className="complete">
                              ✓ Completo
                            </small>
                          ) : (
                            <small className="exceeded">
                              Excedido: {((totalUsed - 1) * 100).toFixed(1)}%
                            </small>
                          );
                        })()}
                      </div>

                      <div className="amount-display">
                        <label>Valor:</label>
                        <span>
                          {(() => {
                            const share = userShares.find(s => s.userId === user.id);
                            const inputValue = parseFloat(inputValues[user.id] || '0');
                            const percentage = !isNaN(inputValue) ? inputValue / 100 : (share ? share.percentage : 0);
                            return formatCurrency(percentage * itemAmount);
                          })()}
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
              );
            })}
          </div>

          <div className="total-info">
            <p>
              <strong>Total Selecionado:</strong> {(() => {
                const totalPercentage = selectedUsers.reduce((sum, selectedUserId) => {
                  const share = userShares.find(s => s.userId === selectedUserId);
                  const inputValue = parseFloat(inputValues[selectedUserId] || '0');
                  const userPercentage = !isNaN(inputValue) ? inputValue / 100 : (share ? share.percentage : 0);
                  return sum + userPercentage;
                }, 0);
                return formatPercentage(totalPercentage);
              })()}
            </p>
            <p>
              <strong>Valor Total:</strong> {(() => {
                const totalAmount = selectedUsers.reduce((sum, selectedUserId) => {
                  const share = userShares.find(s => s.userId === selectedUserId);
                  const inputValue = parseFloat(inputValues[selectedUserId] || '0');
                  const userPercentage = !isNaN(inputValue) ? inputValue / 100 : (share ? share.percentage : 0);
                  return sum + (userPercentage * itemAmount);
                }, 0);
                return formatCurrency(totalAmount);
              })()}
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