document.addEventListener('DOMContentLoaded', function() {
    // Filter lessons based on selected criteria
    function filterLessons() {
        const subjectFilter = document.getElementById('lessonSubjectFilter').value;
        const weekFilter = document.getElementById('lessonWeekFilter').value;
        const searchText = document.getElementById('lessonSearch').value.toLowerCase();
        
        const lessonCards = document.querySelectorAll('.lesson-card');
        
        lessonCards.forEach(card => {
            const subjectWeekText = card.querySelector('.lesson-subject-week').textContent;
            const titleText = card.querySelector('.lesson-title').textContent.toLowerCase();
            const descriptionText = card.querySelector('.lesson-description').textContent.toLowerCase();
            
            const [subject, week] = subjectWeekText.split(' - ');
            
            let showCard = true;
            
            // Apply subject filter - extract just the subject code from the filter value
            if (subjectFilter) {
                // Extract subject code from filter value (e.g., "MATH101 - Calculus I" -> "MATH101")
                const filterSubjectCode = subjectFilter.split(' - ')[0];
                if (subject !== filterSubjectCode) {
                    showCard = false;
                }
            }
            
            // Apply week filter
            if (weekFilter && week !== weekFilter) {
                showCard = false;
            }
            
            // Apply search filter
            if (searchText && 
                !titleText.includes(searchText) && 
                !descriptionText.includes(searchText) &&
                !subject.toLowerCase().includes(searchText) &&
                !week.toLowerCase().includes(searchText)) {
                showCard = false;
            }
            
            // Show or hide card
            if (showCard) {
                card.style.display = 'block';
                card.closest('.col').style.display = 'block';
            } else {
                card.style.display = 'none';
                card.closest('.col').style.display = 'none';
            }
        });
    }

    // Add event listeners for filter controls
    document.getElementById('lessonSubjectFilter').addEventListener('change', filterLessons);
    document.getElementById('lessonWeekFilter').addEventListener('change', filterLessons);
    document.getElementById('lessonSearch').addEventListener('input', filterLessons);

    // Lesson type to button text mapping
    const lessonTypeMap = {
        'PDF': 'Download',
        'Presentation': 'Download',
        'Video': 'View',
        'Text': 'Download'
    };

    // Get badge color based on lesson type
    function getBadgeColor(type) {
        const colors = {
            'PDF': 'bg-primary',
            'Video': 'bg-success',
            'Presentation': 'bg-info',
            'Text': 'bg-secondary'
        };
        return colors[type] || 'bg-secondary';
    }

    // Upload Lesson functionality
    document.querySelector('#uploadLessonModal .btn-primary').addEventListener('click', function() {
        const title = document.getElementById('lessonTitle').value.trim();
        const subject = document.getElementById('lessonSubject').value;
        const week = document.getElementById('lessonWeek').value;
        const description = document.getElementById('lessonDescription').value.trim();
        const lessonType = document.getElementById('lessonType').value;
        
        // Simple validation - check for empty/null/undefined or empty string
        if (!title || title === '') {
            alert('Please enter a lesson title.');
            return;
        }
        if (!subject || subject === '') {
            alert('Please select a subject.');
            return;
        }
        if (!week || week === '') {
            alert('Please select a week.');
            return;
        }
        if (!description || description === '') {
            alert('Please enter a description.');
            return;
        }
        if (!lessonType || lessonType === '') {
            alert('Please select a content type.');
            return;
        }
        
        // Generate unique lesson ID
        const lessonId = Date.now(); // Simple unique ID using timestamp
        
        // Create new lesson card
        const newCol = document.createElement('div');
        newCol.className = 'col';
        
        const newLessonCard = document.createElement('div');
        newLessonCard.className = 'card h-100 lesson-card';
        newLessonCard.setAttribute('data-lesson-id', lessonId);
        
        // Card body
        const cardBody = document.createElement('div');
        cardBody.className = 'card-body';
        
        const titleElement = document.createElement('h5');
        titleElement.className = 'card-title lesson-title';
        titleElement.textContent = title;
        
        const descriptionElement = document.createElement('p');
        descriptionElement.className = 'card-text lesson-description';
        descriptionElement.textContent = description;
        
        const subjectWeekDiv = document.createElement('div');
        subjectWeekDiv.className = 'd-flex justify-content-between align-items-center';
        
        const subjectWeekElement = document.createElement('small');
        subjectWeekElement.className = 'text-muted lesson-subject-week';
        subjectWeekElement.textContent = `${subject} - ${week}`;
        
        const badgeElement = document.createElement('span');
        badgeElement.className = `badge ${getBadgeColor(lessonType)} lesson-type`;
        badgeElement.setAttribute('data-original-type', lessonType);
        badgeElement.textContent = lessonType;
        
        subjectWeekDiv.appendChild(subjectWeekElement);
        subjectWeekDiv.appendChild(badgeElement);
        
        cardBody.appendChild(titleElement);
        cardBody.appendChild(descriptionElement);
        cardBody.appendChild(subjectWeekDiv);
        
        // Card footer with buttons
        const cardFooter = document.createElement('div');
        cardFooter.className = 'card-footer';
        
        const buttonGroup = document.createElement('div');
        buttonGroup.className = 'btn-group w-100';
        buttonGroup.setAttribute('role', 'group');
        
        // View/Download button
        const viewDownloadBtn = document.createElement('button');
        viewDownloadBtn.className = 'btn btn-outline-primary btn-sm view-download-btn';
        viewDownloadBtn.type = 'button';
        viewDownloadBtn.setAttribute('data-file-url', '#');
        viewDownloadBtn.innerHTML = '<icon></icon> ' + lessonTypeMap[lessonType];
        
        // Edit button
        const editBtn = document.createElement('button');
        editBtn.className = 'btn btn-outline-warning btn-sm edit-btn';
        editBtn.type = 'button';
        editBtn.innerHTML = '<icon></icon> Edit';
        
        // Delete button
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-outline-danger btn-sm delete-btn';
        deleteBtn.type = 'button';
        deleteBtn.innerHTML = '<icon></icon> Delete';
        
        buttonGroup.appendChild(viewDownloadBtn);
        buttonGroup.appendChild(editBtn);
        buttonGroup.appendChild(deleteBtn);
        
        cardFooter.appendChild(buttonGroup);
        
        // Assemble card
        newLessonCard.appendChild(cardBody);
        newLessonCard.appendChild(cardFooter);
        newCol.appendChild(newLessonCard);
        
        // Add to container
        document.getElementById('lessonsContainer').appendChild(newCol);
        
        // Add event listeners to new buttons
        addEventListenersToCard(newLessonCard);
        
        // Reset form and close modal
        document.getElementById('lessonTitle').value = '';
        document.getElementById('lessonSubject').value = '';
        document.getElementById('lessonWeek').value = '';
        document.getElementById('lessonDescription').value = '';
        document.getElementById('lessonType').value = '';
        
        const modalElement = document.getElementById('uploadLessonModal');
        const modalInstance = bootstrap.Modal.getInstance(modalElement);
        modalInstance.hide();
        
        alert('Lesson uploaded successfully!');
    });

    // Helper function to add event listeners to a lesson card
    function addEventListenersToCard(card) {
        // View/Download button
        const viewDownloadBtn = card.querySelector('.view-download-btn');
        viewDownloadBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const lessonCard = this.closest('.lesson-card');
            const lessonType = lessonCard.querySelector('.lesson-type').getAttribute('data-original-type');
            const fileUrl = this.getAttribute('data-file-url');
            
            if (lessonType === 'Video') {
                // Open video in modal
                const videoPlayer = document.getElementById('videoPlayer');
                videoPlayer.src = fileUrl;
                const videoModal = new bootstrap.Modal(document.getElementById('videoViewerModal'));
                document.getElementById('videoViewerModalLabel').textContent = lessonCard.querySelector('.lesson-title').textContent;
                videoModal.show();
            } else {
                // Download file
                window.open(fileUrl, '_blank');
            }
        });
        
        // Edit button
        const editBtn = card.querySelector('.edit-btn');
        editBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const lessonCard = this.closest('.lesson-card');
            const lessonId = lessonCard.getAttribute('data-lesson-id');
            const title = lessonCard.querySelector('.lesson-title').textContent;
            const description = lessonCard.querySelector('.lesson-description').textContent;
            const subjectWeek = lessonCard.querySelector('.lesson-subject-week').textContent;
            const [subject, week] = subjectWeek.split(' - ');
            const lessonType = lessonCard.querySelector('.lesson-type').getAttribute('data-original-type');
            const fileUrl = lessonCard.querySelector('.view-download-btn').getAttribute('data-file-url');
            
            // Populate edit form
            document.getElementById('editLessonId').value = lessonId;
            document.getElementById('editLessonTitle').value = title;
            document.getElementById('editLessonDescription').value = description;
            document.getElementById('editLessonSubject').value = subject;
            document.getElementById('editLessonWeek').value = week;
            document.getElementById('editLessonType').value = lessonType;
            document.getElementById('editLessonFileUrl').value = fileUrl;
            
            // Show modal
            const editModal = new bootstrap.Modal(document.getElementById('editLessonModal'));
            editModal.show();
        });
        
        // Delete button
        const deleteBtn = card.querySelector('.delete-btn');
        deleteBtn.addEventListener('click', async function(e) {
            e.preventDefault();
            const lessonCard = this.closest('.lesson-card');
            const lessonTitle = lessonCard.querySelector('.lesson-title').textContent;
            
            if (await window.confirmAsync(`Are you sure you want to delete "${lessonTitle}"?`)) {
                lessonCard.closest('.col').remove();
            }
        });
    }

    // Download/View button functionality
    document.querySelectorAll('.view-download-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const lessonCard = this.closest('.lesson-card');
            const lessonType = lessonCard.querySelector('.lesson-type').getAttribute('data-original-type');
            const fileUrl = this.getAttribute('data-file-url');
            
            if (lessonType === 'Video') {
                // Open video in modal
                const videoPlayer = document.getElementById('videoPlayer');
                videoPlayer.src = fileUrl;
                const videoModal = new bootstrap.Modal(document.getElementById('videoViewerModal'));
                document.getElementById('videoViewerModalLabel').textContent = lessonCard.querySelector('.lesson-title').textContent;
                videoModal.show();
            } else {
                // Download file
                window.open(fileUrl, '_blank');
            }
        });
    });

    // Edit button functionality
    document.querySelectorAll('.edit-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const lessonCard = this.closest('.lesson-card');
            const lessonId = lessonCard.getAttribute('data-lesson-id');
            const title = lessonCard.querySelector('.lesson-title').textContent;
            const description = lessonCard.querySelector('.lesson-description').textContent;
            const subjectWeek = lessonCard.querySelector('.lesson-subject-week').textContent;
            const [subject, week] = subjectWeek.split(' - ');
            const lessonType = lessonCard.querySelector('.lesson-type').getAttribute('data-original-type');
            const fileUrl = lessonCard.querySelector('.view-download-btn').getAttribute('data-file-url');
            
            // Populate edit form
            document.getElementById('editLessonId').value = lessonId;
            document.getElementById('editLessonTitle').value = title;
            document.getElementById('editLessonDescription').value = description;
            document.getElementById('editLessonSubject').value = subject;
            document.getElementById('editLessonWeek').value = week;
            document.getElementById('editLessonType').value = lessonType;
            document.getElementById('editLessonFileUrl').value = fileUrl;
            
            // Show modal
            const editModal = new bootstrap.Modal(document.getElementById('editLessonModal'));
            editModal.show();
        });
    });

    // Save edited lesson
    document.getElementById('saveLessonChanges').addEventListener('click', function() {
        const lessonId = document.getElementById('editLessonId').value;
        const lessonCard = document.querySelector(`.lesson-card[data-lesson-id="${lessonId}"]`);
        
        if (lessonCard) {
            // Update card content
            lessonCard.querySelector('.lesson-title').textContent = document.getElementById('editLessonTitle').value;
            lessonCard.querySelector('.lesson-description').textContent = document.getElementById('editLessonDescription').value;
            
            const subject = document.getElementById('editLessonSubject').value;
            const week = document.getElementById('editLessonWeek').value;
            lessonCard.querySelector('.lesson-subject-week').textContent = `${subject} - ${week}`;
            
            const newType = document.getElementById('editLessonType').value;
            const typeBadge = lessonCard.querySelector('.lesson-type');
            typeBadge.textContent = newType;
            typeBadge.setAttribute('data-original-type', newType);
            
            // Update badge color
            const currentClassList = typeBadge.classList;
            currentClassList.remove('bg-primary', 'bg-success', 'bg-info', 'bg-secondary');
            currentClassList.add(getBadgeColor(newType));
            
            // Update file URL
            const newFileUrl = document.getElementById('editLessonFileUrl').value;
            lessonCard.querySelector('.view-download-btn').setAttribute('data-file-url', newFileUrl);
            
            // Update button text if type changed
            const button = lessonCard.querySelector('.view-download-btn');
            const iconElement = button.querySelector('icon');
            
            if (iconElement) {
                // Find the next text node and update it
                let nextNode = iconElement.nextSibling;
                while (nextNode && nextNode.nodeType !== Node.TEXT_NODE) {
                    nextNode = nextNode.nextSibling;
                }
                
                if (nextNode) {
                    nextNode.textContent = ' ' + lessonTypeMap[newType];
                } else {
                    // If no text node found, create one
                    const textNode = document.createTextNode(' ' + lessonTypeMap[newType]);
                    iconElement.parentNode.appendChild(textNode);
                }
            } else {
                // If no icon element, just set button text directly
                button.textContent = lessonTypeMap[newType];
            }
            
            // Close modal using the Bootstrap modal instance
            const modalElement = document.getElementById('editLessonModal');
            const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
            modalInstance.hide();
        }
    });

    // Delete button functionality
    document.querySelectorAll('.delete-btn').forEach(button => {
        button.addEventListener('click', async function(e) {
            e.preventDefault();
            const lessonCard = this.closest('.lesson-card');
            const lessonTitle = lessonCard.querySelector('.lesson-title').textContent;
            
            if (await window.confirmAsync(`Are you sure you want to delete "${lessonTitle}"?`)) {
                lessonCard.remove();
            }
        });
    });

    // Update button text based on lesson type on page load
    document.querySelectorAll('.lesson-card').forEach(card => {
        const type = card.querySelector('.lesson-type').getAttribute('data-original-type');
        const button = card.querySelector('.view-download-btn');
        const iconElement = button.querySelector('icon');
        
        if (iconElement) {
            // Find the next text node and update it
            let nextNode = iconElement.nextSibling;
            while (nextNode && nextNode.nodeType !== Node.TEXT_NODE) {
                nextNode = nextNode.nextSibling;
            }
            
            if (nextNode) {
                nextNode.textContent = ' ' + lessonTypeMap[type];
            } else {
                // If no text node found, create one
                const textNode = document.createTextNode(' ' + lessonTypeMap[type]);
                iconElement.parentNode.appendChild(textNode);
            }
        } else {
            // If no icon element, just set button text directly
            button.textContent = lessonTypeMap[type];
        }
    });
});
