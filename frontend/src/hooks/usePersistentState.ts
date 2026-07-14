import { useEffect, useRef, useState, type Dispatch, type SetStateAction } from 'react';

const PREFIX = 'csp.table.';

export type Serializer<T> = {
  serialize: (value: T) => string;
  deserialize: (raw: string) => T;
};

function jsonSerializer<T>(): Serializer<T> {
  return {
    serialize: (v) => JSON.stringify(v),
    deserialize: (raw) => JSON.parse(raw) as T,
  };
}

/** Serializer for Set<string> state (e.g. expanded-row ids), stored as a JSON array. */
export const setSerializer: Serializer<Set<string>> = {
  serialize: (v) => JSON.stringify([...v]),
  deserialize: (raw) => new Set(JSON.parse(raw) as string[]),
};

function readStored<T>(storageKey: string, deserialize: (raw: string) => T): T | undefined {
  try {
    const raw = window.sessionStorage.getItem(storageKey);
    if (raw === null) return undefined;
    return deserialize(raw);
  } catch {
    // storage unavailable or corrupt JSON — caller falls back to initial value
    return undefined;
  }
}

/**
 * useState that mirrors its value to sessionStorage under `${namespace}.${key}`.
 * Restores on mount; degrades to in-memory-only if storage is unavailable.
 * `namespace` MUST begin with `csp.table.` so clearPersistedTableState() sweeps it.
 */
export function usePersistentState<T>(
  namespace: string,
  key: string,
  initialValue: T,
  serializer: Serializer<T> = jsonSerializer<T>(),
): [T, Dispatch<SetStateAction<T>>] {
  const storageKey = `${namespace}.${key}`;
  const serializerRef = useRef(serializer);

  const [value, setValue] = useState<T>(() => {
    const stored = readStored(storageKey, serializerRef.current.deserialize);
    return stored === undefined ? initialValue : stored;
  });

  useEffect(() => {
    try {
      window.sessionStorage.setItem(storageKey, serializerRef.current.serialize(value));
    } catch {
      // storage unavailable/quota — keep working from in-memory state
    }
  }, [storageKey, value]);

  return [value, setValue];
}

/** Remove every persisted table-state key. Call on logout. */
export function clearPersistedTableState(): void {
  try {
    const toRemove: string[] = [];
    for (let i = 0; i < window.sessionStorage.length; i += 1) {
      const k = window.sessionStorage.key(i);
      if (k && k.startsWith(PREFIX)) toRemove.push(k);
    }
    toRemove.forEach((k) => window.sessionStorage.removeItem(k));
  } catch {
    // no-op
  }
}
