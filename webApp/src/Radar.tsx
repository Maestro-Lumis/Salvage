import React from 'react';
import './Radar.css';

interface DealItem {
  name: string;
  hashName: string;
  currentPrice: number;
  dealScore: number;
  dealLevel: string;
  imageUrl: string;
  volume24h: number;
  median30d: number;
}

interface Props {
  items: DealItem[];
  onSelect: (item: DealItem) => void;
}

const DOT_COLORS: Record<string, string> = {
  TREASURE: '#f59e0b',
  GOOD: '#22c55e',
  NEUTRAL: '#94a3b8',
  BAD: '#ef4444'
};

function hashToAngle(str: string): number {
  let h = 0;
  for (let i = 0; i < str.length; i++) {
    h = (h * 31 + str.charCodeAt(i)) & 0xffffffff;
  }
  return (h >>> 0) % 360;
}

function scoreToRadius(score: number, maxR: number): number {
  if (score >= 25) return maxR * 0.88;
  if (score >= 10) return maxR * 0.72;
  if (score >= 0)  return maxR * 0.54;
  return maxR * 0.35;
}

export function Radar({ items, onSelect }: Props) {
  const size = 320;
  const cx = size / 2;
  const cy = size / 2;
  const maxR = size / 2 - 16;

  const rings = [0.35, 0.54, 0.72, 0.88].map(r => r * maxR);
  const ringLabels = ['BAD', 'NEUTRAL', 'GOOD', 'TREASURE'];

  return (
    <div className="radar-wrap">
      <svg
        viewBox={`0 0 ${size} ${size}`}
        style={{ width: '100%', maxWidth: 360, display: 'block', margin: '0 auto' }}
      >
        {/* Фоновые кольца */}
        {rings.map((r, i) => (
          <circle key={i} cx={cx} cy={cy} r={r} fill="none" stroke="#e2e8f0" strokeWidth="1" />
        ))}

        {/* Крестовины */}
        <line x1={cx} y1={16} x2={cx} y2={size - 16} stroke="#e2e8f0" strokeWidth="1" />
        <line x1={16} y1={cy} x2={size - 16} y2={cy} stroke="#e2e8f0" strokeWidth="1" />

        {/* Метки зон */}
        {rings.map((r, i) => (
          <text key={i} x={cx + 4} y={cy - r + 11} fontSize="8" fill="#cbd5e1" fontWeight="600">
            {ringLabels[i]}
          </text>
        ))}

        {/* Стрелка радара */}
        <g style={{ transformOrigin: `${cx}px ${cy}px` }}>
          <line x1={cx} y1={cy} x2={cx} y2={16} stroke="#0b3d5c" strokeWidth="1.5" opacity="0.25" />
          <animateTransform
            attributeName="transform"
            type="rotate"
            from={`0 ${cx} ${cy}`}
            to={`360 ${cx} ${cy}`}
            dur="5s"
            repeatCount="indefinite"
          />
        </g>

        {/* Точки предметов */}
        {items.map((item) => {
          const angle = hashToAngle(item.hashName);
          const r = scoreToRadius(item.dealScore, maxR);
          const rad = (angle - 90) * (Math.PI / 180);
          const x = cx + r * Math.cos(rad);
          const y = cy + r * Math.sin(rad);
          const dotColor = DOT_COLORS[item.dealLevel] ?? '#94a3b8';
          const isHot = item.dealLevel === 'TREASURE' || item.dealLevel === 'GOOD';

          return (
            <g key={item.hashName} onClick={() => onSelect(item)} style={{ cursor: 'pointer' }}>
              {isHot && (
                <circle cx={x} cy={y} r="10" fill="none" stroke={dotColor} strokeWidth="1" opacity="0">
                  <animate attributeName="r" values="6;20" dur="2s" repeatCount="indefinite" />
                  <animate attributeName="opacity" values="0.6;0" dur="2s" repeatCount="indefinite" />
                </circle>
              )}
              <circle cx={x} cy={y} r="6" fill={dotColor} stroke="#fff" strokeWidth="2" />
            </g>
          );
        })}

        {/* Центр */}
        <circle cx={cx} cy={cy} r="4" fill="#0b3d5c" />

        {/* Стороны света */}
        <text x={cx} y={10} textAnchor="middle" fontSize="9" fill="#94a3b8" fontWeight="700">N</text>
        <text x={size - 8} y={cy + 4} textAnchor="middle" fontSize="9" fill="#94a3b8" fontWeight="700">E</text>
        <text x={cx} y={size - 4} textAnchor="middle" fontSize="9" fill="#94a3b8" fontWeight="700">S</text>
        <text x={8} y={cy + 4} textAnchor="middle" fontSize="9" fill="#94a3b8" fontWeight="700">W</text>
      </svg>

      {/* Легенда */}
      <div className="radar-legend">
        {Object.entries(DOT_COLORS).map(([level, color]) => (
          <div key={level} className="radar-legend-item">
            <div className="radar-legend-dot" style={{ background: color }} />
            <span>{{ TREASURE: 'Клад', GOOD: 'Выгодно', NEUTRAL: 'Нейтрально', BAD: 'Переплата' }[level]}</span>
          </div>
        ))}
      </div>
    </div>
  );
}