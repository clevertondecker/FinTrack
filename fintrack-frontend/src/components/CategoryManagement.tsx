import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Plus, Pencil, Trash2, Merge, GripVertical, Save, X,
  AlertTriangle, Check, Loader2, ArrowLeft, Info
} from 'lucide-react';
import apiService from '../services/api';
import { Category, CategoryUsageResponse } from '../types/invoice';
import './CategoryManagement.css';

const PRESET_COLORS = [
  '#FF6B6B', '#FF8E53', '#FFC853', '#51CF66', '#20C997',
  '#22B8CF', '#339AF0', '#5C7CFA', '#845EF7', '#CC5DE8',
  '#F06595', '#868E96', '#495057', '#E64980', '#D6336C',
];

interface EditingCategory {
  id: number | null;
  name: string;
  color: string;
  icon: string;
}

interface MergeState {
  sourceId: number | null;
  targetId: number | null;
}

export default function CategoryManagement() {
  const { t } = useTranslation();
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<EditingCategory | null>(null);
  const [mergeState, setMergeState] = useState<MergeState | null>(null);
  const [usageInfo, setUsageInfo] = useState<CategoryUsageResponse | null>(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const nameInputRef = useRef<HTMLInputElement>(null);
  const dragItem = useRef<number | null>(null);
  const dragOverItem = useRef<number | null>(null);

  const loadCategories = useCallback(async () => {
    try {
      const response = await apiService.getCategories();
      setCategories(response.categories);
    } catch {
      setError(t('categories.loadError'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  useEffect(() => {
    if (editing && nameInputRef.current) {
      nameInputRef.current.focus();
    }
  }, [editing]);

  const clearMessages = useCallback(() => {
    setError('');
    setSuccess('');
  }, []);

  const showSuccess = useCallback((msg: string) => {
    setSuccess(msg);
    setTimeout(() => setSuccess(''), 3000);
  }, []);

  const startAdd = useCallback(() => {
    clearMessages();
    setEditing({ id: null, name: '', color: PRESET_COLORS[0], icon: '' });
    setMergeState(null);
    setDeleteConfirm(null);
  }, [clearMessages]);

  const startEdit = useCallback((cat: Category) => {
    clearMessages();
    setEditing({
      id: cat.id,
      name: cat.name,
      color: cat.color || '#868E96',
      icon: cat.icon || '',
    });
    setMergeState(null);
    setDeleteConfirm(null);
  }, [clearMessages]);

  const cancelEdit = useCallback(() => {
    setEditing(null);
    clearMessages();
  }, [clearMessages]);

  const handleSave = useCallback(async () => {
    if (!editing) return;
    if (!editing.name.trim()) {
      setError(t('categories.nameRequired'));
      return;
    }
    setSaving(true);
    clearMessages();
    try {
      if (editing.id) {
        await apiService.updateCategory(editing.id, editing.name.trim(), editing.color, editing.icon || undefined);
        showSuccess(t('categories.updated'));
      } else {
        await apiService.createCategory(editing.name.trim(), editing.color, editing.icon || undefined);
        showSuccess(t('categories.created'));
      }
      setEditing(null);
      await loadCategories();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : t('categories.saveError');
      if (typeof err === 'object' && err !== null && 'response' in err) {
        const axiosErr = err as { response?: { data?: { message?: string; error?: string } } };
        setError(axiosErr.response?.data?.message || axiosErr.response?.data?.error || msg);
      } else {
        setError(msg);
      }
    } finally {
      setSaving(false);
    }
  }, [editing, t, clearMessages, showSuccess, loadCategories]);

  const handleDelete = useCallback(async (id: number) => {
    setSaving(true);
    clearMessages();
    try {
      await apiService.deleteCategory(id);
      showSuccess(t('categories.deleted'));
      setDeleteConfirm(null);
      await loadCategories();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string; error?: string } } };
      const msg = axiosErr?.response?.data?.message || axiosErr?.response?.data?.error || t('categories.deleteError');
      setError(msg);
      setDeleteConfirm(null);
    } finally {
      setSaving(false);
    }
  }, [t, clearMessages, showSuccess, loadCategories]);

  const confirmDelete = useCallback(async (id: number) => {
    clearMessages();
    setEditing(null);
    setMergeState(null);
    try {
      const usage = await apiService.getCategoryUsage(id);
      setUsageInfo(usage);
      setDeleteConfirm(id);
    } catch {
      setDeleteConfirm(id);
      setUsageInfo(null);
    }
  }, [clearMessages]);

  const startMerge = useCallback((sourceId: number) => {
    clearMessages();
    setEditing(null);
    setDeleteConfirm(null);
    setMergeState({ sourceId, targetId: null });
  }, [clearMessages]);

  const handleMerge = useCallback(async () => {
    if (!mergeState?.sourceId || !mergeState?.targetId) return;
    setSaving(true);
    clearMessages();
    try {
      await apiService.mergeCategories(mergeState.sourceId, mergeState.targetId);
      showSuccess(t('categories.merged'));
      setMergeState(null);
      await loadCategories();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string; error?: string } } };
      setError(axiosErr?.response?.data?.message || axiosErr?.response?.data?.error || t('categories.mergeError'));
    } finally {
      setSaving(false);
    }
  }, [mergeState, t, clearMessages, showSuccess, loadCategories]);

  const handleDragStart = useCallback((idx: number) => {
    dragItem.current = idx;
  }, []);

  const handleDragEnter = useCallback((idx: number) => {
    dragOverItem.current = idx;
  }, []);

  const handleDragEnd = useCallback(async () => {
    if (dragItem.current === null || dragOverItem.current === null) return;
    if (dragItem.current === dragOverItem.current) return;

    const items = [...categories];
    const dragged = items.splice(dragItem.current, 1)[0];
    items.splice(dragOverItem.current, 0, dragged);

    setCategories(items);
    dragItem.current = null;
    dragOverItem.current = null;

    try {
      await apiService.reorderCategories(items.map(c => c.id));
    } catch {
      setError(t('categories.reorderError'));
      await loadCategories();
    }
  }, [categories, t, loadCategories]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSave();
    if (e.key === 'Escape') cancelEdit();
  }, [handleSave, cancelEdit]);

  if (loading) {
    return (
      <div className="category-management">
        <div className="category-loading">
          <Loader2 size={24} className="spin" />
          <span>{t('common.loading')}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="category-management">
      <div className="category-header">
        <div className="category-header-info">
          <h2>{t('categories.title')}</h2>
          <span className="category-count">{categories.length} {t('categories.items')}</span>
        </div>
        <button className="category-add-btn" onClick={startAdd} disabled={!!editing}>
          <Plus size={16} />
          {t('categories.add')}
        </button>
      </div>

      {error && (
        <div className="category-alert category-alert-error">
          <AlertTriangle size={16} />
          <span>{error}</span>
          <button onClick={() => setError('')}><X size={14} /></button>
        </div>
      )}
      {success && (
        <div className="category-alert category-alert-success">
          <Check size={16} />
          <span>{success}</span>
        </div>
      )}

      {editing && !editing.id && (
        <div className="category-edit-card">
          <h3>{t('categories.newCategory')}</h3>
          <div className="category-edit-form">
            <div className="category-edit-row">
              <input
                ref={nameInputRef}
                type="text"
                value={editing.name}
                onChange={e => setEditing({ ...editing, name: e.target.value })}
                onKeyDown={handleKeyDown}
                placeholder={t('categories.namePlaceholder')}
                className="category-name-input"
                maxLength={100}
              />
            </div>
            <div className="category-color-picker">
              <label>{t('categories.color')}</label>
              <div className="color-swatches">
                {PRESET_COLORS.map(c => (
                  <button
                    key={c}
                    className={`color-swatch ${editing.color === c ? 'active' : ''}`}
                    style={{ backgroundColor: c }}
                    onClick={() => setEditing({ ...editing, color: c })}
                  />
                ))}
                <input
                  type="color"
                  value={editing.color}
                  onChange={e => setEditing({ ...editing, color: e.target.value })}
                  className="color-custom"
                  title={t('categories.customColor')}
                />
              </div>
            </div>
            <div className="category-edit-actions">
              <button className="btn-save" onClick={handleSave} disabled={saving}>
                {saving ? <Loader2 size={14} className="spin" /> : <Save size={14} />}
                {t('common.save')}
              </button>
              <button className="btn-cancel" onClick={cancelEdit}>
                <X size={14} /> {t('common.cancel')}
              </button>
            </div>
          </div>
        </div>
      )}

      {mergeState && (
        <div className="category-merge-bar">
          <Info size={16} />
          <span>{t('categories.mergeHint')}</span>
          <button className="btn-cancel" onClick={() => setMergeState(null)}>
            <X size={14} /> {t('common.cancel')}
          </button>
        </div>
      )}

      <div className="category-list">
        {categories.map((cat, index) => {
          const isEditing = editing?.id === cat.id;
          const isMergeSource = mergeState?.sourceId === cat.id;
          const isMergeTarget = mergeState && !isMergeSource;

          return (
            <div
              key={cat.id}
              className={`category-item ${isMergeSource ? 'merge-source' : ''} ${isMergeTarget ? 'merge-target' : ''}`}
              draggable={!editing && !mergeState}
              onDragStart={() => handleDragStart(index)}
              onDragEnter={() => handleDragEnter(index)}
              onDragEnd={handleDragEnd}
              onDragOver={e => e.preventDefault()}
            >
              {!editing && !mergeState && (
                <div className="drag-handle">
                  <GripVertical size={16} />
                </div>
              )}

              <div className="category-color-dot" style={{ backgroundColor: cat.color || '#868E96' }} />

              {isEditing && editing ? (
                <div className="category-inline-edit">
                  <input
                    ref={nameInputRef}
                    type="text"
                    value={editing.name}
                    onChange={e => setEditing({ ...editing, name: e.target.value })}
                    onKeyDown={handleKeyDown}
                    className="category-name-input"
                    maxLength={100}
                  />
                  <div className="color-swatches-inline">
                    {PRESET_COLORS.map(c => (
                      <button
                        key={c}
                        className={`color-swatch-sm ${editing.color === c ? 'active' : ''}`}
                        style={{ backgroundColor: c }}
                        onClick={() => setEditing({ ...editing, color: c })}
                      />
                    ))}
                    <input
                      type="color"
                      value={editing.color}
                      onChange={e => setEditing({ ...editing, color: e.target.value })}
                      className="color-custom-sm"
                    />
                  </div>
                  <div className="inline-edit-actions">
                    <button className="btn-save-sm" onClick={handleSave} disabled={saving}>
                      {saving ? <Loader2 size={14} className="spin" /> : <Check size={14} />}
                    </button>
                    <button className="btn-cancel-sm" onClick={cancelEdit}>
                      <X size={14} />
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <span className="category-name">{cat.name}</span>
                  {isMergeTarget ? (
                    <button
                      className="btn-merge-into"
                      onClick={() => setMergeState({ ...mergeState!, targetId: cat.id })}
                    >
                      <Merge size={14} />
                      {t('categories.mergeInto')}
                    </button>
                  ) : !mergeState && (
                    <div className="category-actions">
                      <button className="action-btn" onClick={() => startEdit(cat)} title={t('common.edit')}>
                        <Pencil size={14} />
                      </button>
                      <button className="action-btn" onClick={() => startMerge(cat.id)} title={t('categories.merge')}>
                        <Merge size={14} />
                      </button>
                      <button
                        className="action-btn action-btn-danger"
                        onClick={() => confirmDelete(cat.id)}
                        title={t('common.delete')}
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          );
        })}

        {categories.length === 0 && (
          <div className="category-empty">
            <p>{t('categories.empty')}</p>
          </div>
        )}
      </div>

      {deleteConfirm !== null && (
        <div className="category-modal-overlay" onClick={() => setDeleteConfirm(null)}>
          <div className="category-modal" onClick={e => e.stopPropagation()}>
            <h3>
              <AlertTriangle size={18} className="text-warning" />
              {t('categories.confirmDelete')}
            </h3>
            <p>{t('categories.confirmDeleteMsg', {
              name: categories.find(c => c.id === deleteConfirm)?.name
            })}</p>
            {usageInfo && usageInfo.totalUsage > 0 && (
              <div className="usage-warning">
                <p>{t('categories.usageWarning')}</p>
                <ul>
                  {usageInfo.itemCount > 0 && (
                    <li>{usageInfo.itemCount} {t('categories.usageItems')}</li>
                  )}
                  {usageInfo.ruleCount > 0 && (
                    <li>{usageInfo.ruleCount} {t('categories.usageRules')}</li>
                  )}
                  {usageInfo.budgetCount > 0 && (
                    <li>{usageInfo.budgetCount} {t('categories.usageBudgets')}</li>
                  )}
                  {usageInfo.subscriptionCount > 0 && (
                    <li>{usageInfo.subscriptionCount} {t('categories.usageSubscriptions')}</li>
                  )}
                </ul>
                <p className="merge-suggestion">{t('categories.mergeSuggestion')}</p>
              </div>
            )}
            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => setDeleteConfirm(null)}>
                {t('common.cancel')}
              </button>
              <button
                className="btn-delete"
                onClick={() => handleDelete(deleteConfirm)}
                disabled={saving || (usageInfo !== null && usageInfo.totalUsage > 0)}
              >
                {saving ? <Loader2 size={14} className="spin" /> : <Trash2 size={14} />}
                {t('common.delete')}
              </button>
            </div>
          </div>
        </div>
      )}

      {mergeState?.targetId && (
        <div className="category-modal-overlay" onClick={() => setMergeState(null)}>
          <div className="category-modal" onClick={e => e.stopPropagation()}>
            <h3>
              <Merge size={18} />
              {t('categories.confirmMerge')}
            </h3>
            <p>{t('categories.confirmMergeMsg', {
              source: categories.find(c => c.id === mergeState.sourceId)?.name,
              target: categories.find(c => c.id === mergeState.targetId)?.name,
            })}</p>
            <div className="merge-preview">
              <div className="merge-arrow">
                <span className="merge-from" style={{
                  borderColor: categories.find(c => c.id === mergeState.sourceId)?.color || '#868E96'
                }}>
                  {categories.find(c => c.id === mergeState.sourceId)?.name}
                </span>
                <ArrowLeft size={20} className="arrow-icon" />
                <span className="merge-to" style={{
                  borderColor: categories.find(c => c.id === mergeState.targetId)?.color || '#868E96'
                }}>
                  {categories.find(c => c.id === mergeState.targetId)?.name}
                </span>
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => setMergeState(null)}>
                {t('common.cancel')}
              </button>
              <button className="btn-merge" onClick={handleMerge} disabled={saving}>
                {saving ? <Loader2 size={14} className="spin" /> : <Merge size={14} />}
                {t('categories.mergeConfirm')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
