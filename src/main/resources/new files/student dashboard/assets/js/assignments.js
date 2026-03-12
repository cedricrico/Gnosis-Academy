// Assignment data - in a real application, this would come from a database
const assignments = {
    1: {
        title: "Database ER Diagram",
        subject: "Database Systems",
        dueDate: "March 20, 2026",
        description: "Create an Entity-Relationship diagram for a university database system. The diagram should include entities for Students, Courses, Instructors, and Departments with appropriate relationships and attributes.",
        status: "uploaded"
    },
    2: {
        title: "Programming Project 1",
        subject: "Computer Science",
        dueDate: "March 25, 2026",
        description: "Develop a console-based application for managing student records. The application should allow adding, editing, deleting, and searching student information.",
        status: "not_submitted"
    },
    3: {
        title: "Web App Prototype",
        subject: "Computer Science",
        dueDate: "April 1, 2026",
        description: "Create a responsive web application prototype for an online bookstore. Include features for browsing books, user registration, and shopping cart functionality.",
        status: "not_submitted"
    },
    4: {
        title: "Mathematics Problem Set",
        subject: "Mathematics",
        dueDate: "March 18, 2026",
        description: "Solve the following calculus problems: 1) Find the derivative of f(x) = 3x^4 - 2x^3 + 5x - 7, 2) Calculate the definite integral from 0 to 2 of (x^2 + 3x) dx, 3) Solve the differential equation dy/dx = 2x.",
        status: "uploaded"
    },
    5: {
        title: "English Essay",
        subject: "English",
        dueDate: "March 15, 2026",
        description: "Write a 1000-word essay on the impact of social media on modern communication. Include arguments for both positive and negative effects, supported by credible sources.",
        status: "uploaded"
    }
};

// Store current assignment ID being viewed
let currentAssignmentId = null;

// Initialize modal functionality
document.addEventListener('DOMContentLoaded', function() {
    const modal = new bootstrap.Modal(document.getElementById('assignmentModal'));
    const viewButtons = document.querySelectorAll('.view-assignment-btn');
    const submitBtn = document.getElementById('submitAssignmentBtn');
    const fileInput = document.getElementById('assignmentFile');
    
    // Add click event to all View Assignment buttons
    viewButtons.forEach(button => {
        button.addEventListener('click', function() {
            currentAssignmentId = this.getAttribute('data-assignment-id');
            const assignment = assignments[currentAssignmentId];
            
            if (assignment) {
                // Populate modal with assignment data
                document.getElementById('assignmentTitle').textContent = assignment.title;
                document.getElementById('assignmentSubject').textContent = assignment.subject;
                document.getElementById('assignmentDueDate').textContent = assignment.dueDate;
                document.getElementById('assignmentDescription').textContent = assignment.description;
                
                // Reset file input
                fileInput.value = '';
                
                // Show modal
                modal.show();
            }
        });
    });
    
    // Handle assignment submission
    submitBtn.addEventListener('click', function() {
        if (!fileInput.files.length) {
            alert('Please select a file to upload.');
            return;
        }
        
        const file = fileInput.files[0];
        const allowedTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'image/jpeg', 'image/jpg', 'image/png'];
        
        if (!allowedTypes.includes(file.type)) {
            alert('Invalid file type. Please upload a PDF, DOCX, JPG, or PNG file.');
            return;
        }
        
        // Update assignment status
        if (currentAssignmentId && assignments[currentAssignmentId]) {
            assignments[currentAssignmentId].status = "uploaded";
            
            // Update the status badge in the table
            updateAssignmentStatus(currentAssignmentId, "uploaded");
        }
        
        // In a real application, you would upload the file to a server here
        alert('Assignment submitted successfully!');
        modal.hide();
    });
    
    // Function to update assignment status in the table
    function updateAssignmentStatus(assignmentId, status) {
        // Find the table row for this assignment
        const button = document.querySelector(`.view-assignment-btn[data-assignment-id="${assignmentId}"]`);
        if (!button) return;
        
        const row = button.closest('tr');
        if (!row) return;
        
        // Get the status cell (last cell in the row)
        const statusCell = row.cells[4]; // Index 4 is the Status column
        
        if (status === "uploaded") {
            statusCell.innerHTML = '<span class="badge bg-success">Uploaded</span>';
        } else {
            statusCell.innerHTML = '<span class="badge bg-secondary">Not Submitted</span>';
        }
    }
    
    // Initialize status badges on page load
    function initializeStatusBadges() {
        for (const assignmentId in assignments) {
            const assignment = assignments[assignmentId];
            if (assignment) {
                updateAssignmentStatus(assignmentId, assignment.status);
            }
        }
    }
    
    // Call initialization function
    initializeStatusBadges();
});