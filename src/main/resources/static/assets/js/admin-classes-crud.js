document.addEventListener('DOMContentLoaded', function () {
    const createClassForm = document.getElementById('createClassForm');
    const subjectsContainer = document.getElementById('subjectsContainer');
    const recentClassesTableBody = document.getElementById('recentClassesTableBody');
    const courseSelect = document.getElementById('courseSelect');
    const sectionNameInput = document.getElementById('sectionName');
    const submitButton = createClassForm ? createClassForm.querySelector('button[type="submit"]') : null;
    const addSubjectBtn = document.getElementById('addSubjectBtn');
    const loadingSpinner = document.querySelector('.loading-spinner');
    const successToast = document.getElementById('successToast');
    const errorToast = document.getElementById('errorToast');

    // Optional UI pieces on Admin-dashboard.html (Class Management section).
    const sectionAccordion = document.getElementById('sectionAccordion');
    const masterlistTableBody = document.getElementById('masterlistTableBody');
    const masterlistSearchInput = document.getElementById('masterlistSearchInput');
    const masterlistClassLabel = document.getElementById('masterlistClassLabel');
    const masterlistTabLink = document.querySelector('.nav-tabs a[href="#masterlistTab"]');

    if (!createClassForm || !subjectsContainer || !recentClassesTableBody || !courseSelect || !sectionNameInput) {
        return;
    }

    const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const defaultSubmitHtml = submitButton ? submitButton.innerHTML : 'Create Class';

    let editingClassId = null;
    let classesCache = [];
    let masterlistCache = [];
    let activeMasterlistClassId = null;
    let uniqueIdSeed = Date.now();
    let instructorOptions = readInstructorOptionsFromDom();

    function nextUniqueId() {
        uniqueIdSeed += 1;
        return uniqueIdSeed;
    }

    function normalizeText(value) {
        return (value || '').trim();
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function showLoading() {
        if (loadingSpinner) {
            loadingSpinner.classList.remove('d-none');
        }
    }

    function hideLoading() {
        if (loadingSpinner) {
            loadingSpinner.classList.add('d-none');
        }
    }

    function showToast(type, message) {
        const toastEl = type === 'success' ? successToast : errorToast;
        if (!toastEl || !window.bootstrap || !bootstrap.Toast) {
            return;
        }
        const body = toastEl.querySelector('.toast-body');
        if (body) {
            body.textContent = message;
        }
        bootstrap.Toast.getOrCreateInstance(toastEl).show();
    }

    function readInstructorOptionsFromDom() {
        const select = subjectsContainer.querySelector('.subject-instructor');
        if (!select) {
            return [];
        }

        return Array.from(select.options)
            .filter(option => normalizeText(option.value) && normalizeText(option.text) !== 'Select Instructor')
            .map(option => ({
                professorId: normalizeText(option.value),
                fullName: normalizeText(option.text)
            }));
    }

    function buildInstructorOptionsHtml(selectedId, selectedName) {
        const selectedIdNormalized = normalizeText(selectedId);
        const selectedNameNormalized = normalizeText(selectedName);
        const seen = new Set();
        const optionsHtml = ['<option value="">Select Instructor</option>'];

        instructorOptions.forEach(option => {
            const value = normalizeText(option.professorId);
            const name = normalizeText(option.fullName);
            if (!value || !name) {
                return;
            }
            seen.add(value);
            const selectedAttr = value === selectedIdNormalized ? ' selected' : '';
            optionsHtml.push(
                `<option value="${escapeHtml(value)}"${selectedAttr}>${escapeHtml(name)}</option>`
            );
        });

        if (selectedIdNormalized && !seen.has(selectedIdNormalized)) {
            const fallbackLabel = selectedNameNormalized || selectedIdNormalized;
            optionsHtml.push(
                `<option value="${escapeHtml(selectedIdNormalized)}" selected>${escapeHtml(fallbackLabel)}</option>`
            );
        }

        return optionsHtml.join('');
    }

    function applyInstructorOptionsToAll() {
        const selects = subjectsContainer.querySelectorAll('.subject-instructor');
        selects.forEach(select => {
            const currentValue = normalizeText(select.value);
            const currentText = normalizeText(select.options[select.selectedIndex]?.text || '');
            select.innerHTML = buildInstructorOptionsHtml(currentValue, currentText);

            if (currentValue) {
                select.value = currentValue;
            }

            if (!select.value && currentText && currentText !== 'Select Instructor') {
                const matchingByText = Array.from(select.options).find(option => normalizeText(option.text) === currentText);
                if (matchingByText) {
                    select.value = matchingByText.value;
                }
            }
        });
    }

    async function fetchJson(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = { ...(options.headers || {}) };
        if (!headers['Content-Type'] && method !== 'GET' && method !== 'HEAD') {
            headers['Content-Type'] = 'application/json';
        }
        if (csrfToken && method !== 'GET' && method !== 'HEAD') {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch(url, {
            credentials: 'same-origin',
            ...options,
            method,
            headers
        });

        let payload = null;
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            payload = await response.json();
        }

        if (!response.ok) {
            const message = payload && payload.message ? payload.message : `Request failed (${response.status})`;
            throw new Error(message);
        }

        return payload;
    }

    function buildScheduleSlotHtml(slot) {
        const slotId = nextUniqueId();
        const selectedDays = Array.isArray(slot.days) ? slot.days : [];
        const startTime = normalizeText(slot.startTime);
        const endTime = normalizeText(slot.endTime);

        const dayCheckboxes = days.map(day => {
            const id = `day-${day.toLowerCase()}-${slotId}`;
            const checked = selectedDays.includes(day) ? ' checked' : '';
            const shortLabel = day.slice(0, 3);
            return `
                <div class="form-check form-check-inline">
                    <input class="form-check-input day-checkbox" type="checkbox" value="${escapeHtml(day)}" id="${escapeHtml(id)}"${checked}>
                    <label class="form-check-label small" for="${escapeHtml(id)}">${escapeHtml(shortLabel)}</label>
                </div>
            `;
        }).join('');

        return `
            <div class="schedule-slot border rounded p-2 mb-2">
                <div class="row align-items-center">
                    <div class="col-md-5">
                        <div class="day-selector">
                            <label class="form-label small">Days:</label>
                            ${dayCheckboxes}
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="row">
                            <div class="col-6">
                                <label class="form-label small">Start Time</label>
                                <input type="time" class="form-control form-control-sm start-time" value="${escapeHtml(startTime)}">
                            </div>
                            <div class="col-6">
                                <label class="form-label small">End Time</label>
                                <input type="time" class="form-control form-control-sm end-time" value="${escapeHtml(endTime)}">
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <button class="btn btn-sm btn-outline-danger remove-slot" type="button">Remove</button>
                    </div>
                </div>
            </div>
        `;
    }

    function buildSubjectEntryHtml(subject) {
        const subjectName = normalizeText(subject.name);
        const subjectCode = normalizeText(subject.code);
        const instructorId = normalizeText(subject.instructorId);
        const instructorName = normalizeText(subject.instructorName);
        const schedule = Array.isArray(subject.schedule) && subject.schedule.length > 0
            ? subject.schedule
            : [{ days: [], startTime: '', endTime: '' }];

        const scheduleSlotsHtml = schedule.map(buildScheduleSlotHtml).join('');

        return `
            <div class="subject-entry mb-3 p-3 border rounded">
                <div class="row align-items-center">
                    <div class="col-md-4">
                        <input type="text" class="form-control subject-name" placeholder="Subject Name" value="${escapeHtml(subjectName)}" required>
                    </div>
                    <div class="col-md-3">
                        <input type="text" class="form-control subject-code" placeholder="Subject Code" value="${escapeHtml(subjectCode)}" required>
                    </div>
                    <div class="col-md-3">
                        <select class="form-select subject-instructor" required>
                            ${buildInstructorOptionsHtml(instructorId, instructorName)}
                        </select>
                    </div>
                    <div class="col-md-2 text-center">
                        <button type="button" class="btn btn-outline-danger remove-subject p-2" style="min-width: 40px;">x</button>
                    </div>
                </div>
                <div class="schedule-section mt-3">
                    <h6>Schedule</h6>
                    <div class="schedule-slots">
                        ${scheduleSlotsHtml}
                    </div>
                    <button class="btn btn-sm btn-outline-primary add-slot-btn" type="button">Add Another Time Slot</button>
                </div>
            </div>
        `;
    }

    function resetSubjectsToDefault() {
        subjectsContainer.innerHTML = buildSubjectEntryHtml({
            name: '',
            code: '',
            instructorId: '',
            instructorName: '',
            schedule: [{ days: [], startTime: '', endTime: '' }]
        });
        applyInstructorOptionsToAll();
    }

    function setSubmitMode(mode) {
        if (!submitButton) {
            return;
        }

        if (mode === 'edit') {
            submitButton.textContent = 'Update Class';
        } else {
            submitButton.innerHTML = defaultSubmitHtml;
        }
    }

    function toPayloadFromForm() {
        const courseCode = normalizeText(courseSelect.value);
        const selectedOption = courseSelect.options[courseSelect.selectedIndex];
        const courseName = normalizeText(selectedOption ? selectedOption.text : '');
        const sectionName = normalizeText(sectionNameInput.value);

        if (!courseCode) {
            throw new Error('Please select a course.');
        }
        if (!sectionName) {
            throw new Error('Please enter a section name.');
        }

        const subjectEntries = subjectsContainer.querySelectorAll('.subject-entry');
        if (subjectEntries.length === 0) {
            throw new Error('Please add at least one subject.');
        }

        const subjects = [];
        const seenCodes = new Set();

        subjectEntries.forEach((entry, index) => {
            const subjectName = normalizeText(entry.querySelector('.subject-name')?.value);
            const subjectCode = normalizeText(entry.querySelector('.subject-code')?.value);
            const instructorSelect = entry.querySelector('.subject-instructor');
            const instructorId = normalizeText(instructorSelect?.value);
            const instructorName = normalizeText(instructorSelect?.options[instructorSelect.selectedIndex]?.text || '');
            const scheduleSlots = entry.querySelectorAll('.schedule-slot');

            if (!subjectName || !subjectCode || !instructorId || !instructorName || instructorName === 'Select Instructor') {
                throw new Error(`Please complete all fields for subject ${index + 1}.`);
            }

            const subjectCodeKey = subjectCode.toUpperCase();
            if (seenCodes.has(subjectCodeKey)) {
                throw new Error(`Duplicate subject code: ${subjectCode}.`);
            }
            seenCodes.add(subjectCodeKey);

            if (scheduleSlots.length === 0) {
                throw new Error(`Please add at least one schedule slot for subject ${subjectCode}.`);
            }

            const schedule = [];
            scheduleSlots.forEach(slot => {
                const checkedDays = Array.from(slot.querySelectorAll('.day-checkbox:checked')).map(checkbox => normalizeText(checkbox.value));
                const startTime = normalizeText(slot.querySelector('.start-time')?.value);
                const endTime = normalizeText(slot.querySelector('.end-time')?.value);

                if (checkedDays.length === 0) {
                    throw new Error(`Please select at least one day for subject ${subjectCode}.`);
                }
                if (!startTime || !endTime) {
                    throw new Error(`Please set start and end time for subject ${subjectCode}.`);
                }
                if (startTime >= endTime) {
                    throw new Error(`End time must be after start time for subject ${subjectCode}.`);
                }

                schedule.push({
                    days: checkedDays,
                    startTime,
                    endTime
                });
            });

            subjects.push({
                name: subjectName,
                code: subjectCode,
                instructorId,
                instructorName,
                schedule
            });
        });

        return {
            courseCode,
            courseName,
            sectionName,
            subjects
        };
    }

    function renderClassRows() {
        if (!Array.isArray(classesCache) || classesCache.length === 0) {
            recentClassesTableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No classes created yet.</td></tr>';
            return;
        }

        const rowsHtml = classesCache.map(schoolClass => {
            const subjects = Array.isArray(schoolClass.subjects) ? schoolClass.subjects : [];

            const subjectsHtml = subjects
                .map(subject => `<span class="badge bg-primary me-1 mb-1">${escapeHtml(subject.name)}</span>`)
                .join(' ');

            const codesHtml = subjects
                .map(subject => `<span class="badge bg-secondary me-1 mb-1">${escapeHtml(subject.code)}</span>`)
                .join(' ');

            const instructorsHtml = subjects
                .map(subject => {
                    const instructorName = normalizeText(subject.instructorName) || 'Unknown Instructor';
                    const subjectCode = normalizeText(subject.code);
                    return `<div>${escapeHtml(instructorName)}${subjectCode ? ` <small class="text-muted">(${escapeHtml(subjectCode)})</small>` : ''}</div>`;
                })
                .join('');

            return `
                <tr data-class-id="${escapeHtml(schoolClass.id)}">
                    <td>${escapeHtml(schoolClass.courseName)}</td>
                    <td>${subjectsHtml || '<span class="text-muted">-</span>'}</td>
                    <td>${codesHtml || '<span class="text-muted">-</span>'}</td>
                    <td>${instructorsHtml || '<span class="text-muted">-</span>'}</td>
                    <td>${escapeHtml(schoolClass.sectionName)}</td>
                    <td>
                        <button type="button" class="btn btn-outline-primary btn-sm" data-class-action="edit" data-class-id="${escapeHtml(schoolClass.id)}" aria-label="Edit class">
                            <svg class="bi bi-pencil" xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" fill="currentColor" viewBox="0 0 16 16">
                                <path d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708l-10 10a.5.5 0 0 1-.168.11l-5 2a.5.5 0 0 1-.65-.65l2-5a.5.5 0 0 1 .11-.168l10-10zM11.207 2.5 13.5 4.793 14.793 3.5 12.5 1.207zm1.586 3L10.5 3.207 4 9.707V10h.5a.5.5 0 0 1 .5.5v.5h.5a.5.5 0 0 1 .5.5v.5h.293zm-9.761 5.175-.106.106-1.528 3.821 3.821-1.528.106-.106A.5.5 0 0 1 5 12.5V12h-.5a.5.5 0 0 1-.5-.5V11h-.5a.5.5 0 0 1-.468-.325z"></path>
                            </svg>
                        </button>
                        <button type="button" class="btn btn-outline-danger btn-sm" data-class-action="delete" data-class-id="${escapeHtml(schoolClass.id)}" aria-label="Delete class">
                            <svg class="bi bi-trash" xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" fill="currentColor" viewBox="0 0 16 16">
                                <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0z"></path>
                                <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4zM2.5 3h11V2h-11z"></path>
                            </svg>
                        </button>
                    </td>
                </tr>
            `;
        }).join('');

        recentClassesTableBody.innerHTML = rowsHtml;
    }

    function renderSectionAccordion() {
        if (!sectionAccordion) {
            return;
        }

        if (!Array.isArray(classesCache) || classesCache.length === 0) {
            sectionAccordion.innerHTML = `
                <div class="text-muted small p-3">
                    No classes found yet. Create a class to populate the section table.
                </div>
            `;
            return;
        }

        sectionAccordion.innerHTML = classesCache.map((schoolClass, index) => {
            const classId = escapeHtml(schoolClass.id);
            const collapseId = `sectionClass-${classId}`;
            const headingId = `headingClass-${classId}`;
            const show = index === 0 ? ' show' : '';
            const expanded = index === 0 ? 'true' : 'false';
            const collapsed = index === 0 ? '' : ' collapsed';

            const subjects = Array.isArray(schoolClass.subjects) ? schoolClass.subjects : [];
            const subjectSummary = subjects.length > 0
                ? subjects.map(subject => escapeHtml(subject.code || subject.name || '-')).join(', ')
                : '-';

            return `
                <div class="accordion-item">
                    <h2 class="accordion-header" id="${headingId}">
                        <button class="accordion-button${collapsed}" type="button" data-bs-toggle="collapse" data-bs-target="#${collapseId}" aria-expanded="${expanded}" aria-controls="${collapseId}">
                            ${escapeHtml(schoolClass.courseName)} - ${escapeHtml(schoolClass.sectionName)}
                        </button>
                    </h2>
                    <div id="${collapseId}" class="accordion-collapse collapse${show}" data-bs-parent="#sectionAccordion" aria-labelledby="${headingId}">
                        <div class="accordion-body">
                            <p class="mb-1"><strong>Subjects:</strong> <span class="text-muted">${subjectSummary}</span></p>
                            <div class="btn-group w-100" role="group">
                                <button type="button" class="btn btn-outline-primary" data-admin-action="open-masterlist" data-class-id="${classId}">Open Masterlist</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    }

    function showMasterlistTab() {
        if (masterlistTabLink && window.bootstrap && bootstrap.Tab) {
            bootstrap.Tab.getOrCreateInstance(masterlistTabLink).show();
            return;
        }

        const masterlistTab = document.getElementById('masterlistTab');
        if (masterlistTab) {
            document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('show', 'active'));
            masterlistTab.classList.add('show', 'active');
        }
    }

    function renderMasterlistRows(filterText) {
        if (!masterlistTableBody) {
            return;
        }

        const query = normalizeText(filterText).toLowerCase();
        const rows = (Array.isArray(masterlistCache) ? masterlistCache : []).filter(student => {
            if (!query) {
                return true;
            }
            const id = normalizeText(student.studentId).toLowerCase();
            const name = normalizeText(student.fullName).toLowerCase();
            return id.includes(query) || name.includes(query);
        });

        if (rows.length === 0) {
            masterlistTableBody.innerHTML = `
                <tr>
                    <td colspan="3" class="text-muted small">No students found.</td>
                </tr>
            `;
            return;
        }

        masterlistTableBody.innerHTML = rows.map(student => {
            const status = normalizeText(student.status) || 'Active';
            const badgeClass = status.toLowerCase().includes('drop') ? 'bg-warning' : 'bg-success';
            return `
                <tr>
                    <td>${escapeHtml(student.studentId)}</td>
                    <td>${escapeHtml(student.fullName)}</td>
                    <td><span class="badge ${badgeClass}">${escapeHtml(status)}</span></td>
                </tr>
            `;
        }).join('');
    }

    async function loadMasterlistForClass(classId) {
        if (!classId) {
            return;
        }

        showLoading();
        try {
            const data = await fetchJson(`/api/admin/classes/${classId}/masterlist`);
            masterlistCache = Array.isArray(data) ? data : [];
            activeMasterlistClassId = classId;

            const classItem = classesCache.find(item => Number(item.id) === Number(classId));
            if (masterlistClassLabel) {
                masterlistClassLabel.textContent = classItem
                    ? `(${classItem.courseName} - ${classItem.sectionName})`
                    : '(Selected class)';
            }

            renderMasterlistRows(masterlistSearchInput ? masterlistSearchInput.value : '');
        } catch (error) {
            showToast('error', error.message || 'Unable to load masterlist.');
        } finally {
            hideLoading();
        }
    }

    function setFormForEditing(schoolClass) {
        editingClassId = schoolClass.id;
        setSubmitMode('edit');

        if (courseSelect.value !== schoolClass.courseCode) {
            const hasCourseOption = Array.from(courseSelect.options).some(option => option.value === schoolClass.courseCode);
            if (!hasCourseOption) {
                const option = document.createElement('option');
                option.value = schoolClass.courseCode;
                option.textContent = schoolClass.courseName;
                courseSelect.appendChild(option);
            }
            courseSelect.value = schoolClass.courseCode;
        }

        sectionNameInput.value = normalizeText(schoolClass.sectionName);
        const subjects = Array.isArray(schoolClass.subjects) ? schoolClass.subjects : [];
        if (subjects.length === 0) {
            resetSubjectsToDefault();
        } else {
            subjectsContainer.innerHTML = subjects.map(buildSubjectEntryHtml).join('');
        }

        applyInstructorOptionsToAll();
        createClassForm.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function resetFormToCreateMode() {
        editingClassId = null;
        createClassForm.reset();
        setSubmitMode('create');
        resetSubjectsToDefault();
    }

    async function refreshClasses() {
        classesCache = await fetchJson('/api/admin/classes');
        if (!Array.isArray(classesCache)) {
            classesCache = [];
        }
        renderClassRows();
        renderSectionAccordion();

        if (activeMasterlistClassId) {
            const stillExists = classesCache.some(item => Number(item.id) === Number(activeMasterlistClassId));
            if (!stillExists) {
                activeMasterlistClassId = null;
                masterlistCache = [];
                if (masterlistClassLabel) {
                    masterlistClassLabel.textContent = '(No class selected)';
                }
                renderMasterlistRows(masterlistSearchInput ? masterlistSearchInput.value : '');
            }
        }
    }

    async function refreshInstructorOptions() {
        const fetched = await fetchJson('/api/admin/professors');
        if (Array.isArray(fetched) && fetched.length > 0) {
            instructorOptions = fetched;
            applyInstructorOptionsToAll();
        }
    }

    async function handleClassSubmit(event) {
        event.preventDefault();
        event.stopImmediatePropagation();

        let payload;
        try {
            payload = toPayloadFromForm();
        } catch (validationError) {
            showToast('error', validationError.message);
            return;
        }

        showLoading();
        try {
            if (editingClassId) {
                await fetchJson(`/api/admin/classes/${editingClassId}`, {
                    method: 'PUT',
                    body: JSON.stringify(payload)
                });
                showToast('success', 'Class updated successfully.');
            } else {
                await fetchJson('/api/admin/classes', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showToast('success', 'Class created successfully.');
            }

            resetFormToCreateMode();
            await refreshClasses();
        } catch (error) {
            showToast('error', error.message || 'Unable to save class.');
        } finally {
            hideLoading();
        }
    }

    createClassForm.addEventListener('submit', handleClassSubmit, true);

    recentClassesTableBody.addEventListener('click', async function (event) {
        const actionButton = event.target.closest('[data-class-action]');
        if (!actionButton) {
            return;
        }

        const classId = Number(actionButton.getAttribute('data-class-id'));
        if (!classId) {
            return;
        }

        const classItem = classesCache.find(item => Number(item.id) === classId);
        if (!classItem) {
            showToast('error', 'Class not found.');
            return;
        }

        const action = actionButton.getAttribute('data-class-action');
        if (action === 'edit') {
            setFormForEditing(classItem);
            return;
        }

        if (action === 'delete') {
            const confirmed = await window.confirmAsync(`Delete class ${classItem.courseName} - ${classItem.sectionName}?`);
            if (!confirmed) {
                return;
            }

            showLoading();
            try {
                await fetchJson(`/api/admin/classes/${classId}`, { method: 'DELETE' });
                showToast('success', 'Class deleted successfully.');
                if (editingClassId === classId) {
                    resetFormToCreateMode();
                }
                await refreshClasses();
            } catch (error) {
                showToast('error', error.message || 'Unable to delete class.');
            } finally {
                hideLoading();
            }
        }
    });

    if (sectionAccordion) {
        sectionAccordion.addEventListener('click', function (event) {
            const button = event.target.closest('[data-admin-action="open-masterlist"]');
            if (!button) {
                return;
            }

            const classId = Number(button.getAttribute('data-class-id'));
            if (!classId) {
                return;
            }

            showMasterlistTab();
            loadMasterlistForClass(classId);
        });
    }

    if (masterlistSearchInput) {
        masterlistSearchInput.addEventListener('input', function () {
            renderMasterlistRows(masterlistSearchInput.value);
        });
    }

    subjectsContainer.addEventListener('click', function (event) {
        const removeButton = event.target.closest('.remove-subject');
        if (!removeButton) {
            return;
        }
        const subjectEntries = subjectsContainer.querySelectorAll('.subject-entry');
        if (subjectEntries.length <= 1) {
            return;
        }
        const entry = removeButton.closest('.subject-entry');
        if (entry) {
            entry.remove();
        }
    });

    if (addSubjectBtn) {
        addSubjectBtn.addEventListener('click', function () {
            setTimeout(applyInstructorOptionsToAll, 0);
        });
    }

    (async function initializeClassCrud() {
        showLoading();
        try {
            try {
                await refreshInstructorOptions();
            } catch (_ignored) {
                // Fall back to instructors already present in the HTML.
            }
            await refreshClasses();
            applyInstructorOptionsToAll();
        } catch (error) {
            showToast('error', error.message || 'Unable to load class data.');
        } finally {
            hideLoading();
        }
    })();
});
