document.addEventListener('DOMContentLoaded', function() {
    const employeeIdInput = document.getElementById('employeeId');
    const employeePasswordInput = document.getElementById('employeePassword');
    const toggleBtn = document.getElementById('employeeTogglePassword');
    const eyeIcon = document.getElementById('employeeEyeIcon');
    const form = document.getElementById('employee-login-form');
    const showIcon = '/assets/img/visible.png';
    const hideIcon = '/assets/img/hide.png';
    
    // Add input event listener for real-time validation
    if (employeeIdInput) {
        employeeIdInput.addEventListener('input', function() {
            const value = this.value;
            // Auto-add dash after 4 digits if not present
            if (value.length === 4 && !value.includes('-')) {
                this.value = value + '-';
            }
        });
    }
    
    // Form validation
    if (form && employeeIdInput) {
        form.addEventListener('submit', function(event) {
            const idValue = employeeIdInput.value;
            const pattern = /^\d{4}-\d{5}$/;
            
            if (!pattern.test(idValue)) {
                employeeIdInput.classList.add('is-invalid');
                event.preventDefault();
                event.stopPropagation();
            } else {
                employeeIdInput.classList.remove('is-invalid');
                // Add your employee login logic here
            }
        });
    }
    
    // Password visibility toggle
    if (toggleBtn && employeePasswordInput && eyeIcon) {
        toggleBtn.addEventListener('click', function() {
            const isPassword = employeePasswordInput.type === 'password';
            employeePasswordInput.type = isPassword ? 'text' : 'password';
            eyeIcon.src = isPassword ? hideIcon : showIcon;
        });
    }
});
