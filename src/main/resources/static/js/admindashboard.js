document.addEventListener('DOMContentLoaded', function() {
    // Create overlay for mobile sidebar
    const overlay = document.createElement('div');
    overlay.className = 'sidebar-overlay';
    document.body.appendChild(overlay);
    
    // Sidebar Toggle
    const sidebarCollapse = document.getElementById('sidebarCollapse');
    const sidebar = document.getElementById('sidebar');
    const content = document.getElementById('content');
    
    function toggleSidebar() {
        sidebar.classList.toggle('active');
        content.classList.toggle('active');
        overlay.classList.toggle('active');
    }
    
    function closeSidebar() {
        sidebar.classList.remove('active');
        content.classList.remove('active');
        overlay.classList.remove('active');
    }
    
    if (sidebarCollapse) {
        sidebarCollapse.addEventListener('click', function(e) {
            e.stopPropagation();
            toggleSidebar();
        });
    }
    
    // Close sidebar when clicking on overlay
    overlay.addEventListener('click', function() {
        closeSidebar();
    });
    
    // Close sidebar when clicking on content on mobile
    if (content) {
        content.addEventListener('click', function(e) {
            if (window.innerWidth <= 992 && sidebar.classList.contains('active')) {
                // Only close if clicking outside the sidebar
                if (!e.target.closest('#sidebar')) {
                    closeSidebar();
                }
            }
        });
    }
    
    // Close sidebar when window is resized to desktop size
    window.addEventListener('resize', function() {
        if (window.innerWidth > 992 && sidebar.classList.contains('active')) {
            closeSidebar();
        }
    });

    // Navigation Handling
    const navLinks = document.querySelectorAll('.sidebar a');
    const contentSections = document.querySelectorAll('.content-section');

    function activateSection(targetSection) {
        if (!targetSection) {
            return;
        }

        const targetEl = document.querySelector(targetSection);
        if (!targetEl) {
            return;
        }

        navLinks.forEach(l => {
            if (l.parentElement) {
                l.parentElement.classList.remove('active');
            }
        });
        contentSections.forEach(section => section.classList.remove('active'));

        const matchingLink = document.querySelector(`.sidebar a[href="${targetSection}"]`);
        if (matchingLink && matchingLink.parentElement) {
            matchingLink.parentElement.classList.add('active');
        }

        targetEl.classList.add('active');
    }

    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();

            const targetSection = this.getAttribute('href');
            if (!targetSection) {
                return;
            }

            activateSection(targetSection);
            if (window.location.hash !== targetSection) {
                window.location.hash = targetSection;
            }
        });
    });

    if (window.location.hash) {
        activateSection(window.location.hash);
    }

    window.addEventListener('hashchange', function() {
        activateSection(window.location.hash);
    });

    // Subject Tags Management
    const addMultipleBtn = document.getElementById('addMultipleSubjects');
    const subjectTagsContainer = document.getElementById('subjectTagsContainer');
    const tagInput = document.getElementById('tagInput');
    const addTagBtn = document.getElementById('addTag');
    const subjectInput = document.getElementById('subjectInput');
    
    if (addMultipleBtn) {
        addMultipleBtn.addEventListener('click', function() {
            subjectTagsContainer.classList.toggle('d-none');
            subjectInput.disabled = !subjectInput.disabled;
        });
    }
    
    function addTag(tagText) {
        if (tagText.trim() === '') return;
        
        const tag = document.createElement('span');
        tag.className = 'subject-tag';
        tag.innerHTML = `${tagText} <span class="remove-tag">&times;</span>`;
        
        const removeBtn = tag.querySelector('.remove-tag');
        removeBtn.addEventListener('click', function() {
            tag.remove();
        });
        
        subjectTagsContainer.querySelector('.d-flex').appendChild(tag);
        tagInput.value = '';
    }
    
    if (addTagBtn) {
        addTagBtn.addEventListener('click', function() {
            addTag(tagInput.value);
        });
    }
    
    if (tagInput) {
        tagInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                addTag(this.value);
            }
        });
    }
    // Class create/edit/delete is handled by admin-classes-crud.js (API-backed).

    // Toast Notification System
    const successToast = document.getElementById('successToast');
    const errorToast = document.getElementById('errorToast');
    
    function showToast(type, message) {
        let toast;
        if (type === 'success') {
            toast = new bootstrap.Toast(successToast);
            successToast.querySelector('.toast-body').textContent = message;
        } else {
            toast = new bootstrap.Toast(errorToast);
            errorToast.querySelector('.toast-body').textContent = message;
        }
        toast.show();
    }

    // Loading Spinner
    const loadingSpinner = document.querySelector('.loading-spinner');
    
    function showLoading() {
        loadingSpinner.classList.remove('d-none');
    }
    
    function hideLoading() {
        loadingSpinner.classList.add('d-none');
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    async function fetchAdminJson(url, options = {}) {
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

        const contentType = response.headers.get('content-type') || '';
        let payload = null;
        if (contentType.includes('application/json')) {
            payload = await response.json();
        }

        if (!response.ok) {
            let message = payload && payload.message ? payload.message : '';
            if (!message || message === 'No message available') {
                if (payload && payload.error) {
                    message = payload.error;
                } else if (response.statusText) {
                    message = response.statusText;
                } else {
                    message = `Request failed (${response.status})`;
                }
            }
            throw new Error(message);
        }

        if (response.status === 204 || response.status === 205) {
            return null;
        }

        if (!contentType.includes('application/json')) {
            throw new Error('Admin session required. Please log in again.');
        }

        return payload;
    }

    const instructorDirectory = new Map();

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function toSafeId(value) {
        return String(value ?? '').replace(/[^A-Za-z0-9_-]/g, '_');
    }

    function parseNameParts(fullNameRaw) {
        const fullName = String(fullNameRaw ?? '').trim();
        if (!fullName) {
            return { firstName: '', middleInitial: '', lastName: '' };
        }
        const parts = fullName.split(/\s+/);
        if (parts.length === 1) {
            return { firstName: parts[0], middleInitial: '', lastName: parts[0] };
        }
        const firstName = parts[0];
        const lastName = parts[parts.length - 1];
        let middleInitial = '';
        if (parts.length > 2) {
            middleInitial = parts[1].replace(/\./g, '').charAt(0);
        }
        return { firstName, middleInitial, lastName };
    }

    // Confirmation Modal
    const confirmationModal = new bootstrap.Modal(document.getElementById('confirmationModal'));
    const confirmActionBtn = document.getElementById('confirmAction');
    
    // Example: Add confirmation to generic delete buttons (exclude instructor delete).
    const deleteButtons = document.querySelectorAll('.btn-outline-danger');
    deleteButtons.forEach(btn => {
        if (btn.classList.contains('delete-instructor')) {
            return;
        }
        btn.addEventListener('click', function() {
            confirmationModal.show();

            // Store the action to be confirmed
            confirmActionBtn.onclick = function() {
                showLoading();
                setTimeout(() => {
                    hideLoading();
                    showToast('success', 'Action completed successfully!');
                    confirmationModal.hide();
                }, 1000);
            };
        });
    });

    // Subject Management
    const addSubjectBtn = document.getElementById('addSubjectBtn');
    const subjectsContainer = document.getElementById('subjectsContainer');
    
    function addSubjectEntry() {
        const uniqueId = Date.now();
        const subjectEntry = document.createElement('div');
        subjectEntry.className = 'subject-entry mb-3 p-3 border rounded';
        subjectEntry.innerHTML = `
            <div class="row align-items-center">
                <div class="col-md-4">
                    <input type="text" class="form-control subject-name" placeholder="Subject Name" required>
                </div>
                <div class="col-md-3">
                    <input type="text" class="form-control subject-code" placeholder="Subject Code" required>
                </div>
                <div class="col-md-3">
                    <select class="form-select subject-instructor" required>
                        <option value="" selected>Select Instructor</option>
                        <option value="1">Dr. John Smith</option>
                        <option value="2">Prof. Sarah Johnson</option>
                        <option value="3">Dr. Michael Brown</option>
                        <option value="4">Prof. Emily Davis</option>
                    </select>
                </div>
                <div class="col-md-2 text-center">
                    <button type="button" class="btn btn-outline-danger remove-subject p-2" style="min-width: 40px;">×</button>
                </div>
            </div>
            <div class="schedule-section mt-3">
                <h6>Schedule</h6>
                <div class="schedule-slots">
                    <div class="schedule-slot border rounded p-2 mb-2">
                        <div class="row align-items-center">
                            <div class="col-md-5">
                                <div class="day-selector">
                                    <label class="form-label small">Days:</label>
                                    <div class="form-check form-check-inline">
                                        <input class="form-check-input day-checkbox" type="checkbox" value="Monday" id="day-monday-${uniqueId}">
                                        <label class="form-check-label small" for="day-monday-${uniqueId}">Mon</label>
                                    </div>
                                    <div class="form-check form-check-inline">
                                        <input class="form-check-input day-checkbox" type="checkbox" value="Tuesday" id="day-tuesday-${uniqueId}">
                                        <label class="form-check-label small" for="day-tuesday-${uniqueId}">Tue</label>
                                    </div>
                                    <div class="form-check form-check-inline">
                                        <input class="form-check-input day-checkbox" type="checkbox" value="Wednesday" id="day-wednesday-${uniqueId}">
                                        <label class="form-check-label small" for="day-wednesday-${uniqueId}">Wed</label>
                                    </div>
                                    <div class="form-check form-check-inline">
                                        <input class="form-check-input day-checkbox" type="checkbox" value="Thursday" id="day-thursday-${uniqueId}">
                                        <label class="form-check-label small" for="day-thursday-${uniqueId}">Thu</label>
                                    </div>
                                    <div class="form-check form-check-inline">
                                        <input class="form-check-input day-checkbox" type="checkbox" value="Friday" id="day-friday-${uniqueId}">
                                        <label class="form-check-label small" for="day-friday-${uniqueId}">Fri</label>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="row">
                                    <div class="col-6">
                                        <label class="form-label small">Start Time</label>
                                        <input type="time" class="form-control form-control-sm start-time">
                                    </div>
                                    <div class="col-6">
                                        <label class="form-label small">End Time</label>
                                        <input type="time" class="form-control form-control-sm end-time">
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <button class="btn btn-sm btn-outline-danger remove-slot" type="button">Remove</button>
                            </div>
                        </div>
                    </div>
                </div>
                <button class="btn btn-sm btn-outline-primary add-slot-btn" type="button">Add Another Time Slot</button>
            </div>
        `;
        
        subjectsContainer.appendChild(subjectEntry);
        
        // Add remove functionality
        const removeBtn = subjectEntry.querySelector('.remove-subject');
        removeBtn.addEventListener('click', function() {
            if (subjectsContainer.children.length > 1) {
                subjectEntry.remove();
            }
        });
    }
    
    if (addSubjectBtn && subjectsContainer) {
        addSubjectBtn.addEventListener('click', addSubjectEntry);
        
        // Add remove functionality to initial subject entry
        const initialRemoveBtn = subjectsContainer.querySelector('.remove-subject');
        if (initialRemoveBtn) {
            initialRemoveBtn.addEventListener('click', function() {
                if (subjectsContainer.children.length > 1) {
                    this.closest('.subject-entry').remove();
                }
            });
        }
    }
    
    // Auto-generate subject code based on course and subject
    const courseSelect = document.getElementById('courseSelect');
    
    if (courseSelect) {
        courseSelect.addEventListener('change', function() {
            if (this.value !== 'Select Course') {
                const courseCode = this.value.split(' ').map(word => word[0]).join('').toUpperCase();
                const subjectEntries = subjectsContainer.querySelectorAll('.subject-entry');
                
                subjectEntries.forEach(entry => {
                    const subjectCodeInput = entry.querySelector('.subject-code');
                    if (subjectCodeInput.value === '') {
                        subjectCodeInput.value = `${courseCode}${Math.floor(100 + Math.random() * 900)}`;
                    }
                });
            }
        });
    }

    // Schedule slot functionality
    function addScheduleSlot(container) {
        const uniqueId = Date.now();
        const slotTemplate = `
            <div class="schedule-slot border rounded p-2 mb-2">
                <div class="row align-items-center">
                    <div class="col-md-5">
                        <div class="day-selector">
                            <label class="form-label small">Days:</label>
                            <div class="form-check form-check-inline">
                                <input class="form-check-input day-checkbox" type="checkbox" value="Monday" id="day-monday-${uniqueId}">
                                <label class="form-check-label small" for="day-monday-${uniqueId}">Mon</label>
                            </div>
                            <div class="form-check form-check-inline">
                                <input class="form-check-input day-checkbox" type="checkbox" value="Tuesday" id="day-tuesday-${uniqueId}">
                                <label class="form-check-label small" for="day-tuesday-${uniqueId}">Tue</label>
                            </div>
                            <div class="form-check form-check-inline">
                                <input class="form-check-input day-checkbox" type="checkbox" value="Wednesday" id="day-wednesday-${uniqueId}">
                                <label class="form-check-label small" for="day-wednesday-${uniqueId}">Wed</label>
                            </div>
                            <div class="form-check form-check-inline">
                                <input class="form-check-input day-checkbox" type="checkbox" value="Thursday" id="day-thursday-${uniqueId}">
                                <label class="form-check-label small" for="day-thursday-${uniqueId}">Thu</label>
                            </div>
                            <div class="form-check form-check-inline">
                                <input class="form-check-input day-checkbox" type="checkbox" value="Friday" id="day-friday-${uniqueId}">
                                <label class="form-check-label small" for="day-friday-${uniqueId}">Fri</label>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="row">
                            <div class="col-6">
                                <label class="form-label small">Start Time</label>
                                <input type="time" class="form-control form-control-sm start-time">
                            </div>
                            <div class="col-6">
                                <label class="form-label small">End Time</label>
                                <input type="time" class="form-control form-control-sm end-time">
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <button class="btn btn-sm btn-outline-danger remove-slot" type="button">Remove</button>
                    </div>
                </div>
            </div>
        `;
        
        const slotElement = document.createElement('div');
        slotElement.innerHTML = slotTemplate;
        container.appendChild(slotElement);
        
        // Add event listener for the remove button
        const removeButton = slotElement.querySelector('.remove-slot');
        removeButton.addEventListener('click', function() {
            slotElement.remove();
        });
    }
    
    // Add event listeners for existing add-slot buttons
    document.addEventListener('click', async function(e) {
        if (e.target && e.target.classList.contains('add-slot-btn')) {
            const container = e.target.closest('.schedule-section').querySelector('.schedule-slots');
            addScheduleSlot(container);
        }
    });
    
    // Add event listeners for existing remove-slot buttons
    document.addEventListener('click', async function(e) {
        if (e.target && e.target.classList.contains('remove-slot')) {
            e.target.closest('.schedule-slot').remove();
        }
    });
    
    // Add/Drop Students: handled by `admin-classes-crud.js` using backend APIs.

    // Class management tabs: rely on Bootstrap's built-in tab behavior (data-bs-toggle="tab").

    // Add Instructor Form Submission
    const addInstructorForm = document.getElementById('addInstructorForm');
    const formatEmployeeId = (value) => {
        const raw = String(value || '');
        if (/[A-Za-z]/.test(raw)) {
            return raw.trim();
        }
        const digits = raw.replace(/\D/g, '').slice(0, 9);
        if (digits.length <= 4) {
            return digits;
        }
        return `${digits.slice(0, 4)}-${digits.slice(4)}`;
    };

    const employeeIdInputs = [
        document.getElementById('employeeId'),
        document.getElementById('editInstructorEmployeeId')
    ].filter(Boolean);

    employeeIdInputs.forEach(input => {
        input.addEventListener('input', function() {
            const formatted = formatEmployeeId(this.value);
            if (this.value !== formatted) {
                const cursorAtEnd = this.selectionStart === this.value.length;
                this.value = formatted;
                if (cursorAtEnd) {
                    this.setSelectionRange(this.value.length, this.value.length);
                }
            }
        });

        input.addEventListener('blur', function() {
            this.value = formatEmployeeId(this.value);
        });
    });
    if (addInstructorForm) {
        addInstructorForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const employeeId = document.getElementById('employeeId').value.trim();
            const firstName = document.getElementById('firstName').value.trim();
            const middleInitial = document.getElementById('middleInitial').value.trim();
            const lastName = document.getElementById('lastName').value.trim();
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const department = document.getElementById('department').value;
            
            // Basic validation
            if (!employeeId || !firstName || !lastName || !email || !password || !department) {
                showToast('error', 'Please fill in all required fields');
                return;
            }

            if (!/^\d{4}-\d{5}$/.test(employeeId)) {
                showToast('error', 'Employee ID must match 0000-00000.');
                return;
            }
            
            showLoading();
            try {
                await fetchAdminJson('/api/admin/professors', {
                    method: 'POST',
                    body: JSON.stringify({
                        employeeId,
                        firstName,
                        middleInitial,
                        lastName,
                        department,
                        email,
                        password
                    })
                });

                addInstructorForm.reset();
                await loadInstructorsFromDb();
                showToast('success', 'Instructor added successfully.');
            } catch (error) {
                showToast('error', error.message || 'Unable to add instructor.');
            } finally {
                hideLoading();
            }
        });
    }

    let instructorSearchQuery = '';

    function filterInstructorsTable(query) {
        const tableBody = document.querySelector('#instructor-management-section table tbody');
        if (!tableBody) {
            return;
        }
        const normalized = query.trim().toLowerCase();
        const rows = tableBody.querySelectorAll('tr.accordion-toggle');
        rows.forEach(row => {
            const rowText = row.textContent.toLowerCase();
            const match = normalized === '' || rowText.includes(normalized);
            row.style.display = match ? '' : 'none';
            const detailRow = row.nextElementSibling;
            if (detailRow && detailRow.classList.contains('instructor-details-row')) {
                if (!match) {
                    detailRow.style.display = 'none';
                    const collapse = detailRow.querySelector('.instructor-detail-collapse');
                    if (collapse && collapse.classList.contains('show')) {
                        if (typeof bootstrap !== 'undefined' && bootstrap.Collapse) {
                            const instance = bootstrap.Collapse.getInstance(collapse)
                                || bootstrap.Collapse.getOrCreateInstance(collapse, { toggle: false });
                            instance.hide();
                        } else {
                            collapse.classList.remove('show');
                        }
                    }
                } else {
                    detailRow.style.display = '';
                }
            }
        });
    }

    const instructorSearchInput = document.getElementById('searchInstructor');
    const instructorSearchBtn = document.getElementById('searchInstructorBtn');
    if (instructorSearchInput) {
        instructorSearchInput.addEventListener('input', function() {
            instructorSearchQuery = this.value;
            filterInstructorsTable(instructorSearchQuery);
        });
    }
    if (instructorSearchBtn && instructorSearchInput) {
        instructorSearchBtn.addEventListener('click', function(e) {
            e.preventDefault();
            instructorSearchQuery = instructorSearchInput.value;
            filterInstructorsTable(instructorSearchQuery);
        });
    }

    async function loadInstructorsFromDb() {
        try {
            const payload = await fetchAdminJson('/api/admin/professors/directory');
            const tableBody = document.querySelector('#instructor-management-section table tbody');
            if (!tableBody) {
                return;
            }

            if (!Array.isArray(payload) || payload.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No instructors found.</td></tr>';
                return;
            }

            instructorDirectory.clear();

            tableBody.innerHTML = payload.map((professor, index) => {
                const professorId = professor.id;
                const employeeId = professor.employeeId || '';
                const name = professor.fullName || '-';
                const department = professor.department || '-';
                const email = professor.email || '-';
                const status = 'Active';
                const safeId = toSafeId(employeeId || `${name}-${index}`);
                const detailId = `instructorDetails-${safeId}`;

                if (employeeId) {
                    instructorDirectory.set(employeeId, {
                        employeeId,
                        fullName: name,
                        department,
                        email,
                        status
                    });
                }

                const viewIcon = `<svg class="bi bi-eye" fill="currentColor" height="16" viewBox="0 0 16 16" width="16" xmlns="http://www.w3.org/2000/svg"><path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8z"></path><path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z"></path></svg>`;
                const editIcon = `<svg class="bi bi-pencil" fill="currentColor" height="16" viewBox="0 0 16 16" width="16" xmlns="http://www.w3.org/2000/svg"><path d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708l-10 10a.5.5 0 0 1-.168.11l-5 2a.5.5 0 0 1-.65-.65l2-5a.5.5 0 0 1 .11-.168l10-10zM11.207 2.5 13.5 4.793 14.793 3.5 12.5 1.207 11.207 2.5zm1.586 3L10.5 3.207 4 9.707V10h.5a.5.5 0 0 1 .5.5v.5h.5a.5.5 0 0 1 .5.5v.5h.293l6.5-6.5zm-9.761 5.175-.106.106-1.528 3.821 3.821-1.528.106-.106A.5.5 0 0 1 5 12.5V12h-.5a.5.5 0 0 1-.5-.5V11h-.5a.5.5 0 0 1-.468-.325z"></path></svg>`;
                const deleteIcon = `<svg class="bi bi-trash" fill="currentColor" height="16" viewBox="0 0 16 16" width="16" xmlns="http://www.w3.org/2000/svg"><path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"></path><path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2 5v1h12V5H2z" fill-rule="evenodd"></path></svg>`;

                return `
                    <tr aria-controls="${escapeHtml(detailId)}" aria-expanded="false" class="accordion-toggle" data-bs-target="#${escapeHtml(detailId)}" role="button" tabindex="0" data-employee-id="${escapeHtml(employeeId)}">
                        <td>${escapeHtml(name)}</td>
                        <td>${escapeHtml(employeeId || '-')}</td>
                        <td>${escapeHtml(department)}</td>
                        <td>${escapeHtml(email)}</td>
                        <td>
                            <button class="btn btn-outline-primary btn-sm view-instructor" data-id="${escapeHtml(employeeId)}" title="View Details">${viewIcon}<span class="ms-1">View</span></button>
                            <button class="btn btn-outline-warning btn-sm edit-instructor" data-id="${escapeHtml(employeeId)}" title="Edit Instructor">${editIcon}<span class="ms-1">Edit</span></button>
                            <button class="btn btn-outline-danger btn-sm delete-instructor" data-id="${escapeHtml(employeeId)}" data-record-id="${escapeHtml(professorId == null ? '' : String(professorId))}" title="Delete Instructor">${deleteIcon}<span class="ms-1">Delete</span></button>
                        </td>
                        <td><span class="badge bg-success">${escapeHtml(status)}</span></td>
                    </tr>
                    <tr class="instructor-details-row">
                        <td colspan="6" class="p-0">
                            <div id="${escapeHtml(detailId)}" class="collapse instructor-detail-collapse">
                                <div class="p-3">
                                    <div class="row g-3">
                                        <div class="col-md-4">
                                            <div class="card h-100">
                                                <div class="card-body text-center">
                                                    <div class="bg-light rounded-circle d-inline-flex justify-content-center align-items-center mb-3" style="width:80px;height:80px;">
                                                        <svg class="bi bi-person-fill" xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" fill="currentColor" viewBox="0 0 16 16" style="font-size:2.5rem;">
                                                            <path d="M3 14s-1 0-1-1 1-4 6-4 6 3 6 4-1 1-1 1zm5-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6"></path>
                                                        </svg>
                                                    </div>
                                                    <h5 class="mb-1">${escapeHtml(name)}</h5>
                                                    <p class="text-muted mb-2">${escapeHtml(department)} Department</p>
                                                    <div class="small text-muted">${escapeHtml(email)}</div>
                                                    <div class="small text-muted mt-1">ID: ${escapeHtml(employeeId || '-')}</div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-md-8">
                                            <div class="card h-100">
                                                <div class="card-header bg-light">
                                                    <h6 class="mb-0">Instructor Details</h6>
                                                </div>
                                                <div class="card-body">
                                                    <div class="row mb-2">
                                                        <div class="col-sm-4"><strong>Employee ID</strong></div>
                                                        <div class="col-sm-8">${escapeHtml(employeeId || '-')}</div>
                                                    </div>
                                                    <div class="row mb-2">
                                                        <div class="col-sm-4"><strong>Email</strong></div>
                                                        <div class="col-sm-8">${escapeHtml(email)}</div>
                                                    </div>
                                                    <div class="row mb-2">
                                                        <div class="col-sm-4"><strong>Department</strong></div>
                                                        <div class="col-sm-8">${escapeHtml(department)}</div>
                                                    </div>
                                                    <button class="btn btn-outline-secondary btn-sm mt-2 close-instructor-details" data-target="${escapeHtml(detailId)}">Close</button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </td>
                    </tr>
                `;
            }).join('');

            bindInstructorCollapseEvents();
            if (instructorSearchQuery) {
                filterInstructorsTable(instructorSearchQuery);
            }
        } catch (error) {
            showToast('error', error.message || 'Unable to load instructors from database.');
        }
    }

    function showInstructorDetailsById(employeeId) {
        if (!employeeId) {
            return;
        }
        const detailId = `instructorDetails-${toSafeId(employeeId)}`;
        const detailEl = document.getElementById(detailId);
        if (!detailEl) {
            return;
        }

        // Close any other open instructor panels first
        document.querySelectorAll('#instructor-management-section .instructor-detail-collapse.show').forEach(openEl => {
            if (openEl === detailEl) {
                return;
            }
            if (typeof bootstrap !== 'undefined' && bootstrap.Collapse) {
                const instance = bootstrap.Collapse.getInstance(openEl)
                    || bootstrap.Collapse.getOrCreateInstance(openEl, { toggle: false });
                instance.hide();
            } else {
                openEl.classList.remove('show');
            }
        });

        if (typeof bootstrap !== 'undefined' && bootstrap.Collapse) {
            const collapse = bootstrap.Collapse.getInstance(detailEl)
                || bootstrap.Collapse.getOrCreateInstance(detailEl, { toggle: false });
            if (detailEl.classList.contains('show')) {
                collapse.hide();
            } else {
                collapse.show();
            }
        } else {
            detailEl.classList.toggle('show');
        }
    }

    function fillEditInstructorModal(employeeId) {
        const data = instructorDirectory.get(employeeId);
        if (!data) {
            showToast('error', 'Instructor not found.');
            return false;
        }
        const nameParts = parseNameParts(data.fullName);
        const idField = document.getElementById('editInstructorId');
        const firstNameField = document.getElementById('editInstructorFirstName');
        const middleField = document.getElementById('editInstructorMiddleInitial');
        const lastNameField = document.getElementById('editInstructorLastName');
        const emailField = document.getElementById('editInstructorEmail');
        const departmentField = document.getElementById('editInstructorDepartment');
        const statusField = document.getElementById('editInstructorStatus');

        if (idField) idField.value = data.employeeId;
        const editEmployeeIdField = document.getElementById('editInstructorEmployeeId');
        if (editEmployeeIdField) editEmployeeIdField.value = formatEmployeeId(data.employeeId);
        if (firstNameField) firstNameField.value = nameParts.firstName || '';
        if (middleField) middleField.value = nameParts.middleInitial || '';
        if (lastNameField) lastNameField.value = nameParts.lastName || '';
        if (emailField) emailField.value = data.email || '';
        if (departmentField) departmentField.value = data.department || '';
        if (statusField) statusField.value = data.status || 'Active';
        return true;
    }

    document.addEventListener('click', async function(e) {
        const viewBtn = e.target.closest('.view-instructor');
        if (viewBtn) {
            e.preventDefault();
            e.stopPropagation();
            const employeeId = viewBtn.getAttribute('data-id')
                || viewBtn.closest('tr')?.getAttribute('data-employee-id');
            if (!employeeId) {
                showToast('error', 'Missing employee ID.');
                return;
            }
            showInstructorDetailsById(employeeId);
            return;
        }

        const editBtn = e.target.closest('.edit-instructor');
        if (editBtn) {
            e.preventDefault();
            e.stopPropagation();
            const employeeId = editBtn.getAttribute('data-id');
            if (!employeeId) {
                showToast('error', 'Missing employee ID.');
                return;
            }
            if (fillEditInstructorModal(employeeId)) {
                const modalEl = document.getElementById('editInstructorModal');
                if (modalEl) {
                    bootstrap.Modal.getOrCreateInstance(modalEl).show();
                }
            }
            return;
        }

        const deleteBtn = e.target.closest('.delete-instructor');
        if (deleteBtn) {
            e.preventDefault();
            e.stopPropagation();
            const employeeId = deleteBtn.getAttribute('data-id');
            const recordId = deleteBtn.getAttribute('data-record-id');
            if (!employeeId && !recordId) {
                showToast('error', 'Missing instructor identifier.');
                return;
            }

            const deleteUrl = employeeId
                ? `/api/admin/professors/${encodeURIComponent(employeeId)}`
                : `/api/admin/professors/by-id/${encodeURIComponent(recordId)}`;

            if (confirmationModal && confirmActionBtn) {
                confirmationModal.show();
                confirmActionBtn.onclick = async function() {
                    showLoading();
                    try {
                        await fetchAdminJson(deleteUrl, {
                            method: 'DELETE'
                        });
                        confirmationModal.hide();
                        await loadInstructorsFromDb();
                        showToast('success', 'Instructor deleted.');
                    } catch (error) {
                        showToast('error', error.message || 'Unable to delete instructor.');
                    } finally {
                        hideLoading();
                    }
                };
            } else if (await window.confirmAsync('Delete this instructor?')) {
                await fetchAdminJson(deleteUrl, {
                    method: 'DELETE'
                });
                await loadInstructorsFromDb();
            }
            return;
        }

        const closeBtn = e.target.closest('.close-instructor-details');
        if (closeBtn) {
            e.preventDefault();
            e.stopPropagation();
            const targetId = closeBtn.getAttribute('data-target');
            const detailEl = targetId ? document.getElementById(targetId) : null;
            if (detailEl) {
                const collapse = bootstrap.Collapse.getInstance(detailEl)
                    || bootstrap.Collapse.getOrCreateInstance(detailEl, { toggle: false });
                collapse.hide();
            }
        }
    });

    const saveInstructorChangesBtn = document.getElementById('saveInstructorChanges');
    if (saveInstructorChangesBtn) {
        saveInstructorChangesBtn.addEventListener('click', async function() {
            const employeeId = document.getElementById('editInstructorId')?.value?.trim();
            const employeeIdEdit = document.getElementById('editInstructorEmployeeId')?.value?.trim();
            const firstName = document.getElementById('editInstructorFirstName')?.value?.trim();
            const middleInitial = document.getElementById('editInstructorMiddleInitial')?.value?.trim();
            const lastName = document.getElementById('editInstructorLastName')?.value?.trim();
            const email = document.getElementById('editInstructorEmail')?.value?.trim();
            const department = document.getElementById('editInstructorDepartment')?.value?.trim();

            if (!employeeId || !employeeIdEdit || !firstName || !lastName || !email || !department) {
                showToast('error', 'Please fill in all required fields.');
                return;
            }

            if (employeeIdEdit !== employeeId && !/^\d{4}-\d{5}$/.test(employeeIdEdit)) {
                showToast('error', 'Employee ID must match 0000-00000.');
                return;
            }

            showLoading();
            try {
                await fetchAdminJson(`/api/admin/professors/${encodeURIComponent(employeeId)}`, {
                    method: 'PUT',
                    body: JSON.stringify({
                        employeeId: employeeIdEdit,
                        firstName,
                        middleInitial,
                        lastName,
                        department,
                        email
                    })
                });

                const modalEl = document.getElementById('editInstructorModal');
                if (modalEl) {
                    bootstrap.Modal.getOrCreateInstance(modalEl).hide();
                }
                await loadInstructorsFromDb();
                showToast('success', 'Instructor updated successfully.');
            } catch (error) {
                showToast('error', error.message || 'Unable to update instructor.');
            } finally {
                hideLoading();
            }
        });
    }
    
    // Handle accordion behavior for instructor rows
    document.addEventListener('click', function(e) {
        // Check if we're clicking on an accordion toggle in the instructor management section
        if (e.target.closest('#instructor-management-section .accordion-toggle')) {
            if (e.target.closest('.view-instructor, .edit-instructor, .delete-instructor, .close-instructor-details')) {
                return;
            }
            const clickedRow = e.target.closest('tr');
            const targetCollapse = clickedRow.getAttribute('data-bs-target');

            // Find all accordion toggles in the same table
            const allToggles = document.querySelectorAll('#instructor-management-section .accordion-toggle');
            
            // Close all other collapses
            allToggles.forEach(toggle => {
                if (toggle !== clickedRow) {
                    const otherTarget = toggle.getAttribute('data-bs-target');
                    const collapseElement = document.querySelector(otherTarget);
                    if (collapseElement) {
                        const bsCollapse = bootstrap.Collapse.getInstance(collapseElement)
                            || bootstrap.Collapse.getOrCreateInstance(collapseElement);
                        if (bsCollapse) {
                            bsCollapse.hide();
                        }
                    }
                }
            });

            if (targetCollapse && !clickedRow.hasAttribute('data-bs-toggle')) {
                const collapseElement = document.querySelector(targetCollapse);
                if (collapseElement) {
                    const bsCollapse = bootstrap.Collapse.getInstance(collapseElement)
                        || bootstrap.Collapse.getOrCreateInstance(collapseElement, { toggle: false });
                    if (bsCollapse) {
                        bsCollapse.toggle();
                    }
                }
            }
        }
    });

    function bindInstructorCollapseEvents() {
        const instructorDetailCollapses = document.querySelectorAll('#instructor-management-section .instructor-detail-collapse');
        instructorDetailCollapses.forEach(collapse => {
            if (collapse.dataset.bound === 'true') {
                return;
            }
            collapse.dataset.bound = 'true';
            collapse.addEventListener('show.bs.collapse', function() {
                // Hide all other collapses in the same section
                const section = this.closest('#instructor-management-section');
                if (!section) {
                    return;
                }
                const otherCollapses = section.querySelectorAll('.collapse.show');
                otherCollapses.forEach(other => {
                    if (other !== this) {
                        const bsCollapse = bootstrap.Collapse.getInstance(other)
                            || bootstrap.Collapse.getOrCreateInstance(other);
                        if (bsCollapse) {
                            bsCollapse.hide();
                        }
                    }
                });
            });
        });
    }

    // Responsive table enhancements
    function makeTablesResponsive() {
        const tables = document.querySelectorAll('.table');
        tables.forEach(table => {
            if (table.offsetWidth > window.innerWidth * 0.9) {
                table.parentElement.classList.add('table-responsive');
            }
        });
    }
    
    window.addEventListener('resize', makeTablesResponsive);
    makeTablesResponsive();

    // Class Masterlist Toggle Functionality
    document.addEventListener('click', function(e) {
        // Check if the clicked element is a "View Class Masterlist" button
        if (e.target && e.target.textContent.trim() === 'View Class Masterlist') {
            // Prevent the click from bubbling up to the parent row
            e.stopPropagation();
            
            // Find the Class Masterlist card within the same section
            const masterlistCard = document.getElementById('classMasterlistCard');
            
            // Toggle the visibility of the Class Masterlist card
            if (masterlistCard) {
                if (masterlistCard.style.display === 'none') {
                    masterlistCard.style.display = 'block';
                } else {
                    masterlistCard.style.display = 'none';
                }
            }
        }
    });

    // Tooltip initialization
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Student Management Functionality
    const students = [];
    let availableSections = [];

    // Keep track of the currently expanded row
    let currentlyExpandedRow = null;

    async function loadAvailableSections() {
        try {
            const payload = await fetchAdminJson('/api/admin/classes');
            const sectionMap = new Map();

            if (Array.isArray(payload)) {
                payload.forEach(entry => {
                    const section = (entry.sectionName || '').trim();
                    if (!section) {
                        return;
                    }
                    const courseLabel = (entry.courseName || entry.courseCode || '').trim();
                    const key = `${courseLabel}::${section}`;
                    if (!sectionMap.has(key)) {
                        sectionMap.set(key, { section, courseLabel });
                    }
                });
            }

            availableSections = Array.from(sectionMap.values());
        } catch (error) {
            availableSections = [];
        }
    }

    function buildSectionOptions(currentSection) {
        const options = [];
        const normalizedCurrent = (currentSection || '').trim();
        let hasMatch = false;

        availableSections.forEach(entry => {
            const label = entry.courseLabel ? `${entry.courseLabel} - ${entry.section}` : entry.section;
            const selected = normalizedCurrent && entry.section === normalizedCurrent;
            if (selected) {
                hasMatch = true;
            }
            options.push(
                `<option value="${escapeHtml(entry.section)}" data-course="${escapeHtml(entry.courseLabel || '')}" ${selected ? 'selected' : ''}>${escapeHtml(label)}</option>`
            );
        });

        if (normalizedCurrent && !hasMatch) {
            options.unshift(`<option value="${escapeHtml(normalizedCurrent)}" selected>${escapeHtml(normalizedCurrent)}</option>`);
        }

        if (options.length === 0) {
            options.push('<option value="">No sections available</option>');
        }

        return options.join('');
    }

    const availableCourses = [
        "Computer Science",
        "Business Administration",
        "Engineering",
        "Arts and Sciences"
    ];

    function buildCourseOptions(selectedCourse) {
        const normalized = String(selectedCourse ?? '').trim();
        const baseOption = `<option value="">Select Course</option>`;
        const options = availableCourses.map(course => {
            const selected = course === normalized ? ' selected' : '';
            return `<option value="${course}"${selected}>${course}</option>`;
        });
        return [baseOption, ...options].join('');
    }

    // Function to render students table
    function renderStudentsTable() {
        const tableBody = document.getElementById("studentsTableBody");
        tableBody.innerHTML = "";

        students.forEach(student => {
            // Main student row
            const row = document.createElement("tr");
            row.className = "student-row";
            row.setAttribute("data-student-id", student.id);
            row.innerHTML = `
                <td>${student.id}</td>
                <td>${student.fullName}</td>
                <td>${student.course || '-'}</td>
                <td>${student.section || '-'}</td>
                <td><span class="badge ${student.status === 'Active' ? 'bg-success' : 'bg-danger'}">${student.status}</span></td>
                <td>
                    <button class="btn btn-outline-primary btn-sm view-student" data-id="${student.id}">
                        <icon></icon> View
                    </button>
                    <button class="btn btn-outline-warning btn-sm edit-student" data-id="${student.id}">
                        <icon></icon> Edit
                    </button>
                    <button class="btn btn-outline-danger btn-sm delete-student" data-id="${student.id}">
                        <icon></icon> Delete
                    </button>
                </td>
            `;
            
            // Collapsible detail row
            const detailRow = document.createElement("tr");
            detailRow.className = "student-detail-row";
            detailRow.setAttribute("data-student-id", student.id);
            detailRow.style.display = "none";
            detailRow.innerHTML = `
                <td colspan="6" class="p-0">
                    <div class="student-details-container">
                        <!-- View Panel -->
                        <div id="viewStudentPanel-${student.id}" class="student-view-panel p-3" style="display: none;">
                            <h5 class="mb-4">Student Information</h5>
                            <div class="row mb-3">
                                <div class="col-md-3"><label class="form-label"><strong>Full Name:</strong></label></div>
                                <div class="col-md-9"><span class="view-full-name">${student.fullName}</span></div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-3"><label class="form-label"><strong>Course:</strong></label></div>
                                <div class="col-md-9"><span class="view-course">${student.course}</span></div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-3"><label class="form-label"><strong>Section:</strong></label></div>
                                <div class="col-md-9"><span class="view-section">${student.section}</span></div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-3"><label class="form-label"><strong>Status:</strong></label></div>
                                <div class="col-md-9"><span class="view-status"><span class="badge ${student.status === 'Active' ? 'bg-success' : 'bg-danger'}">${student.status}</span></span></div>
                            </div>
                            <div class="text-end">
                                <button class="btn btn-secondary btn-sm close-details" data-id="${student.id}">Close</button>
                            </div>
                        </div>
                        
                        <!-- Edit Panel -->
                        <div id="editStudentPanel-${student.id}" class="student-edit-panel p-3" style="display: none;">
                            <h5 class="mb-4">Edit Student Information</h5>
                            <form class="edit-student-form" data-id="${student.id}">
                                <input type="hidden" class="edit-student-id" value="${student.id}">
                                <div class="row mb-3">
                                    <div class="col-md-4">
                                        <label class="form-label">Student ID</label>
                                        <input class="form-control edit-student-id-input" type="text" value="${student.id}" pattern="^[0-9]{4}-[0-9]{5}$" maxlength="10" inputmode="numeric" required>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label">Full Name</label>
                                        <input class="form-control edit-full-name" type="text" value="${student.fullName}" required>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label">Course</label>
                                        <select class="form-select edit-course">
                                            ${buildCourseOptions(student.course)}
                                        </select>
                                    </div>
                                </div>
                                <div class="row mb-3">
                                    <div class="col-md-6">
                                        <label class="form-label">Section</label>
                                        <select class="form-select edit-section-select">
                                            ${buildSectionOptions(student.section)}
                                        </select>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label">Status</label>
                                        <select class="form-select edit-status" required>
                                            <option value="Active" ${student.status === 'Active' ? 'selected' : ''}>Active</option>
                                            <option value="Dropped" ${student.status === 'Dropped' ? 'selected' : ''}>Dropped</option>
                                        </select>
                                    </div>
                                </div>
                                <div class="text-end">
                                    <button class="btn btn-secondary btn-sm cancel-edit" type="button" data-id="${student.id}">Cancel</button>
                                    <button class="btn btn-primary btn-sm save-changes" type="submit">Save Changes</button>
                                </div>
                            </form>
                        </div>
                    </div>
                </td>
            `;
            
            tableBody.appendChild(row);
            tableBody.appendChild(detailRow);
        });

        // Add event listeners to view buttons
        document.querySelectorAll(".view-student").forEach(button => {
            button.addEventListener("click", function() {
                const studentId = this.getAttribute("data-id");
                toggleStudentDetails(studentId, "view");
            });
        });

        // Add event listeners to edit buttons
        document.querySelectorAll(".edit-student").forEach(button => {
            button.addEventListener("click", function() {
                const studentId = this.getAttribute("data-id");
                toggleStudentDetails(studentId, "edit");
            });
        });

        document.querySelectorAll(".edit-section-select").forEach(select => {
            select.addEventListener("change", function() {
                const courseLabel = this.options[this.selectedIndex]?.dataset?.course;
                if (!courseLabel) {
                    return;
                }
                const courseInput = this.closest("form")?.querySelector(".edit-course");
                if (courseInput && !courseInput.value.trim()) {
                    courseInput.value = courseLabel;
                }
            });
        });

        // Add event listeners to delete buttons
        document.querySelectorAll(".delete-student").forEach(button => {
            button.addEventListener("click", async function(e) {
                e.preventDefault();
                e.stopPropagation();
                const studentId = this.getAttribute("data-id");
                if (!studentId) {
                    showToast("error", "Missing student ID.");
                    return;
                }

                if (confirmationModal && confirmActionBtn) {
                    confirmationModal.show();
                    confirmActionBtn.onclick = async function() {
                        showLoading();
                        try {
                            await fetchAdminJson(`/api/admin/students/${encodeURIComponent(studentId)}`, {
                                method: 'DELETE'
                            });
                            confirmationModal.hide();
                            await loadStudentsFromDb();
                            showToast("success", "Student deleted successfully!");
                        } catch (error) {
                            showToast("error", error.message || "Unable to delete student.");
                        } finally {
                            hideLoading();
                        }
                    };
                } else if (await window.confirmAsync("Delete this student?")) {
                    await fetchAdminJson(`/api/admin/students/${encodeURIComponent(studentId)}`, {
                        method: 'DELETE'
                    });
                    await loadStudentsFromDb();
                }
            });
        });

        // Add event listeners to close buttons
        document.querySelectorAll(".close-details").forEach(button => {
            button.addEventListener("click", function() {
                const studentId = this.getAttribute("data-id");
                hideStudentDetails(studentId);
            });
        });

        // Add event listeners to cancel edit buttons
        document.querySelectorAll(".cancel-edit").forEach(button => {
            button.addEventListener("click", function() {
                const studentId = this.getAttribute("data-id");
                hideStudentDetails(studentId);
            });
        });

        // Add event listeners to save forms
        document.querySelectorAll(".edit-student-form").forEach(form => {
            form.addEventListener("submit", function(e) {
                e.preventDefault();
                saveStudentChanges(this);
            });
        });

        document.querySelectorAll(".edit-student-id-input").forEach(input => {
            input.addEventListener("input", function() {
                const digits = this.value.replace(/\D/g, '').slice(0, 9);
                if (digits.length <= 4) {
                    this.value = digits;
                } else {
                    this.value = `${digits.slice(0, 4)}-${digits.slice(4)}`;
                }
            });
        });

        // Add click event to student rows to toggle details
        document.querySelectorAll(".student-row").forEach(row => {
            row.addEventListener("click", function(e) {
                // Don't trigger if clicking on action buttons
                if (!e.target.closest('.btn')) {
                    const studentId = this.getAttribute("data-student-id");
                    toggleStudentDetails(studentId, "view");
                }
            });
        });
    }

    let studentSearchQuery = '';

    function filterStudentsTable(query) {
        const tableBody = document.getElementById("studentsTableBody");
        if (!tableBody) {
            return;
        }
        const normalized = query.trim().toLowerCase();
        const rows = tableBody.querySelectorAll("tr.student-row");
        rows.forEach(row => {
            const rowText = row.textContent.toLowerCase();
            const match = normalized === '' || rowText.includes(normalized);
            row.style.display = match ? '' : 'none';
            const detailRow = row.nextElementSibling;
            if (detailRow && detailRow.classList.contains('student-detail-row')) {
                detailRow.style.display = match ? '' : 'none';
            }
        });
    }

    const studentSearchInput = document.getElementById('searchStudent');
    const studentSearchBtn = document.getElementById('searchStudentBtn');
    if (studentSearchInput) {
        studentSearchInput.addEventListener('input', function() {
            studentSearchQuery = this.value;
            filterStudentsTable(studentSearchQuery);
        });
    }
    if (studentSearchBtn && studentSearchInput) {
        studentSearchBtn.addEventListener('click', function(e) {
            e.preventDefault();
            studentSearchQuery = studentSearchInput.value;
            filterStudentsTable(studentSearchQuery);
        });
    }

    // Function to toggle student details panel
    function toggleStudentDetails(studentId, panelType) {
        // If the same row is clicked again, collapse it
        if (currentlyExpandedRow === studentId) {
            hideStudentDetails(studentId);
            return;
        }

        // Hide currently expanded row if any
        if (currentlyExpandedRow) {
            hideStudentDetails(currentlyExpandedRow);
        }

        // Show the clicked row
        showStudentDetails(studentId, panelType);
        currentlyExpandedRow = studentId;
    }

    // Function to show student details
    function showStudentDetails(studentId, panelType) {
        const detailRow = document.querySelector(`.student-detail-row[data-student-id="${studentId}"]`);
        if (detailRow) {
            detailRow.style.display = "";
            
            // Hide both panels initially
            const viewPanel = document.getElementById(`viewStudentPanel-${studentId}`);
            const editPanel = document.getElementById(`editStudentPanel-${studentId}`);
            viewPanel.style.display = "none";
            editPanel.style.display = "none";
            
            // Show the requested panel
            if (panelType === "view") {
                viewPanel.style.display = "block";
            } else if (panelType === "edit") {
                editPanel.style.display = "block";
            }
        }
    }

    // Function to hide student details
    function hideStudentDetails(studentId) {
        const detailRow = document.querySelector(`.student-detail-row[data-student-id="${studentId}"]`);
        if (detailRow) {
            detailRow.style.display = "none";
        }
        currentlyExpandedRow = null;
    }

    // Function to save student changes
    async function saveStudentChanges(form) {
        const studentId = form.getAttribute("data-id");
        const updatedStudentId = form.querySelector(".edit-student-id-input")?.value?.trim() || '';
        const fullName = form.querySelector(".edit-full-name").value.trim();
        const course = form.querySelector(".edit-course").value.trim();
        const sectionSelect = form.querySelector(".edit-section-select");
        const section = sectionSelect ? sectionSelect.value.trim() : "";
        if (!updatedStudentId) {
            showToast("error", "Student ID is required.");
            return;
        }
        if (!/^\d{4}-\d{5}$/.test(updatedStudentId)) {
            showToast("error", "Student ID must be in format: 0000-00000");
            return;
        }
        if (!fullName) {
            showToast("error", "Full name is required.");
            return;
        }

        const payload = {
            studentId: updatedStudentId,
            fullName,
            course,
            section,
            status: form.querySelector(".edit-status").value
        };

        showLoading();
        try {
            await fetchAdminJson(`/api/admin/students/${encodeURIComponent(studentId)}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });

            hideStudentDetails(studentId);
            await loadStudentsFromDb();
            showToast("success", "Student information updated successfully!");
        } catch (error) {
            showToast("error", error.message || "Unable to update student.");
        } finally {
            hideLoading();
        }
    }

    async function loadStudentsFromDb() {
        try {
            await loadAvailableSections();
            const payload = await fetchAdminJson('/api/admin/students');
            students.length = 0;

            if (Array.isArray(payload)) {
                payload.forEach(student => {
                    students.push({
                        id: student.studentId || '',
                        fullName: student.fullName || '',
                        course: student.course || '',
                        section: student.section || '',
                        status: student.status || 'Active'
                    });
                });
            }

            renderStudentsTable();
            if (studentSearchQuery) {
                filterStudentsTable(studentSearchQuery);
            }
        } catch (error) {
            showToast("error", error.message || "Unable to load students from database.");
        }
    }

    function formatStudentId(value) {
        const digits = value.replace(/\D/g, '').slice(0, 9);
        if (digits.length <= 4) {
            return digits;
        }
        return `${digits.slice(0, 4)}-${digits.slice(4)}`;
    }

    function isValidStudentId(value) {
        return /^\d{4}-\d{5}$/.test(value.trim());
    }

    function isValidFullName(value) {
        const trimmed = value.trim();
        const pattern = /^[A-Za-z]+([ '\-][A-Za-z]+)*$/;
        return trimmed.length >= 2 && pattern.test(trimmed);
    }

    function applyInputValidity(input, isValid, message) {
        if (!input) {
            return;
        }
        input.setCustomValidity(isValid ? '' : message);
        if (isValid) {
            input.classList.remove('is-invalid');
            input.classList.add('is-valid');
        } else {
            input.classList.add('is-invalid');
            input.classList.remove('is-valid');
        }
    }

    const addStudentIdInput = document.getElementById("addStudentId");
    if (addStudentIdInput) {
        addStudentIdInput.addEventListener('input', function() {
            this.value = formatStudentId(this.value);
            applyInputValidity(this, isValidStudentId(this.value), 'Student ID must be in format: 0000-00000');
        });
        addStudentIdInput.addEventListener('blur', function() {
            applyInputValidity(this, isValidStudentId(this.value), 'Student ID must be in format: 0000-00000');
        });
    }

    const addStudentFullNameInput = document.getElementById("addFullName");
    if (addStudentFullNameInput) {
        addStudentFullNameInput.addEventListener('input', function() {
            applyInputValidity(this, isValidFullName(this.value), 'Full name must be at least 2 letters and contain letters only');
        });
        addStudentFullNameInput.addEventListener('blur', function() {
            applyInputValidity(this, isValidFullName(this.value), 'Full name must be at least 2 letters and contain letters only');
        });
    }

    // Add new student
    document.getElementById("addStudentForm").addEventListener("submit", async function(e) {
        e.preventDefault();
        
        const studentId = document.getElementById("addStudentId").value.trim();
        const fullName = document.getElementById("addFullName").value.trim();
        const course = document.getElementById("addCourse").value;
        const section = document.getElementById("addSection").value;
        const status = document.getElementById("addStatus").value;
        const password = document.getElementById("addPassword").value;
        
        // Basic validation
        if (!studentId || !fullName || !course || !section || !status || !password) {
            showToast("error", "Please fill in all fields");
            return;
        }
        if (!isValidStudentId(studentId)) {
            showToast("error", "Student ID must be in format: 0000-00000");
            return;
        }
        if (!isValidFullName(fullName)) {
            showToast("error", "Full name must be at least 2 letters and contain letters only");
            return;
        }
        if (password.length < 8) {
            showToast("error", "Password must be at least 8 characters long.");
            return;
        }

        showLoading();
        try {
            await fetchAdminJson('/api/admin/students', {
                method: 'POST',
                body: JSON.stringify({
                    studentId,
                    fullName,
                    course,
                    section,
                    status,
                    password
                })
            });

            const collapseElement = document.getElementById("studentFormCollapse");
            const bsCollapse = bootstrap.Collapse.getInstance(collapseElement);
            if (bsCollapse) {
                bsCollapse.hide();
            } else {
                collapseElement.classList.remove("show");
            }

            document.getElementById("addStudentForm").reset();
            await loadStudentsFromDb();
            showToast("success", "Student added successfully!");
        } catch (error) {
            showToast("error", error.message || "Unable to add student.");
        } finally {
            hideLoading();
        }
    });

    // Initialize students table on page load
    if (document.getElementById("student-management-section")) {
        loadStudentsFromDb();
    }

    // Initialize instructor table from DB.
    if (document.getElementById("instructor-management-section")) {
        loadInstructorsFromDb();
    }

    // Profile section functionality
    const profileForm = document.getElementById('profileForm');
    const changePasswordForm = document.getElementById('changePasswordForm');
    const adminUsernameField = document.getElementById('adminUsername');
    const adminFirstNameField = document.getElementById('adminFirstName');
    const adminLastNameField = document.getElementById('adminLastName');
    const adminEmailField = document.getElementById('adminEmail');
    const adminRoleField = document.getElementById('adminRole');

    function applyAdminProfile(profile) {
        if (!profile) {
            return;
        }
        if (adminUsernameField) adminUsernameField.value = profile.username || '';
        if (adminFirstNameField) adminFirstNameField.value = profile.firstName || '';
        if (adminLastNameField) adminLastNameField.value = profile.lastName || '';
        if (adminEmailField) adminEmailField.value = profile.email || '';
        if (adminRoleField) adminRoleField.value = profile.role || 'Administrator';
    }

    async function loadAdminProfile() {
        if (!profileForm) {
            return;
        }
        try {
            const payload = await fetchAdminJson('/api/admin/profile');
            applyAdminProfile(payload);
        } catch (error) {
            showToast('error', error.message || 'Unable to load admin profile.');
        }
    }

    if (profileForm) {
        profileForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const firstName = adminFirstNameField?.value?.trim();
            const lastName = adminLastNameField?.value?.trim();
            const email = adminEmailField?.value?.trim();

            if (!firstName || !lastName || !email) {
                showToast('error', 'Please fill in all profile fields.');
                return;
            }

            showLoading();
            try {
                const payload = await fetchAdminJson('/api/admin/profile', {
                    method: 'PUT',
                    body: JSON.stringify({ firstName, lastName, email })
                });
                applyAdminProfile(payload);
                showToast('success', 'Profile updated successfully.');
            } catch (error) {
                showToast('error', error.message || 'Unable to update profile.');
            } finally {
                hideLoading();
            }
        });
    }

    if (changePasswordForm) {
        changePasswordForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const currentPassword = document.getElementById('currentPassword').value;
            const newPassword = document.getElementById('newPassword').value;
            const confirmNewPassword = document.getElementById('confirmNewPassword').value;
            
            // Basic validation
            if (!currentPassword || !newPassword || !confirmNewPassword) {
                showToast('error', 'Please fill in all password fields');
                return;
            }
            
            if (newPassword !== confirmNewPassword) {
                showToast('error', 'New passwords do not match');
                return;
            }
            
            if (newPassword.length < 6) {
                showToast('error', 'Password must be at least 6 characters long');
                return;
            }

            showLoading();
            try {
                await fetchAdminJson('/api/admin/profile/password', {
                    method: 'PUT',
                    body: JSON.stringify({
                        currentPassword,
                        newPassword,
                        confirmNewPassword
                    })
                });
                showToast('success', 'Password changed successfully.');
                changePasswordForm.reset();
            } catch (error) {
                showToast('error', error.message || 'Unable to change password.');
            } finally {
                hideLoading();
            }
        });
    }

    if (profileForm) {
        loadAdminProfile();
    }

    // Password toggles: Admin profile
    const toggleCurrentPassword = document.getElementById('toggleCurrentPassword');
    const currentPasswordInput = document.getElementById('currentPassword');
    const toggleCurrentPasswordIcon = document.getElementById('toggleCurrentPasswordIcon');

    if (toggleCurrentPassword && currentPasswordInput && toggleCurrentPasswordIcon) {
        toggleCurrentPassword.addEventListener('click', function() {
            const showing = currentPasswordInput.type === 'text';
            currentPasswordInput.type = showing ? 'password' : 'text';
            toggleCurrentPasswordIcon.src = showing ? '/assets/img/hide.png' : '/assets/img/visible.png';
        });
    }

    const toggleNewPassword = document.getElementById('toggleNewPassword');
    const newPasswordInput = document.getElementById('newPassword');
    const toggleNewPasswordIcon = document.getElementById('toggleNewPasswordIcon');

    if (toggleNewPassword && newPasswordInput && toggleNewPasswordIcon) {
        toggleNewPassword.addEventListener('click', function() {
            const showing = newPasswordInput.type === 'text';
            newPasswordInput.type = showing ? 'password' : 'text';
            toggleNewPasswordIcon.src = showing ? '/assets/img/hide.png' : '/assets/img/visible.png';
        });
    }

    const toggleConfirmNewPassword = document.getElementById('toggleConfirmNewPassword');
    const confirmNewPasswordInput = document.getElementById('confirmNewPassword');
    const toggleConfirmNewPasswordIcon = document.getElementById('toggleConfirmNewPasswordIcon');

    if (toggleConfirmNewPassword && confirmNewPasswordInput && toggleConfirmNewPasswordIcon) {
        toggleConfirmNewPassword.addEventListener('click', function() {
            const showing = confirmNewPasswordInput.type === 'text';
            confirmNewPasswordInput.type = showing ? 'password' : 'text';
            toggleConfirmNewPasswordIcon.src = showing ? '/assets/img/hide.png' : '/assets/img/visible.png';
        });
    }

    // Password toggle: Add Instructor
    const toggleInstructorPassword = document.getElementById('toggleInstructorPassword');
    const instructorPassword = document.getElementById('password');
    const instructorPasswordIcon = document.getElementById('instructorPasswordIcon');

    if (toggleInstructorPassword && instructorPassword && instructorPasswordIcon) {
        toggleInstructorPassword.addEventListener('click', function() {
            const showing = instructorPassword.type === 'text';
            instructorPassword.type = showing ? 'password' : 'text';
            instructorPasswordIcon.src = showing ? '/assets/img/hide.png' : '/assets/img/visible.png';
        });
    }

    // Password toggle: Add Student
    const toggleStudentPassword = document.getElementById('toggleStudentPassword');
    const studentPassword = document.getElementById('addPassword');
    const studentPasswordIcon = document.getElementById('studentPasswordIcon');

    if (toggleStudentPassword && studentPassword && studentPasswordIcon) {
        toggleStudentPassword.addEventListener('click', function() {
            const showing = studentPassword.type === 'text';
            studentPassword.type = showing ? 'password' : 'text';
            studentPasswordIcon.src = showing ? '/assets/img/hide.png' : '/assets/img/visible.png';
        });
    }
    
    // Handle dropdown profile link separately since it's not in the sidebar
    const dropdownProfileLink = document.querySelector('.dropdown-menu a[href="#profile-section"]');
    if (dropdownProfileLink) {
        dropdownProfileLink.addEventListener('click', function(e) {
            e.preventDefault();

            activateSection('#profile-section');
            if (window.location.hash !== '#profile-section') {
                window.location.hash = '#profile-section';
            }
        });
    }
});
