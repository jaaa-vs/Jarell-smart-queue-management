// Smart Queue v4.0 - Enhanced UX
const API_BASE = '/api';

// DOM elements
const $currentDisplay = document.getElementById('current-display');
const $serveBtn = document.getElementById('serve-btn');
const $status = document.getElementById('status');

// Audio context for beeps
let audioCtx;
try {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
} catch(e) {
    console.log('Audio not supported');
}

// Beep sound
function playBeep(frequency = 800, duration = 200) {
    if (!audioCtx) return;
    const oscillator = audioCtx.createOscillator();
    const gainNode = audioCtx.createGain();
    
    oscillator.connect(gainNode);
    gainNode.connect(audioCtx.destination);
    
    oscillator.frequency.value = frequency;
    oscillator.type = 'sine';
    
    gainNode.gain.setValueAtTime(0.3, audioCtx.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + duration / 1000);
    
    oscillator.start(audioCtx.currentTime);
    oscillator.stop(audioCtx.currentTime + duration / 1000);
}

// Success celebration
function celebrate() {
    playBeep(523, 150);
    playBeep(659, 150);
    playBeep(784, 300);
    
    // Confetti
    confetti({
        particleCount: 100,
        spread: 70,
        origin: { y: 0.6 }
    });
}

// Toast notification
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => toast.classList.add('show'), 100);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 400);
    }, 3500);
}

// API wrapper with loading
async function apiCall(endpoint, options = {}) {
    const btn = options.btn;
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = `<span class="loading"><span class="spinner"></span> Loading...</span>`;
    }
    
    try {
        const res = await fetch(API_BASE + endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
        
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return await res.json();
    } catch (error) {
        showToast('Network error: ' + error.message, 'error');
        throw error;
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = btn.dataset.original || btn.textContent;
        }
    }
}

// Load queue data
async function refreshData() {
    try {
        const [waiting, calling, nextbatch, stats] = await Promise.all([
            fetch(API_BASE + '/waiting').then(r => r.json()),
            fetch(API_BASE + '/calling').then(r => r.json()),
            fetch(API_BASE + '/nextbatch').then(r => r.json()),
            fetch(API_BASE + '/stats').then(r => r.json())
        ]);

        // Update current
        if (calling.data?.length) {
            $currentDisplay.textContent = calling.data[0];
            $currentDisplay.classList.add('serving', 'pulse-glow');
            $serveBtn.disabled = false;
        } else {
            $currentDisplay.textContent = '-';
            $currentDisplay.classList.remove('serving', 'pulse-glow');
            $serveBtn.disabled = true;
        }

        // Update lists
        updateList('waiting-list', waiting.data || []);
        updateList('next-list', nextbatch.data || []);

        // Update stats
        updateStats(stats);

        showStatus('Live', 'online');
    } catch (error) {
        showStatus('Connection Error', 'offline');
        showToast('Server unavailable', 'error');
    }
}

// Update list
function updateList(id, data) {
    const ul = document.getElementById(id);
    ul.innerHTML = '';
    data.slice(0, 5).forEach((item, index) => {
        const li = document.createElement('li');
        li.textContent = item;
        li.style.animationDelay = `${index * 0.1}s`;
        ul.appendChild(li);
    });
}

// Update stats
function updateStats(stats) {
    document.querySelectorAll('.stat-number').forEach((el, i) => {
        const keys = ['waiting', 'nextBatch', 'servedToday'];
        el.textContent = stats[keys[i]] || 0;
    });
}

// Status
function showStatus(msg, type) {
    $status.textContent = msg;
    $status.className = `status-${type}`;
}

// Button actions
async function generateNumber() {
    try {
        await apiCall('/generate', { btn: event.target });
        showToast('Number generated! 🎫', 'success');
        refreshData();
    } catch {}
}

async function callNext() {
    try {
        playBeep();
        await apiCall('/callnext', { btn: event.target });
        showToast('Next called! 🔊', 'success');
        refreshData();
    } catch {}
}

async function serveCurrent() {
    const current = $currentDisplay.textContent;
    if (current === '-') return;
    
    try {
        await apiCall(`/serve/${current}`, { btn: event.target });
        celebrate();
        showToast(`${current} served! ✅`, 'success');
        refreshData();
    } catch {}
}

async function resetQueue() {
    if (!confirm('Clear all waiting queues? This cannot be undone.')) return;
    
    try {
        await apiCall('/reset', { btn: event.target });
        showToast('Queue reset! 🗑️', 'warning');
        refreshData();
    } catch {}
}

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
    switch(e.code) {
        case 'KeyG': generateNumber(); break;
        case 'KeyN': callNext(); break;
        case 'KeyS': serveCurrent(); break;
        case 'KeyR': resetQueue(); break;
    }
});

// Init
document.addEventListener('DOMContentLoaded', () => {
    // Save original button text
    document.querySelectorAll('.btn').forEach(btn => {
        btn.dataset.original = btn.innerHTML;
    });
    
    refreshData();
    setInterval(refreshData, 2000);
});

// Confetti (CDN)
function confetti(options) {
    // Simple canvas confetti
    const canvas = document.createElement('canvas');
    canvas.id = 'confetti-canvas';
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    document.body.appendChild(canvas);
    
    const ctx = canvas.getContext('2d');
    const particles = [];
    
    for (let i = 0; i < (options.particleCount || 50); i++) {
        particles.push({
            x: Math.random() * canvas.width,
            y: -10,
            size: Math.random() * 8 + 4,
            speed: Math.random() * 3 + 1,
            color: ['#ff6b6b', '#4ecdc4', '#45b7d1', '#f9ca24', '#f0932b'][Math.floor(Math.random()*5)],
            rotation: Math.random() * 360,
            rotSpeed: Math.random() * 10 - 5
        });
    }
    
    function animate() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
        particles.forEach(p => {
            ctx.save();
            ctx.translate(p.x, p.y);
            ctx.rotate(p.rotation * Math.PI / 180);
            ctx.fillStyle = p.color;
            ctx.fillRect(-p.size/2, -p.size/2, p.size, p.size);
            ctx.restore();
            
            p.y += p.speed;
            p.rotation += p.rotSpeed;
            p.x += Math.sin(p.y * 0.01) * 2;
        });
        
        if (particles.some(p => p.y < canvas.height)) {
            requestAnimationFrame(animate);
        } else {
            canvas.remove();
        }
    }
    
    animate();
}

// Service Worker for offline (future)
if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js');
}
