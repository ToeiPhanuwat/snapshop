import type { ActionState } from "@/app/admin/actions";

export function ActionMessage({ state }: { state: ActionState }) {
  if (!state) {
    return null;
  }
  return (
    <div className={`text-xs ${state.ok ? "text-emerald-700" : "text-rose-700"}`} role="status">
      <p>{state.message}</p>
      {state.errors?.length ? (
        <ul className="mt-1 list-disc space-y-0.5 pl-4">
          {state.errors.map((error) => (
            <li key={error}>{error}</li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
