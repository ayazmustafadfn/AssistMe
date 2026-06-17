// "Bir İşlem" — sayı oyunu mantığı
// Klasik kurallar: 6 sayı (büyük + küçük), hedefe +, -, x, ÷ ile ulaş.
// Ara sonuçlar daima pozitif tam sayı olmalı; bölme tam bölünmeli.
// ÜRETİM GARANTİSİ: hedef, verilen sayılardan gerçekten ulaşılabilen
// bir değerden seçilir; dolayısıyla her soru çözülebilirdir.

const LARGE = [25, 50, 75, 100];
const SMALL = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

function rand(n) {
  return Math.floor(Math.random() * n);
}

function shuffle(arr) {
  const a = arr.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = rand(i + 1);
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

// 6 sayı seç. largeCount: kaç tane "büyük" sayı (0..4)
export function pickNumbers(largeCount) {
  if (largeCount == null) {
    // 0..4 büyük; 1-2 ağırlıklı
    largeCount = [0, 1, 1, 2, 2, 2, 3, 4][rand(8)];
  }
  largeCount = Math.max(0, Math.min(4, largeCount));
  const larges = shuffle(LARGE).slice(0, largeCount);
  const smallPool = [];
  for (const n of SMALL) smallPool.push(n, n); // her küçük sayıdan iki adet
  const smalls = shuffle(smallPool).slice(0, 6 - largeCount);
  return shuffle([...larges, ...smalls]);
}

const OP_SYMBOL = { '+': '+', '-': '−', '*': '×', '/': '÷' };

// İki sayı + operatör -> { value } veya null (kural ihlali)
export function applyOp(a, b, op) {
  switch (op) {
    case '+':
      return a + b;
    case '*':
      return a * b;
    case '-':
      return a > b ? a - b : null; // sonuç pozitif olmalı
    case '/':
      return b !== 0 && a % b === 0 ? a / b : null; // tam bölünme
    default:
      return null;
  }
}

// Verilen sayılardan tüm sayıları birleştirerek tek bir hedef üretir
// ve bu hedefe ulaştıran adımları döndürür (örnek çözüm).
function buildOnce(nums) {
  let pool = nums.map((v) => ({ v }));
  const steps = [];
  while (pool.length > 1) {
    const i = rand(pool.length);
    let j = rand(pool.length);
    while (j === i) j = rand(pool.length);
    const a = pool[i].v;
    const b = pool[j].v;

    // Geçerli işlemleri topla (büyük operandı önce yaz)
    const hi = Math.max(a, b);
    const lo = Math.min(a, b);
    const candidates = [];
    candidates.push(['+', hi + lo]);
    if (hi !== lo || hi > 1) candidates.push(['*', hi * lo]);
    if (hi > lo) candidates.push(['-', hi - lo]);
    if (lo !== 0 && hi % lo === 0 && hi !== lo) candidates.push(['/', hi / lo]);

    const valid = candidates.filter(([, r]) => r > 0 && Number.isInteger(r));
    const [op, res] = valid[rand(valid.length)];

    steps.push(`${hi} ${OP_SYMBOL[op]} ${lo} = ${res}`);

    const next = pool.filter((_, idx) => idx !== i && idx !== j);
    next.push({ v: res });
    pool = next;
  }
  return { target: pool[0].v, steps };
}

// Belirli aralıkta, çözülebilir bir soru üretir.
// minTarget/maxTarget: klasikte 101–999.
export function generatePuzzle(opts = {}) {
  const { largeCount, minTarget = 101, maxTarget = 999 } = opts;
  for (let attempt = 0; attempt < 400; attempt++) {
    const numbers = pickNumbers(largeCount);
    const { target, steps } = buildOnce(numbers);
    if (target >= minTarget && target <= maxTarget) {
      return { numbers, target, solution: steps };
    }
  }
  // Çok nadir: aralığa düşmediyse en yakını kabul et (yine de çözülebilir)
  const numbers = pickNumbers(largeCount);
  const { target, steps } = buildOnce(numbers);
  return { numbers, target, solution: steps };
}

export { OP_SYMBOL };
