# Nexus Server (Port 3066)

The self-hosted backend & AI orchestration engine for the **NEXUS** Anti-Procrastination Android app.

---

## 🚀 Quick Start (Docker)

```bash
cd server
docker compose up -d
```

The container starts with persistent SQLite storage in `./data/nexus.db`.

---

## 🌐 Web Control Panel & Admin Panel

Open your browser at:
```
http://<your-server-ip>:3066/
```

### Initial Default Credentials:
- **Username:** `admin`
- **Password:** `nexusadmin123`

*(You can create users and update settings directly inside the Admin Panel).*

---

## 🔑 Features in Web Panel
1. **User Management**: Add user accounts, enable/disable access, view active users.
2. **AI & OpenRouter Config**: Enter your OpenRouter API key (`sk-or-v1-...`) and select your preferred intelligence model (`deepseek/deepseek-r1`, `anthropic/claude-3.5-sonnet`, etc.). Keys are stored securely server-side and never sent to phones.
3. **User Telemetry**: View live schedules, 66-day habit streaks, and friction logs.
4. **Mobile Setup**: Connection instructions and network status.

---

## 📱 Connecting Nothing Phone (3a)
1. Open the **Nexus** app.
2. Enter your server URL: `http://<your-server-ip>:3066`
3. Enter your username & password created in the Admin Panel.
4. Tap **Authenticate**. All data is synchronized from your server with zero local database on device.
