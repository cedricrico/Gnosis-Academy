document.addEventListener('DOMContentLoaded', function() {
    const professorIdInput = document.getElementById('professorId');
    const professorPasswordInput = document.getElementById('professorPassword');
    const toggleBtn = document.getElementById('professorTogglePassword');
    const eyeIcon = document.getElementById('professorEyeIcon');
    const form = document.getElementById('professor-login-form');
    
    // Add input event listener for real-time validation
    professorIdInput.addEventListener('input', function() {
        const value = this.value;
        // Auto-add dash after 4 digits if not present
        if (value.length === 4 && !value.includes('-')) {
            this.value = value + '-';
        }
    });
    
    // Form validation
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
    
    // Password visibility toggle
    toggleBtn.addEventListener('click', function() {
        const isPassword = professorPasswordInput.type === 'password';
        professorPasswordInput.type = isPassword ? 'text' : 'password';
        eyeIcon.src = isPassword ? '/photos/hide.png?v=20260303' : '/photos/visible.png?v=20260303';
    });
});
