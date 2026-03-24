document.addEventListener('DOMContentLoaded', function () {
    const termCheckboxes = document.querySelectorAll('[data-terms-checkbox="true"]');

    termCheckboxes.forEach(function (checkbox) {
        const modalId = checkbox.dataset.termsModalId;
        const modalElement = modalId ? document.getElementById(modalId) : null;
        if (!modalElement || typeof bootstrap === 'undefined') {
            return;
        }

        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        const acceptButton = modalElement.querySelector('[data-terms-accept="true"]');
        const declineButton = modalElement.querySelector('[data-terms-decline="true"]');
        const relatedLinks = document.querySelectorAll('[data-terms-link="true"][data-terms-modal-id="' + modalId + '"]');

        checkbox.dataset.termsAccepted = checkbox.checked ? 'true' : 'false';

        checkbox.addEventListener('click', function (event) {
            if (checkbox.checked || checkbox.dataset.termsAccepted === 'true') {
                checkbox.dataset.termsAccepted = 'false';
                checkbox.setCustomValidity('You must agree to the terms and conditions.');
                return;
            }

            event.preventDefault();
            modal.show();
        });

        relatedLinks.forEach(function (link) {
            link.addEventListener('click', function (event) {
                event.preventDefault();
                modal.show();
            });
        });

        if (acceptButton) {
            acceptButton.addEventListener('click', function () {
                checkbox.checked = true;
                checkbox.dataset.termsAccepted = 'true';
                checkbox.setCustomValidity('');
                checkbox.classList.remove('is-invalid');
                modal.hide();
            });
        }

        if (declineButton) {
            declineButton.addEventListener('click', function () {
                checkbox.checked = false;
                checkbox.dataset.termsAccepted = 'false';
                checkbox.setCustomValidity('You must agree to the terms and conditions.');
            });
        }

        checkbox.addEventListener('change', function () {
            if (checkbox.checked && checkbox.dataset.termsAccepted !== 'true') {
                checkbox.checked = false;
                checkbox.setCustomValidity('You must agree to the terms and conditions.');
            } else if (checkbox.checked) {
                checkbox.setCustomValidity('');
            } else {
                checkbox.dataset.termsAccepted = 'false';
                checkbox.setCustomValidity('You must agree to the terms and conditions.');
            }
        });
    });
});
