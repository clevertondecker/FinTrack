import React from 'react';
import { Bell, UserCircle, Settings } from 'lucide-react';
import LanguageSelector from '../LanguageSelector';
import { useAuth } from '../../contexts/AuthContext';

interface HeaderProps {
  title: string;
  userName?: string;
}

export default function Header({ title, userName }: HeaderProps) {
  const { logout } = useAuth();

  return (
    <header className="sticky top-0 left-60 right-0 h-16 bg-white border-b border-gray-200 flex items-center px-8 z-10 shadow-sm">
      <div className="flex-1 flex items-center gap-4">
        <span className="text-xl font-bold text-primary tracking-tight">{title}</span>
      </div>
      <div className="flex items-center gap-4">
        <button className="relative p-2 rounded-full hover:bg-gray-100 transition-colors">
          <Bell size={22} />
        </button>
        <span className="font-medium text-textSecondary">{userName}</span>
        <LanguageSelector />
        <button className="p-2 rounded-full hover:bg-gray-100 transition-colors">
          <UserCircle size={28} />
        </button>
        <button 
          onClick={logout}
          className="p-2 rounded-full hover:bg-gray-100 transition-colors"
        >
          <Settings size={22} />
        </button>
      </div>
    </header>
  );
} 