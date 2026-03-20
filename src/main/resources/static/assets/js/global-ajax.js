(function () {
    function readMeta(name) {
        const meta = document.querySelector(`meta[name="${name}"]`);
        return meta ? meta.getAttribute('content') : null;
    }

    function getCsrf() {
        const token = readMeta('_csrf');
        const header = readMeta('_csrf_header') || 'X-CSRF-TOKEN';
        return { token, header };
    }

    async function apiFetchJson(url, options = {}) {
        const headers = new Headers(options.headers || {});
        if (!headers.has('Accept')) {
            headers.set('Accept', 'application/json');
        }

        const csrf = getCsrf();
        if (csrf.token && csrf.header && !headers.has(csrf.header)) {
            headers.set(csrf.header, csrf.token);
        }

        const response = await fetch(url, {
            ...options,
            headers,
            credentials: options.credentials || 'same-origin'
        });

        const contentType = (response.headers.get('content-type') || '').toLowerCase();
        let payload = null;

        try {
            if (contentType.includes('application/json')) {
                payload = await response.json();
            } else {
                payload = await response.text();
            }
        } catch (e) {
            payload = null;
        }

        if (!response.ok) {
            const message = payload && typeof payload === 'object'
                ? (payload.message || payload.error || JSON.stringify(payload))
                : (payload || `Request failed (${response.status}).`);
            const error = new Error(message);
            error.status = response.status;
            error.payload = payload;
            throw error;
        }

        return payload;
    }

    function shouldSkipRefresh() {
        if (document.visibilityState !== 'visible') {
            return true;
        }
        if (document.querySelector('.modal.show')) {
            return true;
        }
        return document.body && document.body.dataset && document.body.dataset.suspendAutoRefresh === 'true';
    }

    function scheduleAutoRefresh() {
        const interval = Number(window.AUTO_REFRESH_INTERVAL_MS) || 60000;
        if (!Number.isFinite(interval) || interval <= 0) {
            return;
        }
        setInterval(() => {
            if (shouldSkipRefresh()) {
                return;
            }
            const refreshFn = window.refreshPageData;
            if (typeof refreshFn === 'function') {
                try {
                    refreshFn();
                } catch (e) {
                    // no-op
                }
            }
        }, interval);
    }

    window.apiFetchJson = apiFetchJson;
    window.registerAutoRefresh = function (fn) {
        if (typeof fn === 'function') {
            window.refreshPageData = fn;
        }
    };

    scheduleAutoRefresh();
})();
