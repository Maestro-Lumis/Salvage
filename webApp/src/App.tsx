import React, { useEffect, useState } from 'react';
import './App.css';

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

type SortKey = 'dealScore' | 'currentPrice' | 'volume24h';

export function App() {
  const [items, setItems] = useState<DealItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState<SortKey>('dealScore');

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

  const filtered = items
    .filter(i => i.name.toLowerCase().includes(search.toLowerCase()))
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

        {loading && <div className="state">Загрузка данных...</div>}
        {error && <div className="state error">Ошибка: {error}</div>}
        {!loading && !error && filtered.length === 0 && (
          <div className="state">Ничего не найдено</div>
        )}
        {!loading && !error && (
          <div className="grid">
            {filtered.map(item => (
              <div key={item.hashName} className="card">
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
    </div>
  );
}