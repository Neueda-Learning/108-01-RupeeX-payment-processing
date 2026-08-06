/**
 * Guardrail helpers that scope bot operations to the current user's own
 * account. This is UX-level enforcement based on the client-supplied
 * `BotUser` (there is no real backend authentication in this app yet) — it
 * stops the bot from casually leaking or acting on other members' data, but
 * a malicious client could still lie about its own identity. Admins are
 * unrestricted; members are locked to their own account for every op.
 */

export type BotUser = {
  customerId?: string;
  name?: string;
  accountNumber?: string;
  role?: "admin" | "member" | string;
};

export class AccessDeniedError extends Error {
  constructor(message = "You can only access your own account.") {
    super(message);
    this.name = "AccessDeniedError";
  }
}

export function isAdmin(user?: BotUser | null): boolean {
  return user?.role === "admin";
}

/** Throws AccessDeniedError if a member references an account that isn't their own. */
export function assertOwnAccount(user: BotUser | null | undefined, accountNumber?: string | null): void {
  if (isAdmin(user)) return;
  if (!user?.accountNumber) {
    throw new AccessDeniedError("No account is associated with the current user.");
  }
  if (!accountNumber) return;
  if (accountNumber.toUpperCase() !== user.accountNumber.toUpperCase()) {
    throw new AccessDeniedError("You can only access your own account.");
  }
}

/** Returns all accounts for an admin, or only the member's own account. */
export function filterAccountsForUser<T extends { accountNumber: string }>(
  user: BotUser | null | undefined,
  accounts: T[]
): T[] {
  if (isAdmin(user)) return accounts;
  if (!user?.accountNumber) return [];
  return accounts.filter((a) => a.accountNumber?.toUpperCase() === user.accountNumber!.toUpperCase());
}

/** Throws AccessDeniedError if a member's own account isn't one of the payment's parties. */
export function assertOwnsPayment(
  user: BotUser | null | undefined,
  payment: { sourceAccount?: string; destinationAccount?: string } | null | undefined
): void {
  if (isAdmin(user)) return;
  if (!user?.accountNumber) {
    throw new AccessDeniedError("No account is associated with the current user.");
  }
  const own = user.accountNumber.toUpperCase();
  const source = payment?.sourceAccount?.toUpperCase();
  const destination = payment?.destinationAccount?.toUpperCase();
  if (source !== own && destination !== own) {
    throw new AccessDeniedError("You can only view or manage payments involving your own account.");
  }
}
