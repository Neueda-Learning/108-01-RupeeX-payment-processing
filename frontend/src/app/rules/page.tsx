"use client";

import { useEffect, useState } from "react";
import {
  createFraudRule,
  deleteFraudRule,
  getFraudRules,
  updateFraudRule,
} from "@/lib/api";
import type { FraudRule, FraudRuleInput, FraudRuleType } from "@/lib/types";

type RuleHelp = {
  label: string;
  description: string;
  thresholdHint: string;
};

/** FATF grey/blacklist + OFAC sanctioned countries commonly flagged in AML/CFT */
const HIGH_RISK_COUNTRIES: { code: string; name: string; basis: string }[] = [
  { code: "AF", name: "Afghanistan", basis: "FATF blacklist" },
  { code: "BY", name: "Belarus", basis: "OFAC sanctions" },
  { code: "MM", name: "Myanmar / Burma", basis: "FATF blacklist" },
  { code: "KP", name: "North Korea", basis: "FATF blacklist / OFAC" },
  { code: "IR", name: "Iran", basis: "FATF blacklist / OFAC" },
  { code: "RU", name: "Russia", basis: "OFAC sanctions" },
  { code: "SY", name: "Syria", basis: "OFAC sanctions" },
  { code: "SD", name: "Sudan", basis: "OFAC sanctions" },
  { code: "SS", name: "South Sudan", basis: "FATF grey list" },
  { code: "YE", name: "Yemen", basis: "FATF grey list" },
  { code: "ML", name: "Mali", basis: "FATF grey list" },
  { code: "BF", name: "Burkina Faso", basis: "FATF grey list" },
  { code: "SO", name: "Somalia", basis: "FATF grey list" },
  { code: "HT", name: "Haiti", basis: "FATF grey list" },
  { code: "PK", name: "Pakistan", basis: "FATF grey list" },
  { code: "VE", name: "Venezuela", basis: "OFAC sanctions" },
  { code: "LY", name: "Libya", basis: "FATF grey list" },
  { code: "NI", name: "Nicaragua", basis: "OFAC sanctions" },
  { code: "CF", name: "Central African Republic", basis: "FATF grey list" },
  { code: "CD", name: "DR Congo", basis: "FATF grey list" },
  { code: "MZ", name: "Mozambique", basis: "FATF grey list" },
  { code: "TZ", name: "Tanzania", basis: "FATF grey list" },
  { code: "CM", name: "Cameroon", basis: "FATF grey list" },
  { code: "ZW", name: "Zimbabwe", basis: "FATF grey list" },
  { code: "SN", name: "Senegal", basis: "FATF grey list" },
];

const COUNTRIES_MARKER = "[Countries:";

/** Parse country codes embedded in description by this form */
function parseCountriesFromDescription(desc: string): {
  countries: string[];
  cleanDescription: string;
} {
  const idx = desc.indexOf(COUNTRIES_MARKER);
  if (idx === -1) return { countries: [], cleanDescription: desc.trim() };
  const cleanDescription = desc.slice(0, idx).trim();
  const raw = desc.slice(idx + COUNTRIES_MARKER.length);
  const end = raw.indexOf("]");
  const countries =
    end === -1
      ? []
      : raw
          .slice(0, end)
          .split(",")
          .map((c) => c.trim())
          .filter(Boolean);
  return { countries, cleanDescription };
}

const RULE_TYPES: FraudRuleType[] = [
  "LARGE_TRANSACTION",
  "NIGHT_TRANSACTION",
  "VELOCITY_CHECK",
  "REPEATED_FAILED_ATTEMPTS",
  "BLACKLISTED_ACCOUNT",
  "HIGH_RISK_COUNTRY",
  "NEW_ACCOUNT",
  "SUSPICIOUS_FREQUENCY",
];

