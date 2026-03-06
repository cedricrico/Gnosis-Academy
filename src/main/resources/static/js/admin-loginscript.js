document.addEventListener('DOMContentLoaded', function () {
    const adminUsernameInput = document.getElementById('adminUsername');
    const adminPasswordInput = document.getElementById('adminPassword');
    const adminToggleBtn = document.getElementById('adminTogglePassword');
    const adminEyeIcon = document.getElementById('adminEyeIcon');
    const adminLoginForm = document.getElementById('adminLoginForm');

    if (!adminUsernameInput || !adminPasswordInput || !adminToggleBtn || !adminEyeIcon || !adminLoginForm) {
        return;
    }

    adminLoginForm.addEventListener('submit', function (event) {
        const usernameValue = adminUsernameInput.value.trim();
        const passwordValue = adminPasswordInput.value.trim();

        if (usernameValue === '') {
            adminUsernameInput.classList.add('is-invalid');
            event.preventDefault();
            event.stopPropagation();
        } else {
            adminUsernameInput.classList.remove('is-invalid');
        }

        if (passwordValue === '') {
            adminPasswordInput.classList.add('is-invalid');
            event.preventDefault();
            event.stopPropagation();
        } else {
            adminPasswordInput.classList.remove('is-invalid');
        }
    });

    adminToggleBtn.addEventListener('click', function () {
        const isPassword = adminPasswordInput.type === 'password';
        adminPasswordInput.type = isPassword ? 'text' : 'password';

        if (isPassword) {
            adminEyeIcon.src = '/photos/hide.png';
            adminEyeIcon.alt = 'Hide Password';
        } else {
            adminEyeIcon.src = '/photos/visible.png';
            adminEyeIcon.alt = 'Show Password';
        }
    });

    adminUsernameInput.addEventListener('input', function () {
        if (this.value.trim() !== '') {
            this.classList.remove('is-invalid');
        }
    });

    adminPasswordInput.addEventListener('input', function () {
        if (this.value.trim() !== '') {
            this.classList.remove('is-invalid');
        }
    });
});
