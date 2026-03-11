// announcements-1.js - Announcement filtering functionality

document.addEventListener('DOMContentLoaded', function() {
    console.log('Announcements filtering script loaded');
    
    // Get filter elements
    const subjectFilter = document.getElementById('announcementSubjectFilter');
    const statusFilter = document.getElementById('announcementStatusFilter');
    const searchInput = document.getElementById('announcementSearch');
    
    console.log('Found elements:', { subjectFilter, statusFilter, searchInput });
    
    // Add event listeners for real-time filtering
    if (subjectFilter) {
        subjectFilter.addEventListener('change', function() {
            console.log('Subject filter changed:', this.value);
            filterAnnouncements();
        });
    }
    
    if (statusFilter) {
        statusFilter.addEventListener('change', function() {
            console.log('Status filter changed:', this.value);
            filterAnnouncements();
        });
    }
    
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            console.log('Search input:', this.value);
            filterAnnouncements();
        });
    }
    
    // Make filterAnnouncements function globally available for refresh functionality
    window.filterAnnouncements = function() {
        const announcementCards = document.querySelectorAll('.announcement-card');
        const selectedSubject = subjectFilter ? subjectFilter.value : '';
        const selectedStatus = statusFilter ? statusFilter.value : '';
        const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';
        
        announcementCards.forEach(card => {
            const cardSubject = card.getAttribute('data-subject');
            const cardStatus = card.getAttribute('data-status');
            const cardTitle = card.querySelector('.card-header h5').textContent.toLowerCase();
            const cardContent = card.querySelector('.card-body p').textContent.toLowerCase();
            const cardSubjectText = card.querySelector('.card-body small').textContent.toLowerCase();
            
            // Check subject filter
            const subjectMatch = !selectedSubject || selectedSubject === '' || cardSubject === selectedSubject;
            
            // Check status filter
            const statusMatch = !selectedStatus || selectedStatus === '' || cardStatus === selectedStatus;
            
            // Check search filter
            const searchMatch = !searchTerm || 
                cardTitle.includes(searchTerm) || 
                cardContent.includes(searchTerm) || 
                cardSubjectText.includes(searchTerm);
            
            // Hide or show the entire column card based on all filters
            if (subjectMatch && statusMatch && searchMatch) {
                card.style.display = 'block';
            } else {
                card.style.display = 'none';
            }
        });
    };
    
    // Initial filter to ensure consistency
    window.filterAnnouncements();
});