document.addEventListener('DOMContentLoaded', function() {
    const state = {
        cards: [],
        selectedQuizId: null,
        selectedQuiz: null,
        rows: [],
        filteredRows: [],
        page: 1,
        pageSize: 10
    };

    const elements = {
        refreshButton: document.getElementById('refreshQuizResultsBtn'),
        cardsLoading: document.getElementById('quizCardsLoading'),
        cardsError: document.getElementById('quizCardsError'),
        cardsEmpty: document.getElementById('quizCardsEmpty'),
        cardsGrid: document.getElementById('quizCardsGrid'),
        cardsCount: document.getElementById('quizCardsCount'),
        placeholder: document.getElementById('resultTablePlaceholder'),
        tableLoading: document.getElementById('resultTableLoading'),
        tableError: document.getElementById('resultTableError'),
        tableEmpty: document.getElementById('resultTableEmpty'),
        tableWrapper: document.getElementById('resultTableWrapper'),
        tableBody: document.getElementById('quizResultsTableBody'),
        tableTitle: document.getElementById('resultTableTitle'),
        tableSubtitle: document.getElementById('resultTableSubtitle'),
        searchInput: document.getElementById('resultSearchInput'),
        sortSelect: document.getElementById('resultSortSelect'),
        prevPageButton: document.getElementById('resultPrevPageBtn'),
        nextPageButton: document.getElementById('resultNextPageBtn'),
        paginationInfo: document.getElementById('resultPaginationInfo'),
        selectedSubject: document.getElementById('selectedQuizSubject'),
        selectedSection: document.getElementById('selectedQuizSection'),
        selectedStudents: document.getElementById('selectedQuizStudents'),
        selectedTopScore: document.getElementById('selectedQuizTopScore')
    };

    function normalize(value) {
        return String(value ?? '').trim();
    }

    async function fetchJson(url) {
        const response = await fetch(url, { cache: 'no-store' });
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(payload && payload.message ? payload.message : 'Request failed.');
        }
        return response.json();
    }

    function setCardsState(mode) {
        elements.cardsLoading.classList.toggle('d-none', mode !== 'loading');
        elements.cardsError.classList.toggle('d-none', mode !== 'error');
        elements.cardsEmpty.classList.toggle('d-none', mode !== 'empty');
        elements.cardsGrid.classList.toggle('d-none', mode !== 'ready');
    }

    function setTableState(mode) {
        elements.placeholder.classList.toggle('d-none', mode !== 'placeholder');
        elements.tableLoading.classList.toggle('d-none', mode !== 'loading');
        elements.tableError.classList.toggle('d-none', mode !== 'error');
        elements.tableEmpty.classList.toggle('d-none', mode !== 'empty');
        elements.tableWrapper.classList.toggle('d-none', mode !== 'ready');
    }

    function formatScore(row) {
        const correctAnswers = Number(row.correctAnswers ?? 0);
        const totalQuestions = Number(row.totalQuestions ?? 0);
        const percent = Number(row.scorePercent ?? 0);
        return {
            text: `${correctAnswers}/${totalQuestions}`,
            percentText: `${Math.round(percent)}%`
        };
    }

    function buildQuizCard(card) {
        const column = document.createElement('div');
        column.className = 'col-12 quiz-row-divider';

        const description = normalize(card.description) || 'No quiz description provided.';
        const isActive = String(card.id) === String(state.selectedQuizId);
        const status = normalize(card.status || 'Draft');
        const statusClass = status.toLowerCase().replace(/\s+/g, '-');

        column.innerHTML = `
            <div class="card quiz-result-card${isActive ? ' active' : ''}" data-quiz-id="${card.id}">
                <div class="card-body">
                    <div class="quiz-row-main">
                        <div class="quiz-card-header">
                            <div class="quiz-card-title-block">
                                <h5 class="card-title quiz-result-title mb-1">${escapeHtml(card.title || 'Untitled Quiz')}</h5>
                                <div class="quiz-result-subject">${escapeHtml(card.subject || 'No subject')}</div>
                            </div>
                        </div>
                        <div class="quiz-result-meta mt-2">
                            <span class="badge text-bg-light border">${escapeHtml(card.section || 'All Sections')}</span>
                            <span class="badge text-bg-light border">${Number(card.studentCount ?? 0)} students</span>
                        </div>
                        <p class="quiz-result-description mb-0 mt-2">${escapeHtml(description)}</p>
                    </div>
                    <div>
                        <span class="badge text-bg-light border quiz-status-badge ${escapeHtml(statusClass)}">${escapeHtml(status || 'Draft')}</span>
                    </div>
                    <div class="quiz-result-kpi">
                        <div class="quiz-attempt-count">${Number(card.studentCount ?? 0)}/${Number(card.enrolledCount ?? 0)}</div>
                        <div class="quiz-attempt-label">Students Taken</div>
                    </div>
                    <div class="quiz-result-actions">
                        <button class="btn quiz-action-btn${isActive ? ' view-active' : ''}" type="button" data-action="view" data-quiz-id="${card.id}" aria-label="View quiz results">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                                <path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8M1.173 8a13 13 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5s3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.12 12.5 8 12.5s-3.879-1.168-5.168-2.457A13 13 0 0 1 1.172 8z"></path>
                                <path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5"></path>
                            </svg>
                        </button>
                    </div>
                </div>
            </div>
        `;
        return column;
    }

    function renderQuizCards() {
        elements.cardsGrid.innerHTML = '';
        elements.cardsCount.textContent = `${state.cards.length} quizzes`;
        if (!state.cards.length) {
            setCardsState('empty');
            return;
        }
        state.cards.forEach(card => {
            elements.cardsGrid.appendChild(buildQuizCard(card));
        });
        setCardsState('ready');
    }

    function updateSelectedSummary() {
        const quiz = state.selectedQuiz;
        if (!quiz) {
            elements.tableTitle.textContent = 'Student Results';
            elements.tableSubtitle.textContent = 'Select a quiz card to inspect attempts.';
            elements.selectedSubject.textContent = '-';
            elements.selectedSection.textContent = '-';
            elements.selectedStudents.textContent = '0';
            elements.selectedTopScore.textContent = '-';
            return;
        }
        elements.tableTitle.textContent = quiz.title || 'Student Results';
        elements.tableSubtitle.textContent = normalize(quiz.description) || 'Review each student\'s best score and total attempts for this quiz.';
        elements.selectedSubject.textContent = normalize(quiz.subject) || '-';
        elements.selectedSection.textContent = normalize(quiz.section) || 'All Sections';
        elements.selectedStudents.textContent = String((quiz.rows || []).length);
        const topScore = (quiz.rows || []).reduce((best, row) => Math.max(best, Number(row.scorePercent ?? 0)), 0);
        elements.selectedTopScore.textContent = quiz.rows && quiz.rows.length ? `${Math.round(topScore)}%` : '-';
    }

    function applyFilters() {
        const searchTerm = normalize(elements.searchInput.value).toLowerCase();
        const sortValue = normalize(elements.sortSelect.value) || 'highest';
        const baseRows = Array.isArray(state.rows) ? [...state.rows] : [];

        state.filteredRows = baseRows.filter(row => {
            if (!searchTerm) {
                return true;
            }
            const haystack = [
                row.studentId,
                row.studentName,
                row.course,
                row.section
            ].map(value => normalize(value).toLowerCase()).join(' ');
            return haystack.includes(searchTerm);
        });

        state.filteredRows.sort((left, right) => {
            if (sortValue === 'lowest') {
                return Number(left.scorePercent ?? 0) - Number(right.scorePercent ?? 0);
            }
            if (sortValue === 'name_asc') {
                return normalize(left.studentName).localeCompare(normalize(right.studentName));
            }
            if (sortValue === 'name_desc') {
                return normalize(right.studentName).localeCompare(normalize(left.studentName));
            }
            if (sortValue === 'attempts_desc') {
                return Number(right.attemptsTaken ?? 0) - Number(left.attemptsTaken ?? 0);
            }
            if (sortValue === 'attempts_asc') {
                return Number(left.attemptsTaken ?? 0) - Number(right.attemptsTaken ?? 0);
            }
            return Number(right.scorePercent ?? 0) - Number(left.scorePercent ?? 0);
        });

        state.page = 1;
        renderResultsTable();
    }

    function buildScoreCell(row) {
        const score = formatScore(row);
        const flags = [];
        if (row.highestScorer) {
            flags.push('<span class="badge text-bg-success">Highest</span>');
        }
        if (row.lowScore) {
            flags.push('<span class="badge text-bg-danger">Needs Review</span>');
        }
        return `
            <div class="score-cell">
                <div class="score-pill">${score.text} <span class="text-muted fw-normal">(${score.percentText})</span></div>
                <div class="score-flags">${flags.join(' ')}</div>
            </div>
        `;
    }

    function renderResultsTable() {
        const rows = state.filteredRows;
        if (!state.selectedQuiz) {
            setTableState('placeholder');
            updateSelectedSummary();
            return;
        }
        updateSelectedSummary();
        if (!rows.length) {
            setTableState(state.rows.length ? 'empty' : 'empty');
            elements.paginationInfo.textContent = 'Showing 0 of 0 students';
            elements.prevPageButton.disabled = true;
            elements.nextPageButton.disabled = true;
            elements.tableEmpty.textContent = state.rows.length
                ? 'No students match the current filter.'
                : 'No student attempts have been recorded for this quiz yet.';
            return;
        }

        const startIndex = (state.page - 1) * state.pageSize;
        const pageRows = rows.slice(startIndex, startIndex + state.pageSize);
        elements.tableBody.innerHTML = '';
        pageRows.forEach(row => {
            const tableRow = document.createElement('tr');
            tableRow.innerHTML = `
                <td>
                    <div class="student-cell-name">${escapeHtml(row.studentName || '-')}</div>
                    <div class="student-cell-id">${escapeHtml(row.studentId || '-')}</div>
                </td>
                <td>
                    <div class="student-cell-name">${escapeHtml(row.course || '-')}</div>
                    <div class="student-cell-meta">${escapeHtml(row.section || '-')}</div>
                </td>
                <td><span class="attempt-pill">${Number(row.attemptsTaken ?? 0)}</span></td>
                <td>${buildScoreCell(row)}</td>
            `;
            elements.tableBody.appendChild(tableRow);
        });

        const totalPages = Math.max(Math.ceil(rows.length / state.pageSize), 1);
        elements.paginationInfo.textContent = `Showing ${startIndex + 1}-${Math.min(startIndex + pageRows.length, rows.length)} of ${rows.length} students`;
        elements.prevPageButton.disabled = state.page <= 1;
        elements.nextPageButton.disabled = state.page >= totalPages;
        setTableState('ready');
    }

    async function loadQuizCards() {
        setCardsState('loading');
        try {
            const cards = await fetchJson('/api/quizzes');
            state.cards = Array.isArray(cards) ? cards : [];
            renderQuizCards();
            if (!state.cards.length) {
                state.selectedQuizId = null;
                state.selectedQuiz = null;
                state.rows = [];
                state.filteredRows = [];
                renderResultsTable();
                return;
            }

            const firstCard = state.cards.find(card => String(card.id) === String(state.selectedQuizId)) || state.cards[0];
            await loadQuizDetails(firstCard.id, false);
        } catch (error) {
            console.error(error);
            setCardsState('error');
            setTableState('placeholder');
        }
    }

    async function loadQuizDetails(quizId, scrollIntoView) {
        state.selectedQuizId = quizId;
        renderQuizCards();
        setTableState('loading');
        try {
            const quiz = await fetchJson(`/api/quizzes/${quizId}/results`);
            state.selectedQuiz = quiz;
            state.rows = Array.isArray(quiz.rows) ? quiz.rows : [];
            elements.searchInput.value = '';
            elements.sortSelect.value = 'highest';
            applyFilters();
            if (scrollIntoView) {
                elements.tableTitle.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        } catch (error) {
            console.error(error);
            state.selectedQuiz = null;
            state.rows = [];
            state.filteredRows = [];
            setTableState('error');
        }
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll('\'', '&#39;');
    }

    elements.cardsGrid.addEventListener('click', function(event) {
        const viewButton = event.target.closest('[data-action="view"]');
        if (viewButton) {
            const quizId = viewButton.getAttribute('data-quiz-id');
            if (quizId) {
                loadQuizDetails(quizId, true).catch(error => {
                    console.error(error);
                });
            }
            return;
        }

        const card = event.target.closest('.quiz-result-card[data-quiz-id]');
        if (!card) {
            return;
        }
        const quizId = card.getAttribute('data-quiz-id');
        if (!quizId) {
            return;
        }
        loadQuizDetails(quizId, true).catch(error => {
            console.error(error);
        });
    });

    elements.refreshButton.addEventListener('click', function() {
        loadQuizCards().catch(error => {
            console.error(error);
            if (window.alert) {
                window.alert(error.message || 'Unable to refresh quiz results.');
            }
        });
    });

    elements.searchInput.addEventListener('input', applyFilters);
    elements.sortSelect.addEventListener('change', applyFilters);
    elements.prevPageButton.addEventListener('click', function() {
        if (state.page <= 1) {
            return;
        }
        state.page -= 1;
        renderResultsTable();
    });
    elements.nextPageButton.addEventListener('click', function() {
        const totalPages = Math.max(Math.ceil(state.filteredRows.length / state.pageSize), 1);
        if (state.page >= totalPages) {
            return;
        }
        state.page += 1;
        renderResultsTable();
    });

    loadQuizCards().catch(error => {
        console.error(error);
        setCardsState('error');
    });

    window.setInterval(function() {
        if (!state.selectedQuizId) {
            loadQuizCards().catch(() => {});
            return;
        }
        Promise.all([
            fetchJson('/api/quizzes').then(cards => {
                state.cards = Array.isArray(cards) ? cards : [];
                renderQuizCards();
            }),
            fetchJson(`/api/quizzes/${state.selectedQuizId}/results`).then(quiz => {
                state.selectedQuiz = quiz;
                state.rows = Array.isArray(quiz.rows) ? quiz.rows : [];
                applyFilters();
            })
        ]).catch(() => {});
    }, 30000);
});
