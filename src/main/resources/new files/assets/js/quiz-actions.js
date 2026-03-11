// Quiz Actions JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Quiz Filtering and Search functionality
    const quizSubjectFilter = document.getElementById('quizSubjectFilter');
    const quizStatusFilter = document.getElementById('quizStatusFilter');
    const quizSearch = document.getElementById('quizSearch');
    const quizTableBody = document.querySelector('table.table tbody');
    
    // Add event listeners for filtering
    if (quizSubjectFilter) {
        quizSubjectFilter.addEventListener('change', filterQuizzes);
    }
    if (quizStatusFilter) {
        quizStatusFilter.addEventListener('change', filterQuizzes);
    }
    if (quizSearch) {
        quizSearch.addEventListener('input', filterQuizzes);
    }
    
    // Event delegation for table actions - attach to tbody for dynamic content
    quizTableBody.addEventListener('click', function(e) {
        const target = e.target;
        
        // Find the closest button element
        const button = target.closest('button.btn');
        
        // If clicked element is not a button or doesn't have the right classes, return
        if (!button || !(button.classList.contains('btn-outline-primary') || 
                         button.classList.contains('btn-outline-warning') || 
                         button.classList.contains('btn-outline-danger'))) {
            return;
        }
        
        const row = button.closest('tr');
        const cells = row.querySelectorAll('td');
        
        // Extract quiz data from the row
        const quizData = {
            id: cells[0].textContent,
            title: cells[1].textContent,
            subject: cells[2].textContent,
            questions: cells[3].textContent,
            duration: cells[4].textContent,
            status: cells[5].querySelector('.badge').textContent
        };
        
        // Handle View button (eye icon)
        if (button.classList.contains('btn-outline-primary')) {
            e.preventDefault();
            showViewModal(quizData);
        }
        // Handle Edit button (pencil icon)
        else if (button.classList.contains('btn-outline-warning')) {
            e.preventDefault();
            showEditModal(quizData, row);
        }
        // Handle Delete button (trash icon)
        else if (button.classList.contains('btn-outline-danger')) {
            e.preventDefault();
            deleteQuiz(row);
        }
    });
    
    // Filter quizzes based on selected filters and search
    function filterQuizzes() {
        const subjectFilter = quizSubjectFilter ? quizSubjectFilter.value : '';
        const statusFilter = quizStatusFilter ? quizStatusFilter.value : '';
        const searchTerm = quizSearch ? quizSearch.value.toLowerCase() : '';
        
        const rows = quizTableBody.querySelectorAll('tr');
        let visibleCount = 0;
        
        rows.forEach(row => {
            const cells = row.querySelectorAll('td');
            const subject = cells[2].textContent;
            const status = cells[5].querySelector('.badge').textContent;
            const title = cells[1].textContent.toLowerCase();
            
            // Check subject filter
            const subjectMatch = subjectFilter === '' || subject.includes(subjectFilter);
            
            // Check status filter  
            const statusMatch = statusFilter === '' || status.toLowerCase() === statusFilter.toLowerCase();
            
            // Check search term
            const searchMatch = searchTerm === '' || 
                               title.includes(searchTerm) || 
                               subject.toLowerCase().includes(searchTerm);
            
            // Show/hide row based on all filters
            if (subjectMatch && statusMatch && searchMatch) {
                row.style.display = '';
                visibleCount++;
            } else {
                row.style.display = 'none';
            }
        });
        
        // Update row numbers after filtering
        updateRowNumbers();
    }
    
    // View Modal Functionality
    function showViewModal(quizData) {
        // Create or update view modal
        let modal = document.getElementById('viewQuizModal');
        
        if (!modal) {
            modal = createViewModal();
            document.body.appendChild(modal);
        }
        
        // Populate modal with quiz data
        document.getElementById('viewQuizTitle').textContent = quizData.title;
        document.getElementById('viewQuizSubject').textContent = quizData.subject;
        document.getElementById('viewQuizQuestions').textContent = quizData.questions;
        document.getElementById('viewQuizDuration').textContent = quizData.duration;
        document.getElementById('viewQuizStatus').textContent = quizData.status;
        
        // Set status badge color
        const statusBadge = document.getElementById('viewQuizStatus');
        statusBadge.className = 'badge ' + getStatusColor(quizData.status);
        
        // Show modal
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
    }
    
    // Edit Modal Functionality
    function showEditModal(quizData, row) {
        // Create or update edit modal
        let modal = document.getElementById('editQuizModal');
        
        if (!modal) {
            modal = createEditModal();
            document.body.appendChild(modal);
        }
        
        // Populate form fields
        document.getElementById('editQuizTitle').value = quizData.title;
        document.getElementById('editQuizSubject').value = quizData.subject;
        document.getElementById('editQuizQuestions').value = quizData.questions.replace(/\D/g, ''); // Extract numbers only
        document.getElementById('editQuizDuration').value = quizData.duration.replace(/\D/g, ''); // Extract numbers only
        
        // Set status select
        const statusSelect = document.getElementById('editQuizStatus');
        statusSelect.value = quizData.status.toLowerCase();
        
        // Set up save handler
        const saveButton = document.getElementById('saveQuizChanges');
        saveButton.onclick = function() {
            saveQuizChanges(quizData.id, row);
        };
        
        // Show modal
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
    }
    
    // Save Quiz Changes
    function saveQuizChanges(quizId, row) {
        const title = document.getElementById('editQuizTitle').value;
        const subject = document.getElementById('editQuizSubject').value;
        const questions = document.getElementById('editQuizQuestions').value;
        const duration = document.getElementById('editQuizDuration').value + ' mins';
        const status = document.getElementById('editQuizStatus').value;
        
        // Update table row
        const cells = row.querySelectorAll('td');
        cells[1].textContent = title;
        cells[2].textContent = subject;
        cells[3].textContent = questions;
        cells[4].textContent = duration;
        
        // Update status badge
        const statusBadge = cells[5].querySelector('.badge');
        statusBadge.textContent = status.charAt(0).toUpperCase() + status.slice(1);
        statusBadge.className = 'badge ' + getStatusColor(status);
        
        // Close modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('editQuizModal'));
        modal.hide();
    }
    
    // Delete Quiz Functionality
    function deleteQuiz(row) {
        const quizTitle = row.querySelector('td:nth-child(2)').textContent;
        
        if (confirm(`Are you sure you want to delete the quiz "${quizTitle}"? This action cannot be undone.`)) {
            row.remove();
            // Optionally update row numbers
            updateRowNumbers();
        }
    }
    
    // Update row numbers after deletion or filtering
    function updateRowNumbers() {
        const visibleRows = quizTableBody.querySelectorAll('tr:not([style*=\"display: none\"])');
        visibleRows.forEach((row, index) => {
            row.querySelector('td:first-child').textContent = index + 1;
        });
    }
    
    // Helper function to get status badge color
    function getStatusColor(status) {
        switch(status.toLowerCase()) {
            case 'published': return 'bg-success';
            case 'draft': return 'bg-warning';
            case 'archived': return 'bg-secondary';
            default: return 'bg-secondary';
        }
    }
    
    // Create View Modal HTML
    function createViewModal() {
        const modalDiv = document.createElement('div');
        modalDiv.innerHTML = `
            <div class="modal fade" id="viewQuizModal" tabindex="-1" aria-labelledby="viewQuizModalLabel" aria-hidden="true">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="viewQuizModalLabel">Quiz Details</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <div class="row mb-3">
                                <div class="col-sm-4 fw-bold">Title:</div>
                                <div class="col-sm-8" id="viewQuizTitle"></div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-sm-4 fw-bold">Subject:</div>
                                <div class="col-sm-8" id="viewQuizSubject"></div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-sm-4 fw-bold">Questions:</div>
                                <div class="col-sm-8" id="viewQuizQuestions"></div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-sm-4 fw-bold">Duration:</div>
                                <div class="col-sm-8" id="viewQuizDuration"></div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-sm-4 fw-bold">Status:</div>
                                <div class="col-sm-8">
                                    <span id="viewQuizStatus" class="badge"></span>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        return modalDiv.firstElementChild;
    }
    
    // Create Edit Modal HTML
    function createEditModal() {
        const modalDiv = document.createElement('div');
        modalDiv.innerHTML = `
            <div class="modal fade" id="editQuizModal" tabindex="-1" aria-labelledby="editQuizModalLabel" aria-hidden="true">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="editQuizModalLabel">Edit Quiz</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <form>
                                <div class="mb-3">
                                    <label class="form-label" for="editQuizTitle">Quiz Title</label>
                                    <input class="form-control" type="text" id="editQuizTitle" required>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label" for="editQuizSubject">Subject</label>
                                    <select class="form-select" id="editQuizSubject">
                                        <option value="MATH101">MATH101 - Calculus I</option>
                                        <option value="PHYS101">PHYS101 - General Physics I</option>
                                        <option value="CHEM101">CHEM101 - General Chemistry I</option>
                                    </select>
                                </div>
                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label" for="editQuizQuestions">Number of Questions</label>
                                            <input class="form-control" type="number" id="editQuizQuestions" min="1" required>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label" for="editQuizDuration">Duration (minutes)</label>
                                            <input class="form-control" type="number" id="editQuizDuration" min="1" required>
                                        </div>
                                    </div>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label" for="editQuizStatus">Status</label>
                                    <select class="form-select" id="editQuizStatus">
                                        <option value="published">Published</option>
                                        <option value="draft">Draft</option>
                                        <option value="archived">Archived</option>
                                    </select>
                                </div>
                            </form>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="button" class="btn btn-primary" id="saveQuizChanges">Save Changes</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        return modalDiv.firstElementChild;
    }
});