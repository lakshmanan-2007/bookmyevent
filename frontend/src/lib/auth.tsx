"use client";

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { AuthUser } from "./types";

interface AuthContextValue {
  user: AuthUser | null;
  ready: boolean;
  signIn: (user: AuthUser) => void;
  signOut: () => void;
}

const STORAGE_KEY = "bookmyevent.auth";
const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [ready, setReady] = useState(false);

  // Restore session on first client render.
  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) setUser(JSON.parse(raw) as AuthUser);
    } catch {
      /* ignore malformed storage */
    }
    setReady(true);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      ready,
      signIn: (u) => {
        setUser(u);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(u));
      },
      signOut: () => {
        setUser(null);
        localStorage.removeItem(STORAGE_KEY);
      },
    }),
    [user, ready]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
