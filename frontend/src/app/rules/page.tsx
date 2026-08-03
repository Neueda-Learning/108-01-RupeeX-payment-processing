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
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
          Fraud Rules Workspace
        </p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900">
          Create and manage fraud detection policies
        </h1>
      </header>

      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      <form onSubmit={submit} className="panel rounded-2xl p-6">
        <h2 className="text-lg font-semibold text-slate-900">
          {editingRuleId ? "Edit rule" : "Create rule"}
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          A fraud rule checks each payment and adds a risk score when the
          condition matches.
        </p>

        <div className="mt-4 rounded-lg border border-orange-200 bg-orange-50 px-4 py-3">
          <p className="text-sm font-semibold text-orange-800">
            Rule type: {selectedRuleHelp.label}
          </p>
          <p className="mt-1 text-sm text-orange-700">
            {selectedRuleHelp.description}
          </p>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <label className="text-sm text-slate-700">
            <span className="mb-1 block font-medium">Rule Name</span>
            <input
              placeholder="Example: Large transfer check"
              value={form.name}
              onChange={(event) =>
                setForm((current) => ({ ...current, name: event.target.value }))
              }
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2"
              required
            />
          </label>

          <label className="text-sm text-slate-700">
            <span className="mb-1 block font-medium">Rule Type</span>
            <select
              value={form.ruleType}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  ruleType: event.target.value as FraudRuleType,
                }))
              }
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2"
            >
              {RULE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {RULE_HELP[type].label}
                </option>
              ))}
            </select>
          </label>

          {form.ruleType === "HIGH_RISK_COUNTRY" ? (
            <div className="sm:col-span-2 text-sm text-slate-700">
              <span className="mb-2 block font-medium">
                Flagged Countries
                <span className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-xs font-normal text-slate-500">
                  {selectedCountries.length} selected
                </span>
              </span>
              <p className="mb-2 text-xs text-slate-500">
                Tick the countries whose transactions should be flagged. Sources
                are FATF grey/black lists and OFAC sanctions.
              </p>
              <div className="grid grid-cols-1 gap-1.5 rounded-lg border border-slate-200 bg-white p-3 sm:grid-cols-2 max-h-60 overflow-y-auto">
                {HIGH_RISK_COUNTRIES.map(({ code, name, basis }) => (
                  <label
                    key={code}
                    className="flex items-start gap-2 rounded p-1 hover:bg-slate-50 cursor-pointer"
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
                      <span className="font-medium text-slate-800">{name}</span>
                      <span className="ml-1 text-slate-400 text-xs">
                        ({code})
                      </span>
                      <span className="block text-xs text-slate-400">
                        {basis}
                      </span>
                    </span>
                  </label>
                ))}
              </div>
            </div>
          ) : (
            <label className="text-sm text-slate-700">
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
                className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2"
                required
              />
            </label>
          )}

          <label className="text-sm text-slate-700">
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
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2"
              required
            />
          </label>

          <label className="sm:col-span-2 flex items-center gap-2 text-sm text-slate-700">
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

          <label className="sm:col-span-2 text-sm text-slate-700">
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
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2"
              rows={3}
              required
            />
          </label>
        </div>

        <div className="mt-4 flex gap-2">
          <button
            type="submit"
            className="rounded-lg bg-orange-600 px-4 py-2 text-sm font-semibold text-white hover:bg-orange-700"
          >
            {editingRuleId ? "Update" : "Create"}
          </button>
          {editingRuleId && (
            <button
              type="button"
              onClick={resetForm}
              className="rounded-lg border border-slate-200 px-4 py-2 text-sm"
            >
              Cancel edit
            </button>
          )}
        </div>
      </form>

      <section className="panel rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5">
            <thead className="bg-slate-50">
              <tr>
                {[
                  "Name",
                  "Type",
                  "Threshold",
                  "Score",
                  "Enabled",
                  "Actions",
                ].map((header) => (
                  <th
                    key={header}
                    className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500"
                  >
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-black/5">
              {rules.map((rule) => (
                <tr key={rule.id}>
                  <td className="px-6 py-4 text-sm text-slate-800">
                    {rule.name}
                  </td>
                  <td className="px-6 py-4 text-xs text-slate-500">
                    {rule.ruleType}
                  </td>
                  <td className="px-6 py-4 text-sm">{rule.threshold}</td>
                  <td className="px-6 py-4 text-sm">
                    {rule.scoreContribution}
                  </td>
                  <td className="px-6 py-4 text-sm">
                    {rule.enabled ? "Yes" : "No"}
                  </td>
                  <td className="px-6 py-4 text-sm">
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
                        }}
                        className="text-orange-700 hover:text-orange-800"
                      >
                        Edit
                      </button>
                      <button
                        onClick={async () => {
                          await deleteFraudRule(rule.id);
                          await loadRules();
                        }}
                        className="text-red-700 hover:text-red-800"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
