// assignments.js - Assignment table functionality

// Assignment data storage (in-memory for demonstration)
let assignmentsData = [];

// View Assignment Modal functionality
function viewAssignment(row) {
    const cells = row.querySelectorAll('td');
    const assignmentData = {
        title: cells[1].textContent,
        subject: cells[2].textContent,
        dueDate: cells[3].textContent,
        status: cells[4].querySelector('.badge').textContent,
        submissions: cells[5].textContent
    };
    
    document.getElementById('viewAssignmentTitle').textContent = assignmentData.title;
    document.getElementById('viewSubject').textContent = assignmentData.subject;
    document.getElementById('viewDueDate').textContent = assignmentData.dueDate;
    document.getElementById('viewStatus').textContent = assignmentData.status;
    document.getElementById('viewSubmissions').textContent = assignmentData.submissions;
    
    // Set badge color based on status
    const statusBadge = document.getElementById('viewStatus');
    statusBadge.className = 'badge ' + getStatusClass(assignmentData.status);
    
    const viewModal = new bootstrap.Modal(document.getElementById('viewAssignmentModal'));
    viewModal.show();
}

// Edit Assignment Modal functionality
function editAssignment(row) {
    const cells = row.querySelectorAll('td');
    const assignmentData = {
        id: parseInt(cells[0].textContent),
        title: cells[1].textContent,
        subject: cells[2].textContent,
        dueDate: cells[3].textContent,
        status: cells[4].querySelector('.badge').textContent,
        submissions: cells[5].textContent
    };
    
    document.getElementById('editAssignmentTitle').value = assignmentData.title;
    document.getElementById('editSubject').value = assignmentData.subject;
    
    // Convert date from "Oct 15, 2023" to "2023-10-15"
    const dateParts = assignmentData.dueDate.split(' ');
    const monthMap = {
        'Jan': '01', 'Feb': '02', 'Mar': '03', 'Apr': '04', 'May': '05', 'Jun': '06',
        'Jul': '07', 'Aug': '08', 'Sep': '09', 'Oct': '10', 'Nov': '11', 'Dec': '12'
    };
    const formattedDate = `${dateParts[2]}-${monthMap[dateParts[0]]}-${dateParts[1].replace(',', '').padStart(2, '0')}`;
    document.getElementById('editDueDate').value = formattedDate;
    
    document.getElementById('editStatus').value = assignmentData.status.toLowerCase();
    
    // Store row reference for updating
    document.getElementById('editAssignmentModal').dataset.rowIndex = row.rowIndex;
    
    const editModal = new bootstrap.Modal(document.getElementById('editAssignmentModal'));
    editModal.show();
}

// Delete Assignment functionality
function deleteAssignment(row) {
    const cells = row.querySelectorAll('td');
    const assignmentTitle = cells[1].textContent;
    
    document.getElementById('deleteAssignmentTitle').textContent = assignmentTitle;
    document.getElementById('deleteAssignmentModal').dataset.rowIndex = row.rowIndex;
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteAssignmentModal'));
    deleteModal.show();
}

// Save edited assignment
function saveEditedAssignment() {
    const rowIndex = document.getElementById('editAssignmentModal').dataset.rowIndex;
    const row = document.querySelector(`tbody tr:nth-child(${rowIndex})`);
    
    const title = document.getElementById('editAssignmentTitle').value;
    const subject = document.getElementById('editSubject').value;
    const dueDate = document.getElementById('editDueDate').value;
    const status = document.getElementById('editStatus').value;
    
    // Convert date from "2023-10-15" to "Oct 15, 2023"
    const date = new Date(dueDate);
    const formattedDate = date.toLocaleDateString('en-US', { 
        month: 'short', 
        day: 'numeric', 
        year: 'numeric' 
    });
    
    // Update row data
    const cells = row.querySelectorAll('td');
    cells[1].textContent = title;
    cells[2].textContent = subject;
    cells[3].textContent = formattedDate;
    
    // Update status with appropriate badge
    const statusCell = cells[4];
    statusCell.innerHTML = `<span class="badge ${getStatusClass(status)}">${status.charAt(0).toUpperCase() + status.slice(1)}</span>`;
    
    // Update assignments data
    if (assignmentsData[rowIndex - 1]) {
        assignmentsData[rowIndex - 1] = {
            title,
            subject,
            dueDate: formattedDate,
            status: status.charAt(0).toUpperCase() + status.slice(1)
        };
    }
    
    // Close modal
    const editModal = bootstrap.Modal.getInstance(document.getElementById('editAssignmentModal'));
    editModal.hide();
}

