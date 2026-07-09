"use client";

import Link from "next/link";
import { useAuth } from "@/lib/auth";

export default function Navbar() {
  const { user, signOut } = useAuth();
  const initial = user?.name?.trim()?.charAt(0)?.toUpperCase() ?? "?";

  return (
    <header className="navbar">
      <Link href="/" className="brand">
        <span className="brand-mark">B</span> BookMyEvent
      </Link>

      <nav className="nav-links">
        <Link href="/" className="navlink">
          Events
        </Link>
        {user ? (
          <>
            <Link href="/bookings" className="navlink">
              My Bookings
            </Link>
            <span className="nav-user">
              <span className="avatar">{initial}</span>
              <span className="nav-name">{user.name.split(" ")[0]}</span>
            </span>
            <button className="btn btn-ghost" onClick={signOut}>
              Sign out
            </button>
          </>
        ) : (
          <>
            <Link href="/login" className="navlink">
              Login
            </Link>
            <Link href="/register" className="btn btn-primary">
              Sign up
            </Link>
          </>
        )}
      </nav>
    </header>
  );
}
