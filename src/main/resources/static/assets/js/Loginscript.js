document.addEventListener('DOMContentLoaded', function() {
    const studentIdInput = document.getElementById('studentId');
    const passwordInput = document.getElementById('password');
    const toggleBtn = document.getElementById('togglePassword');
    const eyeIcon = document.getElementById('confirmEyeIcon');
    const showIcon = '/assets/img/visible.png';
    const hideIcon = '/assets/img/hide.png';

    // Add input event listener for real-time validation
    if (studentIdInput) {
        studentIdInput.addEventListener('input', function() {
            const value = this.value;
            // Auto-add dash after 4 digits if not present
            if (value.length === 4 && !value.includes('-')) {
                this.value = value + '-';
            }
        });
    }

    // Form validation
    const form = document.querySelector('form');
    if (form && studentIdInput) {
        form.addEventListener('submit', function(event) {
            const idValue = studentIdInput.value;
            const pattern = /^\d{4}-\d{5}$/;

            if (!pattern.test(idValue)) {
                studentIdInput.classList.add('is-invalid');
                event.preventDefault();
                event.stopPropagation();
            } else {
                studentIdInput.classList.remove('is-invalid');
            }
        });
    }

    // Password visibility toggle
    if (toggleBtn && passwordInput && eyeIcon) {
        toggleBtn.addEventListener('click', function() {
            const isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';
            eyeIcon.src = isPassword ? hideIcon : showIcon;
        });
    }
});
