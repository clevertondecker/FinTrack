import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { useAuth } from './AuthContext';

const STORAGE_PREFIX = 'fintrack_help_seen_';

interface HelpContextType {
  isHelpActive: boolean;
  toggleHelp: () => void;
  dismissHelp: () => void;
}

const HelpContext = createContext<HelpContextType | undefined>(undefined);

export function useHelp(): HelpContextType {
  const context = useContext(HelpContext);
  if (!context) {
    throw new Error('useHelp must be used within a HelpProvider');
  }
  return context;
}

function getStorageKey(userId: number | undefined): string {
  return `${STORAGE_PREFIX}${userId ?? 'anonymous'}`;
}

function hasSeenHelp(userId: number | undefined): boolean {
  try {
    return localStorage.getItem(getStorageKey(userId)) === 'true';
  } catch {
    return false;
  }
}

function markHelpAsSeen(userId: number | undefined) {
  try {
    localStorage.setItem(getStorageKey(userId), 'true');
  } catch {
    // localStorage unavailable
  }
}

interface HelpProviderProps {
  children: ReactNode;
}

export const HelpProvider: React.FC<HelpProviderProps> = ({ children }) => {
  const { user } = useAuth();
  const [isHelpActive, setIsHelpActive] = useState(false);

  useEffect(() => {
    if (user?.id && !hasSeenHelp(user.id)) {
      setIsHelpActive(true);
    }
  }, [user?.id]);

  const toggleHelp = useCallback(() => {
    setIsHelpActive((prev) => !prev);
  }, []);

  const dismissHelp = useCallback(() => {
    setIsHelpActive(false);
    markHelpAsSeen(user?.id);
  }, [user?.id]);

  const value: HelpContextType = { isHelpActive, toggleHelp, dismissHelp };

  return <HelpContext.Provider value={value}>{children}</HelpContext.Provider>;
};
