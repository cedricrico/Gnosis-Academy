document.addEventListener('DOMContentLoaded', function() {
    const employeeIdInput = document.getElementById('employeeId');
    const employeePasswordInput = document.getElementById('employeePassword');
    const confirmEmployeePasswordInput = document.getElementById('confirmEmployeePassword');
    const togglePasswordBtn = document.getElementById('employeeTogglePassword');
    const toggleConfirmPasswordBtn = document.getElementById('employeeToggleConfirmPassword');
    const eyeIcon = document.getElementById('employeeEyeIcon');
    const eyeIcon2 = document.getElementById('employeeEyeIcon2');
    const form = document.getElementById('employee-register-form');
    
    // Employee ID format validation and auto-dash insertion
    employeeIdInput.addEventListener('input', function() {
        const value = this.value;
        // Auto-add dash after 4 digits if not present
        if (value.length === 4 && !value.includes('-')) {
            this.value = value + '-';
        }
        
        // Validate format in real-time
        const pattern = /^\d{4}-\d{5}$/;
        if (pattern.test(value)) {
            this.classList.remove('is-invalid');
            this.classList.add('is-valid');
        } else if (value.length > 0) {
            this.classList.add('is-invalid');
            this.classList.remove('is-valid');
        } else {
            this.classList.remove('is-invalid', 'is-valid');
        }
    });
    
    // Password match validation
    confirmEmployeePasswordInput.addEventListener('input', checkPasswordMatch);
    
    function checkPasswordMatch() {
        const password = employeePasswordInput.value;
        const confirmPassword = confirmEmployeePasswordInput.value;
        const feedbackElement = document.getElementById('confirmEmployeePasswordFeedback');
        
        if (confirmPassword.length > 0) {
            if (password !== confirmPassword) {
                feedbackElement.textContent = 'Passwords do not match';
                confirmEmployeePasswordInput.classList.add('is-invalid');
                confirmEmployeePasswordInput.classList.remove('is-valid');
                return false;
            } else {
                feedbackElement.textContent = '';
                confirmEmployeePasswordInput.classList.remove('is-invalid');
                confirmEmployeePasswordInput.classList.add('is-valid');
                return true;
            }
        }
        return true;
    }
    
    // Password visibility toggle
    togglePasswordBtn.addEventListener('click', function() {
        const isPassword = employeePasswordInput.type === 'password';
        employeePasswordInput.type = isPassword ? 'text' : 'password';
        eyeIcon.src = isPassword ? 'hide.png' : 'visible.png';
    });
    
    // Confirm password visibility toggle
    toggleConfirmPasswordBtn.addEventListener('click', function() {
        const isPassword = confirmEmployeePasswordInput.type === 'password';
        confirmEmployeePasswordInput.type = isPassword ? 'text' : 'password';
        eyeIcon2.src = isPassword ? 'hide.png' : 'visible.png';
    });
    
    // Form validation
    form.addEventListener('submit', function(event) {
        if (!form.checkValidity()) {
            event.preventDefault();
            event.stopPropagation();
        } else {
            // Additional custom validation
            const passwordMatch = checkPasswordMatch();
            const employeeIdPattern = /^\d{4}-\d{5}$/;
            const isEmployeeIdValid = employeeIdPattern.test(employeeIdInput.value);
            
            if (!isEmployeeIdValid) {
                employeeIdInput.classList.add('is-invalid');
                event.preventDefault();
                event.stopPropagation();
            }
            
            if (!passwordMatch) {
                event.preventDefault();
                event.stopPropagation();
            }
        }
        
        form.classList.add('was-validated');
    });
});