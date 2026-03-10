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

    // Form Submission with Loading
    const createClassForm = document.getElementById('createClassForm');
    if (createClassForm) {
        createClassForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Validate subjects
            const subjectEntries = subjectsContainer.querySelectorAll('.subject-entry');
            let isValid = true;
            const subjects = [];
            
            subjectEntries.forEach((entry, index) => {
                const subjectName = entry.querySelector('.subject-name').value.trim();
                const subjectCode = entry.querySelector('.subject-code').value.trim();
                const instructorSelect = entry.querySelector('.subject-instructor');
                const instructorValue = instructorSelect.value;
                const instructorText = instructorSelect.options[instructorSelect.selectedIndex].text;
                const scheduleSlots = entry.querySelectorAll('.schedule-slot');
                const scheduleData = [];
                
                if (!subjectName || !subjectCode || !instructorValue) {
                    isValid = false;
                    showToast('error', `Please fill in all fields for subject ${index + 1}`);
                    return;
                }
                
                // Validate schedule data
                scheduleSlots.forEach(slot => {
                    const dayCheckboxes = slot.querySelectorAll('.day-checkbox:checked');
                    const startTime = slot.querySelector('.start-time').value;
                    const endTime = slot.querySelector('.end-time').value;
                    
                    if (dayCheckboxes.length === 0) {
                        isValid = false;
                        showToast('error', `Please select at least one day for subject ${index + 1}`);
                        return;
                    }
                    
                    if (!startTime || !endTime) {
                        isValid = false;
                        showToast('error', `Please fill in start and end times for subject ${index + 1}`);
                        return;
                    }
                    
                    const selectedDays = Array.from(dayCheckboxes).map(cb => cb.value);
                    scheduleData.push({
                        days: selectedDays,
                        startTime: startTime,
                        endTime: endTime
                    });
                });
                
                // Check for duplicate subject codes
                if (subjects.some(s => s.code === subjectCode)) {
                    isValid = false;
                    showToast('error', `Subject code ${subjectCode} is duplicated`);
                    return;
                }
                
                subjects.push({
                    name: subjectName,
                    code: subjectCode,
                    instructor: {
                        id: instructorValue,
                        name: instructorText
                    },
                    schedule: scheduleData
                });
            });
            
            if (!isValid) return;
            
            showLoading();
            
            // Simulate API call
            setTimeout(() => {
                hideLoading();
                showToast('success', 'Class created successfully with ' + subjects.length + ' subjects!');
                // Reset form
                this.reset();
                
                // Reset subjects container to initial state
                subjectsContainer.innerHTML = `
                    <div class="subject-entry mb-3 p-3 border rounded">
                        <div class="row align-items-center">
                            <div class="col-md-4">
                                <input type="text" class="form-control subject-name" placeholder="Subject Name" required>
                            </div>
                            <div class="col-md-3">
                                <input type="text" class="form-control subject-code" placeholder="Subject Code" required>
                            </div>
                            <div class="col-md-4">
                                <select class="form-select subject-instructor" required>
                                    <option value="" selected>Select Instructor</option>
                                    <option value="1">Dr. John Smith</option>
                                    <option value="2">Prof. Sarah Johnson</option>
                                    <option value="3">Dr. Michael Brown</option>
                                    <option value="4">Prof. Emily Davis</option>
                                </select>
                            </div>
                            <div class="col-md-1 text-center">
                                <button type="button" class="btn btn-outline-danger remove-subject p-2" style="min-width: 40px;">×</button>
                            </div>
                        </div>
                    </div>
                `;
                
                // Re-attach remove functionality to initial subject entry
                const initialRemoveBtn = subjectsContainer.querySelector('.remove-subject');
                if (initialRemoveBtn) {
                    initialRemoveBtn.addEventListener('click', function() {
                        if (subjectsContainer.children.length > 1) {
                            this.closest('.subject-entry').remove();
                        }
                    });
                }
            }, 1500);
        });
    }

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
    
    // Save instructor changes functionality
    function initializeSaveInstructorListener() {
        const saveBtn = document.getElementById('saveInstructorChanges');
        if (saveBtn) {
            // Remove any existing listeners first
            saveBtn.removeEventListener('click', saveInstructorChanges);
            saveBtn.addEventListener('click', saveInstructorChanges);
        }
    }
    
    function saveInstructorChanges() {
        const instructorId = document.getElementById('editInstructorId').value;
        const firstName = document.getElementById('editInstructorFirstName').value.trim();
        const middleInitial = document.getElementById('editInstructorMiddleInitial').value.trim();
        const lastName = document.getElementById('editInstructorLastName').value.trim();
        const email = document.getElementById('editInstructorEmail').value.trim();
        const department = document.getElementById('editInstructorDepartment').value;
        const status = document.getElementById('editInstructorStatus').value;
        
        // Basic validation
        if (!firstName || !lastName || !email || !department || !status) {
            showToast('error', 'Please fill in all required fields');
            return;
        }
        
        // Email validation
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            showToast('error', 'Please enter a valid email address');
            return;
        }
        
        // Find the instructor row in the table
        const instructorRow = document.querySelector(`tr button.edit-instructor[data-id="${instructorId}"]`)?.closest('tr');
        if (!instructorRow) {
            showToast('error', 'Instructor not found');
            return;
        }
        
        const cells = instructorRow.querySelectorAll('td');
        
        // Format the full name with title (attempt to preserve existing title)
        let title = '';
        const originalName = cells[0].textContent;
        if (originalName.includes('Dr.')) {
            title = 'Dr. ';
        } else if (originalName.includes('Prof.')) {
            title = 'Prof. ';
        }
        
        const fullName = `${title}${firstName} ${middleInitial ? middleInitial + '. ' : ''}${lastName}`;
        
        // Update the row data
        cells[0].textContent = fullName;
        cells[2].textContent = department; // Department
        cells[3].textContent = email; // Email
        
        // Update status badge
        const statusCell = cells[5];
        const badge = statusCell.querySelector('.badge');
        badge.textContent = status;
        badge.className = 'badge ' + (status === 'Active' ? 'bg-success' : 'bg-warning');
        
        // Update the instructor details panel
        const detailsPanel = document.getElementById(`instructorDetails-${instructorId}`);
        if (detailsPanel) {
            // Update name in the card
            const nameElement = detailsPanel.querySelector('h5');
            if (nameElement) {
                nameElement.textContent = fullName;
            }
            
            // Update department text
            const departmentElement = detailsPanel.querySelector('.text-muted');
            if (departmentElement) {
                departmentElement.textContent = `${department} Department`;
            }
            
            // Update email
            const emailElement = detailsPanel.querySelector('.card-body p:nth-child(3)');
            if (emailElement) {
                emailElement.textContent = email;
                emailElement.innerHTML = `<icon></icon> ${email}`;
            }
        }
        
        // Hide the modal
        const editModal = bootstrap.Modal.getInstance(document.getElementById('editInstructorModal'));
        editModal.hide();
        
        showToast('success', `Instructor ${fullName} updated successfully`);
    }
    
    // Initialize instructor button functionality
    initializeInstructorButtons();
    initializeSaveInstructorListener();

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
        addInstructorForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const firstName = document.getElementById('firstName').value.trim();
            const middleInitial = document.getElementById('middleInitial').value.trim();
            const lastName = document.getElementById('lastName').value.trim();
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const department = document.getElementById('department').value;
            
            // Basic validation
            if (!firstName || !lastName || !email || !password || !department) {
                showToast('error', 'Please fill in all required fields');
                return;
            }
            
            // Email validation
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                showToast('error', 'Please enter a valid email address');
                return;
            }
            
            showLoading();
            
            // Simulate API call
            setTimeout(() => {
                hideLoading();
                
                // Generate employee ID
                const employeeId = 'EMP' + String(Math.floor(Math.random() * 9000) + 1000).padStart(4, '0');
                const fullName = `${firstName} ${middleInitial ? middleInitial + '. ' : ''}${lastName}`;
                
                // Clear existing rows from tbody before adding new ones to prevent duplication
                const tbody = document.querySelector('#instructor-management-section table tbody');
                tbody.innerHTML = '';
                
                // Add to instructors table (with correct column order: Name, Employee ID, Department, Email, Actions, Status)
                const newRow = document.createElement('tr');
                newRow.className = 'accordion-toggle';
                newRow.setAttribute('data-bs-toggle', 'collapse');
                newRow.setAttribute('data-bs-target', `#instructorDetails-${employeeId}`);
                newRow.setAttribute('aria-controls', `instructorDetails-${employeeId}`);
                newRow.setAttribute('aria-expanded', 'false');
                newRow.setAttribute('role', 'button');
                newRow.setAttribute('tabindex', '0');
                newRow.innerHTML = `
                    <td>${fullName}</td>
                    <td>${employeeId}</td>
                    <td>${department}</td>
                    <td>${email}</td>
                    <td>
                        <button class="btn btn-outline-primary btn-sm view-instructor" data-id="${employeeId}" title="View Details">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-eye" viewBox="0 0 16 16">
                                <path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>
                                <path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z"/>
                            </svg>
                        </button>
                        <button class="btn btn-outline-warning btn-sm edit-instructor" data-id="${employeeId}" title="Edit Instructor">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-pencil" viewBox="0 0 16 16">
                                <path d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708l-10 10a.5.5 0 0 1-.168.11l-5 2a.5.5 0 0 1-.65-.65l2-5a.5.5 0 0 1 .11-.168l10-10zM11.207 2.5 13.5 4.793 14.793 3.5 12.5 1.207 11.207 2.5zm1.586 3L10.5 3.207 4 9.707V10h.5a.5.5 0 0 1 .5.5v.5h.5a.5.5 0 0 1 .5.5v.5h.293l6.5-6.5zm-9.761 5.175-.106.106-1.528 3.821 3.821-1.528.106-.106A.5.5 0 0 1 5 12.5V12h-.5a.5.5 0 0 1-.5-.5V11h-.5a.5.5 0 0 1-.468-.325z"/>
                            </svg>
                        </button>
                        <button class="btn btn-outline-danger btn-sm delete-instructor" data-id="${employeeId}" title="Delete Instructor">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-trash" viewBox="0 0 16 16">
                                <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
                                <path fill-rule="evenodd" d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2 5v1h12V5H2z"/>
                            </svg>
                        </button>
                    </td>
                    <td><span class="badge bg-success">Active</span></td>
                `;
                
                tbody.insertBefore(newRow, tbody.firstChild);
                
                // Add event listeners to the new buttons
                attachInstructorButtonListeners(newRow);
                
                // Reset form and collapse it
                addInstructorForm.reset();
                const collapseElement = document.getElementById('instructorFormCollapse');
                const bsCollapse = bootstrap.Collapse.getInstance(collapseElement);
                if (bsCollapse) {
                    bsCollapse.hide();
                }
                
                showToast('success', `Instructor ${fullName} added successfully with ID ${employeeId}`);
            }, 1500);
        });
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

    // Class Masterlist Hierarchical Navigation System
    document.addEventListener('click', function(e) {
        // Masterlist Button Clicked - Toggle Sections Panel
        if (e.target && e.target.id && e.target.id.startsWith('viewMasterlistBtn')) {
            e.stopPropagation();
            
            const container = e.target.closest('.collapse');
            if (!container) return;
            
            const containerId = container.id.replace('instructorDetails', '');
            const sectionsPanel = container.querySelector(`#sectionsPanel${containerId}`);
            const masterlistPanel = container.querySelector(`#masterlistPanel${containerId}`);
            
            if (sectionsPanel && masterlistPanel) {
                // Toggle sections panel visibility
                sectionsPanel.classList.toggle('d-none');
                
                // If sections panel is being closed, also hide masterlist panel
                if (sectionsPanel.classList.contains('d-none')) {
                    masterlistPanel.classList.add('d-none');
                } else {
                    // Ensure masterlist panel is hidden when sections panel opens
                    masterlistPanel.classList.add('d-none');
                }
                
                console.log(`Sections Panel: ${sectionsPanel.classList.contains('d-none') ? 'hidden' : 'visible'}`);
                console.log(`Masterlist Panel: ${masterlistPanel.classList.contains('d-none') ? 'hidden' : 'visible'}`);
            }
        }
        
        // Section Item Clicked - Show Masterlist Panel
        if (e.target && e.target.classList.contains('section-item')) {
            e.preventDefault();
            e.stopPropagation();
            
            const container = e.target.closest('.collapse');
            if (!container) return;
            
            const containerId = container.id.replace('instructorDetails', '');
            const sectionsPanel = container.querySelector(`#sectionsPanel${containerId}`);
            const masterlistPanel = container.querySelector(`#masterlistPanel${containerId}`);
            const currentSectionSpan = container.querySelector(`#currentSection${containerId}`);
            
            // Update section label
            if (currentSectionSpan) {
                currentSectionSpan.textContent = e.target.getAttribute('data-section');
                // Load students for this section
                loadStudentsForSection(containerId, e.target.getAttribute('data-section'));
            }
            
            // Show masterlist panel and hide sections panel
            if (sectionsPanel && masterlistPanel) {
                sectionsPanel.classList.add('d-none');
                masterlistPanel.classList.remove('d-none');
                
                console.log(`Section "${e.target.getAttribute('data-section')}" selected`);
                console.log(`Masterlist Panel now visible`);
            }
        }
        
        // Close button in masterlist panel
        if (e.target && e.target.classList.contains('close-masterlist')) {
            e.stopPropagation();
            
            const container = e.target.closest('.collapse');
            if (!container) return;
            
            const containerId = container.id.replace('instructorDetails', '');
            const sectionsPanel = container.querySelector(`#sectionsPanel${containerId}`);
            const masterlistPanel = container.querySelector(`#masterlistPanel${containerId}`);
            
            // Hide masterlist panel and show sections panel
            if (sectionsPanel && masterlistPanel) {
                masterlistPanel.classList.add('d-none');
                sectionsPanel.classList.remove('d-none');
            }
        }
    });

    // Initialize panels with correct starting state
    function initializeMasterlistPanels() {
        document.querySelectorAll('[id^="sectionsPanel"]').forEach(panel => {
            panel.classList.add('d-none');
        });
        
        document.querySelectorAll('[id^="masterlistPanel"]').forEach(panel => {
            panel.classList.add('d-none');
        });
        
        console.log('All panels initialized to hidden state');
    }

    // Sample student data
    const sampleStudents = {
        'Section A': [
            { id: 'S001', name: 'Alice Johnson', subject: 'Data Structures', grade: 'A' },
            { id: 'S002', name: 'Bob Williams', subject: 'Data Structures', grade: 'B+' },
            { id: 'S003', name: 'Charlie Brown', subject: 'Algorithms', grade: 'A-' },
            { id: 'S004', name: 'Diana Miller', subject: 'Database Systems', grade: 'B' }
        ],
        'Section B': [
            { id: 'S005', name: 'Eva Davis', subject: 'Data Structures', grade: 'A' },
            { id: 'S006', name: 'Frank Wilson', subject: 'Algorithms', grade: 'B' },
            { id: 'S007', name: 'Grace Lee', subject: 'Database Systems', grade: 'A-' }
        ],
        'Section C': [
            { id: 'S008', name: 'Henry Martin', subject: 'Data Structures', grade: 'B+' },
            { id: 'S009', name: 'Ivy Thompson', subject: 'Algorithms', grade: 'A' },
            { id: 'S010', name: 'Jack Anderson', subject: 'Database Systems', grade: 'B-' }
        ]
    };

    // Load students for selected section
    function loadStudentsForSection(containerId, sectionName) {
        const masterlistPanel = document.querySelector(`#masterlistPanel${containerId}`);
        const tableBody = masterlistPanel?.querySelector('.table tbody');
        
        if (!tableBody) return;
        
        const students = sampleStudents[sectionName] || [];
        
        tableBody.innerHTML = '';
        students.forEach(student => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${student.id}</td>
                <td>${student.name}</td>
                <td>${student.subject}</td>
                <td>${student.grade}</td>
                <td>
                    <button class="btn btn-outline-primary btn-sm">
                        <icon></icon> Edit
                    </button>
                    <button class="btn btn-outline-warning btn-sm">
                        <icon></icon> Grade
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });
        
        console.log(`Loaded ${students.length} students for ${sectionName}`);
    }

    // Close sections panel button functionality
    document.addEventListener('click', function(e) {
        if (e.target && e.target.classList.contains('close-sections-panel')) {
            e.stopPropagation();
            
            const container = e.target.closest('.collapse');
            if (!container) return;
            
            const containerId = container.id.replace('instructorDetails', '');
            const sectionsPanel = container.querySelector(`#sectionsPanel${containerId}`);
            const masterlistBtn = container.querySelector(`#viewMasterlistBtn${containerId}`);
            
            // Hide sections panel
            if (sectionsPanel) {
                sectionsPanel.classList.add('d-none');
            }
            
            // Ensure masterlist panel is also hidden
            const masterlistPanel = container.querySelector(`#masterlistPanel${containerId}`);
            if (masterlistPanel) {
                masterlistPanel.classList.add('d-none');
            }
            
            console.log('Sections panel closed');
        }
        
        // Breadcrumb navigation
        if (e.target && e.target.getAttribute('data-action') === 'back-to-sections') {
            e.stopPropagation();
            e.preventDefault();
            
            const container = e.target.closest('.collapse');
            if (!container) return;
            
            const containerId = container.id.replace('instructorDetails', '');
            const sectionsPanel = container.querySelector(`#sectionsPanel${containerId}`);
            const masterlistPanel = container.querySelector(`#masterlistPanel${containerId}`);
            
            // Show sections panel and hide masterlist panel
            if (sectionsPanel && masterlistPanel) {
                masterlistPanel.classList.add('d-none');
                sectionsPanel.classList.remove('d-none');
                
                console.log('Navigated back to sections via breadcrumb');
            }
        }
    });

    // Initialize panels when page loads
    initializeMasterlistPanels();
    
    // Initialize existing instructor buttons
    initializeInstructorButtons();

    // Tooltip initialization
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Student Management Functionality
    const students = [
        { id: "S001", fullName: "Alice Johnson", course: "Computer Science", section: "Section A", status: "Active" },
        { id: "S002", fullName: "Bob Williams", course: "Business Administration", section: "Section B", status: "Dropped" },
        { id: "S003", fullName: "Charlie Brown", course: "Engineering", section: "Section C", status: "Active" },
        { id: "S004", fullName: "Diana Miller", course: "Arts and Sciences", section: "Section A", status: "Active" }
    ];

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
    function saveStudentChanges(form) {
        const studentId = form.getAttribute("data-id");
        const student = students.find(s => s.id === studentId);
        
        if (student) {
            student.fullName = form.querySelector(".edit-full-name").value;
            student.course = form.querySelector(".edit-course").value;
            student.section = form.querySelector(".edit-section").value;
            student.status = form.querySelector(".edit-status").value;
            
            // Hide the panel
            hideStudentDetails(studentId);
            
            // Re-render the table
            renderStudentsTable();
            
            // Show success message
            showToast("success", "Student information updated successfully!");
        }
    }

    // Add new student
    document.getElementById("addStudentForm").addEventListener("submit", function(e) {
        e.preventDefault();
        
        const studentId = document.getElementById("addStudentId").value;
        const fullName = document.getElementById("addFullName").value;
        const course = document.getElementById("addCourse").value;
        const section = document.getElementById("addSection").value;
        const status = document.getElementById("addStatus").value;
        
        // Basic validation
        if (!studentId || !fullName || !course || !section || !status) {
            showToast("error", "Please fill in all fields");
            return;
        }
        
        // Check if student ID already exists
        if (students.some(s => s.id === studentId)) {
            showToast("error", "Student ID already exists");
            return;
        }
        
        // Add new student
        students.push({
            id: studentId,
            fullName: fullName,
            course: course,
            section: section,
            status: status
        });
        
        // Collapse the form
        const collapseElement = document.getElementById("studentFormCollapse");
        const bsCollapse = bootstrap.Collapse.getInstance(collapseElement);
        if (bsCollapse) {
            bsCollapse.hide();
        } else {
            collapseElement.classList.remove("show");
        }
        
        // Reset form
        document.getElementById("addStudentForm").reset();
        
        // Re-render the table
        renderStudentsTable();
        
        // Show success message
        showToast("success", "Student added successfully!");
    });

    // Initialize students table on page load
    if (document.getElementById("student-management-section")) {
        renderStudentsTable();
    }
    
    // Initialize instructor buttons functionality
    function initializeInstructorButtons() {
        document.querySelectorAll('.view-instructor, .edit-instructor, .delete-instructor').forEach(button => {
            button.addEventListener('click', function(e) {
                e.stopPropagation(); // Prevent row collapse when clicking buttons
                
                const instructorId = this.getAttribute('data-id');
                const row = this.closest('tr');
                
                if (this.classList.contains('view-instructor')) {
                    viewInstructor(instructorId, row);
                } else if (this.classList.contains('edit-instructor')) {
                    editInstructor(instructorId, row);
                } else if (this.classList.contains('delete-instructor')) {
                    deleteInstructor(instructorId, row);
                }
            });
        });
    }
    
    function attachInstructorButtonListeners(row) {
        row.querySelectorAll('.view-instructor, .edit-instructor, .delete-instructor').forEach(button => {
            button.addEventListener('click', function(e) {
                e.stopPropagation(); // Prevent row collapse when clicking buttons
                
                const instructorId = this.getAttribute('data-id');
                const instructorRow = this.closest('tr');
                
                if (this.classList.contains('view-instructor')) {
                    viewInstructor(instructorId, instructorRow);
                } else if (this.classList.contains('edit-instructor')) {
                    editInstructor(instructorId, instructorRow);
                } else if (this.classList.contains('delete-instructor')) {
                    deleteInstructor(instructorId, instructorRow);
                }
            });
        });
    }
    
    function viewInstructor(instructorId, row) {
        const cells = row.querySelectorAll('td');
        const name = cells[0].textContent;
        const employeeId = cells[1].textContent;
        const department = cells[2].textContent;
        const email = cells[3].textContent;
        const status = cells[5].querySelector('.badge').textContent;
        
        // Check if an instructor details row already exists for this instructor
        const existingDetailsRow = document.getElementById(`instructorDetails-${instructorId}`);
        
        if (existingDetailsRow) {
            // If the row already exists, just toggle its visibility
            const bsCollapse = bootstrap.Collapse.getOrCreateInstance(existingDetailsRow);
            
            // Close all other instructor detail rows first
            document.querySelectorAll('#instructor-management-section .collapse.show').forEach(collapse => {
                if (collapse !== existingDetailsRow) {
                    const otherCollapse = bootstrap.Collapse.getInstance(collapse);
                    if (otherCollapse) {
                        otherCollapse.hide();
                    }
                }
            });
            
            // Toggle the current row
            if (!bsCollapse._isShown()) {
                bsCollapse.show();
            } else {
                bsCollapse.hide();
            }
            return; // Exit the function since we're just toggling
        }
        
        // Check if details row already exists, if yes, don't create another one
        if (document.getElementById(`instructorDetails-${instructorId}`)) {
            console.log(`Details row for instructor ${instructorId} already exists`);
            return;
        }
        
        // Create details row only if it doesn't exist
        const detailsRow = document.createElement('tr');
        detailsRow.className = 'collapse';
        detailsRow.id = `instructorDetails-${instructorId}`;
        detailsRow.innerHTML = `
            <td colspan="6" class="p-0">
                <div class="p-3">
                    <div class="row">
                        <div class="col-md-4">
                            <div class="card">
                                <div class="card-body text-center">
                                    <div class="bg-light rounded-circle d-inline-flex justify-content-center align-items-center mb-3" style="width: 80px; height: 80px;">
                                        <i class="bi bi-person-fill" style="font-size: 2rem;"></i>
                                    </div>
                                    <h5>${name}</h5>
                                    <p class="text-muted">${department} Department</p>
                                    <p><icon></icon> ${email}</p>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-8">
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <div class="card h-100">
                                        <div class="card-header">
                                            <h6 class="mb-0 card-title"><icon></icon> Subjects Handled</h6>
                                        </div>
                                        <div class="card-body">
                                            <span class="badge bg-primary me-1 mb-1">Data Structures</span>
                                            <span class="badge bg-primary me-1 mb-1">Algorithms</span>
                                            <span class="badge bg-primary me-1 mb-1">Database Systems</span>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <div class="card h-100">
                                        <div class="card-header">
                                            <h6 class="mb-0 card-title"><icon></icon> Sections Handled</h6>
                                        </div>
                                        <div class="card-body">
                                            <span class="badge bg-secondary me-1 mb-1">Section A</span>
                                            <span class="badge bg-secondary me-1 mb-1">Section B</span>
                                            <span class="badge bg-secondary me-1 mb-1">Section C</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <button class="btn btn-primary w-100" id="viewMasterlistBtn-${instructorId}"><icon></icon> View Class Masterlist</button>
                            <div class="mt-3 d-none" id="sectionsPanel-${instructorId}">
                                <div class="card">
                                    <div class="card-header bg-light d-flex justify-content-between align-items-center">
                                        <h6 class="mb-0">Select Section</h6>
                                        <button class="btn btn-sm btn-outline-secondary close-sections-panel"><icon></icon> Close</button>
                                    </div>
                                    <div class="card-body">
                                        <div class="list-group">
                                            <button class="list-group-item list-group-item-action section-item d-flex justify-content-between align-items-center" data-section="Section A">
                                                Section A
                                                <span class="badge bg-primary rounded-pill">4 students</span>
                                            </button>
                                            <button class="list-group-item list-group-item-action section-item d-flex justify-content-between align-items-center" data-section="Section B">
                                                Section B
                                                <span class="badge bg-primary rounded-pill">3 students</span>
                                            </button>
                                            <button class="list-group-item list-group-item-action section-item d-flex justify-content-between align-items-center" data-section="Section C">
                                                Section C
                                                <span class="badge bg-primary rounded-pill">3 students</span>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="mt-3 d-none" id="masterlistPanel-${instructorId}">
                                <div class="card">
                                    <div class="card-header bg-light">
                                        <div class="d-flex justify-content-between align-items-center">
                                            <div>
                                                <div class="breadcrumb-nav mb-1">
                                                    <span class="clickable" data-action="back-to-sections">Sections</span>
                                                    <span class="separator">›</span>
                                                    <span class="current">Class Masterlist</span>
                                                </div>
                                                <h6 class="mb-0">Class Masterlist - <span id="currentSection-${instructorId}">Section A</span></h6>
                                            </div>
                                            <button class="btn btn-sm btn-outline-secondary close-masterlist"><icon></icon> Back to Sections</button>
                                            </div>
                                        </div>
                                        <div class="card-body">
                                            <div class="table-responsive">
                                                <table class="table table-striped table-hover">
                                                    <thead>
                                                        <tr>
                                                            <th>Student ID</th>
                                                            <th>Student Name</th>
                                                            <th>Subject</th>
                                                            <th>Grade</th>
                                                            <th>Actions</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody></tbody>
                                                </table>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </td>
        `;
        
        row.parentNode.insertBefore(detailsRow, row.nextSibling);
        
        // Initialize the collapse functionality
        row.setAttribute('data-bs-target', `#instructorDetails-${instructorId}`);
        row.setAttribute('data-bs-toggle', 'collapse');
        
        // Initialize masterlist functionality for this instructor
        initInstructorMasterlistFunctionality(instructorId);
        
        // Show the details panel
        const bsCollapse = bootstrap.Collapse.getOrCreateInstance(detailsRow);
        
        // Close all other instructor detail rows first
        document.querySelectorAll('#instructor-management-section .collapse.show').forEach(collapse => {
            if (collapse !== detailsRow) {
                const otherCollapse = bootstrap.Collapse.getInstance(collapse);
                if (otherCollapse) {
                    otherCollapse.hide();
                }
            }
        });
        
        bsCollapse.show();
    }
    
    function initInstructorMasterlistFunctionality(instructorId) {
        // Masterlist Button Clicked - Toggle Sections Panel
        const masterlistBtn = document.getElementById(`viewMasterlistBtn-${instructorId}`);
        if (masterlistBtn) {
            masterlistBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                
                const sectionsPanel = document.getElementById(`sectionsPanel-${instructorId}`);
                const masterlistPanel = document.getElementById(`masterlistPanel-${instructorId}`);
                
                if (sectionsPanel && masterlistPanel) {
                    // Toggle sections panel visibility
                    sectionsPanel.classList.toggle('d-none');
                    
                    // If sections panel is being closed, also hide masterlist panel
                    if (sectionsPanel.classList.contains('d-none')) {
                        masterlistPanel.classList.add('d-none');
                    } else {
                        // Ensure masterlist panel is hidden when sections panel opens
                        masterlistPanel.classList.add('d-none');
                    }
                }
            });
        }
        
        // Section Item Clicked - Show Masterlist Panel
        document.addEventListener('click', function(e) {
            if (e.target && e.target.classList.contains('section-item') && e.target.closest(`#sectionsPanel-${instructorId}`)) {
                e.preventDefault();
                e.stopPropagation();
                
                const sectionsPanel = document.getElementById(`sectionsPanel-${instructorId}`);
                const masterlistPanel = document.getElementById(`masterlistPanel-${instructorId}`);
                const currentSectionSpan = document.getElementById(`currentSection-${instructorId}`);
                
                // Update section label
                if (currentSectionSpan) {
                    currentSectionSpan.textContent = e.target.getAttribute('data-section');
                    // Load students for this section
                    loadStudentsForInstructorSection(instructorId, e.target.getAttribute('data-section'));
                }
                
                // Show masterlist panel and hide sections panel
                if (sectionsPanel && masterlistPanel) {
                    sectionsPanel.classList.add('d-none');
                    masterlistPanel.classList.remove('d-none');
                }
            }
        });
        
        // Close button in masterlist panel
        document.addEventListener('click', function(e) {
            if (e.target && e.target.classList.contains('close-masterlist') && e.target.closest(`#masterlistPanel-${instructorId}`)) {
                e.stopPropagation();
                
                const sectionsPanel = document.getElementById(`sectionsPanel-${instructorId}`);
                const masterlistPanel = document.getElementById(`masterlistPanel-${instructorId}`);
                
                // Hide masterlist panel and show sections panel
                if (sectionsPanel && masterlistPanel) {
                    masterlistPanel.classList.add('d-none');
                    sectionsPanel.classList.remove('d-none');
                }
            }
        });
        
        // Close sections panel button functionality
        document.addEventListener('click', function(e) {
            if (e.target && e.target.classList.contains('close-sections-panel') && e.target.closest(`#sectionsPanel-${instructorId}`)) {
                e.stopPropagation();
                
                const sectionsPanel = document.getElementById(`sectionsPanel-${instructorId}`);
                const masterlistBtn = document.getElementById(`viewMasterlistBtn-${instructorId}`);
                
                // Hide sections panel
                if (sectionsPanel) {
                    sectionsPanel.classList.add('d-none');
                }
                
                // Ensure masterlist panel is also hidden
                const masterlistPanel = document.getElementById(`masterlistPanel-${instructorId}`);
                if (masterlistPanel) {
                    masterlistPanel.classList.add('d-none');
                }
            }
        });
        
        // Breadcrumb navigation
        document.addEventListener('click', function(e) {
            if (e.target && e.target.getAttribute('data-action') === 'back-to-sections' && e.target.closest(`#masterlistPanel-${instructorId}`)) {
                e.stopPropagation();
                e.preventDefault();
                
                const sectionsPanel = document.getElementById(`sectionsPanel-${instructorId}`);
                const masterlistPanel = document.getElementById(`masterlistPanel-${instructorId}`);
                
                // Show sections panel and hide masterlist panel
                if (sectionsPanel && masterlistPanel) {
                    masterlistPanel.classList.add('d-none');
                    sectionsPanel.classList.remove('d-none');
                }
            }
        });
    }
    
    function loadStudentsForInstructorSection(instructorId, sectionName) {
        const masterlistPanel = document.getElementById(`masterlistPanel-${instructorId}`);
        const tableBody = masterlistPanel?.querySelector('.table tbody');
        
        if (!tableBody) return;
        
        const sampleStudents = {
            'Section A': [
                { id: 'S001', name: 'Alice Johnson', subject: 'Data Structures', grade: 'A' },
                { id: 'S002', name: 'Bob Williams', subject: 'Data Structures', grade: 'B+' },
                { id: 'S003', name: 'Charlie Brown', subject: 'Algorithms', grade: 'A-' },
                { id: 'S004', name: 'Diana Miller', subject: 'Database Systems', grade: 'B' }
            ],
            'Section B': [
                { id: 'S005', name: 'Eva Davis', subject: 'Data Structures', grade: 'A' },
                { id: 'S006', name: 'Frank Wilson', subject: 'Algorithms', grade: 'B' },
                { id: 'S007', name: 'Grace Lee', subject: 'Database Systems', grade: 'A-' }
            ],
            'Section C': [
                { id: 'S008', name: 'Henry Martin', subject: 'Data Structures', grade: 'B+' },
                { id: 'S009', name: 'Ivy Thompson', subject: 'Algorithms', grade: 'A' },
                { id: 'S010', name: 'Jack Anderson', subject: 'Database Systems', grade: 'B-' }
            ]
        };
        
        const students = sampleStudents[sectionName] || [];
        
        tableBody.innerHTML = '';
        students.forEach(student => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${student.id}</td>
                <td>${student.name}</td>
                <td>${student.subject}</td>
                <td>${student.grade}</td>
                <td>
                    <button class="btn btn-outline-primary btn-sm">
                        <icon></icon> Edit
                    </button>
                    <button class="btn btn-outline-warning btn-sm">
                        <icon></icon> Grade
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });
    }
    
    function editInstructor(instructorId, row) {
        const cells = row.querySelectorAll('td');
        const name = cells[0].textContent;
        const employeeId = cells[1].textContent;
        const department = cells[2].textContent;
        const email = cells[3].textContent;
        const status = cells[5].querySelector('.badge').textContent;
        
        // Improved name parsing logic
        let firstName = '';
        let middleInitial = '';
        let lastName = '';
        
        // Extract title if present
        let title = '';
        let nameWithoutTitle = name;
        if (name.includes('Dr.')) {
            title = 'Dr. ';
            nameWithoutTitle = name.replace('Dr.', '').trim();
        } else if (name.includes('Prof.')) {
            title = 'Prof. ';
            nameWithoutTitle = name.replace('Prof.', '').trim();
        }
        
        // Parse the remaining name parts
        const nameParts = nameWithoutTitle.split(' ').filter(part => part.trim() !== '');
        
        if (nameParts.length === 1) {
            lastName = nameParts[0];
        } else if (nameParts.length === 2) {
            firstName = nameParts[0];
            lastName = nameParts[1];
        } else if (nameParts.length >= 3) {
            firstName = nameParts[0];
            lastName = nameParts[nameParts.length - 1];
            // Everything in between is middle name/initial
            const middleParts = nameParts.slice(1, nameParts.length - 1);
            middleInitial = middleParts.join(' '); // Handle multiple middle names
        }
        
        // Populate the edit modal
        document.getElementById('editInstructorId').value = instructorId;
        document.getElementById('editInstructorFirstName').value = firstName;
        document.getElementById('editInstructorMiddleInitial').value = middleInitial;
        document.getElementById('editInstructorEmail').value = email;
        document.getElementById('editInstructorDepartment').value = department;
        document.getElementById('editInstructorStatus').value = status;
        
        // Set the last name field and ensure it's not empty
        document.getElementById('editInstructorLastName').value = lastName;
        
        // Initialize the save listener
        initializeSaveInstructorListener();
        
        // Show the modal
        const modalElement = document.getElementById('editInstructorModal');
        const editModal = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
        editModal.show();
    }
    
    function deleteInstructor(instructorId, row) {
        const cells = row.querySelectorAll('td');
        const name = cells[0].textContent;
        
        // Show confirmation modal
        const modal = document.getElementById('confirmationModal');
        const modalInstance = bootstrap.Modal.getInstance(modal) || new bootstrap.Modal(modal);
        const confirmBtn = document.getElementById('confirmAction');
        
        // Update modal content
        modal.querySelector('.modal-body span').textContent = `Are you sure you want to delete instructor ${name} (${instructorId})? This action cannot be undone.`;
        
        // Set confirmation action
        confirmBtn.onclick = function() {
            showLoading();
            setTimeout(() => {
                hideLoading();
                row.remove();
                showToast('success', `Instructor ${name} has been deleted successfully`);
                modalInstance.hide();
            }, 1000);
        };
        
        modalInstance.show();
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

    // Password toggle functionality for instructor form
    const toggleInstructorPassword = document.getElementById('toggleInstructorPassword');
    const instructorPassword = document.getElementById('password');
    const instructorPasswordIcon = document.getElementById('instructorPasswordIcon');

    if (toggleInstructorPassword && instructorPassword && instructorPasswordIcon) {
        toggleInstructorPassword.addEventListener('click', function() {
            if (instructorPassword.type === 'password') {
                instructorPassword.type = 'text';
                instructorPasswordIcon.src = '/visible.png';
            } else {
                instructorPassword.type = 'password';
                instructorPasswordIcon.src = '/hide.png';
            }
        });
    }

    // Password toggle functionality for student form
    const toggleStudentPassword = document.getElementById('toggleStudentPassword');
    const studentPassword = document.getElementById('addPassword');
    const studentPasswordIcon = document.getElementById('studentPasswordIcon');

    if (toggleStudentPassword && studentPassword && studentPasswordIcon) {
        toggleStudentPassword.addEventListener('click', function() {
            if (studentPassword.type === 'password') {
                studentPassword.type = 'text';
                studentPasswordIcon.src = '/visible.png';
            } else {
                studentPassword.type = 'password';
                studentPasswordIcon.src = '/hide.png';
            }
        });
    }

    // Password toggle functionality for profile section password fields
    const toggleCurrentPassword = document.getElementById('toggleCurrentPassword');
    const toggleCurrentPasswordIcon = document.getElementById('toggleCurrentPasswordIcon');
    const toggleNewPassword = document.getElementById('toggleNewPassword');
    const toggleNewPasswordIcon = document.getElementById('toggleNewPasswordIcon');
    const toggleConfirmNewPassword = document.getElementById('toggleConfirmNewPassword');
    const toggleConfirmNewPasswordIcon = document.getElementById('toggleConfirmNewPasswordIcon');

    if (toggleCurrentPassword && toggleCurrentPasswordIcon) {
        toggleCurrentPassword.addEventListener('click', function() {
            const currentPassword = document.getElementById('currentPassword');
            if (currentPassword.type === 'password') {
                currentPassword.type = 'text';
                toggleCurrentPasswordIcon.src = '/visible.png';
            } else {
                currentPassword.type = 'password';
                toggleCurrentPasswordIcon.src = '/hide.png';
            }
        });
    }

    if (toggleNewPassword && toggleNewPasswordIcon) {
        toggleNewPassword.addEventListener('click', function() {
            const newPassword = document.getElementById('newPassword');
            if (newPassword.type === 'password') {
                newPassword.type = 'text';
                toggleNewPasswordIcon.src = '/visible.png';
            } else {
                newPassword.type = 'password';
                toggleNewPasswordIcon.src = '/hide.png';
            }
        });
    }

    if (toggleConfirmNewPassword && toggleConfirmNewPasswordIcon) {
        toggleConfirmNewPassword.addEventListener('click', function() {
            const confirmNewPassword = document.getElementById('confirmNewPassword');
            if (confirmNewPassword.type === 'password') {
                confirmNewPassword.type = 'text';
                toggleConfirmNewPasswordIcon.src = '/visible.png';
            } else {
                confirmNewPassword.type = 'password';
                toggleConfirmNewPasswordIcon.src = '/hide.png';
            }
        });
    }
});