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
  setCurrentUser: (user: AppUser | null) => void;
  addUser: (user: AppUser) => void;
}

export const useUserStore = create<UserStore>()(
  persist(
    (set) => ({
      currentUser: null,
      users: [],
      setCurrentUser: (user) => set({ currentUser: user }),
      addUser: (user) =>
        set((state) => ({
          users: [...state.users.filter((u) => u.customerId !== user.customerId), user],
          currentUser: user,
        })),
    }),
    { name: "rupeex-users" }
  )
);

