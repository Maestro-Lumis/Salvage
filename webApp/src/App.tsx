import React, { useEffect, useState } from 'react';
import { ItemModal } from './ItemModal';
import { Radar } from './Radar';
import './App.css';

interface DealItem {
  name: string;
  hashName: string;
  type: string;
  currentPrice: number;
  dealScore: number;
  dealLevel: string;
  imageUrl: string;
  volume24h: number;
  median30d: number;
}

type SortKey = 'dealScore' | 'currentPrice' | 'volume24h';

const TYPE_LABELS: Record<string, string> = {
  ALL: 'Все',
  KNIFE: 'Ножи',
  GLOVES: 'Перчатки',
  RIFLE: 'Винтовки',
  SNIPER: 'Снайперки',
  SMG: 'SMG',
  PISTOL: 'Пистолеты',
  SHOTGUN: 'Дробовики',
  HEAVY: 'Тяжёлое',
  CASE: 'Кейсы',
  CAPSULE: 'Капсулы',
  STICKER: 'Стикеры',
  MUSIC_KIT: 'Музыка',
  AGENT: 'Агенты',
  OTHER: 'Другое'
};

export function App() {
  const [items, setItems] = useState<DealItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState<SortKey>('dealScore');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [selectedItem, setSelectedItem] = useState<DealItem | null>(null);
  const [view, setView] = useState<'grid' | 'radar'>('grid');

  useEffect(() => {
    fetch('http://localhost:8081/api/deals')
      .then(res => res.json())
      .then((data: DealItem[]) => {
        setItems(data);
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  const availableTypes = ['ALL', ...Array.from(new Set(items.map(i => i.type)))];

  const filtered = items
    .filter(i => i.name.toLowerCase().includes(search.toLowerCase()))
    .filter(i => typeFilter === 'ALL' || i.type === typeFilter)
    .sort((a, b) => {
      if (sortBy === 'currentPrice') return a.currentPrice - b.currentPrice;
      if (sortBy === 'volume24h') return b.volume24h - a.volume24h;
      return b.dealScore - a.dealScore;
    });

  return (
    <div className="app">
      <nav className="nav">
        <div className="logo">
          <div className="logo-mark">⚓</div>
          <div>
            <div className="logo-name">Salvage</div>
            <div className="logo-sub">Deal Radar</div>
          </div>
        </div>
        <div className="nav-badge">
          <span className="live-dot" />
          {filtered.length} предметов
        </div>
        <div className="view-tabs">
          <button className={view === 'radar' ? 'active' : ''} onClick={() => setView('radar')}>Радар</button>
          <button className={view === 'grid' ? 'active' : ''} onClick={() => setView('grid')}>Сетка</button>
        </div>
      </nav>

      <main className="main">
        <div className="controls">
          <input
            className="search"
            placeholder="Поиск предмета..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          <div className="sort-tabs">
            <button className={sortBy === 'dealScore' ? 'active' : ''} onClick={() => setSortBy('dealScore')}>Deal Score</button>
            <button className={sortBy === 'currentPrice' ? 'active' : ''} onClick={() => setSortBy('currentPrice')}>Цена</button>
            <button className={sortBy === 'volume24h' ? 'active' : ''} onClick={() => setSortBy('volume24h')}>Объём</button>
          </div>
        </div>

        <div className="type-filters">
          {availableTypes.map(type => (
            <button
              key={type}
              className={typeFilter === type ? 'active' : ''}
              onClick={() => setTypeFilter(type)}
            >
              {TYPE_LABELS[type] ?? type}
            </button>
          ))}
        </div>

        {loading && <div className="state">Загрузка данных...</div>}
        {error && <div className="state error">Ошибка: {error}</div>}
        {!loading && !error && filtered.length === 0 && (
          <div className="state">Ничего не найдено</div>
        )}

        {!loading && !error && filtered.length > 0 && view === 'radar' && (
          <Radar items={filtered} onSelect={setSelectedItem} />
        )}

        {!loading && !error && filtered.length > 0 && view === 'grid' && (
          <div className="grid">
            {filtered.map(item => (
              <div key={item.hashName} className="card" onClick={() => setSelectedItem(item)}>
                <div className="card-img">
                  <img src={item.imageUrl} alt={item.name} />
                </div>
                <div className="card-body">
                  <div className="card-name">{item.name}</div>
                  <div className="card-score" data-level={item.dealLevel}>
                    {item.dealScore > 0 ? `+${item.dealScore}%` : `${item.dealScore}%`}
                  </div>
                  <div className="card-median">медиана ${item.median30d.toFixed(2)}</div>
                  <div className="card-row">
                    <span className="card-price">${item.currentPrice.toFixed(2)}</span>
                    <span className="card-vol">{item.volume24h.toLocaleString()} шт</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {selectedItem && (
        <ItemModal item={selectedItem} onClose={() => setSelectedItem(null)} />
      )}
    </div>
  );
}
