document.addEventListener('DOMContentLoaded', function() {
    const employeeIdInput = document.getElementById('employeeId');
    const employeePasswordInput = document.getElementById('employeePassword');
    const confirmEmployeePasswordInput = document.getElementById('confirmEmployeePassword');
    const togglePasswordBtn = document.getElementById('employeeTogglePassword');
    const toggleConfirmPasswordBtn = document.getElementById('employeeToggleConfirmPassword');
    const eyeIcon = document.getElementById('employeeEyeIcon');
    const eyeIcon2 = document.getElementById('employeeEyeIcon2');
    const form = document.getElementById('employee-register-form');
    const showIcon = '/assets/img/visible.png';
    const hideIcon = '/assets/img/hide.png';
    
    // Employee ID format validation and auto-dash insertion
    if (employeeIdInput) {
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
    }
    
    // Password match validation
    if (confirmEmployeePasswordInput) {
        confirmEmployeePasswordInput.addEventListener('input', checkPasswordMatch);
    }
    if (employeePasswordInput) {
        employeePasswordInput.addEventListener('input', checkPasswordMatch);
        employeePasswordInput.addEventListener('input', function() {
            validatePassword(employeePasswordInput);
        });
        employeePasswordInput.addEventListener('blur', function() {
            validatePassword(employeePasswordInput);
        });
    }
    
    function checkPasswordMatch() {
        if (!employeePasswordInput || !confirmEmployeePasswordInput) {
            return true;
        }

        const password = employeePasswordInput.value;
        const confirmPassword = confirmEmployeePasswordInput.value;
        const feedbackElement = document.getElementById('confirmEmployeePasswordFeedback');
        
        if (confirmPassword.length > 0) {
            if (password !== confirmPassword) {
                if (feedbackElement) {
                    feedbackElement.textContent = 'Passwords do not match';
                }
                confirmEmployeePasswordInput.setCustomValidity('Passwords do not match');
                confirmEmployeePasswordInput.classList.add('is-invalid');
                confirmEmployeePasswordInput.classList.remove('is-valid');
                return false;
            } else {
                if (feedbackElement) {
                    feedbackElement.textContent = 'Please confirm your password.';
                }
                confirmEmployeePasswordInput.setCustomValidity('');
                confirmEmployeePasswordInput.classList.remove('is-invalid');
                confirmEmployeePasswordInput.classList.add('is-valid');
                return true;
            }
        }
        if (feedbackElement) {
            feedbackElement.textContent = 'Please confirm your password.';
        }
        confirmEmployeePasswordInput.setCustomValidity('');
        confirmEmployeePasswordInput.classList.remove('is-invalid', 'is-valid');
        return true;
    }

    function validatePassword(input) {
        if (!input) {
            return true;
        }
        const value = input.value;
        const pattern = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$/;
        const isValid = value === '' || pattern.test(value);

        input.setCustomValidity(isValid ? '' : 'Password must be at least 8 characters long and contain at least one number, one lowercase and one uppercase letter.');
        input.classList.toggle('is-invalid', !isValid);
        input.classList.toggle('is-valid', value.length > 0 && isValid);
        return isValid;
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

    function initNameValidation() {
        const firstNameInput = document.getElementById('firstName');
        const lastNameInput = document.getElementById('lastName');
        const middleInitialInput = document.getElementById('middleInitial');
        const emailInput = document.getElementById('email');

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

        if (emailInput) {
            emailInput.addEventListener('input', function() {
                validateEmail(this);
            });
            emailInput.addEventListener('blur', function() {
                validateEmail(this);
            });
        }
    }

    function validateEmail(input) {
        if (!input) {
            return true;
        }
        const value = input.value.trim();
        const pattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        const isValid = value === '' || pattern.test(value);

        input.setCustomValidity(isValid ? '' : 'Please enter a valid email address');
        if (!isValid) {
            input.classList.add('is-invalid');
        } else {
            input.classList.remove('is-invalid');
        }

        return isValid;
    }
    
    // Password visibility toggle
    if (togglePasswordBtn && employeePasswordInput && eyeIcon) {
        togglePasswordBtn.addEventListener('click', function() {
            const isPassword = employeePasswordInput.type === 'password';
            employeePasswordInput.type = isPassword ? 'text' : 'password';
            eyeIcon.src = isPassword ? hideIcon : showIcon;
        });
    }
    
    // Confirm password visibility toggle
    if (toggleConfirmPasswordBtn && confirmEmployeePasswordInput && eyeIcon2) {
        toggleConfirmPasswordBtn.addEventListener('click', function() {
            const isPassword = confirmEmployeePasswordInput.type === 'password';
            confirmEmployeePasswordInput.type = isPassword ? 'text' : 'password';
            eyeIcon2.src = isPassword ? hideIcon : showIcon;
        });
    }
    
    // Form validation
    if (form) {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                // Additional custom validation
                const passwordMatch = checkPasswordMatch();
                const passwordValid = employeePasswordInput ? validatePassword(employeePasswordInput) : true;
                const employeeIdPattern = /^\d{4}-\d{5}$/;
                const isEmployeeIdValid = employeeIdInput ? employeeIdPattern.test(employeeIdInput.value) : true;
                const firstNameInput = document.getElementById('firstName');
                const lastNameInput = document.getElementById('lastName');
                const middleInitialInput = document.getElementById('middleInitial');
                const emailInput = document.getElementById('email');
                
                if (!isEmployeeIdValid && employeeIdInput) {
                    employeeIdInput.classList.add('is-invalid');
                    event.preventDefault();
                    event.stopPropagation();
                }

                if ((firstNameInput && !validateName(firstNameInput))
                    || (lastNameInput && !validateName(lastNameInput))
                    || (middleInitialInput && !validateMiddleInitial(middleInitialInput))
                    || (emailInput && !validateEmail(emailInput))) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                
                if (!passwordMatch) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                if (!passwordValid) {
                    event.preventDefault();
                    event.stopPropagation();
                }
            }
            
            form.classList.add('was-validated');
        });
    }

    initNameValidation();
});
