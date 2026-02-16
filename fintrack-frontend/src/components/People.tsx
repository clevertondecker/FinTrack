import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { TrustedContact, CreateTrustedContactRequest } from '../types/trustedContact';
import './People.css';

const INITIAL_FORM: CreateTrustedContactRequest = {
  name: '',
  email: '',
  tags: '',
  note: ''
};

const TABLE_COLUMN_COUNT = 4;

function getContactApiError(err: unknown, t: (key: string) => string): string {
  if (err == null || typeof err !== 'object' || !('response' in err)) return t('common.error');
  const body = (err as { response?: { data?: { error?: string } } }).response?.data?.error ?? '';
  return body.includes('already exists') ? t('people.errorDuplicateEmail') : t('common.error');
}

const People: React.FC = () => {
  const { t } = useTranslation();
  const [contacts, setContacts] = useState<TrustedContact[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<TrustedContact | null>(null);
  const [formData, setFormData] = useState<CreateTrustedContactRequest>(INITIAL_FORM);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const loadContacts = useCallback(async () => {
    try {
      setLoading(true);
      const list = await apiService.getTrustedContacts(search || undefined);
      setContacts(list);
      setError(null);
    } catch (err) {
      setError(t('common.error'));
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [search, t]);

  useEffect(() => {
    loadContacts();
  }, [loadContacts]);

  const handleOpenCreate = () => {
    setEditing(null);
    setFormData(INITIAL_FORM);
    setShowModal(true);
    setMessage(null);
  };

  const handleOpenEdit = (contact: TrustedContact) => {
    setEditing(contact);
    setFormData({
      name: contact.name,
      email: contact.email,
      tags: contact.tags ?? '',
      note: contact.note ?? ''
    });
    setShowModal(true);
    setMessage(null);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditing(null);
    setFormData(INITIAL_FORM);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.email.trim()) return;
    setSaving(true);
    setMessage(null);
    try {
      if (editing) {
        await apiService.updateTrustedContact(editing.id, formData);
        setMessage({ type: 'success', text: t('people.successUpdate') });
      } else {
        await apiService.createTrustedContact(formData);
        setMessage({ type: 'success', text: t('people.successCreate') });
      }
      await loadContacts();
      handleCloseModal();
    } catch (err: unknown) {
      setMessage({ type: 'error', text: getContactApiError(err, t) });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (contact: TrustedContact) => {
    if (!window.confirm(t('people.confirmRemove'))) return;
    try {
      await apiService.deleteTrustedContact(contact.id);
      setMessage({ type: 'success', text: t('people.successDelete') });
      await loadContacts();
    } catch (err) {
      setMessage({ type: 'error', text: t('common.error') });
    }
  };

  return (
    <div className="people-container">
      <header className="people-header">
        <h1>{t('people.trustedCircle')}</h1>
        <div className="people-header-actions">
          <input
            type="text"
            className="people-search"
            placeholder={t('people.searchPlaceholder')}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <button type="button" className="people-add-button" onClick={handleOpenCreate}>
            {t('people.addPerson')}
          </button>
        </div>
      </header>

      {message && (
        <div className={`people-message ${message.type}`}>{message.text}</div>
      )}
      {error && <div className="people-error">{error}</div>}

      {loading ? (
        <div className="people-loading">{t('common.loading')}</div>
      ) : (
        <div className="people-table-wrap">
          <table className="people-table">
            <thead>
              <tr>
                <th>{t('people.name')}</th>
                <th>{t('people.email')}</th>
                <th>{t('people.tags')}</th>
                <th>{t('people.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {contacts.length === 0 ? (
                <tr>
                  <td colSpan={TABLE_COLUMN_COUNT} className="people-empty">{t('common.noData')}</td>
                </tr>
              ) : (
                contacts.map((c) => (
                  <tr key={c.id}>
                    <td>{c.name}</td>
                    <td>{c.email}</td>
                    <td>{c.tags ?? '—'}</td>
                    <td>
                      <button type="button" className="people-btn-edit" onClick={() => handleOpenEdit(c)}>
                        {t('people.edit')}
                      </button>
                      <button type="button" className="people-btn-remove" onClick={() => handleDelete(c)}>
                        {t('people.remove')}
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {showModal && (
        <div className="people-modal-overlay" onClick={handleCloseModal}>
          <div className="people-modal" onClick={(e) => e.stopPropagation()}>
            <h2>{editing ? t('people.editTitle') : t('people.createTitle')}</h2>
            <form onSubmit={handleSubmit}>
              <div className="people-form-group">
                <label htmlFor="tc-name">{t('people.name')}</label>
                <input
                  id="tc-name"
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData((p) => ({ ...p, name: e.target.value }))}
                  required
                />
              </div>
              <div className="people-form-group">
                <label htmlFor="tc-email">{t('people.email')}</label>
                <input
                  id="tc-email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData((p) => ({ ...p, email: e.target.value }))}
                  required
                />
              </div>
              <div className="people-form-group">
                <label htmlFor="tc-tags">{t('people.tags')}</label>
                <input
                  id="tc-tags"
                  type="text"
                  value={formData.tags ?? ''}
                  onChange={(e) => setFormData((p) => ({ ...p, tags: e.target.value }))}
                />
              </div>
              <div className="people-form-group">
                <label htmlFor="tc-note">{t('people.note')}</label>
                <input
                  id="tc-note"
                  type="text"
                  value={formData.note ?? ''}
                  onChange={(e) => setFormData((p) => ({ ...p, note: e.target.value }))}
                />
              </div>
              <div className="people-form-actions">
                <button type="submit" disabled={saving}>{saving ? t('common.loading') : t('common.save')}</button>
                <button type="button" onClick={handleCloseModal}>{t('common.cancel')}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default People;
