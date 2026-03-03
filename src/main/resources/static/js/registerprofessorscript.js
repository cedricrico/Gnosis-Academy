document.addEventListener('DOMContentLoaded', function() {
    const professorIdInput = document.getElementById('professorId');
    const professorPasswordInput = document.getElementById('professorPassword');
    const confirmProfessorPasswordInput = document.getElementById('confirmProfessorPassword');
    const togglePasswordBtn = document.getElementById('professorTogglePassword');
    const toggleConfirmPasswordBtn = document.getElementById('professorToggleConfirmPassword');
    const eyeIcon = document.getElementById('professorEyeIcon');
    const eyeIcon2 = document.getElementById('professorEyeIcon2');
    const form = document.getElementById('professor-register-form');
    
    // Professor ID format validation and auto-dash insertion
    professorIdInput.addEventListener('input', function() {
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
    confirmProfessorPasswordInput.addEventListener('input', checkPasswordMatch);
    
    function checkPasswordMatch() {
        const password = professorPasswordInput.value;
        const confirmPassword = confirmProfessorPasswordInput.value;
        const feedbackElement = document.getElementById('confirmProfessorPasswordFeedback');
        
        if (confirmPassword.length > 0) {
            if (password !== confirmPassword) {
                feedbackElement.textContent = 'Passwords do not match';
                confirmProfessorPasswordInput.classList.add('is-invalid');
                confirmProfessorPasswordInput.classList.remove('is-valid');
                return false;
            } else {
                feedbackElement.textContent = '';
                confirmProfessorPasswordInput.classList.remove('is-invalid');
                confirmProfessorPasswordInput.classList.add('is-valid');
                return true;
            }
        }
        return true;
    }
    
    // Password visibility toggle
    togglePasswordBtn.addEventListener('click', function() {
        const isPassword = professorPasswordInput.type === 'password';
        professorPasswordInput.type = isPassword ? 'text' : 'password';
        eyeIcon.src = isPassword ? '/photos/hide.png?v=20260303' : '/photos/visible.png?v=20260303';
    });
    
    // Confirm password visibility toggle
    toggleConfirmPasswordBtn.addEventListener('click', function() {
        const isPassword = confirmProfessorPasswordInput.type === 'password';
        confirmProfessorPasswordInput.type = isPassword ? 'text' : 'password';
        eyeIcon2.src = isPassword ? '/photos/hide.png?v=20260303' : '/photos/visible.png?v=20260303';
    });
    
    // Form validation
    form.addEventListener('submit', function(event) {
        if (!form.checkValidity()) {
            event.preventDefault();
            event.stopPropagation();
        } else {
            // Additional custom validation
            const passwordMatch = checkPasswordMatch();
            const professorIdPattern = /^\d{4}-\d{5}$/;
            const isProfessorIdValid = professorIdPattern.test(professorIdInput.value);
            
            if (!isProfessorIdValid) {
                professorIdInput.classList.add('is-invalid');
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
