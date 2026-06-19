import React from 'react';
import './ItemModal.css';

interface DealItem {
  name: string; hashName: string; currentPrice: number;
  dealScore: number; dealLevel: string; imageUrl: string;
  volume24h: number; median30d: number;
}
interface PricePoint { date: string; price: number; volume: number; }
interface Props { item: DealItem; onClose: () => void; }

const LABELS: Record<string, string> = { TREASURE: 'Клад', GOOD: 'Выгодно', NEUTRAL: 'Нейтрально', BAD: 'Переплата' };
const COLORS: Record<string, string> = { TREASURE: '#f59e0b', GOOD: '#22c55e', NEUTRAL: '#94a3b8', BAD: '#ef4444' };

function SimpleLine({ data, median }: { data: PricePoint[]; median: number }) {
  const w = 500, h = 140, pl = 48, pr = 8, pt = 8, pb = 24;
  const prices = data.map(d => d.price).filter(p => p > 0);
  if (prices.length === 0) return null;

  const minP = Math.min(...prices) * 0.97;
  const maxP = Math.max(...prices) * 1.03;
  const rangeP = maxP - minP || 1;

  const cx = (i: number) => pl + (i / (data.length - 1)) * (w - pl - pr);
  const cy = (p: number) => pt + (1 - (p - minP) / rangeP) * (h - pt - pb);

  const points = data
    .filter(d => d.price > 0)
    .map((d, i) => `${cx(i)},${cy(d.price)}`).join(' ');

  const medY = cy(median);
  const ticks = [minP, (minP + maxP) / 2, maxP];

  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto', display: 'block' }}>
      {ticks.map((t, i) => (
        <g key={i}>
          <line x1={pl} x2={w - pr} y1={cy(t)} y2={cy(t)} stroke="#f1f5f9" strokeWidth="1" />
          <text x={pl - 4} y={cy(t) + 4} textAnchor="end" fontSize="10" fill="#94a3b8">${t.toFixed(2)}</text>
        </g>
      ))}
      <line x1={pl} x2={w - pr} y1={medY} y2={medY} stroke="#f59e0b" strokeWidth="1.5" strokeDasharray="5,4" />
      <polyline points={points} fill="none" stroke="#0b3d5c" strokeWidth="2" strokeLinejoin="round" />
      <text x={pl} y={h - 4} fontSize="9" fill="#94a3b8">-29д</text>
      <text x={w - pr} y={h - 4} textAnchor="end" fontSize="9" fill="#94a3b8">сейчас</text>
    </svg>
  );
}

export function ItemModal({ item, onClose }: Props) {
  const [history, setHistory] = React.useState<PricePoint[]>([]);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    fetch(
      'http://localhost:8081/api/history?hashName=' + encodeURIComponent(item.hashName) +
      '&median=' + item.median30d +
      '&current=' + item.currentPrice
    )
      .then(r => r.json())
      .then((d: PricePoint[]) => { setHistory(d); setLoading(false); })
      .catch(() => setLoading(false));
  }, [item.hashName]);

  const steamUrl = 'https://steamcommunity.com/market/listings/730/' + encodeURIComponent(item.hashName);
  const color = COLORS[item.dealLevel] ?? '#94a3b8';
  const scoreText = item.dealScore > 0 ? '+' + item.dealScore + '%' : item.dealScore + '%';

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>x</button>
        <div className="modal-header">
          <img src={item.imageUrl} alt={item.name} className="modal-img" />
          <div className="modal-info">
            <div className="modal-name">{item.name}</div>
            <div className="modal-score" style={{ color }}>{scoreText}<span className="modal-level">{LABELS[item.dealLevel]}</span></div>
            <div className="modal-prices">
              <div className="modal-price-block"><div className="modal-price-label">Сейчас</div><div className="modal-price-val">${item.currentPrice.toFixed(2)}</div></div>
              <div className="modal-price-block"><div className="modal-price-label">Медиана</div><div className="modal-price-val">${item.median30d.toFixed(2)}</div></div>
              <div className="modal-price-block"><div className="modal-price-label">Объём</div><div className="modal-price-val">{item.volume24h.toLocaleString()}</div></div>
            </div>
            <a className="modal-buy" href={steamUrl} target="_blank" rel="noreferrer">Открыть на Steam</a>
          </div>
        </div>
        <div className="modal-chart-section">
          <div className="modal-chart-title">История цены</div>
          {loading && <div className="modal-chart-loading">Загрузка...</div>}
          {!loading && history.length === 0 && <div className="modal-chart-loading">Нет данных</div>}
          {!loading && history.length > 0 && (
            <SimpleLine data={history} median={item.median30d} />
          )}
        </div>
      </div>
    </div>
  );
}