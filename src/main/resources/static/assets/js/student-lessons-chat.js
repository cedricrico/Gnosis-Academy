document.addEventListener('DOMContentLoaded', function () {
    const launcher = document.getElementById('studentChatLauncher');
    const panel = document.getElementById('studentChatPanel');
    const closeButton = document.getElementById('studentChatCloseBtn');
    const clearButton = document.getElementById('studentChatClearBtn');
    const form = document.getElementById('studentChatForm');
    const input = document.getElementById('studentChatInput');
    const messages = document.getElementById('studentChatMessages');
    const status = document.getElementById('studentChatStatus');

    if (!launcher || !panel || !form || !input || !messages || !status) {
        return;
    }

    const integration = window.studentLessonsChatAdapter || {
        isConfigured: true,
        async sendMessage(payload) {
            const response = await fetch('/student/api/chat/lessons', {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            let data = null;
            try {
                data = await response.json();
            } catch (error) {
                data = null;
            }

            if (!response.ok) {
                const message = data && typeof data.message === 'string'
                    ? data.message
                    : 'Chat request failed.';
                throw new Error(message);
            }

            return data;
        }
    };

    function setExpanded(isOpen) {
        panel.classList.toggle('d-none', !isOpen);
        launcher.setAttribute('aria-expanded', String(isOpen));
        if (isOpen) {
            input.focus();
        }
    }

    function appendMessage(role, text) {
        const bubble = document.createElement('div');
        bubble.className = 'student-chat-message ' + role;
        bubble.textContent = text;
        messages.appendChild(bubble);
        messages.scrollTop = messages.scrollHeight;
    }

    function getLessonContext() {
        const subjectLabel = document.getElementById('selectedLessonSubjectLabel');
        const pageTitle = document.querySelector('h1.h2');

        return {
            page: pageTitle ? pageTitle.textContent.trim() : 'Lessons',
            selectedSubject: subjectLabel ? subjectLabel.textContent.trim() : '',
            url: window.location.pathname
        };
    }

    launcher.addEventListener('click', function () {
        setExpanded(panel.classList.contains('d-none'));
    });

    if (closeButton) {
        closeButton.addEventListener('click', function () {
            setExpanded(false);
        });
    }

    if (clearButton) {
        clearButton.addEventListener('click', function () {
            messages.innerHTML = '';
            appendMessage('assistant', 'Conversation cleared. Ask a new question whenever you are ready.');
            status.textContent = integration.isConfigured
                ? 'Connected to the lesson assistant.'
                : 'Assistant is currently unavailable.';
        });
    }

    input.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    form.addEventListener('submit', async function (event) {
        event.preventDefault();

        const content = input.value.trim();
        if (!content) {
            return;
        }

        appendMessage('user', content);
        input.value = '';
        status.textContent = integration.isConfigured ? 'Waiting for assistant response...' : 'Assistant is currently unavailable.';

        const sendButton = document.getElementById('studentChatSendBtn');
        if (sendButton) {
            sendButton.disabled = true;
        }

        try {
            const response = await integration.sendMessage({
                message: content,
                context: getLessonContext()
            });
            const reply = response && typeof response.message === 'string' && response.message.trim()
                ? response.message.trim()
                : 'No response was returned by the chat handler.';
            appendMessage('assistant', reply);
            status.textContent = integration.isConfigured ? 'Connected to the lesson assistant.' : 'Assistant is currently unavailable.';
        } catch (error) {
            appendMessage('assistant', 'Unable to reach the lesson assistant right now. Please try again in a moment.');
            status.textContent = error && error.message ? error.message : 'Chat request failed.';
        } finally {
            if (sendButton) {
                sendButton.disabled = false;
            }
        }
    });
});