const RULE_HELP: Record<FraudRuleType, RuleHelp> = {
  LARGE_TRANSACTION: {
    label: "Large Transaction",
    description: "Flags payments above a high-value amount.",
    thresholdHint: "Amount limit (for example: 100000)",
  },
  NIGHT_TRANSACTION: {
    label: "Night Transaction",
    description: "Flags transactions processed during unusual night hours.",
    thresholdHint: "Hour cutoff or sensitivity value",
  },
  VELOCITY_CHECK: {
    label: "Velocity Check",
    description: "Flags too many transactions in a short time window.",
    thresholdHint: "Maximum allowed count in the window",
  },
  REPEATED_FAILED_ATTEMPTS: {
    label: "Repeated Failures",
    description: "Flags accounts with repeated failed payment attempts.",
    thresholdHint: "Number of failed attempts before flagging",
  },
  BLACKLISTED_ACCOUNT: {
    label: "Blacklisted Account",
    description: "Flags any transfer involving blocked accounts.",
    thresholdHint: "Use 1 as on/off threshold",
  },
  HIGH_RISK_COUNTRY: {
    label: "High-Risk Country",
    description:
      "Flags payments linked to countries on FATF grey/black lists or OFAC sanctions. Select the specific countries this rule should watch.",
    thresholdHint: "Risk level cutoff",
  },
  NEW_ACCOUNT: {
    label: "New Account",
    description: "Flags transactions from newly opened accounts.",
    thresholdHint: "Account age limit in days",
  },
  SUSPICIOUS_FREQUENCY: {
    label: "Suspicious Frequency",
    description: "Flags unusual payment frequency patterns.",
    thresholdHint: "Frequency threshold",
  },
};

const DEFAULT_RULE: FraudRuleInput = {
  name: "",
  description: "",
  ruleType: "LARGE_TRANSACTION",
  threshold: 10000,
  scoreContribution: 10,
  enabled: true,
};

