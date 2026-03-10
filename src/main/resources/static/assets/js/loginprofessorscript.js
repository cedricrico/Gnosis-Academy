document.addEventListener('DOMContentLoaded', function() {
    const professorIdInput = document.getElementById('employeeId') || document.getElementById('professorId');
    const professorPasswordInput = document.getElementById('professorPassword');
    const toggleBtn = document.getElementById('professorTogglePassword');
    const eyeIcon = document.getElementById('professorEyeIcon');
    const form = document.getElementById('professor-login-form');
    const showIcon = '/assets/img/visible.png';
    const hideIcon = '/assets/img/hide.png';
    
    // Add input event listener for real-time validation
    if (professorIdInput) {
        professorIdInput.addEventListener('input', function() {
            const value = this.value;
            // Auto-add dash after 4 digits if not present
            if (value.length === 4 && !value.includes('-')) {
                this.value = value + '-';
            }
        });
    }
    
    // Form validation
    if (form && professorIdInput) {
        form.addEventListener('submit', function(event) {
            const idValue = professorIdInput.value;
            const pattern = /^\d{4}-\d{5}$/;
            
            if (!pattern.test(idValue)) {
                professorIdInput.classList.add('is-invalid');
                event.preventDefault();
                event.stopPropagation();
            } else {
                professorIdInput.classList.remove('is-invalid');
            }
        });
    }
    
    // Password visibility toggle
    if (toggleBtn && professorPasswordInput && eyeIcon) {
        toggleBtn.addEventListener('click', function() {
            const isPassword = professorPasswordInput.type === 'password';
            professorPasswordInput.type = isPassword ? 'text' : 'password';
            eyeIcon.src = isPassword ? hideIcon : showIcon;
        });
    }
});
