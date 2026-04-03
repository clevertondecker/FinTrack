import React from 'react';
import { useTranslation } from 'react-i18next';
import { Invoice, InvoiceItem, Category } from '../../types/invoice';
import { formatCurrency, formatDate, getStatusColor, getStatusText } from '../../utils/invoiceUtils';
import InvoiceItemRow from './InvoiceItemRow';
import ItemsTableHeader from './ItemsTableHeader';

export interface InvoiceDetailProps {
  selectedInvoice: Invoice;
  invoiceItems: InvoiceItem[];
  loadingItems: boolean;
  categories: Category[];
  sortedCategories: Category[];
  categoryLookup: (itemCategory: string | null) => number | string;
  handleUpdateItemCategory: (itemId: number, categoryId: number | null) => Promise<void>;
  updatingCategoryItemId: number | null;
  handleShareItem: (item: InvoiceItem) => void;
  handleRemoveItem: (itemId: number) => Promise<void>;
  removingItemId: number | null;
  quickAddMode: boolean;
  itemForm: { description: string; amount: string; categoryId: string; purchaseDate: string };
  handleItemInputChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  handleQuickAdd: () => void;
  handleAddItem: (e: React.FormEvent) => Promise<void>;
  handleAddMore: () => void;
  handleFinishAdding: () => void;
  handleQuickCategorySelect: (categoryId: string) => void;
  handleQuickAmountSelect: (amount: string) => void;
  handleQuickDateSelect: (date: string) => void;
  addingItem: boolean;
  itemError: string | null;
  showAddMore: boolean;
  onProjectInstallments?: () => void;
  projectingInstallments?: boolean;
}

