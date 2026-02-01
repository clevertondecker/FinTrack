import React, { useState } from 'react';
import Sidebar from './Sidebar';
import Header from './Header';

interface LayoutProps {
  title: string;
  userName?: string;
  children: React.ReactNode;
}

export default function Layout({ title, userName, children }: LayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const toggleSidebar = () => setSidebarOpen(!sidebarOpen);
  const closeSidebar = () => setSidebarOpen(false);

  return (
    <div className="min-h-screen bg-background flex">
      <Sidebar isOpen={sidebarOpen} onClose={closeSidebar} />
      <div className="flex-1 ml-0 md:ml-60 flex flex-col">
        <Header title={title} userName={userName} onMenuClick={toggleSidebar} />
        <main className="flex-1 p-4 md:p-8">{children}</main>
      </div>
    </div>
  );
} 