import React, { useEffect, useState } from 'react';
import './App.css';

interface DealItem {
  name: string;
  hashName: string;
  currentPrice: number;
  dealScore: number;
  imageUrl: string;
  volume24h: number;
}

export function App() {
  const [items, setItems] = useState<DealItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
          {items.length} предметов
        </div>
      </nav>

      <main className="main">
        {loading && <div className="state">Загрузка...</div>}
        {error && <div className="state error">Ошибка: {error}</div>}
        {!loading && !error && (
          <div className="grid">
            {items.map(item => (
              <div key={item.hashName} className="card">
                <div className="card-img">
                  <img src={item.imageUrl} alt={item.name} />
                </div>
                <div className="card-body">
                  <div className="card-name">{item.name}</div>
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