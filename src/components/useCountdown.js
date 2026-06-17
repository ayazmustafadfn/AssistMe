import { useState, useEffect, useRef, useCallback } from 'react';

// Basit geri sayım kancası. active=false ise sayaç durur (rahat mod).
export function useCountdown(seconds, active, onExpire) {
  const [remaining, setRemaining] = useState(seconds);
  const onExpireRef = useRef(onExpire);
  onExpireRef.current = onExpire;

  const reset = useCallback((s = seconds) => setRemaining(s), [seconds]);

  useEffect(() => {
    if (!active) return undefined;
    if (remaining <= 0) {
      onExpireRef.current && onExpireRef.current();
      return undefined;
    }
    const id = setTimeout(() => setRemaining((r) => r - 1), 1000);
    return () => clearTimeout(id);
  }, [active, remaining]);

  return { remaining, reset, setRemaining };
}
