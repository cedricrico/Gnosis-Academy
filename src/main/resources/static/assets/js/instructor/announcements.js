// announcements.js - Handles Announcements functionality (View, Edit, Archive, Export, Refresh)

document.addEventListener('DOMContentLoaded', function() {
    // Modal instances
    let viewModal = null;
    let editModal = null;
    let postModal = null;
    
    // Initialize modals when the page loads
    const viewModalElement = document.getElementById('viewAnnouncementModal');
    const editModalElement = document.getElementById('editAnnouncementModal');
    const postModalElement = document.getElementById('postAnnouncementModal');
    
    if (viewModalElement) {
        viewModal = new bootstrap.Modal(viewModalElement);
    }
    
    if (editModalElement) {
        editModal = new bootstrap.Modal(editModalElement);
    }
    
    if (postModalElement) {
        postModal = new bootstrap.Modal(postModalElement);
    }
    
    // Set default date to today in the new announcement form
    const dateInput = document.getElementById('announcementDate');
    if (dateInput) {
        dateInput.valueAsDate = new Date();
    }
    
    // Add event listeners to all announcement cards
    setupAnnouncementListeners();
    
    // Get buttons by their IDs
    const exportButton = document.getElementById('exportAnnouncementsBtn');
    const refreshButton = document.getElementById('refreshAnnouncementsBtn');
    
    // Export button event listener
    if (exportButton) {
        exportButton.addEventListener('click', exportAnnouncementsToCSV);
    }
    
    // Refresh button event listener
    if (refreshButton) {
        refreshButton.addEventListener('click', refreshAnnouncements);
    }
    
    // Handle edit form submission
    const saveButton = document.getElementById('saveAnnouncementBtn');
    if (saveButton) {
        saveButton.addEventListener('click', function() {
            const form = document.getElementById('editAnnouncementForm');
            const cardIndex = form.dataset.editingCardId;
            
            if (cardIndex !== undefined) {
                const announcementsContainer = document.querySelector('.row.g-4');
                const cardElement = announcementsContainer.children[cardIndex].querySelector('.card');
                
                // Update card content
                const titleInput = document.getElementById('editAnnouncementTitle');
                const contentInput = document.getElementById('editAnnouncementContent');
                const dateInput = document.getElementById('editAnnouncementDate');
                const targetInput = document.getElementById('editAnnouncementTarget');
                
                // Format date for display
                const formattedDate = formatDateForDisplay(dateInput.value);
                
                cardElement.querySelector('.card-header h5').textContent = titleInput.value;
                cardElement.querySelector('.card-body p').textContent = contentInput.value;
                cardElement.querySelector('.card-body small').textContent = `Posted on ${formattedDate} | For: ${targetInput.value}`;
                
                // Close modal
                if (editModal) {
                    editModal.hide();
                }
                
                alert('Announcement updated successfully!');
            }
        });
    }
    
    
    
    // Function to set up event listeners for announcement cards
    function setupAnnouncementListeners() {
        const announcementCards = document.querySelectorAll('.announcement-card');
        
        announcementCards.forEach(card => {
            const cardElement = card.querySelector('.card');
            const buttons = cardElement.querySelectorAll('.btn-group button');
            
            // Remove existing event listeners first to avoid duplicates
            buttons.forEach(button => {
                const newButton = button.cloneNode(true);
                button.parentNode.replaceChild(newButton, button);
            });
            
            const updatedButtons = cardElement.querySelectorAll('.btn-group button');
            
            // View button
            if (updatedButtons[0]) {
                updatedButtons[0].addEventListener('click', function() {
                    handleViewAnnouncement(cardElement);
                });
            }
            
            // Edit button
            if (updatedButtons[1]) {
                updatedButtons[1].addEventListener('click', function() {
                    handleEditAnnouncement(cardElement);
                });
            }
            
            // Archive button
            if (updatedButtons[2]) {
                updatedButtons[2].addEventListener('click', function() {
                    handleArchiveAnnouncement(cardElement);
                });
            }
        });
    }
    
    // Handle View button click
    function handleViewAnnouncement(card) {
        const title = card.querySelector('.card-header h5').textContent;
        const content = card.querySelector('.card-body p').textContent;
        const metadata = card.querySelector('.card-body small').textContent;
        
        // Set modal content
        document.getElementById('viewAnnouncementTitle').textContent = title;
        document.getElementById('viewAnnouncementContent').textContent = content;
        document.getElementById('viewAnnouncementMetadata').textContent = metadata;
        
        // Show modal
        if (viewModal) {
            viewModal.show();
        }
    }
    
    // Handle Edit button click
    function handleEditAnnouncement(card) {
        const title = card.querySelector('.card-header h5').textContent;
        const content = card.querySelector('.card-body p').textContent;
        const metadata = card.querySelector('.card-body small').textContent;
        
        // Extract date, subject, and status from metadata
        const metadataParts = metadata.split(' | ');
        const dateText = metadataParts[0].replace('Posted on ', '');
        const subjectText = metadataParts[1].replace('For: ', '');
        const statusText = card.querySelector('.badge').textContent;
        
        // Set form values
        document.getElementById('editAnnouncementTitle').value = title;
        document.getElementById('editAnnouncementContent').value = content;
        document.getElementById('editAnnouncementDate').value = formatDateForInput(dateText);
        document.getElementById('editAnnouncementTarget').value = subjectText;
        
        // Store reference to the card being edited
        document.getElementById('editAnnouncementForm').dataset.editingCardId = Array.from(card.parentNode.parentNode.children).indexOf(card.parentNode);
        
        // Show modal
        if (editModal) {
            editModal.show();
        }
    }
    
    // Handle Archive button click
    async function handleArchiveAnnouncement(card) {
        if (await window.confirmAsync('Are you sure you want to archive this announcement? This action cannot be undone.')) {
            // Add archived class and update badge
            card.classList.add('border-warning');
            const badge = card.querySelector('.badge');
            if (badge) {
                badge.textContent = 'Archived';
                badge.className = 'badge text-dark bg-warning';
            }
            
            // Move to archived list or remove from active list
            const announcementsContainer = card.closest('.row.g-4');
            const archivedContainer = document.getElementById('archivedAnnouncements');
            
            if (archivedContainer) {
                // Move to archived section
                archivedContainer.appendChild(card.parentNode);
            } else {
                // Create archived section if it doesn't exist
                const archivedSection = document.createElement('div');
                archivedSection.id = 'archivedAnnouncements';
                archivedSection.innerHTML = '<h3 class="mt-5">Archived Announcements</h3><div class="row g-4"></div>';
                announcementsContainer.after(archivedSection);
                archivedSection.querySelector('.row.g-4').appendChild(card.parentNode);
            }
            
            alert('Announcement archived successfully!');
        }
    }
    
    
    
    // Export announcements to CSV
    function exportAnnouncementsToCSV() {
        const announcements = document.querySelectorAll('.announcement-card');
        let csvContent = 'Title,Subject,Date,Status\\n';
        
        announcements.forEach(announcement => {
            if (announcement.style.display !== 'none') {
                const title = announcement.querySelector('.card-header h5').textContent;
                const subject = announcement.getAttribute('data-subject');
                const dateText = announcement.querySelector('.card-body small').textContent.split(' | ')[0].replace('Posted on ', '');
                const status = announcement.querySelector('.badge').textContent;
                
                // Escape commas and quotes in CSV
                const escapeCSV = (str) => `"${str.replace(/"/g, '""')}"`;
                
                csvContent += `${escapeCSV(title)},${escapeCSV(subject)},${escapeCSV(dateText)},${escapeCSV(status)}\\n`;
            }
        });
        
        // Create and download CSV file
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', 'announcements_export.csv');
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
    
    // Refresh announcements list
    function refreshAnnouncements() {
        const refreshButton = document.getElementById('refreshAnnouncementsBtn');
        if (!refreshButton) return;
        
        const originalHTML = refreshButton.innerHTML;
        
        // Add loading animation
        refreshButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span> Refreshing...';
        refreshButton.disabled = true;
        
        // Simulate refresh action (in a real app, this would fetch from server)
        setTimeout(() => {
            // Re-setup event listeners to ensure all buttons work
            setupAnnouncementListeners();
            
            // Re-apply any active filters
            if (typeof window.filterAnnouncements === 'function') {
                window.filterAnnouncements();
            }
            
            // Restore button state
            refreshButton.innerHTML = originalHTML;
            refreshButton.disabled = false;
            
            alert('Announcements refreshed successfully!');
        }, 1000);
    }
    
    // Utility function to format date for input field (YYYY-MM-DD)
    function formatDateForInput(dateString) {
        if (!dateString) return new Date().toISOString().split('T')[0];
        
        try {
            const date = new Date(dateString);
            return date.toISOString().split('T')[0];
        } catch (e) {
            return new Date().toISOString().split('T')[0];
        }
    }
    
    // Utility function to format date for display (Month Day, Year)
    function formatDateForDisplay(dateString) {
        if (!dateString) return new Date().toLocaleDateString('en-US', { 
            year: 'numeric', 
            month: 'short', 
            day: 'numeric' 
        });
        
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'short', 
                day: 'numeric' 
            });
        } catch (e) {
            return new Date().toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'short', 
                day: 'numeric' 
            });
        }
    }
    
    // Handle new announcement form submission
    const postAnnouncementBtn = document.getElementById('postAnnouncementBtn');
    if (postAnnouncementBtn) {
        postAnnouncementBtn.addEventListener('click', function() {
            const title = document.getElementById('announcementTitle').value;
            const subject = document.getElementById('announcementSubject').value;
            const content = document.getElementById('announcementContent').value;
            const date = document.getElementById('announcementDate').value;
            const status = document.getElementById('announcementStatus').value;
            
            // Validate form
            if (!title || !subject || !content || !date || !status) {
                alert('Please fill in all required fields');
                return;
            }
            
            // Format date for display
            const formattedDate = formatDateForDisplay(date);
            
            // Create new announcement card
            const newAnnouncement = document.createElement('div');
            newAnnouncement.className = 'col-12 announcement-card';
            
            newAnnouncement.setAttribute('data-subject', subject);
            newAnnouncement.setAttribute('data-status', status);
            
            // Determine header and badge color based on status
            let headerClass = status === 'Published' ? 'bg-primary' : 'bg-warning';
            let badgeClass = status === 'Published' ? 'text-dark bg-success' : 'text-dark bg-warning';
            
            newAnnouncement.innerHTML = `
                <div class="card">
                    <div class="card-header text-white ${headerClass}">
                        <div class="d-flex justify-content-between align-items-center">
                            <h5 class="mb-0">${title}</h5>
                            <span class="badge ${badgeClass}">${status}</span>
                        </div>
                    </div>
                    <div class="card-body">
                        <p class="card-text">${content}</p>
                        <small class="text-muted">Posted on ${formattedDate} | For: ${subject}</small>
                    </div>
                    <div class="card-footer">
                        <div class="btn-group" role="group">
                            <button type="button" class="btn btn-sm btn-outline-primary">
                                View
                            </button>
                            <button type="button" class="btn btn-sm btn-outline-warning">
                                Edit
                            </button>
                            <button type="button" class="btn btn-sm btn-outline-danger">
                                Archive
                            </button>
                        </div>
                    </div>
                </div>
            `;
            
            // Add to the beginning of the announcements list
            const announcementsContainer = document.querySelector('.row.g-4');
            if (announcementsContainer) {
                announcementsContainer.insertBefore(newAnnouncement, announcementsContainer.firstChild);
                
                // Set up event listeners for the new card
                setupAnnouncementListeners();
                
                // Reset form and close modal
                document.getElementById('announcementForm').reset();
                if (document.getElementById('announcementDate')) {
                    document.getElementById('announcementDate').valueAsDate = new Date();
                }
                
                // Close modal if it exists
                if (postModal) {
                    postModal.hide();
                }
                
                // Show success message
                alert('Announcement posted successfully!');
            }
        });
    }
});
