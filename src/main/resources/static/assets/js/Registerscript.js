(function () {
    'use strict';

    // Enhanced form validation
    var forms = document.querySelectorAll('.needs-validation');

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
        const password = document.getElementById('password').value;
        const { strength, feedback } = checkPasswordStrength(password);
        const strengthBar = document.getElementById('passwordStrength');
        const strengthText = document.getElementById('passwordStrengthText');
        
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
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        const feedbackElement = document.getElementById('confirmPasswordFeedback');
        
        if (confirmPassword.length > 0) {
            if (password !== confirmPassword) {
                feedbackElement.textContent = 'Passwords do not match';
                feedbackElement.style.display = 'block';
                document.getElementById('confirmPassword').classList.add('is-invalid');
                document.getElementById('confirmPassword').classList.remove('is-valid');
                return false;
            } else {
                feedbackElement.style.display = 'none';
                document.getElementById('confirmPassword').classList.remove('is-invalid');
                document.getElementById('confirmPassword').classList.add('is-valid');
                return true;
            }
        }
        return true;
    }

    // Initialize password functionality
    function initPasswordFeatures() {
        const passwordInput = document.getElementById('password');
        const confirmPasswordInput = document.getElementById('confirmPassword');
        
        if (passwordInput) {
            passwordInput.addEventListener('input', updatePasswordStrength);
            passwordInput.addEventListener('blur', updatePasswordStrength);
        }
        
        if (confirmPasswordInput) {
            confirmPasswordInput.addEventListener('input', checkPasswordMatch);
            confirmPasswordInput.addEventListener('blur', checkPasswordMatch);
        }
        
        setupPasswordToggle();
        setupConfirmPasswordToggle();
    }

    function formatStudentId(value) {
        const digits = value.replace(/\D/g, '').slice(0, 9);
        if (digits.length <= 4) {
            return digits;
        }
        return digits.slice(0, 4) + '-' + digits.slice(4);
    }

    function validateStudentId(input) {
        if (!input) {
            return true;
        }
        const pattern = /^\d{4}-\d{5}$/;
        const value = input.value.trim();
        const isValid = value === '' || pattern.test(value);

        input.setCustomValidity(isValid ? '' : 'Student ID must be in format: 0000-00000');
        if (!isValid) {
            input.classList.add('is-invalid');
        } else {
            input.classList.remove('is-invalid');
        }

        return isValid;
    }

    function initStudentIdMask() {
        const studentIdInput = document.getElementById('studentId');
        if (!studentIdInput) {
            return;
        }

        studentIdInput.addEventListener('input', function() {
            this.value = formatStudentId(this.value);
            validateStudentId(this);
        });

        studentIdInput.addEventListener('blur', function() {
            validateStudentId(this);
        });
    }

    function validateName(input) {
        if (!input) {
            return true;
        }
        const value = input.value.trim();
        const pattern = /^[A-Za-z]+([ '\-][A-Za-z]+)*$/;
        const isValid = value === '' || (value.length >= 2 && pattern.test(value));

        input.setCustomValidity(isValid ? '' : 'Name must be at least 2 letters and contain letters only');
        if (!isValid) {
            input.classList.add('is-invalid');
        } else {
            input.classList.remove('is-invalid');
        }

        return isValid;
    }

    function initNameValidation() {
        const firstNameInput = document.getElementById('firstName');
        const lastNameInput = document.getElementById('lastName');
        const middleInitialInput = document.getElementById('middleInitial');

        if (firstNameInput) {
            firstNameInput.addEventListener('input', function() {
                validateName(this);
            });
            firstNameInput.addEventListener('blur', function() {
                validateName(this);
            });
        }

        if (lastNameInput) {
            lastNameInput.addEventListener('input', function() {
                validateName(this);
            });
            lastNameInput.addEventListener('blur', function() {
                validateName(this);
            });
        }

        if (middleInitialInput) {
            middleInitialInput.addEventListener('input', function() {
                validateMiddleInitial(this);
            });
            middleInitialInput.addEventListener('blur', function() {
                validateMiddleInitial(this);
            });
        }
    }

    function validateMiddleInitial(input) {
        if (!input) {
            return true;
        }
        const value = input.value.trim();
        const pattern = /^[A-Za-z]?$/;
        const isValid = pattern.test(value);

        input.setCustomValidity(isValid ? '' : 'Middle initial must be a letter');
        if (!isValid) {
            input.classList.add('is-invalid');
        } else {
            input.classList.remove('is-invalid');
        }

        return isValid;
    }
    
    function setupPasswordToggle() {
        const toggleBtn = document.getElementById('togglePassword');
        const passwordInput = document.getElementById('password');
        const eyeIcon = document.getElementById('confirmEyeIcon');
        const showIcon = '/assets/img/visible.png';
        const hideIcon = '/assets/img/hide.png';
        
        if (toggleBtn && passwordInput && eyeIcon) {
            toggleBtn.addEventListener('click', function() {
                // Toggle the type attribute
                const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
                passwordInput.setAttribute('type', type);
                
                // Toggle the image source
                eyeIcon.src = type === 'password' ? showIcon : hideIcon;
            });
        }
    }
    
    function setupConfirmPasswordToggle() {
        const toggleBtn = document.getElementById('toggleConfirmPassword');
        const confirmPasswordInput = document.getElementById('confirmPassword');
        const eyeIcon = document.getElementById('confirmEyeIcon2');
        const showIcon = '/assets/img/visible.png';
        const hideIcon = '/assets/img/hide.png';
        
        if (toggleBtn && confirmPasswordInput && eyeIcon) {
            toggleBtn.addEventListener('click', function() {
                // Toggle the type attribute
                const type = confirmPasswordInput.getAttribute('type') === 'password' ? 'text' : 'password';
                confirmPasswordInput.setAttribute('type', type);
                
                // Toggle the image source
                eyeIcon.src = type === 'password' ? showIcon : hideIcon;
            });
        }
    }

    Array.prototype.slice.call(forms)
        .forEach(function (form) {
            form.addEventListener('submit', function (event) {
                // Check password match before submission
                const password = document.getElementById('password').value;
                const confirmPassword = document.getElementById('confirmPassword').value;
                
                if (password !== confirmPassword && confirmPassword.length > 0) {
                    event.preventDefault();
                    event.stopPropagation();
                    
                    const feedbackElement = document.getElementById('confirmPasswordFeedback');
                    feedbackElement.textContent = 'Passwords do not match';
                    feedbackElement.style.display = 'block';
                    document.getElementById('confirmPassword').classList.add('is-invalid');
                    return;
                }

                const studentIdInput = document.getElementById('studentId');
                if (studentIdInput && !validateStudentId(studentIdInput)) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                const firstNameInput = document.getElementById('firstName');
                const lastNameInput = document.getElementById('lastName');
                const middleInitialInput = document.getElementById('middleInitial');
                if ((firstNameInput && !validateName(firstNameInput))
                    || (lastNameInput && !validateName(lastNameInput))
                    || (middleInitialInput && !validateMiddleInitial(middleInitialInput))) {
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
        initStudentIdMask();
        initNameValidation();
    });
})();