// Confirm delete assignment
function confirmDeleteAssignment() {
    const rowIndex = document.getElementById('deleteAssignmentModal').dataset.rowIndex;
    const row = document.querySelector(`tbody tr:nth-child(${rowIndex})`);
    
    row.remove();
    
    // Remove from assignments data
    assignmentsData.splice(rowIndex - 1, 1);
    
    // Update row numbers
    updateRowNumbers();
    
    // Close modal
    const deleteModal = bootstrap.Modal.getInstance(document.getElementById('deleteAssignmentModal'));
    deleteModal.hide();
}

// Update row numbers after deletion
function updateRowNumbers() {
    const rows = document.querySelectorAll('tbody tr');
    rows.forEach((row, index) => {
        row.querySelector('td:first-child').textContent = index + 1;
    });
}

// Helper function to get badge class based on status
function getStatusClass(status) {
    status = status.toLowerCase();
    switch(status) {
        case 'active': return 'bg-success';
        case 'draft': return 'bg-warning';
        case 'past due': return 'bg-secondary';
        default: return 'bg-secondary';
    }
}

// Assignment filtering functionality
function filterAssignments() {
    const subjectFilter = document.getElementById('subjectFilter').value;
    const statusFilter = document.getElementById('statusFilter').value;
    const searchText = document.getElementById('searchInput').value.toLowerCase();
    
    const rows = document.querySelectorAll('tbody tr');
    
    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        const assignmentTitle = cells[1].textContent.toLowerCase();
        const subject = cells[2].textContent;
        const statusBadge = cells[4].querySelector('.badge');
        const status = statusBadge ? statusBadge.textContent : '';
        
        // Check subject filter
        const subjectMatch = subjectFilter === '' || subjectFilter === 'All Subjects' || 
                           subject.includes(subjectFilter.split(' - ')[0]);
        
        // Check status filter
        const statusMatch = statusFilter === '' || statusFilter === 'All Statuses' || 
                           status.toLowerCase() === statusFilter.toLowerCase();
        
        // Check search text
        const searchMatch = searchText === '' || 
                           assignmentTitle.includes(searchText) || 
                           subject.toLowerCase().includes(searchText);
        
        // Show/hide row based on all conditions
        if (subjectMatch && statusMatch && searchMatch) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

// NEW ASSIGNMENT FUNCTIONALITY
function createAssignment() {
    // Get form values
    const title = document.getElementById('assignmentTitle').value;
    const subject = document.getElementById('assignmentSubject').value;
    const description = document.getElementById('assignmentDescription').value;
    const dueDate = document.getElementById('dueDate').value;
    const status = 'Draft'; // Default status for new assignments
    
    // Validate required fields
    if (!title || !subject || !dueDate) {
        alert('Please fill in all required fields');
        return;
    }
    
    // Format due date
    const date = new Date(dueDate);
    const formattedDate = date.toLocaleDateString('en-US', { 
        month: 'short', 
        day: 'numeric', 
        year: 'numeric' 
    });
    
    // Get table body
    const tbody = document.querySelector('tbody');
    
    // Create new row
    const newRow = document.createElement('tr');
    const rowNumber = tbody.querySelectorAll('tr').length + 1;
    
    // Extract subject code from value (e.g., "MATH101 - Calculus I" becomes "MATH101")
    const subjectCode = subject.split(' - ')[0];
    
    // Create row content
    newRow.innerHTML = `
        <td scope="row">${rowNumber}</td>
        <td>${title}</td>
        <td>${subjectCode}</td>
        <td>${formattedDate}</td>
        <td><span class="badge ${getStatusClass(status)}">${status}</span></td>
        <td>-/-</td>
        <td>
            <div class="btn-group btn-group-sm" role="group">
                <button class="btn btn-outline-primary" role="button">
                    <icon></icon>
                </button>
                <button class="btn btn-outline-warning" role="button">
                    <icon></icon>
                </button>
                <button class="btn btn-outline-danger" role="button">
                    <icon></icon>
                </button>
            </div>
        </td>
    `;
    
    // Add to table
    tbody.appendChild(newRow);
    
    // Add event listeners to new row buttons
    const buttons = newRow.querySelectorAll('.btn-group .btn');
    buttons[0].addEventListener('click', () => viewAssignment(newRow));
    buttons[1].addEventListener('click', () => editAssignment(newRow));
    buttons[2].addEventListener('click', () => deleteAssignment(newRow));
    
    // Store assignment data
    assignmentsData.push({
        title,
        subject: subjectCode,
        dueDate: formattedDate,
        status,
        submissions: '-/-'
    });
    
    // Close modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('createAssignmentModal'));
    modal.hide();
    
    // Reset form
    document.getElementById('assignmentTitle').value = '';
    document.getElementById('assignmentSubject').value = '';
    document.getElementById('assignmentDescription').value = '';
    document.getElementById('dueDate').value = '';
    
    // Show success message
    alert('Assignment created successfully!');
}

// EXPORT FUNCTIONALITY
function exportAssignments() {
    // Get all assignment rows
    const rows = document.querySelectorAll('tbody tr');
    
    // Create CSV header
    let csvContent = 'Title,Subject,Due Date,Status\\n';
    
    // Add each assignment to CSV
    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        
        // Only include rows that are visible (not filtered out)
        if (row.style.display !== 'none') {
            const title = cells[1].textContent.replace(/,/g, ''); // Remove commas to avoid CSV issues
            const subject = cells[2].textContent;
            const dueDate = cells[3].textContent;
            const status = cells[4].textContent;
            
            csvContent += `"${title}","${subject}","${dueDate}","${status}"\\n`;
        }
    });
    
    // Create download link
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'assignments.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

