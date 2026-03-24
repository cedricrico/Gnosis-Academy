document.addEventListener('DOMContentLoaded', function() {
    const adminUsernameInput = document.getElementById('adminUsername');
    const adminPasswordInput = document.getElementById('adminPassword');
    const adminToggleBtn = document.getElementById('adminTogglePassword');
    const adminEyeIcon = document.getElementById('adminEyeIcon');
    const adminRememberMeCheckbox = document.getElementById('adminRememberMe');
    const rememberedAdminKey = 'gnosis.rememberedAdminUsername';
    const showIcon = '/assets/img/visible.png';
    const hideIcon = '/assets/img/hide.png';
    
    // Form validation
    const adminLoginForm = document.getElementById('adminLoginForm');
    if (adminRememberMeCheckbox) {
        const rememberedAdminUsername = window.localStorage.getItem(rememberedAdminKey);
        if (rememberedAdminUsername) {
            adminUsernameInput.value = rememberedAdminUsername;
            adminRememberMeCheckbox.checked = true;
        }
    }

    if (adminLoginForm && adminUsernameInput && adminPasswordInput) {
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
        
        if (adminRememberMeCheckbox && adminRememberMeCheckbox.checked) {
            window.localStorage.setItem(rememberedAdminKey, usernameValue);
        } else {
            window.localStorage.removeItem(rememberedAdminKey);
        }

        });
    }
    
    // Password visibility toggle
    if (adminToggleBtn && adminPasswordInput && adminEyeIcon) {
        adminToggleBtn.addEventListener('click', function() {
            const isPassword = adminPasswordInput.type === 'password';
            adminPasswordInput.type = isPassword ? 'text' : 'password';
            
            if (isPassword) {
                adminEyeIcon.src = hideIcon;
                adminEyeIcon.alt = 'Hide Password';
            } else {
                adminEyeIcon.src = showIcon;
                adminEyeIcon.alt = 'Show Password';
            }
        });
    }
    
    // Clear validation on input
    if (adminUsernameInput) {
        adminUsernameInput.addEventListener('input', function() {
            if (this.value.trim() !== '') {
                this.classList.remove('is-invalid');
            }
        });
    }
    
    if (adminPasswordInput) {
        adminPasswordInput.addEventListener('input', function() {
            if (this.value.trim() !== '') {
                this.classList.remove('is-invalid');
            }
        });
    }
});
