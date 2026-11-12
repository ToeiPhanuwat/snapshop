/** Plain GET form: works without JavaScript and keeps the search in the URL. */
export function SearchForm({ action, defaultValue, placeholder }: { action: string; defaultValue?: string; placeholder: string }) {
  return (
    <form action={action} className="flex gap-2" role="search">
      <input
        type="search"
        name="q"
        defaultValue={defaultValue}
        placeholder={placeholder}
        className="min-w-0 flex-1 rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm focus:border-orange-400 focus:outline-none"
      />
      <button type="submit" className="rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700">
        ค้นหา
      </button>
    </form>
  );
}
