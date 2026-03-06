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
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Remove active class from all links and sections
            navLinks.forEach(l => l.parentElement.classList.remove('active'));
            contentSections.forEach(section => section.classList.remove('active'));
            
            // Add active class to clicked link
            this.parentElement.classList.add('active');
            
            // Show corresponding section
            const targetSection = this.getAttribute('href');
            document.querySelector(targetSection).classList.add('active');
        });
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
            const message = payload && payload.message ? payload.message : `Request failed (${response.status})`;
            throw new Error(message);
        }

        if (!contentType.includes('application/json')) {
            throw new Error('Admin session required. Please log in again.');
        }

        return payload;
    }

    // Confirmation Modal
    const confirmationModal = new bootstrap.Modal(document.getElementById('confirmationModal'));
    const confirmActionBtn = document.getElementById('confirmAction');
    
    // Example: Add confirmation to delete buttons
    const deleteButtons = document.querySelectorAll('.btn-outline-danger');
    deleteButtons.forEach(btn => {
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
    document.addEventListener('click', function(e) {
        if (e.target && e.target.classList.contains('add-slot-btn')) {
            const container = e.target.closest('.schedule-section').querySelector('.schedule-slots');
            addScheduleSlot(container);
        }
    });
    
    // Add event listeners for existing remove-slot buttons
    document.addEventListener('click', function(e) {
        if (e.target && e.target.classList.contains('remove-slot')) {
            e.target.closest('.schedule-slot').remove();
        }
    });
    
    // Add/Remove Students functionality
    const addStudentBtn = document.getElementById('addStudentBtn');
    const studentIdInput = document.getElementById('studentIdInput');
    const studentNameInput = document.getElementById('studentNameInput');
    const currentStudentsList = document.getElementById('currentStudentsList');
    const droppedStudentsList = document.getElementById('droppedStudentsList');

    if (addStudentBtn) {
        addStudentBtn.addEventListener('click', function() {
            const studentId = studentIdInput.value.trim();
            const studentName = studentNameInput.value.trim();

            if (!studentId) {
                showToast('error', 'Please enter Student ID');
                return;
            }

            if (!studentName) {
                showToast('error', 'Please enter Student Name');
                return;
            }

            // Check if student ID already exists
            const existingStudent = document.querySelector(`#currentStudentsList [data-student-id="${studentId}"]`) || 
                                  document.querySelector(`#droppedStudentsList [data-student-id="${studentId}"]`);
            
            if (existingStudent) {
                showToast('error', `Student ID ${studentId} already exists`);
                return;
            }

            // Create new student entry
            const studentElement = document.createElement('div');
            studentElement.className = 'd-flex justify-content-between align-items-center list-group-item';
            studentElement.setAttribute('data-student-id', studentId);
            studentElement.innerHTML = `
                <span>${studentName} (${studentId})</span>
                <button class="btn btn-outline-danger btn-sm remove-student">Remove</button>
            `;

            currentStudentsList.appendChild(studentElement);

            // Add event listener for remove button using event delegation
            attachRemoveListener(studentElement);

            // Show success message
            showToast('success', `Student ${studentName} (${studentId}) added successfully`);
            
            // Clear inputs
            studentIdInput.value = '';
            studentNameInput.value = '';
        });
    }

    // Function to move student to dropped list
    function moveToDroppedList(studentElement, studentId, studentName) {
        studentElement.remove();
        
        const droppedStudentElement = document.createElement('div');
        droppedStudentElement.className = 'd-flex justify-content-between align-items-center list-group-item';
        droppedStudentElement.setAttribute('data-student-id', studentId);
        droppedStudentElement.innerHTML = `
            <span>${studentName} (${studentId})</span>
            <button class="btn btn-outline-success btn-sm reenroll-student">Re-enroll</button>
        `;

        droppedStudentsList.appendChild(droppedStudentElement);

        // Add event listener for re-enroll button using event delegation
        attachReenrollListener(droppedStudentElement);

        showToast('success', `Student ${studentName} (${studentId}) removed successfully`);
    }

    // Function to move student back to current list
    function moveToCurrentList(studentElement, studentId, studentName) {
        studentElement.remove();
        
        const currentStudentElement = document.createElement('div');
        currentStudentElement.className = 'd-flex justify-content-between align-items-center list-group-item';
        currentStudentElement.setAttribute('data-student-id', studentId);
        currentStudentElement.innerHTML = `
            <span>${studentName} (${studentId})</span>
            <button class="btn btn-outline-danger btn-sm remove-student">Remove</button>
        `;

        currentStudentsList.appendChild(currentStudentElement);

        // Add event listener for remove button using event delegation
        attachRemoveListener(currentStudentElement);

        showToast('success', `Student ${studentName} (${studentId}) re-enrolled successfully`);
    }

    // Function to attach remove listener to a specific element
    function attachRemoveListener(element) {
        const removeBtn = element.querySelector('.remove-student');
        removeBtn.addEventListener('click', function() {
            const studentElement = this.closest('.list-group-item');
            const studentId = studentElement.getAttribute('data-student-id');
            const studentName = studentElement.querySelector('span').textContent.replace(` (${studentId})`, '').trim();
            moveToDroppedList(studentElement, studentId, studentName);
        });
    }

    // Function to attach reenroll listener to a specific element
    function attachReenrollListener(element) {
        const reenrollBtn = element.querySelector('.reenroll-student');
        reenrollBtn.addEventListener('click', function() {
            const studentElement = this.closest('.list-group-item');
            const studentId = studentElement.getAttribute('data-student-id');
            const studentName = studentElement.querySelector('span').textContent.replace(` (${studentId})`, '').trim();
            moveToCurrentList(studentElement, studentId, studentName);
        });
    }

    // Attach event listeners to existing students on page load
    function initializeExistingStudents() {
        const existingRemoveButtons = document.querySelectorAll('#currentStudentsList .remove-student');
        existingRemoveButtons.forEach(btn => {
            attachRemoveListener(btn.closest('.list-group-item'));
        });
        
        const existingReenrollButtons = document.querySelectorAll('#droppedStudentsList .reenroll-student');
        existingReenrollButtons.forEach(btn => {
            attachReenrollListener(btn.closest('.list-group-item'));
        });
    }

    // Initialize existing students when page loads
    initializeExistingStudents();

    // Tab navigation for class management
    const tabPanes = document.querySelectorAll('.tab-pane');
    const navLinksTabs = document.querySelectorAll('.nav-tabs .nav-link');
    
    navLinksTabs.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Check if navigation tabs are visible
            const navTabs = document.querySelector('.nav-tabs');
            
            if (navTabs && navTabs.style.display !== 'none') {
                const target = this.getAttribute('href');
                
                // Hide all tab panes
                tabPanes.forEach(pane => pane.classList.remove('show', 'active'));
                
                // Show target tab pane
                document.querySelector(target).classList.add('show', 'active');
                
                // Update active nav link
                navLinksTabs.forEach(navLink => navLink.classList.remove('active'));
                this.classList.add('active');
            }
        });
    });
    
    // Function to show only masterlist tab (hide navigation)
    function showMasterlistOnly() {
        // Hide all tabs
        tabPanes.forEach(pane => pane.classList.remove('show', 'active'));
        
        // Show masterlist tab
        document.getElementById('masterlistTab').classList.add('show', 'active');
        
        // Hide the navigation tabs
        const navTabs = document.querySelector('.nav-tabs');
        if (navTabs) {
            navTabs.style.display = 'none';
        }
    }
    
    // Function to show student management tabs (show only add/drop and bulk actions)
    function showStudentManagementTabs() {
        // Hide all tabs
        tabPanes.forEach(pane => pane.classList.remove('show', 'active'));
        
        // Show add/drop students tab by default
        document.getElementById('studentManagementTab').classList.add('show', 'active');
        
        // Show the navigation tabs permanently
        const navTabs = document.querySelector('.nav-tabs');
        if (navTabs) {
            navTabs.style.display = 'flex';
        }
        
        // Hide the masterlist header
        const masterlistHeader = document.querySelector('#masterlistTab .card-header');
        if (masterlistHeader) {
            masterlistHeader.style.display = 'none';
        }
        
        // Update active nav link
        navLinksTabs.forEach(navLink => navLink.classList.remove('active'));
        document.querySelector('a[href="#studentManagementTab"]').classList.add('active');
    }
    
    // Function to show only masterlist tab (show masterlist header)
    function showMasterlistOnly() {
        // Hide all tabs
        tabPanes.forEach(pane => pane.classList.remove('show', 'active'));
        
        // Show masterlist tab
        document.getElementById('masterlistTab').classList.add('show', 'active');
        
        // Hide the navigation tabs completely
        const navTabs = document.querySelector('.nav-tabs');
        if (navTabs) {
            navTabs.style.display = 'none';
        }
        
        // Make sure the masterlist header is visible
        const masterlistHeader = document.querySelector('#masterlistTab .card-header');
        if (masterlistHeader) {
            masterlistHeader.style.display = 'block';
        }
    }
    
    // Add event listeners to View Masterlist and Manage Students buttons
    document.addEventListener('click', function(e) {
        // Check if View Masterlist button was clicked
        if (e.target && e.target.textContent.includes('View Masterlist')) {
            showMasterlistOnly();
            e.preventDefault();
        }
        
        // Check if Manage Students button was clicked
        if (e.target && e.target.textContent.includes('Manage Students')) {
            showStudentManagementTabs();
            e.preventDefault();
        }
    });

    // File Upload functionality for CSV and Excel
    const uploadFileBtn = document.getElementById('uploadFileBtn');
    const fileUpload = document.getElementById('fileUpload');
    
    if (uploadFileBtn && fileUpload) {
        uploadFileBtn.addEventListener('click', function() {
            if (!fileUpload.files.length) {
                showToast('error', 'Please select a CSV or Excel file to upload');
                return;
            }
            
            const file = fileUpload.files[0];
            const fileExtension = file.name.split('.').pop().toLowerCase();
            
            if (!['csv', 'xlsx', 'xls'].includes(fileExtension)) {
                showToast('error', 'Please select a valid CSV or Excel file');
                return;
            }
            
            showLoading();
            // Simulate file upload processing
            setTimeout(() => {
                hideLoading();
                showToast('success', `${fileExtension.toUpperCase()} file processed successfully! Students have been added to the class.`);
                fileUpload.value = ''; // Clear file input
            }, 2000);
        });
    }

    // Export functionality (CSV and Excel only)
    const exportButtons = document.querySelectorAll('#bulkActionsTab .btn-outline-primary, #bulkActionsTab .btn-outline-success');
    exportButtons.forEach(button => {
        button.addEventListener('click', function() {
            const exportType = this.textContent.replace('Export as ', '').toLowerCase();
            showLoading();
            setTimeout(() => {
                hideLoading();
                showToast('success', `${exportType.toUpperCase()} file downloaded successfully!`);
            }, 1000);
        });
    });

    // Add Instructor Form Submission
    const addInstructorForm = document.getElementById('addInstructorForm');
    if (addInstructorForm) {
        addInstructorForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const firstName = document.getElementById('firstName').value.trim();
            const middleInitial = document.getElementById('middleInitial').value.trim();
            const lastName = document.getElementById('lastName').value.trim();
            const age = document.getElementById('age').value;
            const sex = document.getElementById('sex').value;
            const password = document.getElementById('password').value;
            const department = document.getElementById('department').value;
            
            // Basic validation
            if (!firstName || !lastName || !age || !sex || !password || !department) {
                showToast('error', 'Please fill in all required fields');
                return;
            }
            
            if (age < 18 || age > 99) {
                showToast('error', 'Age must be between 18 and 99');
                return;
            }
            
            showLoading();
            try {
                await fetchAdminJson('/api/admin/professors', {
                    method: 'POST',
                    body: JSON.stringify({
                        firstName,
                        middleInitial,
                        lastName,
                        age: Number(age),
                        sex,
                        department,
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

            tableBody.innerHTML = payload.map(professor => {
                const id = professor.professorId || '-';
                const name = professor.fullName || '-';
                const department = professor.department || '-';
                const position = professor.position || '-';
                const emailLabel = `${name.toLowerCase().replace(/[^a-z0-9]+/g, '.')}@school.edu`;

                return `
                    <tr>
                        <td>${name}</td>
                        <td><span class="badge bg-success">Active</span></td>
                        <td>
                            <button class="btn btn-outline-primary btn-sm"><icon></icon></button>
                            <button class="btn btn-outline-warning btn-sm"><icon></icon></button>
                            <button class="btn btn-outline-danger btn-sm"><icon></icon></button>
                        </td>
                        <td>${id}</td>
                        <td>${department} ${position !== '-' ? `(${position})` : ''}</td>
                        <td>${emailLabel}</td>
                    </tr>
                `;
            }).join('');
        } catch (error) {
            showToast('error', error.message || 'Unable to load instructors from database.');
        }
    }
    
    // Allow switching between tabs when in student management mode
    const bulkActionsNav = document.querySelector('a[href="#bulkActionsTab"]');
    
    if (bulkActionsNav) {
        bulkActionsNav.addEventListener('click', function(e) {
            // Only allow switching if we're in student management mode (nav tabs are visible)
            const navTabs = document.querySelector('.nav-tabs');
            
            if (navTabs && navTabs.style.display !== 'none') {
                const target = this.getAttribute('href');
                
                // Hide all tab panes
                tabPanes.forEach(pane => pane.classList.remove('show', 'active'));
                
                // Show target tab pane
                document.querySelector(target).classList.add('show', 'active');
                
                // Update active nav link
                navLinksTabs.forEach(navLink => navLink.classList.remove('active'));
                this.classList.add('active');
            }
            e.preventDefault();
        });
    }

    // Handle accordion behavior for instructor rows
    document.addEventListener('click', function(e) {
        // Check if we're clicking on an accordion toggle in the instructor management section
        if (e.target.closest('#instructor-management-section .accordion-toggle')) {
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
                        const bsCollapse = bootstrap.Collapse.getInstance(collapseElement);
                        if (bsCollapse) {
                            bsCollapse.hide();
                        }
                    }
                }
            });
        }
    });

    // Ensure only one instructor detail view is open at a time
    const instructorDetailCollapses = document.querySelectorAll('#instructor-management-section .collapse');
    instructorDetailCollapses.forEach(collapse => {
        collapse.addEventListener('show.bs.collapse', function() {
            // Hide all other collapses in the same section
            const section = this.closest('#instructor-management-section');
            const otherCollapses = section.querySelectorAll('.collapse.show');
            otherCollapses.forEach(other => {
                if (other !== this) {
                    const bsCollapse = bootstrap.Collapse.getInstance(other);
                    if (bsCollapse) {
                        bsCollapse.hide();
                    }
                }
            });
        });
    });

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

    // Keep track of the currently expanded row
    let currentlyExpandedRow = null;

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
                <td>${student.course}</td>
                <td>${student.section}</td>
                <td><span class="badge ${student.status === 'Active' ? 'bg-success' : 'bg-danger'}">${student.status}</span></td>
                <td>
                    <button class="btn btn-outline-primary btn-sm view-student" data-id="${student.id}">
                        <icon></icon> View
                    </button>
                    <button class="btn btn-outline-warning btn-sm edit-student" data-id="${student.id}">
                        <icon></icon> Edit
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
                                        <label class="form-label">Full Name</label>
                                        <input class="form-control edit-full-name" type="text" value="${student.fullName}" required>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label">Course</label>
                                        <select class="form-select edit-course" required>
                                            <option value="">Select Course</option>
                                            <option value="Computer Science" ${student.course === 'Computer Science' ? 'selected' : ''}>Computer Science</option>
                                            <option value="Business Administration" ${student.course === 'Business Administration' ? 'selected' : ''}>Business Administration</option>
                                            <option value="Engineering" ${student.course === 'Engineering' ? 'selected' : ''}>Engineering</option>
                                            <option value="Arts and Sciences" ${student.course === 'Arts and Sciences' ? 'selected' : ''}>Arts and Sciences</option>
                                        </select>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label">Section</label>
                                        <input class="form-control edit-section" type="text" value="${student.section}" required>
                                    </div>
                                </div>
                                <div class="row mb-3">
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
        const payload = {
            fullName: form.querySelector(".edit-full-name").value,
            course: form.querySelector(".edit-course").value,
            section: form.querySelector(".edit-section").value,
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
            const payload = await fetchAdminJson('/api/admin/students');
            students.length = 0;

            if (Array.isArray(payload)) {
                payload.forEach(student => {
                    students.push({
                        id: student.studentId || '',
                        fullName: student.fullName || '',
                        course: student.course || '-',
                        section: student.section || '-',
                        status: student.status || 'Active'
                    });
                });
            }

            renderStudentsTable();
        } catch (error) {
            showToast("error", error.message || "Unable to load students from database.");
        }
    }

    // Add new student
    document.getElementById("addStudentForm").addEventListener("submit", async function(e) {
        e.preventDefault();
        
        const studentId = document.getElementById("addStudentId").value;
        const fullName = document.getElementById("addFullName").value;
        const course = document.getElementById("addCourse").value;
        const section = document.getElementById("addSection").value;
        const status = document.getElementById("addStatus").value;
        const password = document.getElementById("addPassword").value;
        
        // Basic validation
        if (!studentId || !fullName || !course || !section || !status || !password) {
            showToast("error", "Please fill in all fields");
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

    if (profileForm) {
        profileForm.addEventListener('submit', function(e) {
            e.preventDefault();
            // In a real application, you would send the profile data to a server here
            showToast('success', 'Profile updated successfully!');
        });
    }

    if (changePasswordForm) {
        changePasswordForm.addEventListener('submit', function(e) {
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
            
            // In a real application, you would verify the current password and update it on the server
            // For this demo, we'll just simulate a successful password change
            showToast('success', 'Password changed successfully!');
            
            // Reset the form
            changePasswordForm.reset();
        });
    }
    
    // Handle dropdown profile link separately since it's not in the sidebar
    const dropdownProfileLink = document.querySelector('.dropdown-menu a[href="#profile-section"]');
    if (dropdownProfileLink) {
        dropdownProfileLink.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Remove active class from all sidebar links and sections
            navLinks.forEach(l => l.parentElement.classList.remove('active'));
            contentSections.forEach(section => section.classList.remove('active'));
            
            // Add active class to the profile section
            const profileSection = document.querySelector('#profile-section');
            if (profileSection) {
                profileSection.classList.add('active');
            }
            
            // Also activate the profile link in sidebar if it exists
            const sidebarProfileLink = document.querySelector('#sidebar a[href="#profile-section"]');
            if (sidebarProfileLink) {
                sidebarProfileLink.parentElement.classList.add('active');
            }
        });
    }
});
