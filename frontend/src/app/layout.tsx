import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthProvider } from "@/lib/auth";
import Navbar from "@/components/Navbar";
import "./globals.css";

export const metadata: Metadata = {
  title: "BookMyEvent",
  description: "Book tickets for events — Spring Boot + Next.js demo app",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <Navbar />
          <main className="container">{children}</main>
          <footer className="footer">
            Built with Spring Boot &amp; Next.js · BookMyEvent
          </footer>
        </AuthProvider>
      </body>
    </html>
  );
}
