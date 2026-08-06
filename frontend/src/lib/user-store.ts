import { create } from "zustand";
import { persist } from "zustand/middleware";

export type UserRole = "admin" | "member";

export interface AppUser {
  customerId: string;
  name: string;
  email: string;
  phone: string;
  accountNumber: string;
  role: UserRole;
}

interface UserStore {
  currentUser: AppUser | null;
  users: AppUser[];
  // Bumped whenever a new backend account is created (see addUser). Pages
  // that fetch the accounts list once via `getAccounts()` include this in
  // their effect's dependency array so a newly added account/user shows up
  // immediately elsewhere without requiring a manual page refresh.
  accountsVersion: number;
  setCurrentUser: (user: AppUser | null) => void;
  addUser: (user: AppUser) => void;
  /** Merges incoming users into the store without changing currentUser. */
  mergeUsers: (incoming: AppUser[]) => void;
}

export const useUserStore = create<UserStore>()(
  persist(
    (set) => ({
      currentUser: null,
      users: [],
      accountsVersion: 0,
      setCurrentUser: (user) => set({ currentUser: user }),
      addUser: (user) =>
        set((state) => ({
          users: [...state.users.filter((u) => u.customerId !== user.customerId), user],
          currentUser: user,
          accountsVersion: state.accountsVersion + 1,
        })),
      mergeUsers: (incoming) =>
        set((state) => {
          const map = new Map(state.users.map((u) => [u.customerId, u]));
          for (const u of incoming) map.set(u.customerId, u);
          return { users: Array.from(map.values()) };
        }),
    }),
    { name: "rupeex-users" }
  )
);

