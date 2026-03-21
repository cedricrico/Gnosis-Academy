(function () {
    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function relativeTimeLabel(value) {
        if (!value) {
            return 'Just now';
        }
        const createdAt = new Date(value);
        if (Number.isNaN(createdAt.getTime())) {
            return 'Just now';
        }
        const diffMs = Date.now() - createdAt.getTime();
        const diffMinutes = Math.max(Math.floor(diffMs / 60000), 0);
        if (diffMinutes < 1) return 'Just now';
        if (diffMinutes < 60) return `${diffMinutes}m ago`;
        const diffHours = Math.floor(diffMinutes / 60);
        if (diffHours < 24) return `${diffHours}h ago`;
        const diffDays = Math.floor(diffHours / 24);
        if (diffDays < 7) return `${diffDays}d ago`;
        return createdAt.toLocaleDateString();
    }

    document.addEventListener('DOMContentLoaded', function () {
        const button = document.getElementById('navbarNotifications');
        const badge = document.getElementById('studentNotificationBadge');
        const list = document.getElementById('studentNotificationList');
        if (!button || !badge || !list) {
            return;
        }

        const studentId = button.getAttribute('data-student-id') || 'student';
        const storageKey = `student-notifications-seen:${studentId}`;
        let latestIds = [];

        function readSeenIds() {
            try {
                const parsed = JSON.parse(window.localStorage.getItem(storageKey) || '[]');
                return Array.isArray(parsed) ? new Set(parsed.map(String)) : new Set();
            } catch {
                return new Set();
            }
        }

        function writeSeenIds(ids) {
            const uniqueIds = Array.from(new Set(ids.map(String))).slice(-200);
            window.localStorage.setItem(storageKey, JSON.stringify(uniqueIds));
        }

        function updateBadge(items) {
            const seenIds = readSeenIds();
            const unreadCount = items.reduce((count, item) => count + (seenIds.has(String(item.id)) ? 0 : 1), 0);
            if (unreadCount > 0) {
                badge.textContent = unreadCount > 9 ? '9+' : String(unreadCount);
                badge.classList.remove('d-none');
            } else {
                badge.textContent = '0';
                badge.classList.add('d-none');
            }
        }

        function renderItems(items) {
            latestIds = items.map(item => String(item.id));
            if (!items.length) {
                list.innerHTML = '<div class="notification-empty">No new class updates right now.</div>';
                updateBadge(items);
                return;
            }

            list.innerHTML = items.map(item => `
                <a class="notification-item" href="${escapeHtml(item.link || '/student/dashboard')}">
                    <span class="notification-type ${escapeHtml(item.type || 'announcement')}">${escapeHtml(item.type || 'update')}</span>
                    <div class="notification-title">${escapeHtml(item.title || 'New update')}</div>
                    <div class="notification-subject">${escapeHtml(item.subject || '')}</div>
                    <div class="notification-message mt-1">${escapeHtml(item.message || '')}</div>
                    <div class="notification-time mt-1">${escapeHtml(relativeTimeLabel(item.createdAt))}</div>
                </a>
            `).join('');
            updateBadge(items);
        }

        async function loadNotifications() {
            list.innerHTML = '<div class="notification-loading">Loading updates...</div>';
            try {
                const response = await fetch('/student/api/notifications', {
                    credentials: 'same-origin',
                    cache: 'no-store'
                });
                if (!response.ok) {
                    throw new Error('Unable to load notifications.');
                }
                const payload = await response.json();
                renderItems(Array.isArray(payload?.items) ? payload.items : []);
            } catch (error) {
                list.innerHTML = '<div class="notification-empty">Unable to load notifications.</div>';
                badge.classList.add('d-none');
            }
        }

        button.closest('.dropdown')?.addEventListener('shown.bs.dropdown', function () {
            const seenIds = readSeenIds();
            latestIds.forEach(id => seenIds.add(id));
            writeSeenIds(Array.from(seenIds));
            updateBadge(latestIds.map(id => ({ id })));
        });

        loadNotifications().catch(() => {});
        window.setInterval(loadNotifications, 60000);
    });
})();
