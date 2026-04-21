# 🚀 Smart Queue Management System v4.0 (PWA)

**Responsive queue app** - Live control + external display. Works on phone/laptop/monitor!

[![Demo Live](https://img.shields.io/badge/Live_Demo-localhost:8080-blue)](http://localhost:8080/live.html) [![GitHub Repo](https://img.shields.io/badge/Repo-jaaa--vs/Jarell--smart--queue--management-green)](https://github.com/jaaa-vs/Jarell-smart-queue-management)

## 🎯 Features
- ✅ **Fully Responsive** - Mobile/tablet/desktop/4K auto-scale
- ✅ **PWA** - Install as app ("Add to Home Screen")
- ✅ Live queue control + external display
- ✅ MySQL backend + REST API
- ✅ Java desktop + web interfaces

## 📱 Download Options

### 1. **PWA (Recommended - Mobile/Desktop)**
```
1. Run: java QueueApp
2. Visit: localhost:8080 
3. Install: Chrome/Edge → ⋮ → "Install Smart Queue" / "Add to Home Screen"
```
→ Instant app with offline icons!

### 2. **Desktop ZIP (All files)**
```
# Download from GitHub
github.com/jaaa-vs/Jarell-smart-queue-management → Code → Download ZIP
```
→ Extract → `java QueueApp` → Done!

### 3. **Single EXE (Future)**
Launch4j → QueueApp.jar → SmartQueue.exe

## 🚀 Quick Start (Local)

### Prerequisites
```
Java 17+ | MySQL | (setup.sql included)
```

```
1. java -cp ".;*.jar" QueueApp
OR double-click run.bat
2. Browser: localhost:8080/
   - /live.html = Control panel
   - /display.html = External monitor (F11 fullscreen)
```

## 📊 Responsive Breakpoints
| Device | Width | Layout |
|--------|-------|--------|
| Mobile | <480px | Stacked touch |
| Tablet | 768px | 2-col |
| Desktop | 1024px | Standard |
| External | >1920px | Large bold |

## 🛠️ Files Structure
```
├── run.bat (double-click)
├── *.java (compile)
├── *.jar (libs)
├── web/ (PWA - style.css responsive)
└── setup.sql (DB)
```

## 🔗 Live URLs (when running)
```
Live Control: http://localhost:8080/live.html
External Display: http://localhost:8080/display.html
Admin: http://localhost:8080/index.html
API: http://localhost:8080/api/
```

**Enjoy your responsive queue system! 🎉**

---

⭐ Star repo if useful!

