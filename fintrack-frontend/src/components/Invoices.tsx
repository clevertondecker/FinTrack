import React from 'react';
import { useTranslation } from 'react-i18next';
import { FileText, RefreshCw } from 'lucide-react';
import HelpTooltip from './common/HelpTooltip';
import { formatCurrency, formatDate, getStatusColor, getStatusText } from '../utils/invoiceUtils';
import ShareItemModal from './ShareItemModal';
import InvoiceList from './invoices/InvoiceList';
import InvoiceDetail from './invoices/InvoiceDetail';
import ConsolidatedView from './invoices/ConsolidatedView';
import Subscriptions from './Subscriptions';
import { useInvoiceData } from '../hooks/useInvoiceData';
import './Invoices.css';

const Invoices: React.FC = () => {
  const { t } = useTranslation();
  const data = useInvoiceData();

  if (data.loading) {
    return <div className="loading">{t('invoices.loading')}</div>;
  }

  return (
    <div className="invoices-container">
      {/* Tab Navigation */}
      <HelpTooltip textKey="help.invoices.tabs" position="bottom">
      <div className="invoices-tabs">
        <button
          className={`invoices-tab ${data.activeTab === 'invoices' ? 'active' : ''}`}
          onClick={() => data.setActiveTab('invoices')}
        >
          <FileText size={18} />
          {t('invoices.tabInvoices')}
        </button>
        <button
          className={`invoices-tab ${data.activeTab === 'subscriptions' ? 'active' : ''}`}
          onClick={() => data.setActiveTab('subscriptions')}
        >
          <RefreshCw size={18} />
          {t('invoices.tabSubscriptions')}
        </button>
      </div>
      </HelpTooltip>

      {data.activeTab === 'subscriptions' && <Subscriptions />}

      {data.activeTab === 'invoices' && (
      <>
        <InvoiceList
          error={data.error}
          showCreateForm={data.showCreateForm}
          setShowCreateForm={data.setShowCreateForm}
          formData={data.formData}
          handleInputChange={data.handleInputChange}
          handleSubmit={data.handleSubmit}
          handleCancel={data.handleCancel}
          creditCards={data.creditCards}
          showFilters={data.showFilters}
          setShowFilters={data.setShowFilters}
          filters={data.filters}
          filteredInvoices={data.filteredInvoices}
          summary={data.summary}
          handleFilterChange={data.handleFilterChange}
          clearFilters={data.clearFilters}
          groupedInvoices={data.groupedInvoices}
          formatInvoiceAmount={data.formatInvoiceAmount}
          getInvoiceUserAmount={data.getInvoiceUserAmount}
          getInvoiceUserPaid={data.getInvoiceUserPaid}
          getInvoiceUserRemaining={data.getInvoiceUserRemaining}
          getConsolidatedCards={data.getConsolidatedCards}
          isConsolidated={data.isConsolidated}
          handleViewDetails={data.handleViewDetails}
          handleViewConsolidatedDetails={data.handleViewConsolidatedDetails}
          handleOpenPayModal={data.handleOpenPayModal}
          handleDeleteInvoice={data.handleDeleteInvoice}
        />

        {/* Details Modal */}
        {data.showDetailsModal && data.selectedInvoice && (
          <div className="modal-overlay">
            <div className="modal-container consolidated-modal">
              <div className="modal-header">
                <h2>{data.isConsolidatedView ? t('invoices.consolidatedDetailsTitle', 'Fatura Consolidada') : t('invoices.detailsModalTitle')}</h2>
                <button onClick={data.handleCloseDetails} className="close-button">&times;</button>
              </div>

              <div className="modal-content">
                {data.isConsolidatedView && data.cardItemGroups.length > 0 ? (
                  <>
                    <div className="invoice-info">
                      <div className="info-row">
                        <span className="label">{t('invoices.dueDateLabel')}:</span>
                        <span className="value">{formatDate(data.selectedInvoice.dueDate)}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">{t('invoices.statusLabel')}:</span>
                        <span className={`value status ${getStatusColor(data.selectedInvoice.status)}`}>{getStatusText(data.selectedInvoice.status, t)}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">{t('invoices.totalAmountLabel')}:</span>
                        <span className="value">{formatCurrency(data.selectedInvoice.totalAmount)}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">{t('invoices.paidAmountLabel')}:</span>
                        <span className="value">{formatCurrency(data.selectedInvoice.paidAmount)}</span>
                      </div>
                    </div>

                    {data.selectedInvoice.contactShares && data.selectedInvoice.contactShares.length > 0 && (
                      <div className="contact-shares-section">
                        <h4>{t('invoices.contactSharesTitle')}</h4>
                        {data.selectedInvoice.contactShares.map(cs => (
                          <div key={cs.contactEmail} className="contact-share-row">
                            <span className="contact-name">{cs.contactName}</span>
                            <span className="contact-amount">{formatCurrency(cs.totalAmount)}</span>
                          </div>
                        ))}
                      </div>
                    )}

                    <ConsolidatedView
                      cardItemGroups={data.cardItemGroups}
                      activeCardTab={data.activeCardTab}
                      setActiveCardTab={data.setActiveCardTab}
                      invoiceItems={data.invoiceItems}
                      loadingItems={data.loadingItems}
                      sortedCategories={data.sortedCategories}
                      categoryLookup={data.categoryLookup}
                      handleUpdateItemCategory={data.handleUpdateItemCategory}
                      updatingCategoryItemId={data.updatingCategoryItemId}
                      handleShareItem={data.handleShareItem}
                    />
                  </>
                ) : (
                  <InvoiceDetail
                    selectedInvoice={data.selectedInvoice}
                    invoiceItems={data.invoiceItems}
                    loadingItems={data.loadingItems}
                    categories={data.categories}
                    sortedCategories={data.sortedCategories}
                    categoryLookup={data.categoryLookup}
                    handleUpdateItemCategory={data.handleUpdateItemCategory}
                    updatingCategoryItemId={data.updatingCategoryItemId}
                    handleShareItem={data.handleShareItem}
                    handleRemoveItem={data.handleRemoveItem}
                    removingItemId={data.removingItemId}
                    quickAddMode={data.quickAddMode}
                    itemForm={data.itemForm}
                    handleItemInputChange={data.handleItemInputChange}
                    handleQuickAdd={data.handleQuickAdd}
                    handleAddItem={data.handleAddItem}
                    handleAddMore={data.handleAddMore}
                    handleFinishAdding={data.handleFinishAdding}
                    handleQuickCategorySelect={data.handleQuickCategorySelect}
                    handleQuickAmountSelect={data.handleQuickAmountSelect}
                    handleQuickDateSelect={data.handleQuickDateSelect}
                    addingItem={data.addingItem}
                    itemError={data.itemError}
                    showAddMore={data.showAddMore}
                    onProjectInstallments={data.handleProjectInstallments}
                    projectingInstallments={data.projectingInstallments}
                  />
                )}
              </div>
            </div>
          </div>
        )}

        {/* Pay Modal */}
        {data.showPayModal && data.invoiceToPay && (
          <div className="modal-overlay">
            <div className="modal-container">
              <div className="modal-header">
                <h2>{t('invoices.payModalTitle')}</h2>
                <button onClick={data.handleClosePayModal} className="close-button">&times;</button>
              </div>

              <div className="modal-content">
                <div className="pay-info">
                  <p><strong>{t('invoices.creditCardLabel')}:</strong> {data.invoiceToPay.creditCardName}</p>
                  <p><strong>{t('invoices.totalAmountLabel')}:</strong> {data.formatInvoiceAmount(data.invoiceToPay)}</p>
                  <p><strong>{t('invoices.paidAmountLabel')}:</strong> {formatCurrency(data.getInvoiceUserPaid(data.invoiceToPay))}</p>
                  <p><strong>{t('invoices.remainingAmountLabel')}:</strong> {formatCurrency(data.getInvoiceUserRemaining(data.invoiceToPay))}</p>
                </div>
                {data.invoiceToPay._consolidatedCards && data.invoiceToPay._consolidatedCards.length > 1 && (
                  <div className="consolidated-pay-breakdown">
                    <p style={{ fontSize: '0.85rem', color: '#666', marginBottom: '0.5rem' }}>
                      {t('invoices.paymentDistribution', 'O pagamento será distribuído proporcionalmente:')}
                    </p>
                    {data.invoiceToPay._consolidatedCards.map(card => {
                      const remaining = (card.totalAmount || 0) - (card.paidAmount || 0);
                      return remaining > 0 ? (
                        <div key={card.id} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', padding: '2px 0' }}>
                          <span>{card.creditCardName}</span>
                          <span>{formatCurrency(remaining)}</span>
                        </div>
                      ) : null;
                    })}
                  </div>
                )}

                <form onSubmit={data.handlePayInvoice} className="pay-form">
                  <div className="form-group">
                    <label htmlFor="payAmount">{t('invoices.payAmountLabel')}</label>
                    <div className="amount-input-group">
                      <input
                        type="number"
                        id="payAmount"
                        value={data.payAmount}
                        onChange={(e) => data.setPayAmount(e.target.value)}
                        step="0.01"
                        min="0"
                        max={(data.invoiceToPay.totalAmount || 0) - (data.invoiceToPay.paidAmount || 0)}
                        required
                      />
                      <button type="button" onClick={data.handlePayTotal} className="total-button">
                        {t('invoices.totalButton')}
                      </button>
                    </div>
                  </div>

                  {data.payError && <div className="error-message">{data.payError}</div>}

                  <div className="form-actions">
                    <button type="submit" className="submit-button" disabled={data.paying}>
                      {data.paying ? t('invoices.paying') : t('invoices.payButton')}
                    </button>
                    <button type="button" onClick={data.handleClosePayModal} className="cancel-button">
                      {t('common.cancel')}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )}

        {/* Delete Confirmation Modal */}
        {data.showDeleteModal && data.invoiceToDelete && (
          <div className="modal-overlay">
            <div className="modal-container" style={{ maxWidth: '480px' }}>
              <div className="modal-header">
                <h2>{t('invoices.deleteModalTitle', 'Excluir Fatura')}</h2>
                <button onClick={data.handleCloseDeleteModal} className="close-button">&times;</button>
              </div>
              <div className="modal-content">
                <div className="invoice-info" style={{ marginBottom: '1rem' }}>
                  <div className="info-row">
                    <span className="label">{t('invoices.creditCardLabel')}:</span>
                    <span className="value">{data.invoiceToDelete.creditCardName}</span>
                  </div>
                  <div className="info-row">
                    <span className="label">{t('invoices.dueDateLabel')}:</span>
                    <span className="value">{formatDate(data.invoiceToDelete.dueDate)}</span>
                  </div>
                  <div className="info-row">
                    <span className="label">{t('invoices.totalAmountLabel')}:</span>
                    <span className="value">{formatCurrency(data.invoiceToDelete.totalAmount)}</span>
                  </div>
                </div>

                {data.loadingDeleteInfo ? (
                  <div className="loading" style={{ padding: '1rem 0' }}>{t('common.loading', 'Carregando...')}</div>
                ) : data.deleteInfo && data.deleteInfo.sharedItems > 0 ? (
                  <div style={{
                    background: '#fef3c7',
                    border: '1px solid #f59e0b',
                    borderRadius: '8px',
                    padding: '1rem',
                    marginBottom: '1rem'
                  }}>
                    <p style={{ fontWeight: 600, color: '#92400e', marginBottom: '0.5rem', fontSize: '0.95rem' }}>
                      {t('invoices.deleteWarningShared', 'Esta fatura contém itens compartilhados!')}
                    </p>
                    <ul style={{ margin: 0, paddingLeft: '1.25rem', color: '#78350f', fontSize: '0.9rem', lineHeight: '1.6' }}>
                      <li>
                        {t('invoices.deleteSharedItemsCount', {
                          count: data.deleteInfo.sharedItems,
                          total: data.deleteInfo.totalItems,
                          defaultValue: `${data.deleteInfo.sharedItems} de ${data.deleteInfo.totalItems} itens possuem compartilhamentos`
                        })}
                      </li>
                      <li>
                        {t('invoices.deleteSharesCount', {
                          count: data.deleteInfo.totalShares,
                          defaultValue: `${data.deleteInfo.totalShares} compartilhamento(s) serão removidos`
                        })}
                      </li>
                      {data.deleteInfo.paidShares > 0 && (
                        <li style={{ fontWeight: 600 }}>
                          {t('invoices.deletePaidSharesWarning', {
                            count: data.deleteInfo.paidShares,
                            defaultValue: `${data.deleteInfo.paidShares} compartilhamento(s) já foram marcados como pagos`
                          })}
                        </li>
                      )}
                    </ul>
                    <p style={{ color: '#92400e', fontSize: '0.85rem', marginTop: '0.75rem', marginBottom: 0 }}>
                      {t('invoices.deleteSharedConsequence', 'Os outros usuários perderão o acesso a esses itens e seu histórico de compartilhamento.')}
                    </p>
                  </div>
                ) : data.deleteInfo ? (
                  <p style={{ color: '#6b7280', fontSize: '0.9rem', marginBottom: '1rem' }}>
                    {t('invoices.deleteNoShares', {
                      count: data.deleteInfo.totalItems,
                      defaultValue: `Esta fatura possui ${data.deleteInfo.totalItems} item(ns) e nenhum compartilhamento.`
                    })}
                  </p>
                ) : null}

                <p style={{ color: '#dc2626', fontWeight: 500, fontSize: '0.9rem', marginBottom: '1.25rem' }}>
                  {t('invoices.deleteConfirmQuestion', 'Tem certeza que deseja excluir esta fatura? Esta ação não pode ser desfeita.')}
                </p>

                <div className="form-actions">
                  <button
                    onClick={data.handleConfirmDelete}
                    className="submit-button"
                    disabled={data.deleting}
                    style={{ background: '#dc2626' }}
                  >
                    {data.deleting
                      ? t('common.deleting', 'Excluindo...')
                      : t('invoices.confirmDelete', 'Sim, excluir fatura')}
                  </button>
                  <button
                    onClick={data.handleCloseDeleteModal}
                    className="cancel-button"
                    disabled={data.deleting}
                  >
                    {t('common.cancel')}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Share Modal */}
        {data.showShareModal && data.selectedItemForSharing && (
          <ShareItemModal
            isOpen={data.showShareModal}
            onClose={data.handleCloseShareModal}
            invoiceId={data.selectedItemForSharing.invoiceId || data.selectedInvoice?.id || 0}
            itemId={data.selectedItemForSharing.id}
            itemDescription={data.selectedItemForSharing.description}
            itemAmount={data.selectedItemForSharing.amount}
            onSharesUpdated={data.handleSharesUpdated}
          />
        )}
      </>
      )}
    </div>
  );
};

export default Invoices;
