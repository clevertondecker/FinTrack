import React from 'react';
import Sidebar from './Sidebar';
import Header from './Header';

interface LayoutProps {
  title: string;
  userName?: string;
  children: React.ReactNode;
}

export default function Layout({ title, userName, children }: LayoutProps) {
  return (
    <div className="min-h-screen bg-background flex">
      <Sidebar />
      <div className="flex-1 ml-60 flex flex-col">
        <Header title={title} userName={userName} />
        <main className="flex-1 p-8">{children}</main>
      </div>
    </div>
  );
} 