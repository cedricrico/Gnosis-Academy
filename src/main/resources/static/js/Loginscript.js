document.addEventListener('DOMContentLoaded', function() {
    const idNumberInput = document.getElementById('studentId');
    const passwordInput = document.getElementById('password');
    const toggleBtn = document.getElementById('togglePassword');
    const eyeIcon = document.getElementById('confirmEyeIcon');
    const form = document.getElementById('student-login-form');

    if (!idNumberInput || !passwordInput || !toggleBtn || !eyeIcon || !form) {
        return;
    }
    
    // Add input event listener for real-time validation
    idNumberInput.addEventListener('input', function() {
        const value = this.value;
        // Auto-add dash after 4 digits if not present
        if (value.length === 4 && !value.includes('-')) {
            this.value = value + '-';
        }
    });
    
    // Form validation
    form.addEventListener('submit', function(event) {
        const idValue = idNumberInput.value;
        const pattern = /^\d{4}-\d{5}$/;
        
        if (!pattern.test(idValue)) {
            idNumberInput.classList.add('is-invalid');
            event.preventDefault();
            event.stopPropagation();
        } else {
            idNumberInput.classList.remove('is-invalid');
        }
    });
    
    // Password visibility toggle
    toggleBtn.addEventListener('click', function() {
        const isPassword = passwordInput.type === 'password';
        passwordInput.type = isPassword ? 'text' : 'password';
        eyeIcon.src = isPassword ? '/assets/img/hide.png' : '/assets/img/visible.png';
    });
});
