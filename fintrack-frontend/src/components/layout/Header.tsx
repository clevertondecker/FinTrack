import React from 'react';
import { Menu, HelpCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import LanguageSelector from '../LanguageSelector';
import UserProfileDropdown from './UserProfileDropdown';
import NotificationPanel from './NotificationPanel';
import { useHelp } from '../../contexts/HelpContext';

interface HeaderProps {
  title: string;
  userName?: string;
  onMenuClick?: () => void;
}

export default function Header({ title, userName, onMenuClick }: HeaderProps) {
  const { t } = useTranslation();
  const { isHelpActive, toggleHelp, dismissHelp } = useHelp();

  const handleHelpClick = () => {
    if (isHelpActive) {
      dismissHelp();
    } else {
      toggleHelp();
    }
  };

  return (
    <header className="sticky top-0 left-0 md:left-60 right-0 h-16 bg-white border-b border-gray-200 flex items-center px-4 md:px-8 z-10 shadow-sm">
      <div className="flex-1 flex items-center gap-3">
        <button
          onClick={onMenuClick}
          className="p-2 rounded-lg hover:bg-gray-100 transition-colors md:hidden"
        >
          <Menu size={24} />
        </button>
        <span className="text-lg md:text-xl font-bold text-primary tracking-tight truncate">{title}</span>
      </div>
      <div className="flex items-center gap-2 md:gap-4">
        <button
          onClick={handleHelpClick}
          className={`p-2 rounded-full transition-colors ${
            isHelpActive
              ? 'bg-blue-100 text-blue-600 hover:bg-blue-200'
              : 'hover:bg-gray-100 text-gray-600'
          }`}
          title={t(isHelpActive ? 'help.deactivate' : 'help.activate')}
          aria-label={t('help.toggle')}
        >
          <HelpCircle size={20} className="md:w-[22px] md:h-[22px]" />
        </button>
        <NotificationPanel />
        <span className="font-medium text-textSecondary hidden sm:inline">{userName}</span>
        <LanguageSelector />
        <UserProfileDropdown />
      </div>
    </header>
  );
}
