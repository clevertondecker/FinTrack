import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { LogOut, KeyRound, Shield, Calendar, X, Check, ChevronDown, Tag } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import apiService from '../../services/api';

const PASSWORD_MIN_LENGTH = 6;
const SUCCESS_DISPLAY_MS = 2000;
const INPUT_CLASS =
  'w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none';

function PasswordForm({
  onSubmit,
  onCancel,
  isSubmitting,
  error,
  success,
}: {
  onSubmit: (currentPassword: string, newPassword: string) => void;
  onCancel: () => void;
  isSubmitting: boolean;
  error: string;
  success: string;
}) {
  const { t } = useTranslation();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [localError, setLocalError] = useState('');

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      setLocalError('');

      if (newPassword.length < PASSWORD_MIN_LENGTH) {
        setLocalError(t('profile.passwordMinLength'));
        return;
      }
      if (newPassword !== confirmPassword) {
        setLocalError(t('profile.passwordMismatch'));
        return;
      }
      onSubmit(currentPassword, newPassword);
    },
    [currentPassword, newPassword, confirmPassword, t, onSubmit]
  );

  const displayError = localError || error;

  return (
    <form onSubmit={handleSubmit} className="px-3 py-2 space-y-3">
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-semibold text-gray-700">
          {t('profile.changePassword')}
        </h4>
        <button
          type="button"
          onClick={onCancel}
          className="p-1 rounded hover:bg-gray-100 text-gray-400"
        >
          <X size={14} />
        </button>
      </div>

      <input
        type="password"
        placeholder={t('profile.currentPassword')}
        value={currentPassword}
        onChange={(e) => setCurrentPassword(e.target.value)}
        className={INPUT_CLASS}
        required
      />
      <input
        type="password"
        placeholder={t('profile.newPassword')}
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        className={INPUT_CLASS}
        required
        minLength={PASSWORD_MIN_LENGTH}
      />
      <input
        type="password"
        placeholder={t('profile.confirmNewPassword')}
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        className={INPUT_CLASS}
        required
        minLength={PASSWORD_MIN_LENGTH}
      />

      {displayError && (
        <p className="text-xs text-red-500 flex items-center gap-1">
          <X size={12} />
          {displayError}
        </p>
      )}
      {success && (
        <p className="text-xs text-green-600 flex items-center gap-1">
          <Check size={12} />
          {success}
        </p>
      )}

      <button
        type="submit"
        disabled={isSubmitting}
        className="w-full py-2 text-sm font-medium text-white bg-primary rounded-lg hover:bg-primary/90 disabled:opacity-50 transition-colors"
      >
        {isSubmitting ? t('common.loading') : t('profile.changePassword')}
      </button>
    </form>
  );
}

function getInitials(name?: string): string {
  if (!name) return '?';
  return name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

function formatMemberDate(dateStr?: string): string {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

export default function UserProfileDropdown() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const isLocalUser = user?.provider === 'LOCAL';

  const initials = useMemo(() => getInitials(user?.name), [user?.name]);
  const providerLabel = useMemo(
    () => (isLocalUser ? t('profile.providerLocal') : t('profile.providerGoogle')),
    [isLocalUser, t]
  );
  const memberSince = useMemo(() => formatMemberDate(user?.createdAt), [user?.createdAt]);

  const closeDropdown = useCallback(() => {
    setIsOpen(false);
    setShowPasswordForm(false);
    setPasswordError('');
    setPasswordSuccess('');
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        closeDropdown();
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [closeDropdown]);

  const handleToggle = useCallback(() => {
    setIsOpen((prev) => {
      if (prev) {
        setShowPasswordForm(false);
        setPasswordError('');
        setPasswordSuccess('');
      }
      return !prev;
    });
  }, []);

  const handlePasswordSubmit = useCallback(
    async (currentPassword: string, newPassword: string) => {
      setPasswordError('');
      setPasswordSuccess('');
      setIsSubmitting(true);
      try {
        await apiService.changePassword(currentPassword, newPassword);
        setPasswordSuccess(t('profile.passwordChanged'));
        setTimeout(() => closeDropdown(), SUCCESS_DISPLAY_MS);
      } catch {
        setPasswordError(t('profile.passwordChangeError'));
      } finally {
        setIsSubmitting(false);
      }
    },
    [t, closeDropdown]
  );

  const handleLogout = useCallback(() => {
    closeDropdown();
    logout();
  }, [logout, closeDropdown]);

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={handleToggle}
        className="flex items-center gap-2 p-1.5 rounded-full hover:bg-gray-100 transition-colors"
        aria-label={t('profile.title')}
      >
        <div className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center text-xs font-semibold">
          {initials}
        </div>
        <ChevronDown
          size={14}
          className={`text-gray-500 hidden sm:block transition-transform ${isOpen ? 'rotate-180' : ''}`}
        />
      </button>

      {isOpen && (
        <div className="absolute right-0 top-12 w-80 bg-white rounded-xl shadow-xl border border-gray-200 z-50 overflow-hidden">
          {/* Profile header */}
          <div className="px-5 py-4 bg-gradient-to-r from-primary to-blue-600">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-full bg-white/20 backdrop-blur text-white flex items-center justify-center text-lg font-bold border-2 border-white/30">
                {initials}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-white font-semibold truncate">{user?.name}</p>
                <p className="text-blue-100 text-sm truncate">{user?.email}</p>
              </div>
            </div>
          </div>

          {/* User details */}
          <div className="px-5 py-3 border-b border-gray-100">
            <div className="flex items-center gap-2 text-sm text-gray-600 py-1">
              <Shield size={14} className="text-gray-400 flex-shrink-0" />
              <span className="text-gray-500">{t('profile.provider')}:</span>
              <span className="font-medium text-gray-700">{providerLabel}</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-gray-600 py-1">
              <Calendar size={14} className="text-gray-400 flex-shrink-0" />
              <span className="text-gray-500">{t('profile.memberSince')}:</span>
              <span className="font-medium text-gray-700">{memberSince}</span>
            </div>
          </div>

          {/* Actions */}
          <div className="px-2 py-2">
            {isLocalUser && !showPasswordForm && (
              <button
                onClick={() => setShowPasswordForm(true)}
                className="w-full flex items-center gap-3 px-3 py-2.5 text-sm text-gray-700 hover:bg-gray-50 rounded-lg transition-colors"
              >
                <KeyRound size={16} className="text-gray-400" />
                {t('profile.changePassword')}
              </button>
            )}

            {!isLocalUser && (
              <div className="flex items-center gap-3 px-3 py-2.5 text-sm text-gray-400">
                <KeyRound size={16} />
                <span className="italic">{t('profile.oauthCannotChange')}</span>
              </div>
            )}

            {showPasswordForm && (
              <PasswordForm
                onSubmit={handlePasswordSubmit}
                onCancel={() => setShowPasswordForm(false)}
                isSubmitting={isSubmitting}
                error={passwordError}
                success={passwordSuccess}
              />
            )}

            <button
              onClick={() => {
                closeDropdown();
                navigate('/dashboard/categories');
              }}
              className="w-full flex items-center gap-3 px-3 py-2.5 text-sm text-gray-700 hover:bg-gray-50 rounded-lg transition-colors"
            >
              <Tag size={16} className="text-gray-400" />
              {t('categories.manage')}
            </button>
          </div>

          {/* Logout */}
          <div className="px-2 py-2 border-t border-gray-100">
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-3 px-3 py-2.5 text-sm text-red-600 hover:bg-red-50 rounded-lg transition-colors"
            >
              <LogOut size={16} />
              {t('profile.logout')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
