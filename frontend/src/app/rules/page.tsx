"use client";

import { useEffect, useState } from "react";
import {
  createFraudRule,
  deleteFraudRule,
  getFraudRules,
  updateFraudRule,
} from "@/lib/api";
import type { FraudRule, FraudRuleInput, FraudRuleType } from "@/lib/types";

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

  const loadRules = async () => {
    try {
      setRules(await getFraudRules());
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Unable to load rules");
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
  };

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    try {
      if (editingRuleId) {
        await updateFraudRule(editingRuleId, form);
      } else {
        await createFraudRule(form);
      }
      await loadRules();
      resetForm();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Unable to save rule");
    }
  };

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-10 lg:px-8">
      <header>
        <p className="text-sm font-semibold uppercase tracking-wide text-emerald-600">
          Rules Management
        </p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
          Fraud policy console
        </h1>
      </header>

      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">
          {error}
        </p>
      )}

      <form
        onSubmit={submit}
        className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-slate-900"
      >
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white">
          {editingRuleId ? "Edit rule" : "Create rule"}
        </h2>
        <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <input
            placeholder="Rule name"
            value={form.name}
            onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
            className="rounded-lg border border-slate-200 px-3 py-2 dark:border-slate-700 dark:bg-slate-950"
            required
          />
          <select
            value={form.ruleType}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                ruleType: event.target.value as FraudRuleType,
              }))
            }
            className="rounded-lg border border-slate-200 px-3 py-2 dark:border-slate-700 dark:bg-slate-950"
          >
            {RULE_TYPES.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
          <input
            placeholder="Threshold"
            type="number"
            value={form.threshold}
            onChange={(event) =>
              setForm((current) => ({ ...current, threshold: Number(event.target.value) }))
            }
            className="rounded-lg border border-slate-200 px-3 py-2 dark:border-slate-700 dark:bg-slate-950"
            required
          />
          <input
            placeholder="Score contribution"
            type="number"
            value={form.scoreContribution}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                scoreContribution: Number(event.target.value),
              }))
            }
            className="rounded-lg border border-slate-200 px-3 py-2 dark:border-slate-700 dark:bg-slate-950"
            required
          />
          <label className="sm:col-span-2 flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
            <input
              type="checkbox"
              checked={form.enabled}
              onChange={(event) =>
                setForm((current) => ({ ...current, enabled: event.target.checked }))
              }
            />
            Enabled
          </label>
          <textarea
            placeholder="Description"
            value={form.description}
            onChange={(event) =>
              setForm((current) => ({ ...current, description: event.target.value }))
            }
            className="sm:col-span-2 rounded-lg border border-slate-200 px-3 py-2 dark:border-slate-700 dark:bg-slate-950"
            rows={3}
            required
          />
        </div>

        <div className="mt-4 flex gap-2">
          <button
            type="submit"
            className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
          >
            {editingRuleId ? "Update" : "Create"}
          </button>
          {editingRuleId && (
            <button
              type="button"
              onClick={resetForm}
              className="rounded-lg border border-slate-200 px-4 py-2 text-sm dark:border-slate-700"
            >
              Cancel edit
            </button>
          )}
        </div>
      </form>

      <section className="rounded-2xl border border-black/5 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5 dark:divide-white/10">
            <thead className="bg-slate-50 dark:bg-slate-800/50">
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
                    className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400"
                  >
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-black/5 dark:divide-white/10">
              {rules.map((rule) => (
                <tr key={rule.id}>
                  <td className="px-6 py-4 text-sm text-slate-800 dark:text-slate-200">{rule.name}</td>
                  <td className="px-6 py-4 text-xs text-slate-500 dark:text-slate-400">{rule.ruleType}</td>
                  <td className="px-6 py-4 text-sm">{rule.threshold}</td>
                  <td className="px-6 py-4 text-sm">{rule.scoreContribution}</td>
                  <td className="px-6 py-4 text-sm">{rule.enabled ? "Yes" : "No"}</td>
                  <td className="px-6 py-4 text-sm">
                    <div className="flex gap-2">
                      <button
                        onClick={() => {
                          setEditingRuleId(rule.id);
                          setForm({
                            name: rule.name,
                            description: rule.description,
                            ruleType: rule.ruleType,
                            threshold: rule.threshold,
                            scoreContribution: rule.scoreContribution,
                            enabled: rule.enabled,
                          });
                        }}
                        className="text-emerald-700 hover:text-emerald-800 dark:text-emerald-400"
                      >
                        Edit
                      </button>
                      <button
                        onClick={async () => {
                          await deleteFraudRule(rule.id);
                          await loadRules();
                        }}
                        className="text-red-700 hover:text-red-800 dark:text-red-400"
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
