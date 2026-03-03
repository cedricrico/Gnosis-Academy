(function () {
    'use strict';

    // Enhanced form validation
    var forms = document.querySelectorAll('.needs-validation');
    var passwordInput = document.getElementById('password');
    var confirmPasswordInput = document.getElementById('confirmPassword');
    var studentIdInput = document.getElementById('studentId');
    var strengthBar = document.getElementById('passwordStrength');
    var strengthText = document.getElementById('passwordStrengthText');
    var confirmPasswordFeedback = document.getElementById('confirmPasswordFeedback');
    var studentIdPattern = /^\d{4}-\d{5}$/;

    // Password validation functions
    function checkPasswordStrength(password) {
        let strength = 0;
        let feedback = [];

        // Length check
        if (password.length >= 8) strength += 1;
        if (password.length >= 12) strength += 1;

        // Complexity checks
        if (/[a-z]/.test(password)) strength += 1;
        if (/[A-Z]/.test(password)) strength += 1;
        if (/[0-9]/.test(password)) strength += 1;
        if (/[^A-Za-z0-9]/.test(password)) strength += 1;

        // Provide feedback
        if (password.length > 0) {
            if (password.length < 8) feedback.push("Use at least 8 characters");
            if (!/[a-z]/.test(password)) feedback.push("Add lowercase letters");
            if (!/[A-Z]/.test(password)) feedback.push("Add uppercase letters");
            if (!/[0-9]/.test(password)) feedback.push("Add numbers");
            if (!/[^A-Za-z0-9]/.test(password)) feedback.push("Add special characters");
        }

        return { strength: Math.min(strength, 6), feedback: feedback };
    }

    function updatePasswordStrength() {
        if (!passwordInput || !strengthBar || !strengthText) {
            return;
        }

        const password = passwordInput.value;
        const { strength, feedback } = checkPasswordStrength(password);
        
        // Update progress bar
        const width = strength * 16.66; // 6 levels, each 16.66% of 100%
        strengthBar.style.width = width + '%';
        
        // Update color based on strength
        if (strength <= 2) {
            strengthBar.className = 'progress-bar bg-danger';
            strengthText.textContent = 'Weak';
        } else if (strength <= 4) {
            strengthBar.className = 'progress-bar bg-warning';
            strengthText.textContent = 'Medium';
        } else {
            strengthBar.className = 'progress-bar bg-success';
            strengthText.textContent = 'Strong';
        }
        
        // Show feedback if password is being entered
        if (password.length > 0 && feedback.length > 0) {
            strengthText.textContent += ' - ' + feedback[0];
        }
    }

    function checkPasswordMatch() {
        if (!passwordInput || !confirmPasswordInput || !confirmPasswordFeedback) {
            return true;
        }

        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        
        if (confirmPassword.length > 0) {
            if (password !== confirmPassword) {
                confirmPasswordFeedback.textContent = 'Passwords do not match';
                confirmPasswordFeedback.style.display = 'block';
                confirmPasswordInput.classList.add('is-invalid');
                confirmPasswordInput.classList.remove('is-valid');
                return false;
            } else {
                confirmPasswordFeedback.style.display = 'none';
                confirmPasswordInput.classList.remove('is-invalid');
                confirmPasswordInput.classList.add('is-valid');
                return true;
            }
        }
        return true;
    }

    // Initialize password functionality
    function initPasswordFeatures() {
        if (studentIdInput) {
            studentIdInput.addEventListener('input', function () {
                var digitsOnly = this.value.replace(/\D/g, '').slice(0, 9);
                this.value = digitsOnly.length > 4
                    ? digitsOnly.slice(0, 4) + '-' + digitsOnly.slice(4)
                    : digitsOnly;

                if (studentIdPattern.test(this.value)) {
                    this.classList.remove('is-invalid');
                }
            });
        }

        if (passwordInput) {
            passwordInput.addEventListener('input', updatePasswordStrength);
            passwordInput.addEventListener('input', checkPasswordMatch);
            passwordInput.addEventListener('blur', updatePasswordStrength);
        }
        
        if (confirmPasswordInput) {
            confirmPasswordInput.addEventListener('input', checkPasswordMatch);
            confirmPasswordInput.addEventListener('blur', checkPasswordMatch);
        }
        
        setupPasswordToggle();
        setupConfirmPasswordToggle();
    }
    
    function setupPasswordToggle() {
        const toggleBtn = document.getElementById('togglePassword');
        const eyeIcon = document.getElementById('confirmEyeIcon');
        
        if (toggleBtn && passwordInput && eyeIcon) {
            toggleBtn.addEventListener('click', function() {
                // Toggle the type attribute
                const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
                passwordInput.setAttribute('type', type);
                
                // Toggle the image source
                eyeIcon.src = type === 'password' ? '/photos/visible.png' : '/photos/hide.png';
            });
        }
    }
    
    function setupConfirmPasswordToggle() {
        const toggleBtn = document.getElementById('toggleConfirmPassword');
        const eyeIcon = document.getElementById('confirmEyeIcon2');
        
        if (toggleBtn && confirmPasswordInput && eyeIcon) {
            toggleBtn.addEventListener('click', function() {
                // Toggle the type attribute
                const type = confirmPasswordInput.getAttribute('type') === 'password' ? 'text' : 'password';
                confirmPasswordInput.setAttribute('type', type);
                
                // Toggle the image source
                eyeIcon.src = type === 'password' ? '/photos/visible.png' : '/photos/hide.png';
            });
        }
    }

    Array.prototype.slice.call(forms)
        .forEach(function (form) {
            form.addEventListener('submit', function (event) {
                // Check password match before submission
                const password = passwordInput ? passwordInput.value : '';
                const confirmPassword = confirmPasswordInput ? confirmPasswordInput.value : '';
                
                if (password !== confirmPassword && confirmPassword.length > 0) {
                    event.preventDefault();
                    event.stopPropagation();
                    
                    if (confirmPasswordFeedback) {
                        confirmPasswordFeedback.textContent = 'Passwords do not match';
                        confirmPasswordFeedback.style.display = 'block';
                    }
                    if (confirmPasswordInput) {
                        confirmPasswordInput.classList.add('is-invalid');
                    }
                    return;
                }

                if (studentIdInput && !studentIdPattern.test(studentIdInput.value)) {
                    studentIdInput.classList.add('is-invalid');
                    event.preventDefault();
                    event.stopPropagation();
                }

                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                form.classList.add('was-validated');
            }, false);
        });

    // Initialize when DOM is loaded
    document.addEventListener('DOMContentLoaded', function() {
        initPasswordFeatures();
    });
})();
