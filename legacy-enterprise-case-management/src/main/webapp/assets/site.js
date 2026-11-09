/* Keep server-rendered forms and ordinary navigation working without JavaScript. */
(function ($) {
    "use strict";
    $(function () {
        $(".field-errors").each(function () {
            if ($(this).text().trim()) {
                var errorId = this.id;
                $("[aria-describedby]").filter(function () {
                    return (" " + $(this).attr("aria-describedby") + " ").indexOf(" " + errorId + " ") !== -1;
                }).addClass("is-invalid").attr("aria-invalid", "true");
            }
        });
        var path = window.location.pathname;
        $("#primary-navigation .nav-link").each(function () {
            if (this.pathname === path) {
                $(this).attr("aria-current", "page");
            }
        });
        // Struts owns validation. Move focus to its summary after a failed submission.
        $("[data-error-summary]").first().trigger("focus");
        var form = document.getElementById("archive-case-form");
        var dialog = document.getElementById("archive-confirmation");
        if (form && dialog && window.bootstrap) {
            var modal = new bootstrap.Modal(dialog);
            var trigger;
            form.onsubmit = null;
            $(form).on("submit", function (event) {
                event.preventDefault();
                trigger = document.activeElement;
                modal.show();
            });
            $(dialog).on("shown.bs.modal", function () {
                document.getElementById("archive-cancel").focus();
            }).on("hidden.bs.modal", function () {
                if (trigger) {
                    trigger.focus();
                }
            });
            $("#archive-confirm").on("click", function () {
                // Submit exactly the original form, including its id and CSRF token.
                HTMLFormElement.prototype.submit.call(form);
            });
        }
    });
})(jQuery);
