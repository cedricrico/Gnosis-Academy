document.addEventListener('DOMContentLoaded', function() {
    const idNumberInput = document.getElementById('studentId');
    const passwordInput = document.getElementById('password');
    const toggleBtn = document.getElementById('togglePassword');
    const eyeIcon = document.getElementById('confirmEyeIcon');
    const form = document.getElementById('student-login-form');

    if (!idNumberInput || !passwordInput || !toggleBtn || !eyeIcon || !form) {
        return;
    }

    function formatStudentId(value) {
        const digits = value.replace(/\D/g, '').slice(0, 9);
        if (digits.length <= 4) {
            return digits;
        }
        return `${digits.slice(0, 4)}-${digits.slice(4)}`;
    }

    function canTypeMoreDigits(input) {
        const selectionStart = input.selectionStart ?? input.value.length;
        const selectionEnd = input.selectionEnd ?? input.value.length;
        const selectedLength = Math.max(0, selectionEnd - selectionStart);
        const currentDigits = input.value.replace(/\D/g, '');
        return currentDigits.length - selectedLength < 9;
    }

    function validateStudentId() {
        const value = idNumberInput.value.trim();
        const pattern = /^\d{4}-\d{5}$/;
        const isValid = pattern.test(value);
        idNumberInput.setCustomValidity(isValid ? '' : 'Student ID must be in the format 0000-00000');
        idNumberInput.classList.toggle('is-invalid', !isValid);
        return isValid;
    }

    function validatePassword() {
        const value = passwordInput.value.trim();
        const isValid = value.length > 0;
        passwordInput.setCustomValidity(isValid ? '' : 'Password is required');
        passwordInput.classList.toggle('is-invalid', !isValid);
        return isValid;
    }

    idNumberInput.addEventListener('input', function() {
        this.value = formatStudentId(this.value);
        if (this.value.trim() !== '') {
            validateStudentId();
        }
    });

    idNumberInput.addEventListener('keydown', function(event) {
        if (event.ctrlKey || event.metaKey || event.altKey) {
            return;
        }
        const isDigit = /^[0-9]$/.test(event.key);
        if (isDigit && !canTypeMoreDigits(this)) {
            event.preventDefault();
        }
    });

    idNumberInput.addEventListener('blur', validateStudentId);

    passwordInput.addEventListener('input', function() {
        if (this.value.trim() !== '') {
            validatePassword();
        }
    });

    passwordInput.addEventListener('blur', validatePassword);

    form.addEventListener('submit', function(event) {
        const idOk = validateStudentId();
        const passwordOk = validatePassword();
        if (!idOk || !passwordOk) {
            event.preventDefault();
            event.stopPropagation();
        }
    });

    toggleBtn.addEventListener('click', function() {
        const isPassword = passwordInput.type === 'password';
        passwordInput.type = isPassword ? 'text' : 'password';
        eyeIcon.src = isPassword ? '/assets/img/hide.png' : '/assets/img/visible.png';
    });
});
