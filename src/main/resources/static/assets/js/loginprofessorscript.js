document.addEventListener('DOMContentLoaded', function() {
    const professorIdInput = document.getElementById('employeeId') || document.getElementById('professorId');
    const professorPasswordInput = document.getElementById('professorPassword');
    const toggleBtn = document.getElementById('professorTogglePassword');
    const eyeIcon = document.getElementById('professorEyeIcon');
    const form = document.getElementById('professor-login-form');
    const rememberMeCheckbox = document.getElementById('professorRememberMe');
    const rememberedProfessorKey = 'gnosis.rememberedProfessorId';
    const showIcon = '/assets/img/visible.png';
    const hideIcon = '/assets/img/hide.png';

    if (!professorIdInput || !professorPasswordInput || !toggleBtn || !eyeIcon || !form) {
        return;
    }

    const rememberedProfessorId = window.localStorage.getItem(rememberedProfessorKey);
    if (rememberMeCheckbox && rememberedProfessorId) {
        professorIdInput.value = formatEmployeeId(rememberedProfessorId);
        rememberMeCheckbox.checked = true;
        validateEmployeeId();
    }

    function formatEmployeeId(value) {
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

    function validateEmployeeId() {
        const value = professorIdInput.value.trim();
        const pattern = /^\d{4}-\d{5}$/;
        const isValid = pattern.test(value);
        professorIdInput.setCustomValidity(isValid ? '' : 'Employee ID must be in the format 0000-00000');
        professorIdInput.classList.toggle('is-invalid', !isValid);
        return isValid;
    }

    function validatePassword() {
        const value = professorPasswordInput.value.trim();
        const isValid = value.length > 0;
        professorPasswordInput.setCustomValidity(isValid ? '' : 'Password is required');
        professorPasswordInput.classList.toggle('is-invalid', !isValid);
        return isValid;
    }

    professorIdInput.addEventListener('input', function() {
        this.value = formatEmployeeId(this.value);
        if (this.value.trim() !== '') {
            validateEmployeeId();
        }
    });

    professorIdInput.addEventListener('keydown', function(event) {
        if (event.ctrlKey || event.metaKey || event.altKey) {
            return;
        }
        const isDigit = /^[0-9]$/.test(event.key);
        if (isDigit && !canTypeMoreDigits(this)) {
            event.preventDefault();
        }
    });

    professorIdInput.addEventListener('blur', validateEmployeeId);

    professorPasswordInput.addEventListener('input', function() {
        if (this.value.trim() !== '') {
            validatePassword();
        }
    });

    professorPasswordInput.addEventListener('blur', validatePassword);

    form.addEventListener('submit', function(event) {
        const idOk = validateEmployeeId();
        const passwordOk = validatePassword();
        if (!idOk || !passwordOk) {
            event.preventDefault();
            event.stopPropagation();
            return;
        }

        if (rememberMeCheckbox && rememberMeCheckbox.checked) {
            window.localStorage.setItem(rememberedProfessorKey, professorIdInput.value.trim());
        } else {
            window.localStorage.removeItem(rememberedProfessorKey);
        }
    });

    toggleBtn.addEventListener('click', function() {
        const isPassword = professorPasswordInput.type === 'password';
        professorPasswordInput.type = isPassword ? 'text' : 'password';
        eyeIcon.src = isPassword ? hideIcon : showIcon;
    });
});
