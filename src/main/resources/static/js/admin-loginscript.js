document.addEventListener('DOMContentLoaded', function() {
    const adminUsernameInput = document.getElementById('adminUsername');
    const adminPasswordInput = document.getElementById('adminPassword');
    const adminToggleBtn = document.getElementById('adminTogglePassword');
    const adminEyeIcon = document.getElementById('adminEyeIcon');
    const adminLoginForm = document.getElementById('adminLoginForm');

    if (!adminUsernameInput || !adminPasswordInput || !adminToggleBtn || !adminEyeIcon || !adminLoginForm) {
        return;
    }
    
    // Form validation
    adminLoginForm.addEventListener('submit', function(event) {
        const usernameValue = adminUsernameInput.value.trim();
        const passwordValue = adminPasswordInput.value.trim();
        
        // Basic validation - you can add more specific validation as needed
        if (usernameValue === '') {
            adminUsernameInput.classList.add('is-invalid');
            event.preventDefault();
            event.stopPropagation();
            return;
        } else {
            adminUsernameInput.classList.remove('is-invalid');
        }
        
        if (passwordValue === '') {
            adminPasswordInput.classList.add('is-invalid');
            event.preventDefault();
            event.stopPropagation();
            return;
        } else {
            adminPasswordInput.classList.remove('is-invalid');
        }
        
        // Here you would typically make an API call to authenticate the admin
        // For now, we'll just prevent the form submission to demonstrate validation
        console.log('Admin login attempt:', {
            username: usernameValue,
            password: passwordValue
        });
        // event.preventDefault(); // Remove this line when connecting to backend
    });
    
    // Password visibility toggle
    adminToggleBtn.addEventListener('click', function() {
        const isPassword = adminPasswordInput.type === 'password';
        adminPasswordInput.type = isPassword ? 'text' : 'password';
        
        // Toggle the eye icon (assuming you have hide.png and visible.png images)
        if (isPassword) {
            adminEyeIcon.src = '/assets/img/hide.png';
            adminEyeIcon.alt = 'Hide Password';
        } else {
            adminEyeIcon.src = '/assets/img/visible.png';
            adminEyeIcon.alt = 'Show Password';
        }
    });
    
    // Clear validation on input
    adminUsernameInput.addEventListener('input', function() {
        if (this.value.trim() !== '') {
            this.classList.remove('is-invalid');
        }
    });
    
    adminPasswordInput.addEventListener('input', function() {
        if (this.value.trim() !== '') {
            this.classList.remove('is-invalid');
        }
    });
});
