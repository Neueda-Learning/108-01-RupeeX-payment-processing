"use client";

import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { UserCircle, UserPlus, ChevronDown, Check } from "lucide-react";
import { useState } from "react";
import { useUserStore } from "@/lib/user-store";
import type { AppUser } from "@/lib/user-store";
import { AddUserModal } from "./add-user-modal";

export function UserProfile() {
  const { currentUser, users, setCurrentUser } = useUserStore();
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <>
      <DropdownMenu.Root>
        <DropdownMenu.Trigger asChild>
          <button className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 shadow-sm hover:border-orange-300 hover:text-orange-700 focus:outline-none">
            <UserCircle className="h-4 w-4" />
            <span className="max-w-[100px] truncate">
              {currentUser ? currentUser.name.split(" ")[0] : "No user"}
            </span>
            {currentUser && (
              <span className={`rounded-full px-1.5 py-0.5 text-[10px] font-semibold uppercase ${
                currentUser.role === "admin"
                  ? "bg-orange-100 text-orange-700"
                  : "bg-slate-100 text-slate-600"
              }`}>
                {currentUser.role}
              </span>
            )}
            <ChevronDown className="h-3.5 w-3.5 text-slate-400" />
          </button>
        </DropdownMenu.Trigger>

        <DropdownMenu.Portal>
          <DropdownMenu.Content
            align="end"
            sideOffset={8}
            className="z-50 min-w-[200px] rounded-xl border border-black/5 bg-white p-1.5 shadow-lg"
          >
            {users.length > 0 && (
              <>
                <p className="px-2 py-1 text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                  Switch User
                </p>
                {users.map((user: AppUser) => (
                  <DropdownMenu.Item
                    key={user.customerId}
                    onSelect={() => setCurrentUser(user)}
                    className="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-2 text-sm text-slate-700 outline-none hover:bg-slate-50"
                  >
                    <UserCircle className="h-4 w-4 text-slate-400" />
                    <div className="flex-1 overflow-hidden">
                      <p className="truncate font-medium">{user.name}</p>
                      <p className="truncate text-xs text-slate-400">{user.accountNumber}</p>
                    </div>
                    <span className={`rounded-full px-1.5 py-0.5 text-[10px] font-semibold uppercase ${
                      user.role === "admin" ? "bg-orange-100 text-orange-700" : "bg-slate-100 text-slate-600"
                    }`}>
                      {user.role}
                    </span>
                    {currentUser?.customerId === user.customerId && (
                      <Check className="h-3.5 w-3.5 text-orange-500" />
                    )}
                  </DropdownMenu.Item>
                ))}
                <DropdownMenu.Separator className="my-1 h-px bg-slate-100" />
              </>
            )}

            <DropdownMenu.Item
              onSelect={() => setModalOpen(true)}
              className="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-2 text-sm font-medium text-orange-600 outline-none hover:bg-orange-50"
            >
              <UserPlus className="h-4 w-4" />
              Add New User
            </DropdownMenu.Item>
          </DropdownMenu.Content>
        </DropdownMenu.Portal>
      </DropdownMenu.Root>

      <AddUserModal open={modalOpen} onClose={() => setModalOpen(false)} />
    </>
  );
}

