// Quiz Management JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Create Quiz Modal functionality
    const createQuizButton = document.querySelector('#createQuizModal .btn-primary');
    
    if (createQuizButton) {
        createQuizButton.addEventListener('click', createQuiz);
    }
    
    // Create Quiz Function
    function createQuiz() {
        // Get form values
        const title = document.getElementById('quizTitle').value.trim();
        const subject = document.getElementById('quizSubject').value;
        const duration = document.getElementById('quizDuration').value.trim();
        const attempts = document.getElementById('quizAttempts').value.trim();
        const description = document.getElementById('quizDescription').value.trim();
        
        // Get question types
        const mcqChecked = document.getElementById('mcqCheck').checked;
        const tfChecked = document.getElementById('tfCheck').checked;
        const essayChecked = document.getElementById('essayCheck').checked;
        
        // Validation
        if (!title || !subject || !duration) {
            alert('Please fill in all required fields: Title, Subject, and Duration');
            return;
        }
        
        if (!mcqChecked && !tfChecked && !essayChecked) {
            alert('Please select at least one question type');
            return;
        }
        
        // Create new quiz row
        addQuizToTable(title, subject, duration, attempts, description);
        
        // Reset form and close modal
        document.querySelector('#createQuizModal form').reset();
        const modal = bootstrap.Modal.getInstance(document.getElementById('createQuizModal'));
        modal.hide();
    }
    
    // Add quiz to table
    function addQuizToTable(title, subject, duration, attempts, description) {
        const tableBody = document.querySelector('table.table tbody');
        const rows = tableBody.querySelectorAll('tr');
        const nextNumber = rows.length + 1;
        
        // Create new row
        const newRow = document.createElement('tr');
        newRow.innerHTML = `
            <td scope="row">${nextNumber}</td>
            <td>${title}</td>
            <td>${subject}</td>
            <td>0</td>
            <td>${duration} mins</td>
            <td><span class="badge bg-warning">Draft</span></td>
            <td>
                <div class="btn-group btn-group-sm" role="group">
                    <button class="btn btn-outline-primary" type="button">
                        <i class="bi bi-eye"></i>
                    </button>
                    <button class="btn btn-outline-warning" type="button">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-outline-danger" type="button">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </td>
        `;
        
        tableBody.appendChild(newRow);
        
        // Update row numbers
        updateRowNumbers();
    }
    
    // Update row numbers after adding new quiz
    function updateRowNumbers() {
        const rows = document.querySelectorAll('table.table tbody tr');
        rows.forEach((row, index) => {
            row.querySelector('td:first-child').textContent = index + 1;
        });
    }
    
    // Initialize icons for existing rows
    function initializeActionButtons() {
        const rows = document.querySelectorAll('table.table tbody tr');
        rows.forEach(row => {
            const actionButtons = row.querySelectorAll('.btn-group .btn');
            if (actionButtons.length === 3) {
                // Check if the buttons already have proper content
                if (!actionButtons[0].querySelector('i')) {
                    actionButtons[0].innerHTML = '<i class="bi bi-eye"></i>';
                    actionButtons[1].innerHTML = '<i class="bi bi-pencil"></i>';
                    actionButtons[2].innerHTML = '<i class="bi bi-trash"></i>';
                }
                
                // Convert from <a> tags to <button> for better semantics
                if (actionButtons[0].tagName === 'A') {
                    const buttonGroup = row.querySelector('.btn-group');
                    buttonGroup.innerHTML = `
                        <button class="btn btn-outline-primary" type="button">
                            <i class="bi bi-eye"></i>
                        </button>
                        <button class="btn btn-outline-warning" type="button">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-outline-danger" type="button">
                            <i class="bi bi-trash"></i>
                        </button>
                    `;
                }
            }
        });
    }
    
    // Run initialization
    setTimeout(initializeActionButtons, 100);
});