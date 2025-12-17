import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { InvoiceImport as InvoiceImportType, ImportStatus, ImportSource } from '../types/invoiceImport';
import { CreditCard } from '../types/creditCard';
import { Category } from '../types/invoice';
import styles from './InvoiceImport.module.css';

interface InvoiceImportProps {
  onImportSuccess?: () => void;
}

const InvoiceImport: React.FC<InvoiceImportProps> = ({ onImportSuccess }) => {
  const { t } = useTranslation();
  const [file, setFile] = useState<File | null>(null);
  const [selectedCreditCard, setSelectedCreditCard] = useState<number | ''>('');
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [imports, setImports] = useState<InvoiceImportType[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [selectedImport, setSelectedImport] = useState<InvoiceImportType | null>(null);
  const [showManualReview, setShowManualReview] = useState(false);

  useEffect(() => {
    loadCreditCards();
    loadImports();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadCreditCards = async () => {
    try {
      const response = await apiService.getCreditCards();
      setCreditCards(response.creditCards);
    } catch (error) {
      console.error('Error loading credit cards:', error);
      setError(t('invoiceImport.errorLoadingCreditCards'));
    }
  };

  const loadImports = async () => {
    try {
      setLoading(true);
      const response = await apiService.getInvoiceImports();
      setImports(response.imports ?? []);
    } catch (error) {
      console.error('Error loading imports:', error);
      setError(t('invoiceImport.errorLoadingImports'));
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (selectedFile) {
      setFile(selectedFile);
      setError(null);
    }
  };

  const handleCreditCardChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    const value = event.target.value;
    setSelectedCreditCard(value === '' ? '' : Number(value));
    setError(null);
  };

  const handleUpload = async () => {
    if (!file) {
      setError(t('invoiceImport.selectFile'));
      return;
    }

    if (!selectedCreditCard) {
      setError(t('invoiceImport.selectCreditCard'));
      return;
    }

    try {
      setUploading(true);
      setError(null);
      setSuccess(null);

      await apiService.importInvoice(file, {
        creditCardId: Number(selectedCreditCard)
      });

      setSuccess(t('invoiceImport.uploadSuccess'));
      setFile(null);
      setSelectedCreditCard('');
      if (document.getElementById('file-input')) {
        (document.getElementById('file-input') as HTMLInputElement).value = '';
      }
      
      // Reload imports
      await loadImports();
      
      // Call success callback
      if (onImportSuccess) {
        onImportSuccess();
      }
    } catch (error: any) {
      console.error('Error uploading file:', error);
      setError(error.response?.data?.message || t('invoiceImport.uploadError'));
    } finally {
      setUploading(false);
    }
  };

  const handleDeleteImport = async (id: number) => {
    if (!window.confirm(t('invoiceImport.confirmDelete'))) {
      return;
    }

    try {
      await apiService.deleteInvoiceImport(id);
      setSuccess(t('invoiceImport.deleteSuccess'));
      await loadImports();
    } catch (error: any) {
      console.error('Error deleting import:', error);
      setError(error.response?.data?.message || t('invoiceImport.deleteError'));
    }
  };

  const handleManualReview = (importItem: InvoiceImportType) => {
    setSelectedImport(importItem);
    setShowManualReview(true);
  };

  const getStatusColor = (status: ImportStatus) => {
    switch (status) {
      case ImportStatus.COMPLETED:
        return 'text-green-600 bg-green-100';
      case ImportStatus.FAILED:
        return 'text-red-600 bg-red-100';
      case ImportStatus.PROCESSING:
        return 'text-blue-600 bg-blue-100';
      case ImportStatus.MANUAL_REVIEW:
        return 'text-yellow-600 bg-yellow-100';
      case ImportStatus.PENDING:
        return 'text-gray-600 bg-gray-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const getStatusText = (status: ImportStatus) => {
    switch (status) {
      case ImportStatus.COMPLETED:
        return t('invoiceImport.status.completed');
      case ImportStatus.FAILED:
        return t('invoiceImport.status.failed');
      case ImportStatus.PROCESSING:
        return t('invoiceImport.status.processing');
      case ImportStatus.MANUAL_REVIEW:
        return t('invoiceImport.status.manualReview');
      case ImportStatus.PENDING:
        return t('invoiceImport.status.pending');
      default:
        return status;
    }
  };

  const getSourceText = (source: ImportSource) => {
    switch (source) {
      case ImportSource.PDF:
        return t('invoiceImport.source.pdf');
      case ImportSource.IMAGE:
        return t('invoiceImport.source.image');
      case ImportSource.EMAIL:
        return t('invoiceImport.source.email');
      case ImportSource.MANUAL:
        return t('invoiceImport.source.manual');
      default:
        return source;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className={styles.invoiceImportContainer}>
      <div className={styles.invoiceImportHeader}>
        <h2>{t('invoiceImport.title')}</h2>
        <p>{t('invoiceImport.description')}</p>
      </div>

      {/* Upload Section */}
      <div className={styles.uploadSection}>
        <h3>{t('invoiceImport.uploadTitle')}</h3>
        
        <div className={styles.uploadForm}>
          <div className={styles.formGroup}>
            <label htmlFor="file-input">{t('invoiceImport.selectFile')}:</label>
            <input
              id="file-input"
              type="file"
              accept=".pdf,.jpg,.jpeg,.png"
              onChange={handleFileChange}
              className={styles.fileInput}
            />
            {file && (
              <div className={styles.fileInfo}>
                <span>{file.name}</span>
                <span>{(file.size / 1024 / 1024).toFixed(2)} MB</span>
              </div>
            )}
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="credit-card-select">{t('invoiceImport.selectCreditCard')}:</label>
            <select
              id="credit-card-select"
              value={selectedCreditCard}
              onChange={handleCreditCardChange}
              className={styles.selectInput}
            >
              <option value="">{t('invoiceImport.selectCreditCardPlaceholder')}</option>
              {creditCards.map((card) => (
                <option key={card.id} value={card.id}>
                  {card.name} - {card.lastFourDigits}
                </option>
              ))}
            </select>
          </div>

          <button
            onClick={handleUpload}
            disabled={!file || !selectedCreditCard || uploading}
            className={styles.uploadButton}
          >
            {uploading ? t('invoiceImport.uploading') : t('invoiceImport.upload')}
          </button>
        </div>

        {error && <div className={styles.errorMessage}>{error}</div>}
        {success && <div className={styles.successMessage}>{success}</div>}
      </div>

      {/* Imports List */}
      <div className={styles.importsSection}>
        <div className={styles.importsHeader}>
          <h3>{t('invoiceImport.importsTitle')}</h3>
          <button onClick={loadImports} className={styles.refreshButton}>
            {t('invoiceImport.refresh')}
          </button>
        </div>

        {loading ? (
          <div className={styles.loading}>{t('invoiceImport.loading')}</div>
        ) : (imports ?? []).length === 0 ? (
          <div className={styles.noImports}>{t('invoiceImport.noImports')}</div>
        ) : (
          <div className={styles.importsList}>
            {(imports ?? []).map((importItem) => (
              <div key={importItem.id} className={styles.importItem}>
                <div className={styles.importHeader}>
                  <div className={styles.importInfo}>
                    <h4>{importItem.originalFileName}</h4>
                    <span className={`${styles.statusBadge} ${getStatusColor(importItem.status)}`}>
                      {getStatusText(importItem.status)}
                    </span>
                    <span className={styles.sourceBadge}>
                      {getSourceText(importItem.source)}
                    </span>
                  </div>
                  <div className={styles.importActions}>
                    {importItem.status === ImportStatus.MANUAL_REVIEW && (
                      <button
                        onClick={() => handleManualReview(importItem)}
                        className={styles.reviewButton}
                      >
                        {t('invoiceImport.review')}
                      </button>
                    )}
                    <button
                      onClick={() => handleDeleteImport(importItem.id)}
                      className={styles.deleteButton}
                    >
                      {t('invoiceImport.delete')}
                    </button>
                  </div>
                </div>

                <div className={styles.importDetails}>
                  <div className={styles.detailRow}>
                    <span>{t('invoiceImport.importedAt')}:</span>
                    <span>{formatDateTime(importItem.importedAt)}</span>
                  </div>
                  
                  {importItem.processedAt && (
                    <div className={styles.detailRow}>
                      <span>{t('invoiceImport.processedAt')}:</span>
                      <span>{formatDateTime(importItem.processedAt)}</span>
                    </div>
                  )}

                  {importItem.totalAmount && (
                    <div className={styles.detailRow}>
                      <span>{t('invoiceImport.totalAmount')}:</span>
                      <span>R$ {importItem.totalAmount.toFixed(2)}</span>
                    </div>
                  )}

                  {importItem.dueDate && (
                    <div className={styles.detailRow}>
                      <span>{t('invoiceImport.dueDate')}:</span>
                      <span>{formatDate(importItem.dueDate)}</span>
                    </div>
                  )}

                  {importItem.errorMessage && (
                    <div className={styles.errorDetail}>
                      <span>{t('invoiceImport.error')}:</span>
                      <span>{importItem.errorMessage}</span>
                    </div>
                  )}

                  {importItem.extractedText && (
                    <div className={styles.extractedText}>
                      <span>{t('invoiceImport.extractedText')}:</span>
                      <pre>{importItem.extractedText}</pre>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Manual Review Modal */}
      {showManualReview && selectedImport && (
        <ManualReviewModal
          importItem={selectedImport}
          onClose={() => {
            setShowManualReview(false);
            setSelectedImport(null);
          }}
          onSuccess={() => {
            setShowManualReview(false);
            setSelectedImport(null);
            loadImports();
            setSuccess(t('invoiceImport.reviewSuccess'));
          }}
        />
      )}
    </div>
  );
};

// Manual Review Modal Component
interface ManualReviewModalProps {
  importItem: InvoiceImportType;
  onClose: () => void;
  onSuccess: () => void;
}

const ManualReviewModal: React.FC<ManualReviewModalProps> = ({
  importItem,
  onClose,
  onSuccess
}) => {
  const { t } = useTranslation();
  const [totalAmount, setTotalAmount] = useState(importItem.totalAmount || 0);
  const [dueDate, setDueDate] = useState(importItem.dueDate ? importItem.dueDate.split('T')[0] : '');
  const [bankName, setBankName] = useState(importItem.bankName || '');
  const [cardLastFourDigits, setCardLastFourDigits] = useState(importItem.cardLastFourDigits || '');
  const [items, setItems] = useState<Array<{
    description: string;
    amount: number;
    categoryId?: number;
    purchaseDate: string;
    installments?: number;
    totalInstallments?: number;
  }>>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    try {
      const response = await apiService.getCategories();
      setCategories(response.categories);
    } catch (error) {
      console.error('Error loading categories:', error);
    }
  };

  const addItem = () => {
    setItems([...items, {
      description: '',
      amount: 0,
      purchaseDate: new Date().toISOString().split('T')[0],
      installments: 1,
      totalInstallments: 1
    }]);
  };

  const removeItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const updateItem = (index: number, field: string, value: any) => {
    const newItems = [...items];
    newItems[index] = { ...newItems[index], [field]: value };
    setItems(newItems);
  };

  const handleSubmit = async () => {
    if (!totalAmount || !dueDate || items.length === 0) {
      setError(t('invoiceImport.fillAllFields'));
      return;
    }

    try {
      setLoading(true);
      setError(null);

      await apiService.manualReview(importItem.id, {
        totalAmount,
        dueDate,
        bankName: bankName || undefined,
        cardLastFourDigits: cardLastFourDigits || undefined,
        items
      });

      onSuccess();
    } catch (error: any) {
      console.error('Error submitting manual review:', error);
      setError(error.response?.data?.message || t('invoiceImport.reviewError'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalContent}>
        <div className={styles.modalHeader}>
          <h3>{t('invoiceImport.manualReview')}</h3>
          <button onClick={onClose} className={styles.closeButton}>&times;</button>
        </div>

        <div className={styles.modalBody}>
          <div className={styles.formGroup}>
            <label>{t('invoiceImport.totalAmount')}:</label>
            <input
              type="number"
              step="0.01"
              value={totalAmount}
              onChange={(e) => setTotalAmount(Number(e.target.value))}
              className={styles.inputField}
            />
          </div>

          <div className={styles.formGroup}>
            <label>{t('invoiceImport.dueDate')}:</label>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className={styles.inputField}
            />
          </div>

          <div className={styles.formGroup}>
            <label>{t('invoiceImport.bankName')}:</label>
            <input
              type="text"
              value={bankName}
              onChange={(e) => setBankName(e.target.value)}
              className={styles.inputField}
              placeholder={t('invoiceImport.bankNamePlaceholder')}
            />
          </div>

          <div className={styles.formGroup}>
            <label>{t('invoiceImport.cardLastFourDigits')}:</label>
            <input
              type="text"
              value={cardLastFourDigits}
              onChange={(e) => setCardLastFourDigits(e.target.value)}
              className={styles.inputField}
              placeholder={t('invoiceImport.cardLastFourDigitsPlaceholder')}
              maxLength={4}
            />
          </div>

          <div className={styles.itemsSection}>
            <div className={styles.itemsHeader}>
              <h4>{t('invoiceImport.items')}</h4>
              <button onClick={addItem} className={styles.addItemButton}>
                {t('invoiceImport.addItem')}
              </button>
            </div>

            {items.map((item, index) => (
              <div key={index} className={styles.itemRow}>
                <div className={styles.itemFields}>
                  <input
                    type="text"
                    value={item.description}
                    onChange={(e) => updateItem(index, 'description', e.target.value)}
                    placeholder={t('invoiceImport.itemDescription')}
                    className={styles.inputField}
                  />
                  <input
                    type="number"
                    step="0.01"
                    value={item.amount}
                    onChange={(e) => updateItem(index, 'amount', Number(e.target.value))}
                    placeholder={t('invoiceImport.itemAmount')}
                    className={styles.inputField}
                  />
                  <select
                    value={item.categoryId || ''}
                    onChange={(e) => updateItem(index, 'categoryId', e.target.value ? Number(e.target.value) : undefined)}
                    className={styles.selectField}
                  >
                    <option value="">{t('invoiceImport.selectCategory')}</option>
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                  <input
                    type="date"
                    value={item.purchaseDate}
                    onChange={(e) => updateItem(index, 'purchaseDate', e.target.value)}
                    className={styles.inputField}
                  />
                  <input
                    type="number"
                    value={item.installments || 1}
                    onChange={(e) => updateItem(index, 'installments', Number(e.target.value))}
                    placeholder={t('invoiceImport.installments')}
                    className={styles.inputField}
                    min="1"
                  />
                  <input
                    type="number"
                    value={item.totalInstallments || 1}
                    onChange={(e) => updateItem(index, 'totalInstallments', Number(e.target.value))}
                    placeholder={t('invoiceImport.totalInstallments')}
                    className={styles.inputField}
                    min="1"
                  />
                </div>
                <button
                  onClick={() => removeItem(index)}
                  className={styles.removeItemButton}
                >
                  {t('invoiceImport.removeItem')}
                </button>
              </div>
            ))}
          </div>

          {error && <div className={styles.errorMessage}>{error}</div>}
        </div>

        <div className={styles.modalFooter}>
          <button onClick={onClose} className={styles.cancelButton}>
            {t('invoiceImport.cancel')}
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className={styles.submitButton}
          >
            {loading ? t('invoiceImport.submitting') : t('invoiceImport.submit')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default InvoiceImport; 