export default function RulesPage() {
  const [rules, setRules] = useState<FraudRule[]>([]);
  const [form, setForm] = useState<FraudRuleInput>(DEFAULT_RULE);
  const [editingRuleId, setEditingRuleId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selectedCountries, setSelectedCountries] = useState<string[]>([]);
  const selectedRuleHelp = RULE_HELP[form.ruleType];

  const loadRules = async () => {
    try {
      setRules(await getFraudRules());
    } catch (loadError) {
      setError(
        loadError instanceof Error ? loadError.message : "Unable to load rules",
      );
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadRules();
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const resetForm = () => {
    setForm(DEFAULT_RULE);
    setEditingRuleId(null);
    setSelectedCountries([]);
  };

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    // For HIGH_RISK_COUNTRY rules, embed selected country codes into description
    const submittedForm: FraudRuleInput = { ...form };
    if (form.ruleType === "HIGH_RISK_COUNTRY") {
      if (selectedCountries.length === 0) {
        setError(
          "Please select at least one country for the High-Risk Country rule.",
        );
        return;
      }
      const clean = parseCountriesFromDescription(
        form.description,
      ).cleanDescription;
      submittedForm.description = `${clean}\n${COUNTRIES_MARKER}${selectedCountries.join(",")}]`;
      submittedForm.threshold = selectedCountries.length;
    }

    try {
      if (editingRuleId) {
        await updateFraudRule(editingRuleId, submittedForm);
      } else {
        await createFraudRule(submittedForm);
      }
      await loadRules();
      resetForm();
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Unable to save rule",
      );
    }
  };

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-10 lg:px-8">
      <header>
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700 dark:text-orange-400">
          Fraud Rules Workspace
        </p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
          Create and manage fraud detection policies
        </h1>
      </header>

      {error && (
        <div className="rounded-lg border border-red-200 dark:border-red-900 bg-red-50 dark:bg-red-950 px-4 py-3 text-sm text-red-700 dark:text-red-300">
          {error}
        </div>
      )}

      <form onSubmit={submit} className="panel rounded-2xl p-6 dark:bg-slate-900 dark:border-slate-700">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white">
          {editingRuleId ? "Edit rule" : "Create rule"}
        </h2>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          A fraud rule checks each payment and adds a risk score when the
          condition matches.
        </p>

        <div className="mt-4 rounded-lg border border-orange-200 dark:border-orange-900 bg-orange-50 dark:bg-orange-950/50 px-4 py-3">
          <p className="text-sm font-semibold text-orange-800 dark:text-orange-300">
            Rule type: {selectedRuleHelp.label}
          </p>
          <p className="mt-1 text-sm text-orange-700 dark:text-orange-400">
            {selectedRuleHelp.description}
          </p>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <label className="text-sm text-slate-700 dark:text-slate-300">
            <span className="mb-1 block font-medium">Rule Name</span>
            <input
              placeholder="Example: Large transfer check"
              value={form.name}
              onChange={(event) =>
                setForm((current) => ({ ...current, name: event.target.value }))
              }
              className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-slate-900 dark:text-white"
              required
            />
          </label>

          <label className="text-sm text-slate-700 dark:text-slate-300">
            <span className="mb-1 block font-medium">Rule Type</span>
            <select
              value={form.ruleType}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  ruleType: event.target.value as FraudRuleType,
                }))
              }
              className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-slate-900 dark:text-white"
            >
              {RULE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {RULE_HELP[type].label}
                </option>
              ))}
            </select>
          </label>

          {form.ruleType === "HIGH_RISK_COUNTRY" ? (
            <div className="sm:col-span-2 text-sm text-slate-700 dark:text-slate-300">
              <span className="mb-2 block font-medium">
                Flagged Countries
                <span className="ml-2 rounded bg-slate-100 dark:bg-slate-800 px-1.5 py-0.5 text-xs font-normal text-slate-500 dark:text-slate-400">
                  {selectedCountries.length} selected
                </span>
              </span>
              <p className="mb-2 text-xs text-slate-500 dark:text-slate-400">
                Tick the countries whose transactions should be flagged. Sources
                are FATF grey/black lists and OFAC sanctions.
              </p>
              <div className="grid grid-cols-1 gap-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-3 sm:grid-cols-2 max-h-60 overflow-y-auto">
                {HIGH_RISK_COUNTRIES.map(({ code, name, basis }) => (
                  <label
                    key={code}
                    className="flex items-start gap-2 rounded p-2 hover:bg-slate-50 dark:hover:bg-slate-700 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      className="mt-0.5 shrink-0"
                      checked={selectedCountries.includes(code)}
                      onChange={(e) =>
                        setSelectedCountries((prev) =>
                          e.target.checked
                            ? [...prev, code]
                            : prev.filter((c) => c !== code),
                        )
                      }
                    />
                    <span className="leading-snug">
                      <span className="font-medium text-slate-800 dark:text-slate-200">{name}</span>
                      <span className="ml-1 text-slate-400 text-xs">
                        ({code})
                      </span>
                      <span className="block text-xs text-slate-500 dark:text-slate-400">
                        {basis}
                      </span>
                    </span>
                  </label>
                ))}
              </div>
            </div>
          ) : (
            <label className="text-sm text-slate-700 dark:text-slate-300">
              <span className="mb-1 block font-medium">Threshold</span>
              <input
                placeholder={selectedRuleHelp.thresholdHint}
                type="number"
                value={form.threshold}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    threshold: Number(event.target.value),
                  }))
                }
                className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-slate-900 dark:text-white"
                required
              />
            </label>
          )}

          <label className="text-sm text-slate-700 dark:text-slate-300">
            <span className="mb-1 block font-medium">Risk Score Added</span>
            <input
              placeholder="Points added when this rule matches"
              type="number"
              value={form.scoreContribution}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  scoreContribution: Number(event.target.value),
                }))
              }
              className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-slate-900 dark:text-white"
              required
            />
          </label>

          <label className="sm:col-span-2 flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
            <input
              type="checkbox"
              checked={form.enabled}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  enabled: event.target.checked,
                }))
              }
            />
            Enabled
          </label>

          <label className="sm:col-span-2 text-sm text-slate-700 dark:text-slate-300">
            <span className="mb-1 block font-medium">Business Description</span>
            <textarea
              placeholder="Explain what this rule protects against and when it should trigger."
              value={form.description}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  description: event.target.value,
                }))
              }
              className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-slate-900 dark:text-white"
              rows={3}
              required
            />
          </label>
        </div>

        <div className="mt-4 flex gap-2">
          <button
            type="submit"
            className="rounded-lg bg-orange-600 hover:bg-orange-700 dark:bg-orange-700 dark:hover:bg-orange-600 px-4 py-2 text-sm font-semibold text-white transition"
          >
            {editingRuleId ? "Update" : "Create"}
          </button>
          {editingRuleId && (
            <button
              type="button"
              onClick={resetForm}
              className="rounded-lg border border-slate-200 dark:border-slate-700 px-4 py-2 text-sm text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 transition"
            >
              Cancel edit
            </button>
          )}
        </div>
      </form>

      <section className="space-y-6">
        <div>
          <h2 className="text-xl font-semibold text-slate-900 dark:text-white">
            Active Fraud Rules ({rules.length})
          </h2>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            Rules are evaluated for each payment. When a rule matches, its risk
            score is added to the total.
          </p>
        </div>

        {rules.length === 0 ? (
          <div className="panel rounded-2xl px-6 py-8 text-center text-sm text-slate-500 dark:text-slate-400 dark:bg-slate-900 dark:border-slate-700">
            No fraud rules configured yet. Create a rule to start.
          </div>
        ) : (
          <div className="grid gap-4">
            {rules.map((rule) => {
              const help = RULE_HELP[rule.ruleType];
              let blacklistedAccounts: string[] = [];
              if (rule.ruleType === "BLACKLISTED_ACCOUNT") {
                blacklistedAccounts = parseListFromDescription(
                  rule.description,
                );
              }

              const countryList =
                rule.ruleType === "HIGH_RISK_COUNTRY"
                  ? parseCountriesFromDescription(rule.description).countries
                  : [];

              return (
                <div
                  key={rule.id}
                  className={`panel rounded-2xl border-l-4 p-6 transition-all dark:bg-slate-900 dark:border-slate-700 ${
                    rule.enabled
                      ? "border-l-emerald-500 bg-white dark:bg-slate-900/50"
                      : "border-l-slate-300 dark:border-l-slate-600 bg-slate-50 dark:bg-slate-950"
                  }`}
                >
                  <div className="flex flex-col gap-4">
                    {/* Header */}
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div className="flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="text-lg font-semibold text-slate-900 dark:text-white">
                            {rule.name}
                          </h3>
                          <span
                            className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ${
                              rule.enabled
                                ? "bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300"
                                : "bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-400"
                            }`}
                          >
                            {rule.enabled ? "Active" : "Inactive"}
                          </span>
                        </div>
                        <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                          {help?.description || rule.description}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={() => {
                            setEditingRuleId(rule.id);
                            const { countries, cleanDescription } =
                              parseCountriesFromDescription(rule.description);
                            setSelectedCountries(countries);
                            setForm({
                              name: rule.name,
                              description: cleanDescription,
                              ruleType: rule.ruleType,
                              threshold: rule.threshold,
                              scoreContribution: rule.scoreContribution,
                              enabled: rule.enabled,
                            });
                            window.scrollTo({ top: 0, behavior: "smooth" });
                          }}
                          className="rounded-lg border border-slate-200 dark:border-slate-700 px-3 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 transition"
                        >
                          Edit
                        </button>
                        <button
                          onClick={async () => {
                            if (
                              window.confirm(
                                "Delete this rule? This action cannot be undone.",
                              )
                            ) {
                              await deleteFraudRule(rule.id);
                              await loadRules();
                            }
                          }}
                          className="rounded-lg bg-red-50 dark:bg-red-950/50 px-3 py-2 text-sm font-medium text-red-700 dark:text-red-400 hover:bg-red-100 dark:hover:bg-red-900/50 transition"
                        >
                          Delete
                        </button>
                      </div>
                    </div>

                    {/* Settings Grid */}
                    <div className="border-t border-slate-200 dark:border-slate-700 pt-4">
                      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                        <div>
                          <p className="text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">
                            Type
                          </p>
                          <p className="mt-1 text-sm font-medium text-slate-900 dark:text-white">
                            {help?.label || rule.ruleType}
                          </p>
                        </div>

                        {rule.ruleType !== "HIGH_RISK_COUNTRY" &&
                          rule.ruleType !== "BLACKLISTED_ACCOUNT" && (
                            <div>
                              <p className="text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">
                                Threshold
                              </p>
                              <p className="mt-1 text-sm font-medium text-slate-900 dark:text-white">
                                {rule.ruleType === "NIGHT_TRANSACTION"
                                  ? "22:00-06:00"
                                  : rule.threshold === 0
                                    ? "N/A"
                                    : rule.ruleType === "VELOCITY_CHECK" ||
                                        rule.ruleType === "SUSPICIOUS_FREQUENCY"
                                      ? `${rule.threshold} txns/10min`
                                      : rule.ruleType ===
                                          "REPEATED_FAILED_ATTEMPTS"
                                        ? `${rule.threshold} failures`
                                        : rule.ruleType === "NEW_ACCOUNT"
                                          ? `${rule.threshold} days`
                                          : `${rule.threshold}`}
                              </p>
                            </div>
                          )}

                        <div>
                          <p className="text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">
                            Risk Score
                          </p>
                          <p className="mt-1 text-sm font-medium text-slate-900 dark:text-white">
                            +{rule.scoreContribution} pts
                          </p>
                        </div>
                      </div>

                      {/* Blacklist Display */}
                      {rule.ruleType === "BLACKLISTED_ACCOUNT" &&
                        blacklistedAccounts.length > 0 && (
                          <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-700">
                            <p className="text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">
                              Blocked Accounts ({blacklistedAccounts.length})
                            </p>
                            <div className="mt-2 flex flex-wrap gap-2">
                              {blacklistedAccounts.map((acc) => (
                                <span
                                  key={acc}
                                  className="inline-flex items-center gap-1 rounded-full bg-red-100 dark:bg-red-950 px-3 py-1 text-xs font-medium text-red-700 dark:text-red-300"
                                >
                                  🚫 {acc}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}

                      {/* Country Display */}
                      {rule.ruleType === "HIGH_RISK_COUNTRY" &&
                        countryList.length > 0 && (
                          <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-700">
                            <p className="text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">
                              Monitored Countries ({countryList.length})
                            </p>
                            <div className="mt-2 flex flex-wrap gap-2">
                              {countryList.map((code) => {
                                const country = HIGH_RISK_COUNTRIES.find(
                                  (c) => c.code === code,
                                );
                                return (
                                  <span
                                    key={code}
                                    className="inline-flex items-center gap-1 rounded-full bg-orange-100 dark:bg-orange-950 px-3 py-1 text-xs font-medium text-orange-700 dark:text-orange-300"
                                  >
                                    ⚠️ {code}{" "}
                                    {country ? `(${country.name})` : ""}
                                  </span>
                                );
                              })}
                            </div>
                          </div>
                        )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* Blacklist Management Section */}
      <section className="panel rounded-2xl p-6 dark:bg-slate-900 dark:border-slate-700">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white">
          🚫 Blacklist Management
        </h2>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          Manage accounts that should be blocked. Add account numbers to block
          (e.g., BLK-FRAUD-001, ACC-SUSPICIOUS-123).
        </p>

        <div className="mt-4 p-4 rounded-lg bg-amber-50 dark:bg-amber-950/50 border border-amber-200 dark:border-amber-900">
          <p className="text-sm text-amber-800 dark:text-amber-300">
            <strong>How it works:</strong> Edit the &quot;Blacklisted Account
            Rule&quot; above to add or remove accounts. Any payment involving a
            blacklisted account (source or destination) will automatically
            trigger this rule and add{" "}
            {rules.find((r) => r.ruleType === "BLACKLISTED_ACCOUNT")
              ?.scoreContribution || 50}{" "}
            risk points.
          </p>
        </div>

        {(() => {
          const blacklistRule = rules.find(
            (r) => r.ruleType === "BLACKLISTED_ACCOUNT",
          );
          const accounts = blacklistRule
            ? parseListFromDescription(blacklistRule.description)
            : [];
          return (
            <div className="mt-4">
              <div className="flex flex-wrap gap-2">
                {accounts.length === 0 ? (
                  <p className="text-sm text-slate-500 dark:text-slate-400">
                    No accounts on blacklist yet.
                  </p>
                ) : (
                  accounts.map((acc) => (
                    <span
                      key={acc}
                      className="inline-flex items-center gap-2 rounded-full bg-red-100 dark:bg-red-950 px-3 py-1 text-sm font-medium text-red-700 dark:text-red-300"
                    >
                      {acc}
                    </span>
                  ))
                )}
              </div>
              {blacklistRule && (
                <button
                  onClick={() => {
                    setEditingRuleId(blacklistRule.id);
                    setForm({
                      name: blacklistRule.name,
                      description: parseListFromDescription(
                        blacklistRule.description,
                      )
                        .join(", ")
                        .split(", ")
                        .join("\n"),
                      ruleType: blacklistRule.ruleType,
                      threshold: blacklistRule.threshold,
                      scoreContribution: blacklistRule.scoreContribution,
                      enabled: blacklistRule.enabled,
                    });
                    window.scrollTo({ top: 0, behavior: "smooth" });
                  }}
                  className="mt-4 rounded-lg border border-slate-200 dark:border-slate-700 px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 transition"
                >
                  ✎ Manage Blacklist
                </button>
              )}
            </div>
          );
        })()}
      </section>
    </div>
  );
}

function parseListFromDescription(description: string): string[] {
  if (!description) return [];
  return description
    .split(/[,\n]/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}
