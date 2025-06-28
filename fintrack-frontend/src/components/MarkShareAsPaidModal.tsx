import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MarkShareAsPaidRequest } from '../types/itemShare';
import apiService from '../services/api';
import './MarkShareAsPaidModal.css';

interface MarkShareAsPaidModalProps {
  isOpen: boolean;
  onClose: () => void;
  shareId: number;
  shareDescription: string;
  shareAmount: number;
  onPaymentMarked: () => void;
}

const PAYMENT_METHODS = [
  { value: 'PIX', label: 'PIX' },
  { value: 'BANK_TRANSFER', label: 'Transferência Bancária' },
  { value: 'CASH', label: 'Dinheiro' },
  { value: 'CREDIT_CARD', label: 'Cartão de Crédito' },
  { value: 'DEBIT_CARD', label: 'Cartão de Débito' },
  { value: 'OTHER', label: 'Outro' }
];

const MarkShareAsPaidModal: React.FC<MarkShareAsPaidModalProps> = ({
  isOpen,
  onClose,
  shareId,
  shareDescription,
  shareAmount,
  onPaymentMarked
}) => {
  const { t } = useTranslation();
  const [paymentMethod, setPaymentMethod] = useState('PIX');
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().slice(0, 16));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    setSaving(true);
    setError(null);

    try {
      const request: MarkShareAsPaidRequest = {
        paymentMethod,
        paidAt: new Date(paymentDate).toISOString()
      };

      await apiService.markShareAsPaid(shareId, request);
      onPaymentMarked();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao marcar pagamento');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-container mark-share-paid-modal">
        <div className="modal-header">
          <h2>Marcar como Pago</h2>
          <button onClick={onClose} className="close-button">&times;</button>
        </div>
        
        <div className="modal-content">
          <div className="share-info">
            <h3>{shareDescription}</h3>
            <p className="share-amount">Valor: R$ {shareAmount.toFixed(2)}</p>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="paymentMethod">Método de Pagamento:</label>
              <select
                id="paymentMethod"
                value={paymentMethod}
                onChange={(e) => setPaymentMethod(e.target.value)}
                required
              >
                {PAYMENT_METHODS.map(method => (
                  <option key={method.value} value={method.value}>
                    {method.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="paymentDate">Data do Pagamento:</label>
              <input
                type="datetime-local"
                id="paymentDate"
                value={paymentDate}
                onChange={(e) => setPaymentDate(e.target.value)}
                required
              />
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="form-actions">
              <button type="submit" className="submit-button" disabled={saving}>
                {saving ? 'Salvando...' : 'Marcar como Pago'}
              </button>
              <button type="button" onClick={onClose} className="cancel-button">
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default MarkShareAsPaidModal; 