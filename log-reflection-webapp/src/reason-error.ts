import type { Reason } from "@dev.hiconic/gm_gm-core-api";

/** Failure of a service request, which keeps the {@link Reason} so callers can check its type. */
export class ReasonError extends Error {
  constructor(readonly reason: Reason) {
    super(describeReason(reason));
    this.name = "ReasonError";
  }
}

type VisibleReason = {
  TypeSignature?: () => string;
  text?: string | null;
  tracebackId?: string | null;
  reasons?: Iterable<VisibleReason> | null;
};

function describeReason(value: unknown): string {
  if (!value || typeof value !== "object") return String(value);
  const reason = value as VisibleReason;
  const type = reason.TypeSignature?.().split(".").pop() || "Reason";
  const text = reason.text?.trim();
  const id = reason.tracebackId || /tracebackId=([^\s)]+)/.exec(text || "")?.[1];
  const details = Array.from(reason.reasons ?? []).map(describeReason);
  return [`${type}: ${text || "No details available"}`, ...(id && !text?.includes(id) ? [`Traceback ID: ${id}`] : []),
    ...details.map(detail => `  ↳ ${detail}`)].join("\n");
}
