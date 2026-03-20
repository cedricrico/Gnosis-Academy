(function () {
    const pending = [];

    function ensureAjax() {
        if (window.apiFetchJson) {
            return;
        }
        if (document.getElementById('ajax-global')) {
            return;
        }
        const script = document.createElement('script');
        script.id = 'ajax-global';
        script.src = '/assets/js/global-ajax.js';
        document.head.appendChild(script);
    }

    function resolveText(message) {
        if (message && typeof message === 'object') {
            if (message.message) return String(message.message);
            if (message.error) return String(message.error);
            try {
                return JSON.stringify(message);
            } catch (e) {
                return String(message);
            }
        }
        return String(message);
    }

    function classify(text) {
        const lower = text.toLowerCase();
        if (/(success|successfully|updated|created|uploaded|posted|saved|published)/.test(lower)) {
            return 'success';
        }
        if (/(unable|failed|error|not found|unauthorized|invalid|exceeded)/.test(lower)) {
            return 'error';
        }
        if (/(please|required|select|must|need to)/.test(lower)) {
            return 'warning';
        }
        return 'info';
    }

    function show(text) {
        if (window.Swal) {
            Swal.fire({ icon: classify(text), text });
        } else {
            console.log(text);
        }
    }

    function ensureSwal(callback) {
        if (window.Swal) {
            callback();
            return;
        }
        pending.push(callback);
        if (document.getElementById('swal-cdn')) {
            return;
        }
        const script = document.createElement('script');
        script.id = 'swal-cdn';
        script.src = 'https://cdn.jsdelivr.net/npm/sweetalert2@11';
        script.onload = function () {
            while (pending.length) {
                const next = pending.shift();
                try {
                    next();
                } catch (e) {
                    // no-op
                }
            }
        };
        document.head.appendChild(script);
    }

    window.alert = function (message) {
        const text = resolveText(message);
        ensureSwal(function () {
            show(text);
        });
    };

    window.confirmAsync = function (message) {
        const text = resolveText(message);
        return new Promise(function (resolve) {
            ensureSwal(function () {
                if (!window.Swal) {
                    resolve(true);
                    return;
                }
                Swal.fire({
                    icon: 'warning',
                    text,
                    showCancelButton: true,
                    confirmButtonText: 'OK',
                    cancelButtonText: 'Cancel'
                }).then(function (result) {
                    resolve(!!result.isConfirmed);
                });
            });
        });
    };

    ensureAjax();
})();