// REFRESH FUNCTIONALITY
function refreshAssignments() {
    const refreshButton = document.querySelector('.btn-outline-success.btn-sm');
    
    // Add loading animation
    const originalHTML = refreshButton.innerHTML;
    refreshButton.innerHTML = '<icon class="spin-animation"></icon> Refreshing...';
    refreshButton.disabled = true;
    
    // Simulate refresh delay (in real app, this would fetch from server)
    setTimeout(() => {
        // Re-apply filters (this simulates refreshing the view)
        filterAssignments();
        
        // Restore button
        refreshButton.innerHTML = originalHTML;
        refreshButton.disabled = false;
        
        // Show notification
        showNotification('Assignments list refreshed successfully!', 'success');
    }, 1000);
}

// Show notification
function showNotification(message, type) {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999;';
    notification.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(notification);
    
    // Auto remove after 3 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 3000);
}

// Initialize event listeners
document.addEventListener('DOMContentLoaded', function() {
    // Add event listeners to all action buttons
    document.querySelectorAll('tbody tr').forEach(row => {
        const buttons = row.querySelectorAll('.btn-group .btn');
        
        buttons[0].addEventListener('click', () => viewAssignment(row));
        buttons[1].addEventListener('click', () => editAssignment(row));
        buttons[2].addEventListener('click', () => deleteAssignment(row));
    });
    
    // Add event listeners for filters
    document.getElementById('subjectFilter').addEventListener('change', filterAssignments);
    document.getElementById('statusFilter').addEventListener('change', filterAssignments);
    document.getElementById('searchInput').addEventListener('input', filterAssignments);
    
    // Add event listener for Create Assignment button in modal (only if exists)
    const createAssignmentBtn = document.querySelector('#createAssignmentModal .btn-primary');
    if (createAssignmentBtn) {
        createAssignmentBtn.addEventListener('click', createAssignment);
    }
    
    // Add event listeners for Edit Assignment save button (only if exists)
    const saveAssignmentBtn = document.querySelector('#editAssignmentModal .btn-primary');
    if (saveAssignmentBtn) {
        saveAssignmentBtn.addEventListener('click', saveEditedAssignment);
    }
    
    // Add event listener for Delete Assignment confirm button (only if exists)
    const confirmDeleteBtn = document.querySelector('#deleteAssignmentModal .btn-danger');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', confirmDeleteAssignment);
    }
    
    // Add event listeners for Export and Refresh buttons (only if they exist on this page)
    const btnToolbar = document.querySelector('.btn-toolbar');
    if (btnToolbar) {
        const buttonGroups = btnToolbar.querySelectorAll('.btn-group');
        if (buttonGroups.length >= 2) {
            const exportButton = buttonGroups[1].querySelector('button');
            if (exportButton) {
                exportButton.addEventListener('click', exportAssignments);
            }
        }
        if (buttonGroups.length >= 3) {
            const refreshButton = buttonGroups[2].querySelector('button');
            if (refreshButton) {
                refreshButton.addEventListener('click', refreshAssignments);
            }
        }
    }
    
    // Initialize assignments data from existing table
    const rows = document.querySelectorAll('tbody tr');
    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        assignmentsData.push({
            title: cells[1].textContent,
            subject: cells[2].textContent,
            dueDate: cells[3].textContent,
            status: cells[4].querySelector('.badge').textContent,
            submissions: cells[5].textContent
        });
    });
});