const InvoiceDetail: React.FC<InvoiceDetailProps> = ({
  selectedInvoice,
  invoiceItems,
  loadingItems,
  categories,
  sortedCategories,
  categoryLookup,
  handleUpdateItemCategory,
  updatingCategoryItemId,
  handleShareItem,
  handleRemoveItem,
  removingItemId,
  quickAddMode,
  itemForm,
  handleItemInputChange,
  handleQuickAdd,
  handleAddItem,
  handleAddMore,
  handleFinishAdding,
  handleQuickCategorySelect,
  handleQuickAmountSelect,
  handleQuickDateSelect,
  addingItem,
  itemError,
  showAddMore,
  onProjectInstallments,
  projectingInstallments,
}) => {
  const { t } = useTranslation();

  return (
    <>
      <div className="invoice-info">
        <div className="info-row">
          <span className="label">{t('invoices.creditCardLabel')}:</span>
          <span className="value">{selectedInvoice.creditCardName}</span>
        </div>
        <div className="info-row">
          <span className="label">{t('invoices.dueDateLabel')}:</span>
          <span className="value">{formatDate(selectedInvoice.dueDate)}</span>
        </div>
        <div className="info-row">
          <span className="label">{t('invoices.statusLabel')}:</span>
          <span className={`value status ${getStatusColor(selectedInvoice.status)}`}>{getStatusText(selectedInvoice.status, t)}</span>
        </div>
        <div className="info-row">
          <span className="label">{t('invoices.totalAmountLabel')}:</span>
          <span className="value">{formatCurrency(selectedInvoice.totalAmount)}</span>
        </div>
        <div className="info-row">
          <span className="label">{t('invoices.paidAmountLabel')}:</span>
          <span className="value">{formatCurrency(selectedInvoice.paidAmount)}</span>
        </div>
      </div>

      {selectedInvoice.contactShares && selectedInvoice.contactShares.length > 0 && (
        <div className="contact-shares-section">
          <h4>{t('invoices.contactSharesTitle')}</h4>
          {selectedInvoice.contactShares.map(cs => (
            <div key={cs.contactEmail} className="contact-share-row">
              <span className="contact-name">{cs.contactName}</span>
              <span className="contact-amount">{formatCurrency(cs.totalAmount)}</span>
            </div>
          ))}
        </div>
      )}

      <div className="invoice-items-section">
        <div className="section-header">
          <h3>{t('invoices.itemsLabel')}</h3>
          <div className="section-header-actions">
            {onProjectInstallments && !quickAddMode && (
              <button
                className="project-button"
                onClick={onProjectInstallments}
                disabled={projectingInstallments}
                title={t('invoices.projectInstallmentsTooltip', 'Projetar parcelas futuras')}
              >
                {projectingInstallments
                  ? t('invoices.projecting', 'Projetando...')
                  : `📅 ${t('invoices.projectInstallments', 'Projetar parcelas')}`}
              </button>
            )}
            {!quickAddMode && selectedInvoice && (
              <button
                className="quick-add-button"
                onClick={handleQuickAdd}
              >
                + {t('invoices.addItem')}
              </button>
            )}
          </div>
        </div>

        {loadingItems ? (
          <div className="loading">{t('invoices.loadingItems')}</div>
        ) : (
          <>
            {invoiceItems.length === 0 ? (
              <p className="no-items">{t('invoices.noItems')}</p>
            ) : (
              <>
                <ItemsTableHeader />
                <div className="items-list">
                  {invoiceItems.map(item => (
                    <InvoiceItemRow
                      key={item.id}
                      item={item}
                      sortedCategories={sortedCategories}
                      categoryLookup={categoryLookup}
                      onCategoryChange={handleUpdateItemCategory}
                      onShare={handleShareItem}
                      onRemove={handleRemoveItem}
                      updatingCategoryItemId={updatingCategoryItemId}
                      removingItemId={removingItemId}
                    />
                  ))}
                </div>
              </>
            )}

            {quickAddMode && (
              <div className="compact-item-form">
                <div className="form-header">
                  <h4>{t('invoices.addItem')}</h4>
                  <button
                    className="close-form-button"
                    onClick={handleFinishAdding}
                  >
                    ×
                  </button>
                </div>

                <div className="quick-shortcuts">
                  <div className="shortcut-group">
                    <label>Categorias:</label>
                    <div className="shortcut-buttons">
                      {categories.slice(0, 5).map(category => (
                        <button
                          key={category.id}
                          type="button"
                          className={`shortcut-btn ${itemForm.categoryId === category.id.toString() ? 'active' : ''}`}
                          onClick={() => handleQuickCategorySelect(category.id.toString())}
                        >
                          {category.name}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="shortcut-group">
                    <label>Valores comuns:</label>
                    <div className="shortcut-buttons">
                      {['10', '20', '50', '100', '200'].map(amount => (
                        <button
                          key={amount}
                          type="button"
                          className={`shortcut-btn ${itemForm.amount === amount ? 'active' : ''}`}
                          onClick={() => handleQuickAmountSelect(amount)}
                        >
                          R$ {amount}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="shortcut-group">
                    <label>Data:</label>
                    <div className="shortcut-buttons">
                      <button
                        type="button"
                        className={`shortcut-btn ${itemForm.purchaseDate === new Date().toISOString().split('T')[0] ? 'active' : ''}`}
                        onClick={() => handleQuickDateSelect(new Date().toISOString().split('T')[0])}
                      >
                        Hoje
                      </button>
                      <button
                        type="button"
                        className={`shortcut-btn ${itemForm.purchaseDate === new Date(Date.now() - 24*60*60*1000).toISOString().split('T')[0] ? 'active' : ''}`}
                        onClick={() => handleQuickDateSelect(new Date(Date.now() - 24*60*60*1000).toISOString().split('T')[0])}
                      >
                        Ontem
                      </button>
                    </div>
                  </div>
                </div>

                <form onSubmit={handleAddItem} className="compact-form">
                  <div className="form-row-compact">
                    <div className="form-group-compact">
                      <input
                        type="text"
                        name="description"
                        value={itemForm.description}
                        onChange={handleItemInputChange}
                        placeholder={t('invoices.descriptionPlaceholder')}
                        required
                        className="description-input"
                      />
                    </div>
                    <div className="form-group-compact">
                      <input
                        type="number"
                        name="amount"
                        value={itemForm.amount}
                        onChange={handleItemInputChange}
                        placeholder="R$ 0,00"
                        step="0.01"
                        min="0"
                        required
                        className="amount-input"
                      />
                    </div>
                    <div className="form-group-compact">
                      <select
                        name="categoryId"
                        value={itemForm.categoryId}
                        onChange={handleItemInputChange}
                        className="category-select"
                      >
                        <option value="">{t('invoices.selectCategory')}</option>
                        {categories.map(category => (
                          <option key={category.id} value={category.id}>
                            {category.name}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group-compact">
                      <input
                        type="date"
                        name="purchaseDate"
                        value={itemForm.purchaseDate}
                        onChange={handleItemInputChange}
                        className="date-input"
                      />
                    </div>
                    <button type="submit" className="add-button-compact" disabled={addingItem}>
                      {addingItem ? '...' : '+'}
                    </button>
                  </div>
                  {itemError && <div className="error-message">{itemError}</div>}
                </form>
              </div>
            )}

            {showAddMore && (
              <div className="add-more-section">
                <p>Item adicionado com sucesso!</p>
                <div className="add-more-buttons">
                  <button
                    className="add-more-button"
                    onClick={handleAddMore}
                  >
                    + Adicionar outro item
                  </button>
                  <button
                    className="finish-button"
                    onClick={handleFinishAdding}
                  >
                    Finalizar
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </>
  );
};

export default InvoiceDetail;
