"use client";

import { useEffect } from "react";
import { AlertCircle, CheckCircle, Info, X } from "lucide-react";

export type ToastType = "success" | "error" | "info" | "warning";

export interface Toast {
  id: string;
  type: ToastType;
  title: string;
  message: string;
  duration?: number;
}

const toastIcons = {
  success: <CheckCircle className="h-5 w-5 text-emerald-600" />,
  error: <AlertCircle className="h-5 w-5 text-red-600" />,
  warning: <AlertCircle className="h-5 w-5 text-amber-600" />,
  info: <Info className="h-5 w-5 text-blue-600" />,
};

const toastColors = {
  success: "border-emerald-200 bg-emerald-50",
  error: "border-red-200 bg-red-50",
  warning: "border-amber-200 bg-amber-50",
  info: "border-blue-200 bg-blue-50",
};

const toastTextColors = {
  success: "text-emerald-900",
  error: "text-red-900",
  warning: "text-amber-900",
  info: "text-blue-900",
};

export function ToastContainer({
  toasts,
  onRemove,
}: {
  toasts: Toast[];
  onRemove: (id: string) => void;
}) {
  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 pointer-events-none">
      {toasts.map((toast) => (
        <ToastItem
          key={toast.id}
          toast={toast}
          onRemove={onRemove}
        />
      ))}
    </div>
  );
}

function ToastItem({
  toast,
  onRemove,
}: {
  toast: Toast;
  onRemove: (id: string) => void;
}) {
  useEffect(() => {
    const timer = setTimeout(
      () => onRemove(toast.id),
      toast.duration || 5000
    );
    return () => clearTimeout(timer);
  }, [toast.id, toast.duration, onRemove]);

  return (
    <div
      className={`pointer-events-auto rounded-lg border px-4 py-3 shadow-lg animate-in slide-in-from-top-2 fade-in-0 ${toastColors[toast.type]}`}
    >
      <div className="flex gap-3">
        <div className="flex-shrink-0">
          {toastIcons[toast.type]}
        </div>
        <div className="flex-1">
          <h3 className={`text-sm font-semibold ${toastTextColors[toast.type]}`}>
            {toast.title}
          </h3>
          <p className={`text-sm mt-1 ${toastTextColors[toast.type]} opacity-90`}>
            {toast.message}
          </p>
        </div>
        <button
          onClick={() => onRemove(toast.id)}
          className={`flex-shrink-0 ${toastTextColors[toast.type]} hover:opacity-70 transition`}
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}


