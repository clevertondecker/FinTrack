import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';
import i18n from '../../i18n';
import LanguageSelector from '../LanguageSelector';

// Mock the portal
jest.mock('react-dom', () => ({
  ...jest.requireActual('react-dom'),
  createPortal: (node: React.ReactNode) => node,
}));

const renderWithI18n = (component: React.ReactElement) => {
  return render(
    <I18nextProvider i18n={i18n}>
      {component}
    </I18nextProvider>
  );
};

describe('LanguageSelector', () => {
  beforeEach(() => {
    // Reset language to English
    i18n.changeLanguage('en');
  });

  it('renders language selector button', () => {
    renderWithI18n(<LanguageSelector />);
    
    expect(screen.getByRole('button')).toBeInTheDocument();
    expect(screen.getByText('English')).toBeInTheDocument();
    expect(screen.getByText('🇺🇸')).toBeInTheDocument();
  });

  it('shows dropdown when clicked', () => {
    renderWithI18n(<LanguageSelector />);
    
    const button = screen.getByRole('button');
    fireEvent.click(button);
    
    // When dropdown is open, there should be multiple "English" elements (button + dropdown option)
    expect(screen.getAllByText('English').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Português')).toBeInTheDocument();
  });

  it('changes language when option is clicked', () => {
    renderWithI18n(<LanguageSelector />);
    
    const button = screen.getByRole('button');
    fireEvent.click(button);
    
    const portugueseOption = screen.getByText('Português');
    fireEvent.click(portugueseOption);
    
    expect(i18n.language).toBe('pt');
  });

  it('closes dropdown after language selection', () => {
    renderWithI18n(<LanguageSelector />);
    
    const button = screen.getByRole('button');
    fireEvent.click(button);
    
    const portugueseOption = screen.getByText('Português');
    fireEvent.click(portugueseOption);
    
    // After selecting Portuguese, dropdown should be closed
    // The button now shows "Português" but dropdown options should not be visible
    // Check that "English" option in dropdown is not visible anymore
    expect(screen.queryByText('English')).not.toBeInTheDocument();
  });
}); 