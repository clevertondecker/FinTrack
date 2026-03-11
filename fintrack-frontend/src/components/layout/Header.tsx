import React from 'react';
import { Menu } from 'lucide-react';
import LanguageSelector from '../LanguageSelector';
import UserProfileDropdown from './UserProfileDropdown';
import NotificationPanel from './NotificationPanel';

interface HeaderProps {
  title: string;
  userName?: string;
  onMenuClick?: () => void;
}

export default function Header({ title, userName, onMenuClick }: HeaderProps) {
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
        <NotificationPanel />
        <span className="font-medium text-textSecondary hidden sm:inline">{userName}</span>
        <LanguageSelector />
        <UserProfileDropdown />
      </div>
    </header>
  );
}
