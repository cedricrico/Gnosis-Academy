document.addEventListener('DOMContentLoaded', function() {
    const idNumberInput = document.getElementById('idNumber');
    const passwordInput = document.getElementById('password');
    const toggleBtn = document.getElementById('togglePassword');
    const eyeIcon = document.getElementById('confirmEyeIcon');
    
    // Add input event listener for real-time validation
    idNumberInput.addEventListener('input', function() {
        const value = this.value;
        // Auto-add dash after 4 digits if not present
        if (value.length === 4 && !value.includes('-')) {
            this.value = value + '-';
        }
    });
    
    // Form validation
    const form = document.querySelector('form');
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
        eyeIcon.src = isPassword ? 'hide.png' : 'visible.png';
    });
});